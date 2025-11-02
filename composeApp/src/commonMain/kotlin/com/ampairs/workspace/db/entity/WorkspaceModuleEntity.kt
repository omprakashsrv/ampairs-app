package com.ampairs.workspace.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ampairs.common.time.currentTimeMillis
import kotlinx.serialization.Serializable

/**
 * Database entity for Workspace Modules
 * 
 * Stores installed workspace modules with full offline capability
 * Includes sync metadata for Store5 offline-first architecture
 */
@Entity(tableName = "workspace_module")
@Serializable
data class WorkspaceModuleEntity(
    @PrimaryKey 
    @ColumnInfo(name = "id") 
    val id: String,
    
    @ColumnInfo(name = "seq_id") 
    val seqId: Long,
    
    @ColumnInfo(name = "workspace_id") 
    val workspaceId: String,
    
    @ColumnInfo(name = "master_module_id") 
    val masterModuleId: String,
    
    @ColumnInfo(name = "module_code") 
    val moduleCode: String,
    
    @ColumnInfo(name = "status") 
    val status: String,
    
    @ColumnInfo(name = "enabled") 
    val enabled: Boolean,
    
    @ColumnInfo(name = "installed_version") 
    val installedVersion: String,
    
    @ColumnInfo(name = "installed_at") 
    val installedAt: String,
    
    @ColumnInfo(name = "installed_by") 
    val installedBy: String? = null,
    
    @ColumnInfo(name = "installed_by_name") 
    val installedByName: String? = null,
    
    @ColumnInfo(name = "last_updated_at") 
    val lastUpdatedAt: String? = null,
    
    @ColumnInfo(name = "last_updated_by") 
    val lastUpdatedBy: String? = null,
    
    @ColumnInfo(name = "category_override") 
    val categoryOverride: String? = null,
    
    @ColumnInfo(name = "display_order") 
    val displayOrder: Int,
    
    @ColumnInfo(name = "settings_json") 
    val settingsJson: String = "{}",
    
    @ColumnInfo(name = "license_info") 
    val licenseInfo: String? = null,
    
    @ColumnInfo(name = "license_expires_at") 
    val licenseExpiresAt: String? = null,
    
    @ColumnInfo(name = "storage_used_mb") 
    val storageUsedMb: Int,
    
    @ColumnInfo(name = "configuration_notes") 
    val configurationNotes: String? = null,
    
    // Computed/effective fields
    @ColumnInfo(name = "effective_name") 
    val effectiveName: String,
    
    @ColumnInfo(name = "effective_description") 
    val effectiveDescription: String,
    
    @ColumnInfo(name = "effective_icon") 
    val effectiveIcon: String,
    
    @ColumnInfo(name = "effective_color") 
    val effectiveColor: String,
    
    @ColumnInfo(name = "effective_category") 
    val effectiveCategory: String,
    
    // Status indicators
    @ColumnInfo(name = "is_operational") 
    val isOperational: Boolean,
    
    @ColumnInfo(name = "has_valid_license") 
    val hasValidLicense: Boolean,
    
    @ColumnInfo(name = "can_be_updated") 
    val canBeUpdated: Boolean,
    
    @ColumnInfo(name = "needs_attention") 
    val needsAttention: Boolean,
    
    @ColumnInfo(name = "health_score") 
    val healthScore: Double,
    
    @ColumnInfo(name = "engagement_level") 
    val engagementLevel: Double,
    
    @ColumnInfo(name = "is_popular") 
    val isPopular: Boolean,
    
    // Usage metrics
    @ColumnInfo(name = "daily_active_users") 
    val dailyActiveUsers: Int? = null,
    
    @ColumnInfo(name = "monthly_access") 
    val monthlyAccess: Int? = null,
    
    @ColumnInfo(name = "average_session_duration") 
    val averageSessionDuration: String? = null,
    
    @ColumnInfo(name = "last_accessed") 
    val lastAccessed: String? = null,
    
    @ColumnInfo(name = "total_operations") 
    val totalOperations: Int? = null,
    
    @ColumnInfo(name = "error_count") 
    val errorCount: Int? = null,
    
    // Timestamps
    @ColumnInfo(name = "created_at") 
    val createdAt: String,
    
    @ColumnInfo(name = "updated_at") 
    val updatedAt: String,
    
    // Store5 sync metadata
    @ColumnInfo(name = "sync_status") 
    val syncStatus: String = "SYNCED", // SYNCED, PENDING_UPLOAD, PENDING_DELETE, FAILED
    
    @ColumnInfo(name = "sync_retry_count") 
    val syncRetryCount: Int = 0,
    
    @ColumnInfo(name = "last_sync_at") 
    val lastSyncAt: String? = null,
    
    @ColumnInfo(name = "local_created_at") 
    val localCreatedAt: Long = currentTimeMillis(),
    
    @ColumnInfo(name = "local_updated_at") 
    val localUpdatedAt: Long = currentTimeMillis(),
)

/**
 * Database entity for Master Modules (available for installation)
 */
@Entity(tableName = "master_module")
@Serializable
data class MasterModuleEntity(
    @PrimaryKey 
    @ColumnInfo(name = "id") 
    val id: String,
    
    @ColumnInfo(name = "seq_id") 
    val seqId: Long,
    
    @ColumnInfo(name = "module_code") 
    val moduleCode: String,
    
    @ColumnInfo(name = "name") 
    val name: String,
    
    @ColumnInfo(name = "description") 
    val description: String? = null,
    
    @ColumnInfo(name = "tagline") 
    val tagline: String? = null,
    
    @ColumnInfo(name = "category") 
    val category: String,
    
    @ColumnInfo(name = "status") 
    val status: String,
    
    @ColumnInfo(name = "required_tier") 
    val requiredTier: String,
    
    @ColumnInfo(name = "required_role") 
    val requiredRole: String,
    
    @ColumnInfo(name = "complexity") 
    val complexity: String,
    
    @ColumnInfo(name = "version") 
    val version: String,
    
    // Configuration and UI metadata as JSON strings
    @ColumnInfo(name = "configuration_json") 
    val configurationJson: String = "{}",
    
    @ColumnInfo(name = "ui_metadata_json") 
    val uiMetadataJson: String = "{}",
    
    // Provider information
    @ColumnInfo(name = "provider") 
    val provider: String,
    
    @ColumnInfo(name = "support_email") 
    val supportEmail: String? = null,
    
    @ColumnInfo(name = "documentation_url") 
    val documentationUrl: String? = null,
    
    @ColumnInfo(name = "homepage_url") 
    val homepageUrl: String? = null,
    
    @ColumnInfo(name = "setup_guide_url") 
    val setupGuideUrl: String? = null,
    
    // Statistics
    @ColumnInfo(name = "size_mb") 
    val sizeMb: Int,
    
    @ColumnInfo(name = "install_count") 
    val installCount: Int,
    
    @ColumnInfo(name = "rating") 
    val rating: Double,
    
    @ColumnInfo(name = "rating_count") 
    val ratingCount: Int,
    
    @ColumnInfo(name = "featured") 
    val featured: Boolean,
    
    @ColumnInfo(name = "display_order") 
    val displayOrder: Int,
    
    @ColumnInfo(name = "active") 
    val active: Boolean,
    
    @ColumnInfo(name = "release_notes") 
    val releaseNotes: String? = null,
    
    @ColumnInfo(name = "last_updated_at") 
    val lastUpdatedAt: String? = null,
    
    @ColumnInfo(name = "created_at") 
    val createdAt: String,
    
    @ColumnInfo(name = "updated_at") 
    val updatedAt: String,
    
    // Store5 sync metadata
    @ColumnInfo(name = "sync_status") 
    val syncStatus: String = "SYNCED",
    
    @ColumnInfo(name = "last_sync_at") 
    val lastSyncAt: String? = null,
    
    @ColumnInfo(name = "local_created_at") 
    val localCreatedAt: Long = currentTimeMillis(),
    
    @ColumnInfo(name = "local_updated_at") 
    val localUpdatedAt: Long = currentTimeMillis(),
)