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

    /** LiteRT `.tflite` models (Android/iOS). URLs provisional — confirmed when the LiteRT engine lands. */
    val tflite: List<ModelDescriptor> = listOf(
        ModelDescriptor(
            id = "whisper-base",
            displayName = "Whisper Base (multilingual)",
            role = ModelRole.FALLBACK,
            backendId = "whisper-litert",
            fileName = "whisper-base.tflite",
            downloadUrl = "https://huggingface.co/whisper-base.tflite", // provisional — confirm
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
            downloadUrl = "https://huggingface.co/whisper-tiny.tflite", // provisional — confirm
            sizeBytes = 45_000_000L,
            estimatedPeakMemoryBytes = 250_000_000L,
            defaultParams = LlmParams(),
        ),
    )
}
