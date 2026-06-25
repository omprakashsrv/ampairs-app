cmake_minimum_required(VERSION 3.18)

project(whisper_jni C CXX)

# whisper.cpp / ggml v1.9.x require C++17.
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# The whisper.cpp release tag is injected from the Gradle build (single source of truth: the
# `whisper-cpp-xcframework` version in gradle/libs.versions.toml — the same whisper.cpp version the
# iOS cinterop uses). Falls back to a known-good tag for standalone CMake runs.
if (NOT DEFINED WHISPER_CPP_TAG)
    set(WHISPER_CPP_TAG "v1.9.1")
endif ()
message(STATUS "Building whisper.cpp ${WHISPER_CPP_TAG} via FetchContent")

# CRITICAL: always optimize the native lib, even in the app's *debug* build type. AGP passes
# -DCMAKE_BUILD_TYPE=Debug for debug installs, whose default flags are -O0 — and ggml/whisper at -O0
# is 10-30x slower, making on-device inference time out (a ~2s clip can exceed 45s). Force -O3 for the
# Debug config so a normal `assembleDebug` is usable; release builds are already -O3. (-DNDEBUG also
# drops ggml's asserts, matching release.)
set(CMAKE_C_FLAGS_DEBUG   "-O3 -DNDEBUG" CACHE STRING "" FORCE)
set(CMAKE_CXX_FLAGS_DEBUG "-O3 -DNDEBUG" CACHE STRING "" FORCE)

# Build a single self-contained libwhisper_jni.so: whisper + ggml are linked statically into it, so
# there are no transitive .so load-order concerns at runtime. Trim everything we don't ship on mobile.
set(BUILD_SHARED_LIBS      OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER   OFF CACHE BOOL "" FORCE)
# OpenMP is not available in the Android NDK by default; ggml falls back to its own threadpool.
set(GGML_OPENMP            OFF CACHE BOOL "" FORCE)

include(FetchContent)
FetchContent_Declare(
        whisper_cpp
        GIT_REPOSITORY https://github.com/ggml-org/whisper.cpp.git
        GIT_TAG ${WHISPER_CPP_TAG}
        GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(whisper_cpp)

find_library(LOG_LIB log)

add_library(whisper_jni SHARED jni.c)
# jni.c is C, but it links the C++ whisper/ggml static libs — force the C++ linker driver so the
# C++ runtime is pulled in (keeping the C source means the JNI symbol names are NOT name-mangled).
set_target_properties(whisper_jni PROPERTIES LINKER_LANGUAGE CXX)
target_link_libraries(whisper_jni PRIVATE whisper ${LOG_LIB} android)

# 16 KB page-size support (required by Google Play for apps targeting Android 15+ from Nov 1, 2025).
# NDK r27 supports 16 KB alignment but does not enable it by default (only r28+ does), so its LOAD
# segments land on 4 KB boundaries and Play rejects the APK. Force the max ELF page size to 16 KB so
# the segments are aligned and the lib loads on both 4 KB and 16 KB devices.
target_link_options(whisper_jni PRIVATE "-Wl,-z,max-page-size=16384")
