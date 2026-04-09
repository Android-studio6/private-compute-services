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

package com.google.android.libraries.pcc.policies.federatedcompute

val PlatformLoggingPolicy_ConfidentialAgg =
  flavoredPolicies(
    name = "PlatformLoggingPolicy_ConfidentialAgg",
    policyType = MonitorOrImproveUserExperienceWithFederatedCompute,
  ) {
    description =
      """
      To enable querying of Android Platform logs in a privacy-preserving way, using confidential aggregation.

      ALLOWED EGRESSES: FederatedCompute.
      ALLOWED USAGES: Federated analytics.
      """
        .trimIndent()
    flavors(Flavor.PCS_RELEASE) { confidentialAgg() }
    presubmitReviewRequired(OwnersApprovalOnly)
  }
