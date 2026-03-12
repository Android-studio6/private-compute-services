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

package com.google.android.`as`.oss.actionlauncher

import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.android.`as`.oss.actionlauncher.config.ActionLauncherConfig
import com.google.android.`as`.oss.availability.api.agenticintegration.AgenticIntegrationServiceGrpcKt
import com.google.android.`as`.oss.availability.api.agenticintegration.ClientInfo
import com.google.android.`as`.oss.availability.api.agenticintegration.ErrorCode
import com.google.android.`as`.oss.availability.api.agenticintegration.account
import com.google.android.`as`.oss.availability.api.agenticintegration.clientInfo
import com.google.android.`as`.oss.availability.api.agenticintegration.launchRequest
import com.google.android.`as`.oss.availability.api.agenticintegration.textQuery
import com.google.android.`as`.oss.common.Executors.GENERAL_SINGLE_THREAD_EXECUTOR
import com.google.android.`as`.oss.common.config.ConfigReader
import com.google.android.`as`.oss.common.time.TimeSource
import com.google.common.flogger.GoogleLogger
import com.google.common.flogger.android.AndroidLogTag
import com.google.protobuf.util.Durations
import dagger.hilt.android.AndroidEntryPoint
import io.grpc.Metadata
import io.grpc.StatusException
import io.grpc.protobuf.lite.ProtoLiteUtils
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint(AppCompatActivity::class)
class ActionLauncherActivity : Hilt_ActionLauncherActivity() {
  private val dispatcher = GENERAL_SINGLE_THREAD_EXECUTOR.asCoroutineDispatcher()
  private val scope = CoroutineScope(dispatcher)
  @Inject lateinit var configReader: ConfigReader<ActionLauncherConfig>
  @Inject lateinit var stub: AgenticIntegrationServiceGrpcKt.AgenticIntegrationServiceCoroutineStub
  @Inject lateinit var timeSource: TimeSource

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    logger.atInfo().log("ActionLauncherActivity#onCreate")
    if (!isSdkSupported() || !configReader.config.isActionLauncherEnabled) {
      logger.atInfo().log("ActionLauncherActivity is not supported or disabled. Finishing.")
      finish()
      return
    }
    val geminiTextQuery = intent.getStringExtra(Constants.GEMINI_TEXT_QUERY_KEY)
    val shouldAutoSubmit = intent.getBooleanExtra(Constants.SHOULD_AUTO_SUBMIT_KEY, true)
    val shouldAutomate = intent.getBooleanExtra(Constants.SHOULD_AUTOMATE_KEY, false)
    scope.launch {
      if (geminiTextQuery != null) {
        val toastMessageOnFailure = intent.getStringExtra(Constants.TOAST_MESSAGE_ON_FAILURE_KEY)
        val success =
          launchAgenticIntegrationServiceWithTextQuery(
            geminiTextQuery,
            shouldAutoSubmit,
            shouldAutomate,
            toastMessageOnFailure,
          )
        logger
          .atInfo()
          .log("ActionLauncherActivity#launchAgenticIntegrationServiceWithTextQuery: %s", success)
      }
      finish()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    logger.atInfo().log("ActionLauncherActivity#onDestroy")
    scope.cancel()
  }

  private suspend fun launchAgenticIntegrationServiceWithTextQuery(
    inputText: String,
    autoSubmit: Boolean,
    automate: Boolean,
    toastMessageOnFailure: String? = null,
  ): Boolean {
    val requestReceivedAt = Durations.fromMillis(timeSource.now().toEpochMilli())
    try {
      val request = launchRequest {
        clientInfo = getClientInfo()
        textQuery = textQuery {
          query = inputText
          shouldAutoSubmit = autoSubmit
          shouldAutomate = automate
        }
        requestStartTime = requestReceivedAt
      }
      val unused = stub.launch(request)
      return true
    } catch (e: Exception) {
      logger.atSevere().withCause(e).log("Error launching Agentic Integration Service")

      val errorCode = getAgenticIntegrationServiceLaunchErrorCode(e)
      logger.atSevere().log("Error code: %s", errorCode)
      toastMessageOnFailure?.let { if (it.isNotEmpty()) showToast(it) }
      return false
    }
  }

  private fun getClientInfo(): ClientInfo {
    val callingAppName = intent.getStringExtra(Constants.CLIENT_APP_NAME_KEY)
    val callingAppVersion = intent.getStringExtra(Constants.CLIENT_APP_VERSION_KEY)
    val callingPackageName = intent.getStringExtra(Constants.CLIENT_APP_PACKAGE_NAME_KEY)
    val accountName = intent.getStringExtra(Constants.ACCOUNT_NAME_KEY)
    return clientInfo {
      appName = callingAppName ?: ""
      appVersion = callingAppVersion ?: ""
      appPackageName = callingPackageName ?: ""
      if (!accountName.isNullOrEmpty()) {
        clientAccount += account { email = accountName }
      }
    }
  }

  private fun isSdkSupported(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
  }

  private fun getAgenticIntegrationServiceLaunchErrorCode(e: Exception): ErrorCode.Code? {
    if (e !is StatusException) {
      return null
    }
    val trailers = e.trailers
    if (trailers != null) {
      val error = trailers.get(AGENTIC_INTEGRATION_SERVICE_LAUNCH_ERROR_KEY)
      if (error != null && error.hasCode()) {
        return error.code
      }
    }
    return null
  }

  private fun showToast(message: String) {
    this@ActionLauncherActivity.runOnUiThread {
      Toast.makeText(this@ActionLauncherActivity, message, Toast.LENGTH_LONG).show()
    }
  }

  companion object {
    @AndroidLogTag("ActionLauncherActivity") private val logger = GoogleLogger.forEnclosingClass()
    private val AGENTIC_INTEGRATION_SERVICE_LAUNCH_ERROR_KEY: Metadata.Key<ErrorCode> =
      Metadata.Key.of(
        "error-bin",
        ProtoLiteUtils.metadataMarshaller(ErrorCode.getDefaultInstance()),
      )
  }
}
