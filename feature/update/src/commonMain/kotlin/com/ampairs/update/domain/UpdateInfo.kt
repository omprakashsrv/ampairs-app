package com.ampairs.update.domain

import com.ampairs.common.ApiUrlBuilder
import com.ampairs.update.api.UpdateCheckResponse
import com.ampairs.update.api.UpdateInfoApiModel

/**
 * Desktop platform types for update distribution
 */
enum class DesktopPlatform(val platformCode: String) {
    MACOS("MACOS"),
    WINDOWS("WINDOWS"),
    LINUX("LINUX");

    companion object {
        fun fromCode(code: String): DesktopPlatform {
            return entries.find { it.platformCode == code } ?: LINUX
        }
    }
}

/**
 * Domain model for app update information
 */
data class UpdateInfo(
    val version: String,
    val versionCode: Int,
    val releaseDate: String,
    val isMandatory: Boolean,
    val downloadUrl: String,
    val fileSizeMb: Double,
    val platform: DesktopPlatform,
    val releaseNotes: String? = null,
    val minSupportedVersion: String? = null,
    val checksum: String? = null,
)

/**
 * Domain model for update check result
 */
data class UpdateCheckResult(
    val updateAvailable: Boolean,
    val updateInfo: UpdateInfo? = null,
    val message: String? = null,
)

/**
 * Update download state
 */
sealed class UpdateDownloadState {
    data object Idle : UpdateDownloadState()
    data class Downloading(val progress: Float) : UpdateDownloadState()
    data class Downloaded(val filePath: String) : UpdateDownloadState()
    data class Failed(val error: String) : UpdateDownloadState()
}

/**
 * Update installation state
 */
sealed class UpdateInstallState {
    data object Idle : UpdateInstallState()
    data object Installing : UpdateInstallState()
    data object Installed : UpdateInstallState()
    data class Failed(val error: String) : UpdateInstallState()
}

// Extension functions for converting between API and domain models

fun UpdateInfoApiModel.asDomainModel(): UpdateInfo {
    // Download endpoint lives under the same base path as /check: /api/core/v1/app-updates/...
    // (the previous /api/v1/app-updates/download/{uid} was missing the "core" segment → 404).
    val downloadUrl = ApiUrlBuilder.appUpdateUrl("download/${this.uid}")

    return UpdateInfo(
        version = this.version,
        versionCode = this.versionCode,
        releaseDate = this.releaseDate,
        isMandatory = this.isMandatory,
        downloadUrl = downloadUrl,
        fileSizeMb = this.fileSizeMb,
        platform = DesktopPlatform.fromCode(this.platform),
        releaseNotes = this.releaseNotes,
        minSupportedVersion = this.minSupportedVersion,
        checksum = this.checksum,
    )
}

fun UpdateCheckResponse.asDomainModel(): UpdateCheckResult {
    return UpdateCheckResult(
        updateAvailable = this.updateAvailable,
        updateInfo = this.updateInfo?.asDomainModel(),
        message = this.message,
    )
}
