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

package com.google.android.`as`.oss.features.optin.service

import android.content.Context
import android.provider.Settings
import com.google.android.`as`.oss.common.CoroutineQualifiers.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Implementation of [SecureSettings] that calls [Settings.Secure]. */
class SecureSettingsImpl
@Inject
internal constructor(
  @ApplicationContext private val appContext: Context,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SecureSettings {
  override suspend fun getInt(name: String): Int =
    withContext(ioDispatcher) { Settings.Secure.getInt(appContext.contentResolver, name) }

  override suspend fun putInt(name: String, value: Int): Boolean =
    withContext(ioDispatcher) { Settings.Secure.putInt(appContext.contentResolver, name, value) }

  override suspend fun getLong(name: String): Long =
    withContext(ioDispatcher) { Settings.Secure.getLong(appContext.contentResolver, name) }

  override suspend fun putLong(name: String, value: Long): Boolean =
    withContext(ioDispatcher) { Settings.Secure.putLong(appContext.contentResolver, name, value) }

  override suspend fun getFloat(name: String): Float =
    withContext(ioDispatcher) { Settings.Secure.getFloat(appContext.contentResolver, name) }

  override suspend fun putFloat(name: String, value: Float): Boolean =
    withContext(ioDispatcher) { Settings.Secure.putFloat(appContext.contentResolver, name, value) }

  override suspend fun getString(name: String): String? =
    withContext(ioDispatcher) { Settings.Secure.getString(appContext.contentResolver, name) }

  override suspend fun putString(name: String, value: String?): Boolean =
    withContext(ioDispatcher) { Settings.Secure.putString(appContext.contentResolver, name, value) }
}
