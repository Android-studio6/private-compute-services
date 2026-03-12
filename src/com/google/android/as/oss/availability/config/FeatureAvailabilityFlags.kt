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

package com.google.android.`as`.oss.availability.config

import com.google.android.`as`.oss.common.config.FlagManager.BooleanFlag
import com.google.android.`as`.oss.common.config.FlagManager.StringFlag

object FeatureAvailabilityFlags {
  const val PREFIX = "FeatureAvailability__"
  const val DEFAULT_ENABLE = false
  val ENABLE_FEATURE_AVAILABILITY =
    BooleanFlag.create("${PREFIX}enable_feature_availability", DEFAULT_ENABLE)

  const val DEFAULT_AGENTIC_INTEGRATION_PACKAGE_NAME = "com.google.android.googlequicksearchbox"
  val AGENTIC_INTEGRATION_PACKAGE_NAME =
    StringFlag.create(
      "${PREFIX}agentic_integration_package_name",
      DEFAULT_AGENTIC_INTEGRATION_PACKAGE_NAME,
    )

  const val DEFAULT_AGENTIC_INTEGRATION_GRPC_SERVICE_CLASS =
    "com.google.android.apps.search.assistant.surfaces.voice.robin.robinkit.service.RobinKitGrpcService"
  val AGENTIC_INTEGRATION_GRPC_SERVICE_CLASS =
    StringFlag.create(
      "${PREFIX}agentic_integration_grpc_service_class",
      DEFAULT_AGENTIC_INTEGRATION_GRPC_SERVICE_CLASS,
    )
}
