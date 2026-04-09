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

package com.google.android.as.oss.networkusage.db;

import static com.google.android.as.oss.attestation.PccAttestationMeasurementClient.ATTESTATION_FEATURE_NAME;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;
import arcs.core.data.proto.PolicyProto;
import com.google.android.as.oss.networkusage.api.proto.AttestationConnectionKey;
import com.google.android.as.oss.networkusage.api.proto.ConnectionKey;
import com.google.android.as.oss.networkusage.api.proto.FeedbackConnectionKey;
import com.google.android.as.oss.networkusage.api.proto.FlConnectionKey;
import com.google.android.as.oss.networkusage.api.proto.HttpConnectionKey;
import com.google.android.as.oss.networkusage.api.proto.PirConnectionKey;
import com.google.android.as.oss.networkusage.api.proto.SurveyConnectionKey;
import com.google.android.as.oss.networkusage.db.ConnectionDetails.ConnectionType;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.flogger.GoogleLogger;
import io.grpc.binder.PeerUid;
import io.grpc.binder.PeerUids;

/** Utility class for creating NetworkUsageLog entities. */
public final class NetworkUsageLogUtils {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  /**
   * Returns the calling package name for logging purposes.
   *
   * <p>This method is intended for on-device gRPC server implementations to get the calling package
   * for logging purposes.
   *
   * <p>Note: The gRPC service using this method must install the
   * `PeerUids.newPeerIdentifyingServerInterceptor()` for this method to work correctly.
   */
  // PeerUid is used here only for logging. It is NOT used for any security checks.
  public static String getCallingPackageNameForLoggingPurposes(Context context) {
    String callingPackageName = "";
    PeerUid remotePeer = PeerUids.REMOTE_PEER.get();
    if (remotePeer == null
        || PeerUids.getInsecurePackagesForUid(context.getPackageManager(), remotePeer) == null) {
      logger.atWarning().log("Calling package not set in PeerUid.");
    } else {
      callingPackageName =
          PeerUids.getInsecurePackagesForUid(context.getPackageManager(), remotePeer)[0];
      logger.atFine().log("Received call from package %s", callingPackageName);
    }
    return callingPackageName;
  }

  public static ConnectionDetails createHttpConnectionDetails(String urlRegex, String packageName) {
    checkArgument(!Strings.isNullOrEmpty(urlRegex));
    return getDefaultConnectionDetailsBuilder(ConnectionType.HTTP, packageName)
        .setConnectionKey(
            ConnectionKey.newBuilder()
                .setHttpConnectionKey(HttpConnectionKey.newBuilder().setUrlRegex(urlRegex).build())
                .build())
        .build();
  }

  public static ConnectionDetails createAttestationConnectionDetails(
      String featureName, String packageName) {
    checkArgument(!Strings.isNullOrEmpty(featureName));
    checkArgument(!Strings.isNullOrEmpty(packageName));
    return getDefaultConnectionDetailsBuilder(ConnectionType.ATTESTATION_REQUEST, packageName)
        .setConnectionKey(
            ConnectionKey.newBuilder()
                .setAttestationConnectionKey(
                    AttestationConnectionKey.newBuilder().setFeatureName(featureName).build())
                .build())
        .build();
  }

  public static ConnectionDetails createPirConnectionDetails(String urlRegex, String packageName) {
    checkArgument(!Strings.isNullOrEmpty(urlRegex));
    return getDefaultConnectionDetailsBuilder(ConnectionType.PIR, packageName)
        .setConnectionKey(
            ConnectionKey.newBuilder()
                .setPirConnectionKey(PirConnectionKey.newBuilder().setUrlRegex(urlRegex).build())
                .build())
        .build();
  }

  public static ConnectionDetails createSurveyConnectionDetails(
      String urlRegex, String packageName) {
    checkArgument(!Strings.isNullOrEmpty(urlRegex));
    return getDefaultConnectionDetailsBuilder(ConnectionType.SURVEY_REQUEST, packageName)
        .setConnectionKey(
            ConnectionKey.newBuilder()
                .setSurveyConnectionKey(
                    SurveyConnectionKey.newBuilder().setUrlRegex(urlRegex).build())
                .build())
        .build();
  }

  public static ConnectionDetails createFeedbackConnectionDetails(
      String featureName, String packageName) {
    checkArgument(!Strings.isNullOrEmpty(featureName));
    return getDefaultConnectionDetailsBuilder(ConnectionType.FEEDBACK_REQUEST, packageName)
        .setConnectionKey(
            ConnectionKey.newBuilder()
                .setFeedbackConnectionKey(
                    FeedbackConnectionKey.newBuilder().setFeatureName(featureName).build())
                .build())
        .build();
  }

  public static ConnectionDetails createPdConnectionDetails(String clientId, String packageName) {
    checkArgument(!Strings.isNullOrEmpty(clientId));
    return getDefaultConnectionDetailsBuilder(ConnectionType.PD, packageName)
        .setConnectionKey(
            ConnectionKey.newBuilder()
                .setPdConnectionKey(
                    com.google.android.as.oss.networkusage.api.proto.PdConnectionKey.newBuilder()
                        .setClientId(clientId)
                        .build())
                .build())
        .build();
  }

  public static ConnectionDetails createFcCheckInConnectionDetails() {
    return getDefaultConnectionDetailsBuilder(ConnectionType.FC_CHECK_IN).build();
  }

  public static ConnectionDetails createFcTrainingResultUploadConnectionDetails() {
    return getDefaultConnectionDetailsBuilder(ConnectionType.FC_TRAINING_RESULT_UPLOAD).build();
  }

  public static ConnectionDetails createFcTrainingStartQueryConnectionDetails(
      String featureName, String packageName) {
    checkArgument(!Strings.isNullOrEmpty(featureName));
    return getDefaultConnectionDetailsBuilder(ConnectionType.FC_TRAINING_START_QUERY, packageName)
        .setConnectionKey(
            ConnectionKey.newBuilder()
                .setFlConnectionKey(
                    FlConnectionKey.newBuilder().setFeatureName(featureName).build())
                .build())
        .build();
  }

  public static NetworkUsageEntity createHttpNetworkUsageEntity(
      ConnectionDetails connectionDetails, Status status, long downloadSize, String url) {
    checkArgument(connectionDetails.connectionKey().hasHttpConnectionKey());
    checkArgument(
        url.matches(connectionDetails.connectionKey().getHttpConnectionKey().getUrlRegex()));
    checkArgument(connectionDetails.type() == ConnectionType.HTTP);
    return getNetworkUsageEntityForUrl(connectionDetails, status, downloadSize, url);
  }

  public static NetworkUsageEntity createAttestationNetworkUsageEntity(
      String packageName, long downloadSize) {
    checkArgument(!Strings.isNullOrEmpty(packageName));
    ConnectionDetails connectionDetails =
        createAttestationConnectionDetails(ATTESTATION_FEATURE_NAME, packageName);

    return getNetworkUsageEntityBuilder(connectionDetails, Status.SUCCEEDED, downloadSize).build();
  }

  public static NetworkUsageEntity createPirNetworkUsageEntity(
      ConnectionDetails connectionDetails, Status status, long downloadSize, String url) {
    checkArgument(connectionDetails.type() == ConnectionType.PIR);
    checkArgument(connectionDetails.connectionKey().hasPirConnectionKey());
    checkArgument(
        url.matches(connectionDetails.connectionKey().getPirConnectionKey().getUrlRegex()));
    return getNetworkUsageEntityForUrl(connectionDetails, status, downloadSize, url);
  }

  public static NetworkUsageEntity createFcCheckInNetworkUsageEntity(
      long downloadSize, long uploadSize) {
    return NetworkUsageEntity.defaultBuilder()
        .setConnectionDetails(createFcCheckInConnectionDetails())
        .setStatus(Status.SUCCEEDED)
        .setDownloadSize(downloadSize)
        .setUploadSize(uploadSize)
        .build();
  }

  public static NetworkUsageEntity createFcTrainingResultNetworkUsageEntity(
      long runId, long downloadSize, long uploadSize) {
    return NetworkUsageEntity.defaultBuilder()
        .setConnectionDetails(createFcTrainingResultUploadConnectionDetails())
        .setStatus(Status.SUCCEEDED)
        .setFcRunId(runId)
        .setDownloadSize(downloadSize)
        .setUploadSize(uploadSize)
        .build();
  }

  public static NetworkUsageEntity createFcTrainingStartQueryNetworkUsageEntity(
      ConnectionDetails connectionDetails, long runId, Optional<PolicyProto> policyProtoOptional) {
    checkArgument(connectionDetails.type() == ConnectionType.FC_TRAINING_START_QUERY);
    checkArgument(connectionDetails.connectionKey().hasFlConnectionKey());
    NetworkUsageEntity.Builder builder =
        getNetworkUsageEntityBuilder(connectionDetails, Status.SUCCEEDED, /* downloadSize= */ 0)
            .setFcRunId(runId);
    if (policyProtoOptional.isPresent()) {
      PolicyProto policyProto = policyProtoOptional.get();
      checkArgument(policyProto.isInitialized());
      builder.setPolicyProto(policyProto);
    }
    return builder.build();
  }

  public static NetworkUsageEntity createPdNetworkUsageEntry(
      ConnectionDetails connectionDetails, Status status, long downloadSize, String clientId) {
    checkArgument(connectionDetails.type() == ConnectionType.PD);
    checkArgument(connectionDetails.connectionKey().hasPdConnectionKey());
    checkArgument(
        clientId.matches(connectionDetails.connectionKey().getPdConnectionKey().getClientId()));
    return getNetworkUsageEntityBuilder(connectionDetails, status, downloadSize).build();
  }

  public static NetworkUsageEntity createSurveyNetworkUsageEntity(
      ConnectionDetails connectionDetails, Status status, long downloadSize, String url) {
    checkArgument(connectionDetails.type() == ConnectionType.SURVEY_REQUEST);
    checkArgument(connectionDetails.connectionKey().hasSurveyConnectionKey());
    checkArgument(
        url.matches(connectionDetails.connectionKey().getSurveyConnectionKey().getUrlRegex()));
    return getNetworkUsageEntityForUrl(connectionDetails, status, downloadSize, url);
  }

  public static NetworkUsageEntity createFeedbackNetworkUsageEntity(
      ConnectionDetails connectionDetails, Status status, long downloadSize, String featureName) {
    checkArgument(connectionDetails.type() == ConnectionType.FEEDBACK_REQUEST);
    checkArgument(connectionDetails.connectionKey().hasFeedbackConnectionKey());
    checkArgument(
        featureName.matches(
            connectionDetails.connectionKey().getFeedbackConnectionKey().getFeatureName()));
    return getNetworkUsageEntityForUrl(connectionDetails, status, downloadSize, featureName);
  }

  private static NetworkUsageEntity getNetworkUsageEntityForUrl(
      ConnectionDetails connectionDetails, Status status, long downloadSize, String url) {
    checkArgument(!Strings.isNullOrEmpty(url));
    return getNetworkUsageEntityBuilder(connectionDetails, status, downloadSize)
        .setUrl(url)
        .build();
  }

  private static NetworkUsageEntity.Builder getNetworkUsageEntityBuilder(
      ConnectionDetails connectionDetails, Status status, long downloadSize) {
    checkNotNull(connectionDetails);
    checkNotNull(status);
    checkArgument(downloadSize >= 0);

    return NetworkUsageEntity.defaultBuilder()
        .setConnectionDetails(connectionDetails)
        .setStatus(status)
        .setDownloadSize(downloadSize);
  }

  private static ConnectionDetails.Builder getDefaultConnectionDetailsBuilder(
      ConnectionType type, String packageName) {
    checkArgument(!Strings.isNullOrEmpty(packageName));
    return getDefaultConnectionDetailsBuilder(type).setPackageName(packageName);
  }

  private static ConnectionDetails.Builder getDefaultConnectionDetailsBuilder(ConnectionType type) {
    return ConnectionDetails.builder()
        .setType(type)
        .setPackageName("unknown")
        .setConnectionKey(ConnectionKey.getDefaultInstance());
  }

  private NetworkUsageLogUtils() {}
}
