package com.ampairs.agent.speech.whisper

import com.ampairs.agent.llm.LlmParams
import com.ampairs.agent.llm.ModelDescriptor
import com.ampairs.agent.llm.ModelRole

/**
 * Per-platform set of downloadable Whisper models, provided by the platform speech module and consumed
 * by [WhisperModelRegistry]. Wrapped in a dedicated type (not a bare `List<ModelDescriptor>`) so it has
 * its own Metro binding key, distinct from the LLM catalog.
 */
class WhisperModelSet(val models: List<ModelDescriptor>)

/**
 * The switchable Whisper model catalog — two sizes the user can pick between in Assistant settings,
 * downloaded on demand through the existing [com.ampairs.agent.llm.ModelManager] (resumable, checksum,
 * progress) into the shared `agent_models` directory.
 *
 * All platforms run **whisper.cpp / ggml** now (Android via the `:whispercpp` JNI module, Desktop via
 * `whisper-jni`, iOS via cinterop), so there is a single [ggml] catalog. Models are hosted on the
 * canonical `ggerganov/whisper.cpp` HF repo; quantized q5_1 variants keep the download small.
 * `sha256 = null` so the downloader validates against the server `Content-Length` only (per
 * `DefaultModelManager`).
 *
 * `role`/`estimatedPeakMemoryBytes` are required by [ModelDescriptor] but unused for Whisper (it is not
 * an LLM and is never selected by `ProviderRegistry`); set to inert defaults.
 */
object WhisperModelCatalog {

    private const val HF_GGML = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main"

    /** ggml models for whisper.cpp (Android `:whispercpp`, Desktop `whisper-jni`, iOS cinterop). */
    val ggml: List<ModelDescriptor> = listOf(
        // Base (q5_1) is the default/recommended: it's noticeably more accurate than tiny —
        // especially for Hindi and accented speech — while the q5_1 quantization keeps it small
        // (~57 MB) and fast enough for one-shot, on-device CPU inference. Tiny stays available for
        // users who want the lowest latency / smallest download over accuracy.
        ModelDescriptor(
            id = "whisper-base",
            displayName = "Whisper Base (multilingual)",
            role = ModelRole.FALLBACK,
            backendId = "whisper-cpp",
            fileName = "ggml-base-q5_1.bin",
            downloadUrl = "$HF_GGML/ggml-base-q5_1.bin",
            sizeBytes = 57_000_000L,
            estimatedPeakMemoryBytes = 300_000_000L,
            recommended = true,
            defaultParams = LlmParams(),
        ),
        ModelDescriptor(
            id = "whisper-tiny",
            displayName = "Whisper Tiny (multilingual)",
            role = ModelRole.FALLBACK,
            backendId = "whisper-cpp",
            fileName = "ggml-tiny-q5_1.bin",
            downloadUrl = "$HF_GGML/ggml-tiny-q5_1.bin",
            sizeBytes = 32_000_000L,
            estimatedPeakMemoryBytes = 200_000_000L,
            defaultParams = LlmParams(),
        ),
        // Higher-accuracy options (opt-in, not default). Small is still usable on a phone (a few
        // seconds/utterance); large-v3-turbo is the newest, most accurate Whisper but is really only
        // practical on Desktop given its size + compute. Both download on demand via the same picker.
        ModelDescriptor(
            id = "whisper-small",
            displayName = "Whisper Small (multilingual, more accurate)",
            role = ModelRole.FALLBACK,
            backendId = "whisper-cpp",
            fileName = "ggml-small-q5_1.bin",
            downloadUrl = "$HF_GGML/ggml-small-q5_1.bin",
            sizeBytes = 190_000_000L,
            estimatedPeakMemoryBytes = 700_000_000L,
            defaultParams = LlmParams(),
        ),
        ModelDescriptor(
            id = "whisper-large-v3-turbo",
            displayName = "Whisper Large v3 Turbo (best — Desktop)",
            role = ModelRole.FALLBACK,
            backendId = "whisper-cpp",
            fileName = "ggml-large-v3-turbo-q5_0.bin",
            downloadUrl = "$HF_GGML/ggml-large-v3-turbo-q5_0.bin",
            sizeBytes = 574_000_000L,
            estimatedPeakMemoryBytes = 1_600_000_000L,
            defaultParams = LlmParams(),
        ),
    )
}
