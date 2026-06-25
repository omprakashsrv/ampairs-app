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
 * The switchable Whisper model catalog. Two sizes the user can pick between in Assistant settings,
 * downloaded on demand through the existing [com.ampairs.agent.llm.ModelManager] (resumable, checksum,
 * progress) into the shared `agent_models` directory.
 *
 * Two formats because the runtime differs by platform:
 * - [tflite] — LiteRT all-in-one `whisper.tflite` graphs (Android/iOS). URLs are **provisional** (the
 *   LLM [com.ampairs.agent.llm.ModelCatalog] follows the same "confirm before download ships" convention)
 *   and finalized when the Android engine lands.
 * - [ggml] — whisper.cpp ggml `.bin` (Desktop, via `whisper-jni`). Hosted on the canonical
 *   `ggerganov/whisper.cpp` HF repo; quantized q5_1 variants keep the download small. `sha256 = null`
 *   so the downloader validates against the server `Content-Length` only (per `DefaultModelManager`).
 *
 * `role`/`estimatedPeakMemoryBytes` are required by [ModelDescriptor] but unused for Whisper (it is not
 * an LLM and is never selected by `ProviderRegistry`); set to inert defaults.
 */
object WhisperModelCatalog {

    private const val HF_GGML = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main"

    // Canonical mirror of the vilassn/whisper_android assets (the reference impl the LiteRT path is
    // based on): all-in-one multilingual whisper.tflite graphs + the mel/vocab companion.
    private const val HF_TFLITE = "https://huggingface.co/DocWolle/whisper_tflite_models/resolve/main"

    /** ggml models for whisper.cpp / `whisper-jni` (Desktop). */
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

    /** LiteRT all-in-one `.tflite` graphs (Android/iOS), from the DocWolle whisper_tflite mirror. */
    val tflite: List<ModelDescriptor> = listOf(
        ModelDescriptor(
            id = "whisper-base",
            displayName = "Whisper Base (multilingual)",
            role = ModelRole.FALLBACK,
            backendId = "whisper-litert",
            fileName = "whisper-base.tflite",
            downloadUrl = "$HF_TFLITE/whisper-base.tflite",
            sizeBytes = 75_000_000L,
            estimatedPeakMemoryBytes = 400_000_000L,
            recommended = true,
            defaultParams = LlmParams(),
        ),
        ModelDescriptor(
            id = "whisper-tiny",
            displayName = "Whisper Tiny (multilingual)",
            role = ModelRole.FALLBACK,
            backendId = "whisper-litert",
            fileName = "whisper-tiny.tflite",
            downloadUrl = "$HF_TFLITE/whisper-tiny.tflite",
            sizeBytes = 45_000_000L,
            estimatedPeakMemoryBytes = 250_000_000L,
            defaultParams = LlmParams(),
        ),
    )

    /**
     * Mel-filterbank + BPE vocab companion for the `.tflite` path (shared by both sizes). The LiteRT
     * graph emits token ids, so the app needs this to compute the log-mel input and detokenize the
     * output. Downloaded once via the same `ModelManager`, from the DocWolle whisper_tflite mirror.
     */
    val tfliteFilters: ModelDescriptor = ModelDescriptor(
        id = "whisper-filters",
        displayName = "Whisper assets (mel + vocab)",
        role = ModelRole.FALLBACK,
        backendId = "whisper-litert",
        fileName = "filters_vocab_multilingual.bin",
        downloadUrl = "$HF_TFLITE/filters_vocab_multilingual.bin",
        sizeBytes = 1_500_000L,
        estimatedPeakMemoryBytes = 16_000_000L,
        defaultParams = LlmParams(),
    )
}
