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

/** Wrapper for [android.provider.Settings.Secure] to allow testing failure scenarios. */
interface SecureSettings {
  /** @see android.provider.Settings.Secure.getInt */
  suspend fun getInt(name: String): Int

  /** @see android.provider.Settings.Secure.putInt */
  suspend fun putInt(name: String, value: Int): Boolean

  /** @see android.provider.Settings.Secure.getLong */
  suspend fun getLong(name: String): Long

  /** @see android.provider.Settings.Secure.putLong */
  suspend fun putLong(name: String, value: Long): Boolean

  /** @see android.provider.Settings.Secure.getFloat */
  suspend fun getFloat(name: String): Float

  /** @see android.provider.Settings.Secure.putFloat */
  suspend fun putFloat(name: String, value: Float): Boolean

  /** @see android.provider.Settings.Secure.getString */
  suspend fun getString(name: String): String?

  /** @see android.provider.Settings.Secure.putString */
  suspend fun putString(name: String, value: String?): Boolean
}
