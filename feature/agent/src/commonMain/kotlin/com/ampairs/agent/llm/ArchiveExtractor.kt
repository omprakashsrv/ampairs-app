package com.ampairs.agent.llm

/** Archive container a directory-model ships as. Extend (TAR_GZ, etc.) as new model sources appear. */
enum class ArchiveFormat { ZIP }

/**
 * Pluggable archive extractor — the platform-specific half of the generic "download an archive and
 * install it as a directory model" capability (see [ModelManager] / [ModelDescriptor.archiveFormat]).
 *
 * Extraction can't live in `commonMain` (no KMP zip API), so each platform contributes its extractors
 * into a `List<ArchiveExtractor>` via Metro DI. [DefaultModelManager] picks the first one whose
 * [supports] matches the model's format; a platform with no extractor (e.g. iOS today) contributes an
 * empty list, and archive models simply report a clear "unsupported" failure there instead of breaking
 * the build. Adding a new format = add an extractor plugin, nothing else.
 */
interface ArchiveExtractor {
    /** True when this extractor can handle [format]. */
    fun supports(format: ArchiveFormat): Boolean

    /**
     * Extract the archive at [archivePath] into [destDir] (created if absent). Implementations must
     * guard against path traversal ("zip slip") and throw on any failure. Paths are absolute strings so
     * no `commonMain` filesystem type leaks across the platform boundary.
     */
    suspend fun extract(archivePath: String, destDir: String, format: ArchiveFormat)
}
