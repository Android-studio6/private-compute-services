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

package com.google.android.`as`.oss.privateinference.transport

/**
 * A flag to control the IP Relay fallback behavior for `CRONET_MAINLINE_IP_RELAY` mode.
 *
 * When set to `FORCE_STATIC`, the system will skip trying mainline `HttpEngineNativeProvider` and
 * immediately use the static Cronet engine for IP Relay. This provides an escape hatch if
 * unexpected differences are observed between `HttpEngineNativeProvider` and
 * `NativeCronetProvider`.
 */
interface IpRelayFallbackFlag {
  enum class Mode {
    /** Use the default behavior: try mainline first, fallback to static on error. */
    DEFAULT,

    /** Force immediate use of static Cronet, skipping mainline attempt. */
    FORCE_STATIC,
  }

  val mode: Mode
}
