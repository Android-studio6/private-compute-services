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

package com.google.android.as.oss.pd.channel.impl;

import com.google.android.as.oss.pd.api.proto.BlobConstraints.Client;
import com.google.android.as.oss.pd.channel.ChannelProvider;
import com.google.android.as.oss.pd.channel.ChannelProvider.HostConfig;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import io.grpc.ManagedChannelBuilder;

/**
 * A simple {@link com.google.android.as.oss.pd.channel.ChannelProvider} that caches the returned
 * channels by host name.
 */
class ChannelProviderImpl implements ChannelProvider {

  private static final int SERVER_PORT = 443;
  private static final int MAX_RESPONSE_SIZE_IN_BYTES = 32 * 1024 * 1024; // Max download size: 32MB

  private final LoadingCache<Client, ChannelSpec> channelCache;

  ChannelProviderImpl(ImmutableMap<Client, HostConfig> hostConfigs, HostConfig defaultHostConfig) {
    this.channelCache =
        CacheBuilder.newBuilder()
            .build(
                CacheLoader.from(
                    client -> buildChannelSpecFor(hostConfigs, client, defaultHostConfig)));
  }

  @Override
  public ChannelSpec getChannel(Client client) {
    // Using getUnchecked since no checked exception is thrown from the loading function
    return channelCache.getUnchecked(client);
  }

  // Implemented as a static method instead of an instance method to avoid "under-initialization"
  // errors by the static analysis tools.
  private static ChannelSpec buildChannelSpecFor(
      ImmutableMap<Client, HostConfig> hostConfigs, Client client, HostConfig defaultHostConfig) {
    HostConfig hostConfig = hostConfigs.getOrDefault(client, defaultHostConfig);
    return new ChannelSpec(
        ManagedChannelBuilder.forAddress(hostConfig.hostName(), SERVER_PORT)
            .maxInboundMessageSize(MAX_RESPONSE_SIZE_IN_BYTES)
            .build(),
        hostConfig.apiKeyOverride());
  }
}
