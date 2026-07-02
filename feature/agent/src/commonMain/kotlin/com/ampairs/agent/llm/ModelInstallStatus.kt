package com.ampairs.agent.llm

/** Install/download state of an on-device model file, surfaced to the model-management UI. */
sealed interface ModelInstallStatus {
    /** No local file present. */
    data object NotInstalled : ModelInstallStatus

    /** Download in progress. [totalBytes] is -1 when the size is unknown. */
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : ModelInstallStatus {
        /** 0f..1f, or 0f when the total is unknown. */
        val fraction: Float
            get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    }

    /** A verified file is present on disk. */
    data class Installed(val sizeBytes: Long) : ModelInstallStatus

    /** The last download attempt failed; [message] is user-facing. */
    data class Failed(val message: String) : ModelInstallStatus
}
