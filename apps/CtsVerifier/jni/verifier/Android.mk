#
# Copyright (C) 2010 The Android Open Source Project
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
#

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := libctsverifier_jni

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
		CtsVerifierJniOnLoad.cpp \
		com_android_cts_verifier_camera_StatsImage.cpp \
		com_android_cts_verifier_os_FileUtils.cpp

LOCAL_C_INCLUDES := $(JNI_H_INCLUDE)

LOCAL_CXX_STL := libc++_static

LOCAL_SHARED_LIBRARIES := liblog \
		libnativehelper_compat_libc++

include $(BUILD_SHARED_LIBRARY)

