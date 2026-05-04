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

import android.provider.Settings
import com.google.android.`as`.oss.features.optin.api.proto.FeatureOptInServiceGrpcKt.FeatureOptInServiceCoroutineImplBase
import com.google.android.`as`.oss.features.optin.api.proto.OptInSetting
import com.google.android.`as`.oss.features.optin.api.proto.SettingValue
import com.google.android.`as`.oss.features.optin.api.proto.WriteOptInStatusRequest
import com.google.android.`as`.oss.features.optin.api.proto.WriteOptInStatusResponse
import com.google.android.`as`.oss.features.optin.api.proto.WriteResult
import com.google.android.`as`.oss.features.optin.api.proto.settingValue
import com.google.android.`as`.oss.features.optin.api.proto.writeOptInStatusResponse
import com.google.android.`as`.oss.features.optin.api.proto.writeResult
import com.google.common.flogger.GoogleLogger
import io.grpc.Status
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/** Implementation of the FeatureOptInService. */
internal class FeatureOptInServiceImpl
@Inject
internal constructor(private val secureSettings: SecureSettings) :
  FeatureOptInServiceCoroutineImplBase() {

  override suspend fun writeOptInStatus(
    request: WriteOptInStatusRequest
  ): WriteOptInStatusResponse =
    try {
      writeOptInStatusInternal(request)
    } catch (e: Exception) {
      if (e is CancellationException) {
        throw e
      }
      logger.atSevere().withCause(e).log("Error in writeOptInStatus")
      throw Status.UNKNOWN.withDescription("Internal error in FeatureOptInService: ${e.message}")
        .withCause(e)
        .asException()
    }

  private suspend fun writeOptInStatusInternal(
    request: WriteOptInStatusRequest
  ): WriteOptInStatusResponse {
    val results =
      buildList(request.settingsToWriteList.size) {
        for (settingToWrite in request.settingsToWriteList) {
          add(writeSingleSetting(settingToWrite))
        }
      }
    return writeOptInStatusResponse { writeResults += results }
  }

  private suspend fun writeSingleSetting(settingToWrite: OptInSetting): WriteResult {
    val settingName = settingToWrite.settingName
    var wasNew = false

    return writeResult {
      this.setting = settingToWrite
      try {
        val writeSuccess =
          when (settingToWrite.value.valueCase) {
            SettingValue.ValueCase.INT_VALUE -> {
              try {
                val prev = secureSettings.getInt(settingName)
                this.previousValue = settingValue { intValue = prev }
              } catch (e: Settings.SettingNotFoundException) {
                wasNew = true
              }
              secureSettings.putInt(settingName, settingToWrite.value.intValue)
            }
            SettingValue.ValueCase.LONG_VALUE -> {
              try {
                val prev = secureSettings.getLong(settingName)
                this.previousValue = settingValue { longValue = prev }
              } catch (e: Settings.SettingNotFoundException) {
                wasNew = true
              }
              secureSettings.putLong(settingName, settingToWrite.value.longValue)
            }
            SettingValue.ValueCase.FLOAT_VALUE -> {
              try {
                val prev = secureSettings.getFloat(settingName)
                this.previousValue = settingValue { floatValue = prev }
              } catch (e: Settings.SettingNotFoundException) {
                wasNew = true
              }
              secureSettings.putFloat(settingName, settingToWrite.value.floatValue)
            }
            SettingValue.ValueCase.STRING_VALUE -> {
              val prevStr = secureSettings.getString(settingName)
              if (prevStr == null) {
                wasNew = true
              } else {
                this.previousValue = settingValue { stringValue = prevStr }
              }
              secureSettings.putString(settingName, settingToWrite.value.stringValue)
            }
            else -> {
              logger.atWarning().log("Unknown or missing setting value type for '%s'", settingName)
              false
            }
          }

        this.status = if (writeSuccess) WriteResult.Status.SUCCESS else WriteResult.Status.FAILURE
        this.wasNewSetting = wasNew
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        logger.atSevere().withCause(e).log("Error writing setting %s", settingName)
        this.status = WriteResult.Status.FAILURE
      }
      this.wasNewSetting = wasNew
    }
  }

  companion object {
    private val logger = GoogleLogger.forEnclosingClass()
  }
}
