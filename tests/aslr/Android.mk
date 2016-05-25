# Copyright (C) 2016 The Android Open Source Project
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

LOCAL_PATH:= $(call my-dir)

test_executable := CtsAslrMallocTestCases
list_executable := $(test_executable)_list

include $(CLEAR_VARS)
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk

LOCAL_MODULE := $(list_executable)
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
    src/AslrMallocTest.cpp

LOCAL_CFLAGS := \
    -DBUILD_ONLY \

LOCAL_SHARED_LIBRARIES := \
    liblog

include $(BUILD_HOST_NATIVE_TEST)

include $(CLEAR_VARS)
LOCAL_MODULE:= $(test_executable)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_DATA)/nativetest
LOCAL_MULTILIB := both
LOCAL_MODULE_STEM_32 := $(LOCAL_MODULE)32
LOCAL_MODULE_STEM_64 := $(LOCAL_MODULE)64

LOCAL_C_INCLUDES := \
    external/gtest/include

LOCAL_SRC_FILES := \
    src/AslrMallocTest.cpp

LOCAL_SHARED_LIBRARIES := \
    libbase \
    libutils \
    liblog \

LOCAL_STATIC_LIBRARIES := \
    libgtest

# Tag this module as a cts test artifact
LOCAL_COMPATIBILITY_SUITE := cts

LOCAL_CTS_GTEST_LIST_EXECUTABLE := $(ALL_MODULES.$(list_executable).INSTALLED)

include $(BUILD_CTS_EXECUTABLE)