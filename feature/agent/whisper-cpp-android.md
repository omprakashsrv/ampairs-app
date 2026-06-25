# Whisper.cpp on Android — implementation recipe (offline STT)

> Status: **planned / not yet wired.** Android offline Whisper currently uses a LiteRT `.tflite`
> graph (`AndroidWhisperTranscriber`) that fails inside the model's own decoder
> (`gather index out of bounds`). The robust replacement is **whisper.cpp / ggml** — the same engine
> already used on **Desktop** (`whisper-jni`) and **iOS** (cinterop to `whisper.xcframework`). It takes
> raw 16 kHz PCM and does mel + decode internally, so none of the tflite shape/vocab/decoder problems
> exist.
>
> This must be built where an **Android NDK** is available (local Android Studio / a CI runner with the
> NDK). It is the **first native build in this repo** — there is no existing CMake/NDK infra.

## Why a separate module

`feature/agent` uses the `com.android.kotlin.multiplatform.library` plugin, which **cannot run
`externalNativeBuild`** (no CMake/NDK). So the native bridge lives in a dedicated
`com.android.library` module, `:thirdparty:whispercpp`, which `feature/agent`'s `androidMain` depends on.

## RECOMMENDED: reuse the official whisper.cpp Android module (don't hand-roll JNI)

`ggml-org/whisper.cpp` ships a ready Android library at `examples/whisper.android/lib` (MIT). Copy it in
rather than writing the JNI yourself — it's proven and handles CPU-variant `.so` selection. Confirmed
parameters (from the official example + `mikeesto/whispercpp-android`):

- **Keep the package `com.whispercpp.whisper`** — the JNI C symbols (`Java_com_whispercpp_whisper_WhisperLib_...`)
  encode it; renaming breaks the bindings.
- Module layout: `src/main/jni/whisper/{CMakeLists.txt, jni.c}` + `src/main/java/com/whispercpp/whisper/{LibWhisper.kt, WhisperCpuConfig.kt}`.
- `lib/build.gradle`: `namespace "com.whispercpp"`, `ndkVersion "25.2.9519653"` (or whatever NDK you have
  installed — match it), `abiFilters "arm64-v8a","armeabi-v7a","x86","x86_64"`, `externalNativeBuild { cmake { path "src/main/jni/whisper/CMakeLists.txt" } }`.
- Kotlin API: `WhisperContext.createContextFromFile(path)` → `transcribeData(FloatArray): String`. That's
  exactly our contract (raw 16 kHz PCM in, text out) — wrap it in `AndroidWhisperCppTranscriber`.
- whisper.cpp C sources: either a git submodule (Step 1) **or** point CMake at an external clone via a
  Gradle property, as mikeesto does: `-DWHISPER_CPP_DIR=/path/to/whisper.cpp` (set `WHISPER_CPP_DIR`).

With the official module copied in, Steps 3–5 below (hand-written CMake/JNI/binding) are **only a
fallback** if you'd rather minimize vendored code. Either way, Steps 1, 2, 6, 7, 8 apply.

## Step 1 — vendor whisper.cpp (pinned to the iOS version)

```bash
git submodule add https://github.com/ggml-org/whisper.cpp \
  thirdparty/whispercpp/src/main/cpp/whisper.cpp
( cd thirdparty/whispercpp/src/main/cpp/whisper.cpp && git checkout v1.9.1 )
```
(`v1.9.1` matches the `whisper.xcframework` the iOS cinterop already downloads.)

## Step 2 — module `thirdparty/whispercpp/build.gradle.kts`

```kotlin
plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.ampairs.whispercpp"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") } // add "armeabi-v7a" if you ship 32-bit
        externalNativeBuild { cmake { arguments += "-DGGML_OPENMP=OFF" } }
    }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt") } }
    ndkVersion = "27.0.12077973"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
```
Add to `settings.gradle.kts`: `include(":thirdparty:whispercpp")`.

## Step 3 — `thirdparty/whispercpp/src/main/cpp/CMakeLists.txt`

```cmake
cmake_minimum_required(VERSION 3.22)
project(whisper_android)

set(WHISPER_BUILD_TESTS   OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
add_subdirectory(whisper.cpp)          # exposes the `whisper` target

add_library(whisper_android SHARED whisper_jni.cpp)
target_link_libraries(whisper_android whisper log)
```

## Step 4 — JNI bridge `thirdparty/whispercpp/src/main/cpp/whisper_jni.cpp`

```cpp
#include <jni.h>
#include <string>
#include <vector>
#include "whisper.cpp/include/whisper.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_ampairs_whispercpp_WhisperCppAndroid_nativeInit(JNIEnv* env, jobject, jstring path) {
    const char* p = env->GetStringUTFChars(path, nullptr);
    whisper_context_params cp = whisper_context_default_params();
    cp.use_gpu = false;                                  // CPU is reliable; flip on later if desired
    whisper_context* ctx = whisper_init_from_file_with_params(p, cp);
    env->ReleaseStringUTFChars(path, p);
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ampairs_whispercpp_WhisperCppAndroid_nativeTranscribe(
        JNIEnv* env, jobject, jlong h, jfloatArray pcm, jstring lang) {
    auto* ctx = reinterpret_cast<whisper_context*>(h);
    if (!ctx) return env->NewStringUTF("");
    jsize n = env->GetArrayLength(pcm);
    std::vector<float> buf(n);
    env->GetFloatArrayRegion(pcm, 0, n, buf.data());

    whisper_full_params wp = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wp.print_progress = false; wp.print_realtime = false; wp.print_special = false;
    wp.translate = false; wp.no_timestamps = true;
    const char* l = lang ? env->GetStringUTFChars(lang, nullptr) : nullptr;
    if (l) wp.language = l;                               // null/"" => auto-detect
    wp.n_threads = 4;

    std::string out;
    if (whisper_full(ctx, wp, buf.data(), n) == 0) {
        int segs = whisper_full_n_segments(ctx);
        for (int i = 0; i < segs; ++i) out += whisper_full_get_segment_text(ctx, i);
    }
    if (l) env->ReleaseStringUTFChars(lang, l);
    return env->NewStringUTF(out.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_ampairs_whispercpp_WhisperCppAndroid_nativeFree(JNIEnv*, jobject, jlong h) {
    if (h) whisper_free(reinterpret_cast<whisper_context*>(h));
}
```

## Step 5 — Kotlin binding `thirdparty/whispercpp/src/main/kotlin/com/ampairs/whispercpp/WhisperCppAndroid.kt`

```kotlin
package com.ampairs.whispercpp

/** Thin JNI handle to a whisper.cpp context. Not thread-safe — guard with a Mutex at the call site. */
class WhisperCppAndroid private constructor(private var handle: Long) {
    fun transcribe(pcm16kMono: FloatArray, languageTag: String? = null): String =
        if (handle == 0L) "" else nativeTranscribe(handle, pcm16kMono, languageTag)

    fun close() { if (handle != 0L) { nativeFree(handle); handle = 0L } }

    private external fun nativeTranscribe(h: Long, pcm: FloatArray, lang: String?): String
    private external fun nativeFree(h: Long)

    companion object {
        init { System.loadLibrary("whisper_android") }
        @JvmStatic private external fun nativeInit(modelPath: String): Long
        fun load(modelPath: String): WhisperCppAndroid? =
            nativeInit(modelPath).let { if (it == 0L) null else WhisperCppAndroid(it) }
    }
}
```

## Step 6 — agent wiring (swap the broken tflite path)

- `feature/agent/build.gradle.kts` `androidMain`: `implementation(projects.thirdparty.whispercpp)`.
- New `feature/agent/src/androidMain/.../speech/whisper/AndroidWhisperCppTranscriber.kt`
  implementing `WhisperTranscriber`: lazily `WhisperCppAndroid.load(modelPath)` (guarded by a `Mutex`),
  `transcribe(pcm, modelPath, lang)` → `engine.transcribe(pcm, lang)`. **No mel/vocab/filters needed**
  (whisper.cpp does mel internally) — so the `tfliteFilters` companion + `WhisperFeatureExtractor`/
  `WhisperTokenizer` are unused on this path.
- `SpeechAndroidModule`: provide `WhisperModelSet(WhisperModelCatalog.ggml)` (not `.tflite`) and build
  the `"whisper"` adapter with `AndroidWhisperCppTranscriber` + `AndroidAudioCapture`. Delete/retire
  `AndroidWhisperTranscriber` (tflite) and the LiteRT dep if nothing else uses it.

## Step 7 — models (per the article: prefer int8 / Q8_0)

`WhisperModelCatalog.ggml` already points at `ggerganov/whisper.cpp`. The article recommends **Q8_0**
(int8): better accuracy than q5_1 for ~+20 MB. Switch the file names/URLs to
`ggml-base-q8_0.bin` / `ggml-tiny-q8_0.bin` (same repo) when moving to whisper.cpp.

## Step 8 — CI

In `.github/workflows/pr.yml`, the Android job needs the NDK and submodule sources:
- `actions/checkout` with `submodules: recursive`.
- Ensure NDK `27.0.12077973` is installed (`sdkmanager "ndk;27.0.12077973"` or `android-actions/setup-android`).
- First native build is slow (whisper.cpp compile) — expect longer Android job times; cache `~/.gradle`
  and the CMake build dir.

## Later — real-time partials (the article's streaming bit)

Once the one-shot path works, add the sliding window for live partials: a `RingBuffer`, 30 s window /
5 s stride, run `transcribe` each stride, emit `SttEvent.Partial` from `WhisperSttEngine` (it already
streams chunks from `AudioCapture` via a flow — feed them through the ring buffer instead of buffering
the whole utterance). Keep inference on `Dispatchers.Default`; only UI hops to Main.

## Verification (local — can't be done from the headless agent sandbox)

```bash
git submodule update --init --recursive
./gradlew :thirdparty:whispercpp:assembleDebug      # native build (needs NDK)
./gradlew androidApp:assembleDebug                  # links the .so into the app
# install, pick Whisper STT, speak → logcat tag AgentWhisper shows the transcript
```
