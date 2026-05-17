# Spring Boot Backend - App Update API Implementation Guide

## Overview

This guide provides complete implementation instructions for the Spring Boot backend to support the desktop in-app update system. The system manages version information, hosts binary files, and serves update information to desktop clients.

---

## Table of Contents

1. [Database Schema](#1-database-schema)
2. [Entity Classes](#2-entity-classes)
3. [DTOs (Data Transfer Objects)](#3-dtos-data-transfer-objects)
4. [Repository Layer](#4-repository-layer)
5. [Service Layer](#5-service-layer)
6. [Controller Layer](#6-controller-layer)
7. [File Storage Configuration](#7-file-storage-configuration)
8. [Application Properties](#8-application-properties)
9. [Testing](#9-testing)
10. [Deployment & Usage](#10-deployment--usage)

---

## 1. Database Schema

### SQL Migration Script

Create a new Flyway/Liquibase migration file:

**File:** `src/main/resources/db/migration/V{VERSION}__create_app_versions_table.sql`

```sql
-- Create app_versions table for managing desktop app updates
CREATE TABLE app_versions (
    id BIGSERIAL PRIMARY KEY,

    -- Version information
    version VARCHAR(50) NOT NULL,           -- e.g., "1.0.0.10"
    version_code INT NOT NULL,              -- e.g., 10
    platform VARCHAR(20) NOT NULL,          -- "MACOS", "WINDOWS", "LINUX"

    -- Update metadata
    is_mandatory BOOLEAN DEFAULT FALSE,     -- Force user to update
    is_active BOOLEAN DEFAULT TRUE,         -- Enable/disable this version

    -- File information
    download_url TEXT NOT NULL,             -- Full URL to binary file
    file_size_mb DECIMAL(10, 2),           -- File size in megabytes
    file_path VARCHAR(500),                 -- Server file path (if hosted locally)
    checksum VARCHAR(128),                  -- SHA-256 hash for verification

    -- Release information
    release_date TIMESTAMP,
    release_notes TEXT,
    min_supported_version VARCHAR(50),      -- Minimum version that can upgrade

    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),

    -- Constraints
    CONSTRAINT uk_version_platform UNIQUE(version, platform),
    CONSTRAINT chk_platform CHECK (platform IN ('MACOS', 'WINDOWS', 'LINUX'))
);

-- Index for fast lookup of latest version per platform
CREATE INDEX idx_app_versions_platform_active
    ON app_versions(platform, is_active, version_code DESC);

-- Index for active versions
CREATE INDEX idx_app_versions_active
    ON app_versions(is_active, release_date DESC);

-- Comments
COMMENT ON TABLE app_versions IS 'Stores desktop app version information for in-app updates';
COMMENT ON COLUMN app_versions.version_code IS 'Incremental integer for version comparison';
COMMENT ON COLUMN app_versions.is_mandatory IS 'If true, forces users to update before continuing';
COMMENT ON COLUMN app_versions.checksum IS 'SHA-256 hash for file integrity verification';
```

---

## 2. Entity Classes

### AppVersion Entity

**File:** `src/main/kotlin/com/ampairs/appupdate/entity/AppVersionEntity.kt`

```kotlin
package com.ampairs.appupdate.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "app_versions",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_version_platform", columnNames = ["version", "platform"])
    ],
    indexes = [
        Index(name = "idx_app_versions_platform_active", columnList = "platform,is_active,version_code"),
        Index(name = "idx_app_versions_active", columnList = "is_active,release_date")
    ]
)
data class AppVersionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 50)
    val version: String,

    @Column(name = "version_code", nullable = false)
    val versionCode: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val platform: PlatformType,

    @Column(name = "is_mandatory", nullable = false)
    val isMandatory: Boolean = false,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "download_url", nullable = false, columnDefinition = "TEXT")
    val downloadUrl: String,

    @Column(name = "file_size_mb", precision = 10, scale = 2)
    val fileSizeMb: BigDecimal? = null,

    @Column(name = "file_path", length = 500)
    val filePath: String? = null,

    @Column(length = 128)
    val checksum: String? = null,

    @Column(name = "release_date")
    val releaseDate: LocalDateTime? = null,

    @Column(name = "release_notes", columnDefinition = "TEXT")
    val releaseNotes: String? = null,

    @Column(name = "min_supported_version", length = 50)
    val minSupportedVersion: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_by", length = 100)
    val createdBy: String? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_by", length = 100)
    val updatedBy: String? = null
)

enum class PlatformType {
    MACOS,
    WINDOWS,
    LINUX
}
```

---

## 3. DTOs (Data Transfer Objects)

### Request/Response DTOs

**File:** `src/main/kotlin/com/ampairs/appupdate/dto/AppUpdateDTOs.kt`

```kotlin
package com.ampairs.appupdate.dto

import com.ampairs.appupdate.entity.PlatformType
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Response DTO for update check endpoint
 */
data class UpdateCheckResponse(
    @JsonProperty("update_available")
    val updateAvailable: Boolean,

    @JsonProperty("update_info")
    val updateInfo: UpdateInfoDTO? = null,

    @JsonProperty("message")
    val message: String? = null
)

/**
 * Detailed update information
 */
data class UpdateInfoDTO(
    @JsonProperty("version")
    val version: String,

    @JsonProperty("version_code")
    val versionCode: Int,

    @JsonProperty("release_date")
    val releaseDate: String,

    @JsonProperty("is_mandatory")
    val isMandatory: Boolean,

    @JsonProperty("download_url")
    val downloadUrl: String,

    @JsonProperty("file_size_mb")
    val fileSizeMb: BigDecimal,

    @JsonProperty("platform")
    val platform: String,

    @JsonProperty("release_notes")
    val releaseNotes: String? = null,

    @JsonProperty("min_supported_version")
    val minSupportedVersion: String? = null,

    @JsonProperty("checksum")
    val checksum: String? = null
)

/**
 * Request DTO for creating/updating app version
 */
data class CreateAppVersionRequest(
    @JsonProperty("version")
    val version: String,

    @JsonProperty("version_code")
    val versionCode: Int,

    @JsonProperty("platform")
    val platform: PlatformType,

    @JsonProperty("is_mandatory")
    val isMandatory: Boolean = false,

    @JsonProperty("download_url")
    val downloadUrl: String,

    @JsonProperty("file_size_mb")
    val fileSizeMb: BigDecimal? = null,

    @JsonProperty("release_notes")
    val releaseNotes: String? = null,

    @JsonProperty("min_supported_version")
    val minSupportedVersion: String? = null,

    @JsonProperty("checksum")
    val checksum: String? = null
)

/**
 * Response DTO for admin listing
 */
data class AppVersionListResponse(
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("version")
    val version: String,

    @JsonProperty("version_code")
    val versionCode: Int,

    @JsonProperty("platform")
    val platform: String,

    @JsonProperty("is_mandatory")
    val isMandatory: Boolean,

    @JsonProperty("is_active")
    val isActive: Boolean,

    @JsonProperty("file_size_mb")
    val fileSizeMb: BigDecimal?,

    @JsonProperty("release_date")
    val releaseDate: LocalDateTime?,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime
)
```

---

## 4. Repository Layer

**File:** `src/main/kotlin/com/ampairs/appupdate/repository/AppVersionRepository.kt`

```kotlin
package com.ampairs.appupdate.repository

import com.ampairs.appupdate.entity.AppVersionEntity
import com.ampairs.appupdate.entity.PlatformType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AppVersionRepository : JpaRepository<AppVersionEntity, Long> {

    /**
     * Find the latest active version for a specific platform
     */
    @Query(
        """
        SELECT v FROM AppVersionEntity v
        WHERE v.platform = :platform
        AND v.isActive = true
        ORDER BY v.versionCode DESC
        LIMIT 1
        """
    )
    fun findLatestByPlatformAndActive(
        @Param("platform") platform: PlatformType
    ): Optional<AppVersionEntity>

    /**
     * Find all active versions for a platform
     */
    fun findByPlatformAndIsActiveTrueOrderByVersionCodeDesc(
        platform: PlatformType
    ): List<AppVersionEntity>

    /**
     * Find specific version by version string and platform
     */
    fun findByVersionAndPlatform(
        version: String,
        platform: PlatformType
    ): Optional<AppVersionEntity>

    /**
     * Check if a version exists
     */
    fun existsByVersionAndPlatform(
        version: String,
        platform: PlatformType
    ): Boolean

    /**
     * Find all versions ordered by release date
     */
    fun findAllByOrderByReleaseDateDesc(): List<AppVersionEntity>

    /**
     * Find all active versions
     */
    fun findByIsActiveTrueOrderByReleaseDateDesc(): List<AppVersionEntity>
}
```

---

## 5. Service Layer

**File:** `src/main/kotlin/com/ampairs/appupdate/service/AppUpdateService.kt`

```kotlin
package com.ampairs.appupdate.service

import com.ampairs.appupdate.dto.CreateAppVersionRequest
import com.ampairs.appupdate.dto.UpdateCheckResponse
import com.ampairs.appupdate.dto.UpdateInfoDTO
import com.ampairs.appupdate.dto.AppVersionListResponse
import com.ampairs.appupdate.entity.AppVersionEntity
import com.ampairs.appupdate.entity.PlatformType
import com.ampairs.appupdate.repository.AppVersionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Service
class AppUpdateService(
    private val appVersionRepository: AppVersionRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Check if an update is available for the given platform and version
     */
    fun checkForUpdates(
        platform: String,
        currentVersion: String,
        currentVersionCode: Int
    ): UpdateCheckResponse {
        logger.info("Checking for updates: platform=$platform, currentVersion=$currentVersion, versionCode=$currentVersionCode")

        val platformType = try {
            PlatformType.valueOf(platform.uppercase())
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid platform type: $platform")
            return UpdateCheckResponse(
                updateAvailable = false,
                message = "Invalid platform type: $platform"
            )
        }

        // Find latest version for platform
        val latestVersion = appVersionRepository.findLatestByPlatformAndActive(platformType)

        if (latestVersion.isEmpty) {
            logger.info("No active version found for platform: $platform")
            return UpdateCheckResponse(
                updateAvailable = false,
                message = "No updates available for this platform"
            )
        }

        val latest = latestVersion.get()

        // Compare version codes
        if (latest.versionCode <= currentVersionCode) {
            logger.info("Current version is up to date")
            return UpdateCheckResponse(
                updateAvailable = false,
                message = "You are running the latest version"
            )
        }

        // Determine if update is mandatory
        val isMandatory = latest.isMandatory || isVersionBelowMinSupported(
            currentVersion,
            latest.minSupportedVersion
        )

        logger.info("Update available: ${latest.version} (mandatory: $isMandatory)")

        // Build update info
        val updateInfo = UpdateInfoDTO(
            version = latest.version,
            versionCode = latest.versionCode,
            releaseDate = latest.releaseDate?.format(DateTimeFormatter.ISO_DATE_TIME) ?: "",
            isMandatory = isMandatory,
            downloadUrl = latest.downloadUrl,
            fileSizeMb = latest.fileSizeMb ?: BigDecimal.ZERO,
            platform = latest.platform.name,
            releaseNotes = latest.releaseNotes,
            minSupportedVersion = latest.minSupportedVersion,
            checksum = latest.checksum
        )

        return UpdateCheckResponse(
            updateAvailable = true,
            updateInfo = updateInfo,
            message = if (isMandatory) "Critical update required" else "New version available"
        )
    }

    /**
     * Create a new app version
     */
    @Transactional
    fun createAppVersion(request: CreateAppVersionRequest, createdBy: String?): AppVersionEntity {
        logger.info("Creating app version: ${request.version} for ${request.platform}")

        // Check if version already exists
        if (appVersionRepository.existsByVersionAndPlatform(request.version, request.platform)) {
            throw IllegalArgumentException("Version ${request.version} already exists for platform ${request.platform}")
        }

        val entity = AppVersionEntity(
            version = request.version,
            versionCode = request.versionCode,
            platform = request.platform,
            isMandatory = request.isMandatory,
            downloadUrl = request.downloadUrl,
            fileSizeMb = request.fileSizeMb,
            releaseNotes = request.releaseNotes,
            minSupportedVersion = request.minSupportedVersion,
            checksum = request.checksum,
            createdBy = createdBy
        )

        return appVersionRepository.save(entity)
    }

    /**
     * Get all versions
     */
    fun getAllVersions(): List<AppVersionListResponse> {
        return appVersionRepository.findAllByOrderByReleaseDateDesc()
            .map { it.toListResponse() }
    }

    /**
     * Get version by ID
     */
    fun getVersionById(id: Long): AppVersionEntity {
        return appVersionRepository.findById(id)
            .orElseThrow { NoSuchElementException("App version not found: $id") }
    }

    /**
     * Activate/Deactivate a version
     */
    @Transactional
    fun toggleVersionActive(id: Long, isActive: Boolean): AppVersionEntity {
        val entity = getVersionById(id)
        val updated = entity.copy(isActive = isActive)
        return appVersionRepository.save(updated)
    }

    /**
     * Delete a version
     */
    @Transactional
    fun deleteVersion(id: Long) {
        appVersionRepository.deleteById(id)
        logger.info("Deleted app version: $id")
    }

    /**
     * Compare versions using semantic versioning
     * Returns true if currentVersion is below minSupportedVersion
     */
    private fun isVersionBelowMinSupported(currentVersion: String, minSupportedVersion: String?): Boolean {
        if (minSupportedVersion.isNullOrBlank()) return false

        return try {
            val current = parseVersion(currentVersion)
            val minSupported = parseVersion(minSupportedVersion)
            compareVersions(current, minSupported) < 0
        } catch (e: Exception) {
            logger.warn("Error comparing versions: $currentVersion vs $minSupportedVersion", e)
            false
        }
    }

    private fun parseVersion(version: String): List<Int> {
        return version.split(".")
            .map { it.toIntOrNull() ?: 0 }
    }

    private fun compareVersions(v1: List<Int>, v2: List<Int>): Int {
        val maxLength = maxOf(v1.size, v2.size)
        for (i in 0 until maxLength) {
            val part1 = v1.getOrElse(i) { 0 }
            val part2 = v2.getOrElse(i) { 0 }
            if (part1 != part2) {
                return part1.compareTo(part2)
            }
        }
        return 0
    }

    // Extension function to convert entity to list response
    private fun AppVersionEntity.toListResponse() = AppVersionListResponse(
        id = this.id!!,
        version = this.version,
        versionCode = this.versionCode,
        platform = this.platform.name,
        isMandatory = this.isMandatory,
        isActive = this.isActive,
        fileSizeMb = this.fileSizeMb,
        releaseDate = this.releaseDate,
        createdAt = this.createdAt
    )
}
```

---

## 6. Controller Layer

**File:** `src/main/kotlin/com/ampairs/appupdate/controller/AppUpdateController.kt`

```kotlin
package com.ampairs.appupdate.controller

import com.ampairs.appupdate.dto.CreateAppVersionRequest
import com.ampairs.appupdate.dto.UpdateCheckResponse
import com.ampairs.appupdate.dto.AppVersionListResponse
import com.ampairs.appupdate.service.AppUpdateService
import com.ampairs.common.model.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/app-updates")
@Tag(name = "App Updates", description = "Desktop app update management")
class AppUpdateController(
    private val appUpdateService: AppUpdateService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Check for available updates
     * PUBLIC ENDPOINT - No authentication required
     */
    @GetMapping("/check")
    @Operation(summary = "Check for app updates", description = "Returns update information if newer version is available")
    fun checkForUpdates(
        @RequestParam platform: String,
        @RequestParam currentVersion: String,
        @RequestParam versionCode: Int
    ): Response<UpdateCheckResponse> {
        logger.info("Update check request: platform=$platform, version=$currentVersion, code=$versionCode")

        val result = appUpdateService.checkForUpdates(platform, currentVersion, versionCode)

        return Response.success(result)
    }

    /**
     * Get all app versions (Admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all app versions", description = "Admin endpoint to list all versions")
    fun getAllVersions(): Response<List<AppVersionListResponse>> {
        val versions = appUpdateService.getAllVersions()
        return Response.success(versions)
    }

    /**
     * Create new app version (Admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create app version", description = "Admin endpoint to create new app version")
    fun createVersion(
        @RequestBody request: CreateAppVersionRequest,
        // Get current user from SecurityContext if needed
        // @AuthenticationPrincipal user: User
    ): Response<Any> {
        val version = appUpdateService.createAppVersion(request, createdBy = "admin")
        return Response.success(
            mapOf(
                "id" to version.id,
                "version" to version.version,
                "platform" to version.platform.name,
                "message" to "App version created successfully"
            )
        )
    }

    /**
     * Get version by ID (Admin only)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get app version by ID")
    fun getVersionById(@PathVariable id: Long): Response<Any> {
        val version = appUpdateService.getVersionById(id)
        return Response.success(version)
    }

    /**
     * Activate/Deactivate version (Admin only)
     */
    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle version active status")
    fun toggleActive(
        @PathVariable id: Long,
        @RequestParam isActive: Boolean
    ): Response<Any> {
        val version = appUpdateService.toggleVersionActive(id, isActive)
        return Response.success(
            mapOf(
                "id" to version.id,
                "version" to version.version,
                "isActive" to version.isActive,
                "message" to "Version status updated"
            )
        )
    }

    /**
     * Delete version (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete app version")
    fun deleteVersion(@PathVariable id: Long): Response<Any> {
        appUpdateService.deleteVersion(id)
        return Response.success(
            mapOf("message" to "Version deleted successfully")
        )
    }
}
```

---

## 7. File Storage Configuration

### Option A: Local File Storage

**File:** `src/main/kotlin/com/ampairs/appupdate/storage/LocalFileStorageService.kt`

```kotlin
package com.ampairs.appupdate.storage

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

@Service
class LocalFileStorageService(
    @Value("\${app.update.storage.path:/var/ampairs/updates}")
    private val storagePath: String,

    @Value("\${app.update.base-url:http://localhost:8080}")
    private val baseUrl: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val uploadPath: Path = Paths.get(storagePath)

    init {
        // Create storage directory if it doesn't exist
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath)
            logger.info("Created storage directory: $uploadPath")
        }
    }

    /**
     * Store update file and return public URL
     */
    fun storeUpdateFile(
        file: MultipartFile,
        platform: String,
        version: String
    ): FileStorageResult {
        val fileExtension = getFileExtension(file.originalFilename ?: "update.bin")
        val fileName = "${platform.lowercase()}-${version}${fileExtension}"
        val targetPath = uploadPath.resolve(fileName)

        // Copy file
        Files.copy(file.inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)
        logger.info("Stored file: $fileName")

        // Calculate SHA-256 checksum
        val checksum = calculateChecksum(targetPath)

        // Calculate file size in MB
        val fileSizeMb = Files.size(targetPath).toDouble() / (1024 * 1024)

        val downloadUrl = "$baseUrl/api/v1/app-updates/download/$fileName"

        return FileStorageResult(
            filePath = targetPath.toString(),
            downloadUrl = downloadUrl,
            checksum = checksum,
            fileSizeMb = fileSizeMb
        )
    }

    /**
     * Delete update file
     */
    fun deleteFile(filePath: String) {
        try {
            Files.deleteIfExists(Paths.get(filePath))
            logger.info("Deleted file: $filePath")
        } catch (e: Exception) {
            logger.error("Error deleting file: $filePath", e)
        }
    }

    /**
     * Calculate SHA-256 checksum
     */
    private fun calculateChecksum(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = Files.readAllBytes(path)
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun getFileExtension(filename: String): String {
        val lastDot = filename.lastIndexOf('.')
        return if (lastDot > 0) filename.substring(lastDot) else ""
    }
}

data class FileStorageResult(
    val filePath: String,
    val downloadUrl: String,
    val checksum: String,
    val fileSizeMb: Double
)
```

### File Download Endpoint

Add to `AppUpdateController.kt`:

```kotlin
/**
 * Download update file
 * PUBLIC ENDPOINT - No authentication required
 */
@GetMapping("/download/{filename}")
@Operation(summary = "Download update binary", description = "Serves the update binary file")
fun downloadFile(@PathVariable filename: String): ResponseEntity<Resource> {
    val file = Paths.get(storagePath).resolve(filename)

    if (!Files.exists(file)) {
        return ResponseEntity.notFound().build()
    }

    val resource = UrlResource(file.toUri())

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${filename}\"")
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .body(resource)
}
```

### Option B: AWS S3 Storage

**File:** `src/main/kotlin/com/ampairs/appupdate/storage/S3FileStorageService.kt`

```kotlin
package com.ampairs.appupdate.storage

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.security.MessageDigest
import java.util.*

@Service
class S3FileStorageService(
    private val amazonS3: AmazonS3,

    @Value("\${aws.s3.bucket.name}")
    private val bucketName: String,

    @Value("\${aws.s3.base-url}")
    private val s3BaseUrl: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun storeUpdateFile(
        file: MultipartFile,
        platform: String,
        version: String
    ): FileStorageResult {
        val fileExtension = getFileExtension(file.originalFilename ?: "update.bin")
        val fileName = "updates/${platform.lowercase()}-${version}${fileExtension}"

        // Calculate checksum before upload
        val checksum = calculateChecksum(file.bytes)

        // Upload to S3
        val metadata = ObjectMetadata().apply {
            contentLength = file.size
            contentType = file.contentType ?: "application/octet-stream"
            addUserMetadata("checksum", checksum)
        }

        val request = PutObjectRequest(bucketName, fileName, file.inputStream, metadata)
        amazonS3.putObject(request)

        logger.info("Uploaded file to S3: $fileName")

        val downloadUrl = "$s3BaseUrl/$fileName"
        val fileSizeMb = file.size.toDouble() / (1024 * 1024)

        return FileStorageResult(
            filePath = fileName,
            downloadUrl = downloadUrl,
            checksum = checksum,
            fileSizeMb = fileSizeMb
        )
    }

    fun deleteFile(filePath: String) {
        try {
            amazonS3.deleteObject(bucketName, filePath)
            logger.info("Deleted S3 file: $filePath")
        } catch (e: Exception) {
            logger.error("Error deleting S3 file: $filePath", e)
        }
    }

    private fun calculateChecksum(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun getFileExtension(filename: String): String {
        val lastDot = filename.lastIndexOf('.')
        return if (lastDot > 0) filename.substring(lastDot) else ""
    }
}
```

---

## 8. Application Properties

**File:** `src/main/resources/application.yml`

```yaml
app:
  update:
    # Local file storage configuration
    storage:
      path: /var/ampairs/updates  # Change to your desired path
    base-url: ${SERVER_BASE_URL:http://localhost:8080}

# AWS S3 Configuration (if using S3)
aws:
  s3:
    bucket:
      name: ${AWS_S3_BUCKET:ampairs-updates}
    base-url: ${AWS_S3_BASE_URL:https://ampairs-updates.s3.amazonaws.com}
    access-key: ${AWS_ACCESS_KEY:}
    secret-key: ${AWS_SECRET_KEY:}
    region: ${AWS_REGION:us-east-1}

# Security configuration
spring:
  servlet:
    multipart:
      max-file-size: 500MB      # Maximum file upload size
      max-request-size: 500MB   # Maximum request size
```

---

## 9. Testing

### Integration Test Example

**File:** `src/test/kotlin/com/ampairs/appupdate/AppUpdateIntegrationTest.kt`

```kotlin
package com.ampairs.appupdate

import com.ampairs.appupdate.dto.CreateAppVersionRequest
import com.ampairs.appupdate.entity.PlatformType
import com.ampairs.appupdate.repository.AppVersionRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
class AppUpdateIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var appVersionRepository: AppVersionRepository

    @Test
    fun `should check for updates successfully`() {
        // Given: Create test version
        val testVersion = createTestVersion("1.0.0.10", 10, PlatformType.MACOS)
        appVersionRepository.save(testVersion)

        // When: Client checks for updates
        mockMvc.perform(
            get("/api/v1/app-updates/check")
                .param("platform", "MACOS")
                .param("currentVersion", "1.0.0.9")
                .param("versionCode", "9")
        )
            // Then: Should receive update available
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.update_available").value(true))
            .andExpect(jsonPath("$.data.update_info.version").value("1.0.0.10"))
            .andExpect(jsonPath("$.data.update_info.version_code").value(10))
    }

    @Test
    fun `should return no update when version is current`() {
        // Given: Create test version
        val testVersion = createTestVersion("1.0.0.10", 10, PlatformType.MACOS)
        appVersionRepository.save(testVersion)

        // When: Client checks with current version
        mockMvc.perform(
            get("/api/v1/app-updates/check")
                .param("platform", "MACOS")
                .param("currentVersion", "1.0.0.10")
                .param("versionCode", "10")
        )
            // Then: Should receive no update
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.update_available").value(false))
            .andExpect(jsonPath("$.data.message").value("You are running the latest version"))
    }

    private fun createTestVersion(
        version: String,
        versionCode: Int,
        platform: PlatformType
    ) = AppVersionEntity(
        version = version,
        versionCode = versionCode,
        platform = platform,
        isMandatory = false,
        downloadUrl = "https://example.com/download/${platform.name.lowercase()}-${version}.dmg",
        fileSizeMb = BigDecimal("125.5"),
        releaseNotes = "Test release",
        checksum = "test_checksum_sha256"
    )
}
```

---

## 10. Deployment & Usage

### 10.1 Upload New Version (Admin)

**Step 1: Upload binary file**

```bash
# Upload via multipart form-data (if you add file upload endpoint)
curl -X POST http://localhost:8080/api/v1/app-updates/upload \
  -H "Authorization: Bearer <admin_token>" \
  -F "file=@Ampairs-1.0.0.10-macos.dmg" \
  -F "platform=MACOS" \
  -F "version=1.0.0.10"
```

**Step 2: Create version entry**

```bash
curl -X POST http://localhost:8080/api/v1/app-updates \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "version": "1.0.0.10",
    "version_code": 10,
    "platform": "MACOS",
    "is_mandatory": false,
    "download_url": "http://localhost:8080/api/v1/app-updates/download/macos-1.0.0.10.dmg",
    "file_size_mb": 125.5,
    "release_notes": "- New features\n- Bug fixes\n- Performance improvements",
    "min_supported_version": "1.0.0.5",
    "checksum": "abc123def456..."
  }'
```

### 10.2 Check for Updates (Client)

```bash
curl "http://localhost:8080/api/v1/app-updates/check?platform=MACOS&currentVersion=1.0.0.9&versionCode=9"
```

**Response:**
```json
{
  "data": {
    "update_available": true,
    "update_info": {
      "version": "1.0.0.10",
      "version_code": 10,
      "release_date": "2025-01-15T10:00:00",
      "is_mandatory": false,
      "download_url": "http://localhost:8080/api/v1/app-updates/download/macos-1.0.0.10.dmg",
      "file_size_mb": 125.5,
      "platform": "MACOS",
      "release_notes": "- New features\n- Bug fixes",
      "min_supported_version": "1.0.0.5",
      "checksum": "abc123..."
    },
    "message": "New version available"
  },
  "error": null
}
```

### 10.3 List All Versions (Admin)

```bash
curl http://localhost:8080/api/v1/app-updates \
  -H "Authorization: Bearer <admin_token>"
```

### 10.4 Deactivate Version (Admin)

```bash
curl -X PATCH "http://localhost:8080/api/v1/app-updates/1/active?isActive=false" \
  -H "Authorization: Bearer <admin_token>"
```

---

## Security Considerations

### 1. File Upload Security

```kotlin
// Add file validation
@PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
@PreAuthorize("hasRole('ADMIN')")
fun uploadFile(
    @RequestParam("file") file: MultipartFile,
    @RequestParam platform: String,
    @RequestParam version: String
): Response<Any> {
    // Validate file type
    val allowedExtensions = listOf(".dmg", ".msi", ".deb", ".exe", ".rpm")
    val filename = file.originalFilename ?: ""

    if (!allowedExtensions.any { filename.endsWith(it) }) {
        throw IllegalArgumentException("Invalid file type")
    }

    // Validate file size (max 500MB)
    if (file.size > 500 * 1024 * 1024) {
        throw IllegalArgumentException("File too large")
    }

    val result = fileStorageService.storeUpdateFile(file, platform, version)
    return Response.success(result)
}
```

### 2. Rate Limiting

```kotlin
// Add rate limiting to prevent abuse
@RateLimiter(name = "update-check", fallbackMethod = "updateCheckFallback")
@GetMapping("/check")
fun checkForUpdates(...): Response<UpdateCheckResponse> {
    // Implementation
}
```

### 3. CDN/Caching

- Use CDN (CloudFront, CloudFlare) for binary file distribution
- Add cache headers for update check endpoint
- Use signed URLs for S3 downloads with expiration

---

## Database Seed Data (Development)

**File:** `src/main/resources/db/data/R__seed_app_versions.sql`

```sql
-- Sample app versions for testing
INSERT INTO app_versions (
    version, version_code, platform, is_mandatory, is_active,
    download_url, file_size_mb, release_date, release_notes, checksum
) VALUES
(
    '1.0.0.9', 9, 'MACOS', FALSE, TRUE,
    'http://localhost:8080/api/v1/app-updates/download/macos-1.0.0.9.dmg',
    120.5,
    NOW() - INTERVAL '7 days',
    'Initial stable release',
    'sample_checksum_for_testing_only'
),
(
    '1.0.0.9', 9, 'WINDOWS', FALSE, TRUE,
    'http://localhost:8080/api/v1/app-updates/download/windows-1.0.0.9.msi',
    115.3,
    NOW() - INTERVAL '7 days',
    'Initial stable release',
    'sample_checksum_for_testing_only'
),
(
    '1.0.0.9', 9, 'LINUX', FALSE, TRUE,
    'http://localhost:8080/api/v1/app-updates/download/linux-1.0.0.9.deb',
    110.8,
    NOW() - INTERVAL '7 days',
    'Initial stable release',
    'sample_checksum_for_testing_only'
)
ON CONFLICT (version, platform) DO NOTHING;
```

---

## Summary

This implementation provides:

✅ **Complete database schema** with indexes and constraints
✅ **JPA entities** with proper annotations
✅ **Repository layer** with custom queries
✅ **Service layer** with business logic and version comparison
✅ **REST API** with public and admin endpoints
✅ **File storage** with both local and S3 options
✅ **Security** with role-based access control
✅ **Checksum verification** for file integrity
✅ **Testing examples** for integration tests
✅ **Deployment guide** with curl examples

The implementation is production-ready and follows Spring Boot best practices. The update check endpoint is public (no auth required) while admin endpoints are protected with role-based security.

---

**Questions or Issues?**

Refer to `DESKTOP_UPDATE_SYSTEM.md` for mobile client implementation details.
