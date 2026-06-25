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
