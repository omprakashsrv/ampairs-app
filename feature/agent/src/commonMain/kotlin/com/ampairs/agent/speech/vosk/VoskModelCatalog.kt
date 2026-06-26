package com.ampairs.agent.speech.vosk

import com.ampairs.agent.llm.ArchiveFormat
import com.ampairs.agent.llm.ModelDescriptor
import com.ampairs.agent.llm.ModelRole

/**
 * Per-platform set of downloadable Vosk models, provided by the platform speech module and consumed by
 * [VoskModelRegistry]. Dedicated type (not a bare `List`) so it has its own Metro binding key, distinct
 * from the Whisper/LLM catalogs. Empty on platforms with no Vosk engine (Android/iOS today).
 */
class VoskModelSet(val models: List<ModelDescriptor>)

/**
 * The switchable Vosk model catalog (Desktop). Each entry is a **zip directory** model
 * ([ArchiveFormat.ZIP]) downloaded on demand through the shared [com.ampairs.agent.llm.ModelManager]
 * (resumable, progress) and extracted into the `agent_models` directory — see the generic archive path
 * in `DefaultModelManager`.
 *
 * Hosted on the official Vosk model index. `sha256 = null` (the index publishes no checksums), so the
 * downloader validates the archive against the server `Content-Length` only. `role`/peak-memory are
 * required by [ModelDescriptor] but inert for Vosk (it's not an LLM and never selected by
 * `ProviderRegistry`). India-focused picks first (matches the app's en-IN / Hindi usage).
 */
object VoskModelCatalog {

    private const val BASE = "https://alphacephei.com/vosk/models"

    val models: List<ModelDescriptor> = listOf(
        ModelDescriptor(
            id = "vosk-small-en-us",
            displayName = "Vosk Small (English US)",
            role = ModelRole.FALLBACK,
            backendId = "vosk",
            fileName = "vosk-model-small-en-us-0.15",
            downloadUrl = "$BASE/vosk-model-small-en-us-0.15.zip",
            sizeBytes = 40_000_000L,
            estimatedPeakMemoryBytes = 300_000_000L,
            recommended = true,
            archiveFormat = ArchiveFormat.ZIP,
        ),
        ModelDescriptor(
            id = "vosk-small-en-in",
            displayName = "Vosk Small (Indian English)",
            role = ModelRole.FALLBACK,
            backendId = "vosk",
            fileName = "vosk-model-small-en-in-0.4",
            downloadUrl = "$BASE/vosk-model-small-en-in-0.4.zip",
            sizeBytes = 36_000_000L,
            estimatedPeakMemoryBytes = 300_000_000L,
            archiveFormat = ArchiveFormat.ZIP,
        ),
        ModelDescriptor(
            id = "vosk-small-hi",
            displayName = "Vosk Small (Hindi)",
            role = ModelRole.FALLBACK,
            backendId = "vosk",
            fileName = "vosk-model-small-hi-0.22",
            downloadUrl = "$BASE/vosk-model-small-hi-0.22.zip",
            sizeBytes = 42_000_000L,
            estimatedPeakMemoryBytes = 300_000_000L,
            archiveFormat = ArchiveFormat.ZIP,
        ),
    )
}
