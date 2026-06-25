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
    )
}
