# Copyright 2025 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

load("//third_party/protobuf/bazel:java_lite_proto_library.bzl", "java_lite_proto_library")
load("//third_party/protobuf/bazel:proto_library.bzl", "proto_library")
load("//third_party/protobuf/build_defs:kt_jvm_proto_library.bzl", "kt_jvm_lite_proto_library")
load("//tools/build_defs/kotlin:rules.bzl", "kt_jvm_grpc_library")

package(default_visibility = [
    "//javatests/com/google/android/apps/miphone/pcs/features/optin/service:__subpackages__",
    "//src/com/google/android/as/oss/features/optin/client:__subpackages__",
    "//visibility:public",
])

proto_library(
    name = "opt_in_service_proto",
    srcs = ["opt_in_service.proto"],
    has_services = True,
    deps = [""],
)

java_lite_proto_library(
    name = "opt_in_service_java_proto_lite",
    deps = [":opt_in_service_proto"],
)

kt_jvm_lite_proto_library(
    name = "opt_in_service_kt_proto_lite",
    deps = [":opt_in_service_proto"],
)

kt_jvm_grpc_library(
    name = "opt_in_service_kt_grpc",
    srcs = [":opt_in_service_proto"],
    flavor = "lite",
    deps = [":opt_in_service_kt_proto_lite"],
)
