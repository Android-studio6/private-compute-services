/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.`as`.oss.privateinference.library.bsa.token.cache.db

import com.google.android.`as`.oss.common.time.TimeSource
import com.google.android.`as`.oss.privateinference.library.bsa.token.ArateaToken
import com.google.android.`as`.oss.privateinference.library.bsa.token.ArateaTokenParams
import com.google.android.`as`.oss.privateinference.library.bsa.token.ArateaTokenWithoutChallenge
import com.google.android.`as`.oss.privateinference.library.bsa.token.BsaToken
import com.google.android.`as`.oss.privateinference.library.bsa.token.BsaTokenParams
import com.google.android.`as`.oss.privateinference.library.bsa.token.CacheableArateaTokenParams
import com.google.android.`as`.oss.privateinference.library.bsa.token.ProxyToken
import com.google.android.`as`.oss.privateinference.library.bsa.token.ProxyTokenParams
import com.google.android.`as`.oss.privateinference.library.bsa.token.cache.TokenPool
import com.google.android.`as`.oss.privateinference.library.bsa.token.cache.TokenPoolSource
import com.google.android.`as`.oss.privateinference.library.bsa.token.crypto.BsaTokenCipher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Implementation of [TokenPool] which manages its pools of tokens using a [BsaTokenDatabase].
 *
 * A single pool is maintained for each unique instance of [BsaTokenParams] either passed as
 * [refreshParams] or to the [draw] method.
 *
 * If [enableAsyncTokenCacheRefill] is true, then token cache refills will be performed
 * asynchronously and will not block the calling thread. However, any pending tokens needed while
 * drawing from the pool on-demand will be fetched synchronously and will block the calling thread.
 *
 * @param refreshParams List of [BsaTokenParams] to use when making requests to a [TokenPoolSource]
 *   when pre-filling the pool to the [preferredPoolSize].
 * @param minPoolSize The minimum size a pool is allowed to fall-to before the fallback
 *   [TokenPoolSource] is used to re-populate a pool during a [draw] call.
 * @param preferredPoolSize The size a pool is filled-to when a [TokenPoolSource] is used to re-fill
 *   it.
 * @param cipher [BsaTokenCipher] used to encrypt/decrypt tokens when storing them in the database.
 * @param daoProvider Provider for the [BsaTokenDao] used to store/retrieve tokens from the
 *   database.
 * @param timeSource [TimeSource] used to determine token expiration.
 * @param enableAsyncTokenCacheRefill If true, token cache refills will be performed asynchronously
 *   and will not block the calling thread.
 * @param coroutineScope [CoroutineScope] on which any coroutines will be run.
 */
class DatabaseTokenPool<T : BsaToken>(
  private val refreshParams: List<BsaTokenParams<T>>,
  private val minPoolSize: Int,
  private val preferredPoolSize: Int,
  private val cipher: BsaTokenCipher,
  private val daoProvider: () -> BsaTokenDao,
  private val timeSource: TimeSource,
  private val enableAsyncTokenCacheRefill: Boolean,
  private val coroutineScope: CoroutineScope,
) : TokenPool<T> {
  private val dbLock = Mutex()
  private val refillLock = Mutex()

  override suspend fun draw(
    params: BsaTokenParams<T>,
    count: Int,
    fallbackSource: TokenPoolSource<T>,
  ): List<T> = dbLock.withLock {
    val (dbTokens, poolSizePostFetch, _) = daoProvider().drawTokens(params, count, timeSource.now())
    val resultTokens = dbTokens.mapNotNull { decrypt(it) }.toMutableList()

    val resultTokensNeeded = count - resultTokens.size
    val dbTokensNeeded =
      if (poolSizePostFetch < minPoolSize) {
        preferredPoolSize - poolSizePostFetch
      } else {
        0
      }

    if (enableAsyncTokenCacheRefill) {
      // Synchronously fetch any remaining tokens needed for the result to avoid blocking until
      // the cache refill completes.
      if (resultTokensNeeded > 0) {
        resultTokens.addAll(
          fallbackSource(params, resultTokensNeeded, BsaTokenDao.tokenValidator(timeSource))
        )
      }
      // Asynchronously fetch tokens to refill the cache, if there isn't a refill already in
      // progress.
      coroutineScope.launch {
        if (dbTokensNeeded > 0 && refillLock.tryLock()) {
          try {
            val refillTokens =
              fallbackSource(params, dbTokensNeeded, BsaTokenDao.tokenValidator(timeSource))
            dbLock.withLock {
              daoProvider().insertAll(refillTokens.mapNotNull { encrypt(params, it) })
            }
          } finally {
            refillLock.unlock()
          }
        }
      }
    } else {
      val totalTokensNeeded = resultTokensNeeded + dbTokensNeeded
      if (totalTokensNeeded > 0) {
        val newTokens =
          fallbackSource(params, totalTokensNeeded, BsaTokenDao.tokenValidator(timeSource))
        resultTokens += newTokens.take(resultTokensNeeded)
        if (dbTokensNeeded > 0) {
          daoProvider()
            .insertAll(newTokens.drop(resultTokensNeeded).mapNotNull { encrypt(params, it) })
        }
      }
    }
    return@withLock resultTokens
  }

  override suspend fun clear() = dbLock.withLock { daoProvider().deleteAll(refreshParams) }

  override suspend fun refresh(refillSource: TokenPoolSource<T>) = dbLock.withLock {
    daoProvider().deleteAll(refreshParams)
    val toInsert = refreshParams.flatMap { params ->
      refillSource(params, preferredPoolSize, BsaTokenDao.tokenValidator(timeSource)).mapNotNull {
        encrypt(params, it)
      }
    }
    daoProvider().insertAll(toInsert)
  }

  private suspend fun encrypt(params: BsaTokenParams<T>, token: T): BsaTokenEntity? {
    val expiration =
      requireNotNull(token.expirationTime) {
        "BsaTokens must have expiration values to be cached in the database."
      }
    return BsaTokenEntity(
      tokenParams = params,
      encryptedTokenData = cipher.encrypt(token.bytes) ?: return null,
      expiration = expiration,
    )
  }

  @Suppress("UNCHECKED_CAST")
  private suspend fun decrypt(entity: BsaTokenEntity): T? {
    val params = entity.tokenParams
    val data = cipher.decrypt(entity.encryptedTokenData) ?: return null
    return when (params) {
      is ArateaTokenParams -> ArateaToken(data)
      is ProxyTokenParams -> ProxyToken(data, entity.expiration)
      is CacheableArateaTokenParams -> ArateaTokenWithoutChallenge(data, entity.expiration)
    }
      as T
  }
}
