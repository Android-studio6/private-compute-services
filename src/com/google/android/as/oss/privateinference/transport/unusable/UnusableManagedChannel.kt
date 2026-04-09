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

package com.google.android.`as`.oss.privateinference.transport.unusable

import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import java.util.concurrent.TimeUnit

/**
 * A [ManagedChannel] that immediately fails all RPC calls with [Status.UNAVAILABLE].
 *
 * This is used as a fail-safe fallback when required configurations (like proxy settings) are
 * missing, preventing accidental unproxied traffic without crashing the application.
 */
class UnusableManagedChannel(private val reason: String) : ManagedChannel() {

  override fun <ReqT, RespT> newCall(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    callOptions: CallOptions,
  ): ClientCall<ReqT, RespT> =
    object : ClientCall<ReqT, RespT>() {
      override fun start(listener: Listener<RespT>, headers: Metadata) {
        listener.onClose(Status.UNAVAILABLE.withDescription(reason), Metadata())
      }

      override fun request(numMessages: Int) {}

      override fun cancel(message: String?, cause: Throwable?) {}

      override fun halfClose() {}

      override fun sendMessage(message: ReqT) {}
    }

  override fun shutdown(): ManagedChannel = this

  override fun isShutdown(): Boolean = true

  override fun isTerminated(): Boolean = true

  override fun shutdownNow(): ManagedChannel = this

  override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = true

  override fun authority(): String = "unusable-channel-authority"
}
