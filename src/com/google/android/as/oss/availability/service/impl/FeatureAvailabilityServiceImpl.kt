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

package com.google.android.`as`.oss.availability.service.impl

import android.content.Context
import com.google.android.`as`.oss.availability.api.Feature
import com.google.android.`as`.oss.availability.api.FeatureAvailabilityRequest
import com.google.android.`as`.oss.availability.api.FeatureAvailabilityResponse
import com.google.android.`as`.oss.availability.api.FeatureAvailabilityServiceGrpcKt
import com.google.android.`as`.oss.availability.api.agenticintegration.AgenticIntegrationServiceGrpcKt
import com.google.android.`as`.oss.availability.api.agenticintegration.ClientInfo
import com.google.android.`as`.oss.availability.api.agenticintegration.IntegrationCapability
import com.google.android.`as`.oss.availability.api.agenticintegration.availabilityRequest
import com.google.android.`as`.oss.availability.api.agenticintegration.clientInfo
import com.google.android.`as`.oss.availability.api.agenticintegration.getCurrentAccountRequest
import com.google.android.`as`.oss.availability.api.featureAvailability
import com.google.android.`as`.oss.availability.api.featureAvailabilityResponse
import com.google.android.`as`.oss.common.time.TimeSource
import com.google.common.flogger.GoogleLogger
import com.google.common.flogger.android.AndroidLogTag
import com.google.protobuf.util.Durations
import dagger.hilt.android.qualifiers.ApplicationContext
import io.grpc.binder.PeerUids
import javax.inject.Inject

class FeatureAvailabilityServiceImpl
@Inject
internal constructor(
  private val stub: AgenticIntegrationServiceGrpcKt.AgenticIntegrationServiceCoroutineStub,
  private val timeSource: TimeSource,
  @ApplicationContext private val appContext: Context,
) : FeatureAvailabilityServiceGrpcKt.FeatureAvailabilityServiceCoroutineImplBase() {

  override suspend fun isFeatureAvailable(
    request: FeatureAvailabilityRequest
  ): FeatureAvailabilityResponse {
    logger
      .atInfo()
      .log("[FeatureAvailabilityService] isFeatureAvailable() request received: %s", request)
    val response = featureAvailabilityResponse {
      for (feature in request.featureList) {
        featureAvailability += featureAvailability {
          this.feature = feature
          isAvailable =
            when (feature) {
              Feature.FEATURE_GEMINI_TEXT_QUERY ->
                checkAgenticIntegrationServiceAvailability(
                  IntegrationCapability.TEXT_QUERY_CAPABILITY
                )
              Feature.FEATURE_GEMINI_BONOBO ->
                checkAgenticIntegrationServiceAvailability(
                  IntegrationCapability.TEXT_QUERY_WITH_AUTOMATION_CAPABILITY
                )
              else -> false
            }
          accountName =
            when (feature) {
              Feature.FEATURE_GEMINI_TEXT_QUERY -> getAgenticIntegrationServiceAccountName()
              Feature.FEATURE_GEMINI_BONOBO -> getAgenticIntegrationServiceAccountName()
              else -> ""
            }
        }
      }
    }
    logger
      .atInfo()
      .log("[FeatureAvailabilityService] isFeatureAvailable() sending response: %s", response)
    return response
  }

  private suspend fun checkAgenticIntegrationServiceAvailability(
    integrationCapability: IntegrationCapability
  ): Boolean {
    val requestReceivedAt = Durations.fromMillis(timeSource.now().toEpochMilli())
    try {
      val request = availabilityRequest {
        clientInfo = getClientInfo()
        requestedCapabilities += integrationCapability
        requestStartTime = requestReceivedAt
      }
      val availability = stub.getAvailability(request)
      val canSendQuery =
        availability.available.supportedCapabilitiesList.contains(integrationCapability)
      logger.atInfo().log("Has %s Capabilities: %s", integrationCapability.name, canSendQuery)
      return canSendQuery
    } catch (e: Exception) {
      logger
        .atSevere()
        .log(
          "Error fetching availability for Agentic Integration Service: %s",
          e.stackTraceToString(),
        )
      return false
    }
  }

  private suspend fun getAgenticIntegrationServiceAccountName(): String {
    try {
      val request = getCurrentAccountRequest { clientInfo = getClientInfo() }
      val response = stub.getCurrentAccount(request)
      val accountName =
        if (response.hasAccount()) {
          response.account.email
        } else {
          ""
        }
      logger.atInfo().log("Fetched account name from Agentic Integration Service")
      return accountName
    } catch (e: Exception) {
      logger
        .atSevere()
        .log(
          "Error fetching account name from Agentic Integration Service: %s",
          e.stackTraceToString(),
        )
      return ""
    }
  }

  private fun getClientInfo(): ClientInfo {
    val remotePeer = PeerUids.REMOTE_PEER.get()
    val callingPackageName =
      if (
        remotePeer == null ||
          PeerUids.getInsecurePackagesForUid(appContext.packageManager, remotePeer) == null
      ) {
        logger.atWarning().log("Calling package not set in PeerUid.")
        ""
      } else {
        val packageName =
          PeerUids.getInsecurePackagesForUid(appContext.packageManager, remotePeer)[0]
        logger.atFine().log("Received call from package %s", packageName)
        packageName
      }
    val callingAppInfo = appContext.packageManager.getPackageInfo(callingPackageName, 0)
    val callingAppName =
      callingAppInfo.applicationInfo?.let {
        appContext.packageManager.getApplicationLabel(it).toString()
      } ?: ""
    val callingAppVersion = callingAppInfo.versionName ?: ""
    return clientInfo {
      appName = callingAppName
      appVersion = callingAppVersion
      appPackageName = callingPackageName
    }
  }

  companion object {
    @AndroidLogTag("FeatureAvailabilityService")
    private val logger = GoogleLogger.forEnclosingClass()
  }
}
