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

package com.google.android.as.oss.fl.server;

import com.google.android.as.oss.fl.api.proto.PrivateLoggingServiceGrpc;
import com.google.android.apps.miphone.pcs.grpc.Annotations.GrpcService;
import com.google.android.apps.miphone.pcs.grpc.Annotations.GrpcServiceName;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoSet;
import io.grpc.BindableService;
import io.grpc.ServerInterceptors;
import io.grpc.binder.PeerUids;

/** Module to provide GRPC Service used for scheduling/canceling federated jobs. */
@Module
@InstallIn(SingletonComponent.class)
abstract class PrivateLoggerGrpcModule {
  // PeerUid is used here only for logging. It is NOT used for any security or permission checks.
  @Provides
  @IntoSet
  @GrpcService
  static BindableService provideBindableService(PrivateLoggerGrpcBindableService service) {
    return () ->
        ServerInterceptors.intercept(service, PeerUids.newPeerIdentifyingServerInterceptor());
  }

  @Provides
  @IntoSet
  @GrpcServiceName
  static String provideServiceName() {
    return PrivateLoggingServiceGrpc.SERVICE_NAME;
  }

  private PrivateLoggerGrpcModule() {}
}
