#include <jni.h>
#include <android/log.h>
#include <string.h>
#include "whisper.h"
#include "ggml.h"

#define UNUSED(x) (void)(x)
#define TAG "WhisperJNI"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

// Adapted from whisper.cpp's examples/whisper.android JNI bridge. Trimmed to the file-based init +
// one-shot full transcription that the Ampairs Android Whisper STT adapter needs (no asset/stream
// loaders, no benchmarks). Symbol names match com.whispercpp.whisper.WhisperLib$Companion.

JNIEXPORT jlong JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_initContext(
        JNIEnv *env, jobject thiz, jstring model_path_str) {
    UNUSED(thiz);
    const char *model_path_chars = (*env)->GetStringUTFChars(env, model_path_str, NULL);
    struct whisper_context *context =
            whisper_init_from_file_with_params(model_path_chars, whisper_context_default_params());
    (*env)->ReleaseStringUTFChars(env, model_path_str, model_path_chars);
    return (jlong) context;
}

JNIEXPORT void JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_freeContext(
        JNIEnv *env, jobject thiz, jlong context_ptr) {
    UNUSED(env);
    UNUSED(thiz);
    whisper_free((struct whisper_context *) context_ptr);
}

JNIEXPORT void JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_fullTranscribe(
        JNIEnv *env, jobject thiz, jlong context_ptr, jint num_threads, jfloatArray audio_data,
        jstring language) {
    UNUSED(thiz);
    struct whisper_context *context = (struct whisper_context *) context_ptr;
    jfloat *audio_data_arr = (*env)->GetFloatArrayElements(env, audio_data, NULL);
    const jsize audio_data_length = (*env)->GetArrayLength(env, audio_data);

    // language == null/empty  -> "auto" (whisper detects the spoken language and transcribes in it,
    // e.g. Hindi stays Hindi). translate stays false so we never convert to English.
    const char *lang_chars = (language != NULL) ? (*env)->GetStringUTFChars(env, language, NULL) : NULL;

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.translate = false;
    params.language = (lang_chars != NULL && lang_chars[0] != '\0') ? lang_chars : "auto";
    params.n_threads = num_threads;
    params.offset_ms = 0;
    params.no_context = true;
    params.single_segment = false;
    // greedy.best_of defaults to 5: it draws 5 candidate decodings and keeps the best. At
    // temperature 0 (our default) greedy decoding is deterministic, so the extra 4 candidates are
    // pure wasted compute — best_of=1 gives the same output for ~the work of one decode. (best_of > 1
    // only matters on temperature-fallback rounds, which stay enabled to guard against repetition.)
    params.greedy.best_of = 1;
    // We never emit timestamps, so skip the token-level timestamp bookkeeping entirely.
    params.token_timestamps = false;
    // Suppress non-speech tokens (e.g. "[music]"/non-speech artefacts) — cleaner output and a
    // slightly smaller decode search, with no impact on real speech transcription.
    params.suppress_nst = true;

    whisper_reset_timings(context);
    if (whisper_full(context, params, audio_data_arr, audio_data_length) != 0) {
        LOGW("whisper_full failed");
    }
    (*env)->ReleaseFloatArrayElements(env, audio_data, audio_data_arr, JNI_ABORT);
    if (lang_chars != NULL) (*env)->ReleaseStringUTFChars(env, language, lang_chars);
}

JNIEXPORT jint JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_getTextSegmentCount(
        JNIEnv *env, jobject thiz, jlong context_ptr) {
    UNUSED(env);
    UNUSED(thiz);
    return whisper_full_n_segments((struct whisper_context *) context_ptr);
}

JNIEXPORT jstring JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_getTextSegment(
        JNIEnv *env, jobject thiz, jlong context_ptr, jint index) {
    UNUSED(thiz);
    const char *text = whisper_full_get_segment_text((struct whisper_context *) context_ptr, index);
    return (*env)->NewStringUTF(env, text);
}

JNIEXPORT jstring JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_getSystemInfo(
        JNIEnv *env, jobject thiz) {
    UNUSED(thiz);
    return (*env)->NewStringUTF(env, whisper_print_system_info());
}
