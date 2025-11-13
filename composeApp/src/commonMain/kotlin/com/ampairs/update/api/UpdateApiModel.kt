package com.ampairs.update.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API model for app update information from backend
 * Endpoint: /api/v1/app-updates/check
 */
@Serializable
data class UpdateInfoApiModel(
    @SerialName("uid") val uid: String,
    @SerialName("version") val version: String,
    @SerialName("version_code") val versionCode: Int,
    @SerialName("release_date") val releaseDate: String,
    @SerialName("is_mandatory") val isMandatory: Boolean,
    @SerialName("file_size_mb") val fileSizeMb: Double,
    @SerialName("filename") val filename: String,
    @SerialName("platform") val platform: String, // "MACOS", "WINDOWS", "LINUX"
    @SerialName("release_notes") val releaseNotes: String? = null,
    @SerialName("min_supported_version") val minSupportedVersion: String? = null,
    @SerialName("checksum") val checksum: String? = null, // SHA-256 hash for verification
)

/**
 * Request model for checking updates
 */
@Serializable
data class CheckUpdateRequest(
    @SerialName("platform") val platform: String,
    @SerialName("current_version") val currentVersion: String,
    @SerialName("version_code") val versionCode: Int,
)

/**
 * Response wrapper for update check
 */
@Serializable
data class UpdateCheckResponse(
    @SerialName("update_available") val updateAvailable: Boolean,
    @SerialName("update_info") val updateInfo: UpdateInfoApiModel? = null,
    @SerialName("message") val message: String? = null,
)
