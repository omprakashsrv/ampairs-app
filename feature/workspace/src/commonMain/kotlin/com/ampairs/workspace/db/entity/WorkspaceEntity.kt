package com.ampairs.workspace.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.common.time.currentTimeMillis

@Entity(
    tableName = "workspaceEntity",
    indices = [
        Index(
            value = ["id"],
            unique = false
        ), // Remove unique constraint to allow multiple users to have same workspace
        Index(value = ["user_id"], unique = false),
        Index(value = ["user_id", "id"], unique = true), // Unique per user
        Index(value = ["sync_state"], unique = false),
        Index(value = ["last_synced_at"], unique = false)
    ]
)
data class WorkspaceEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val user_id: String = "", // Associate workspace with user
    val name: String,
    val slug: String,
    val description: String = "",
    val workspaceType: String = "BUSINESS",
    val avatarUrl: String = "",
    val isActive: Boolean = true,
    val subscriptionPlan: String = "FREE",
    val maxMembers: Int = 5,
    val storageLimitGb: Int = 1,
    val storageUsedGb: Int = 0,
    val timezone: String = "UTC",
    val language: String = "en",
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String,
    val lastActivityAt: String = "",
    val trialExpiresAt: String = "",
    val memberCount: Int = 1,
    val isTrial: Boolean = false,
    val storagePercentage: Double = 0.0,
    
    // Sync metadata for offline-first functionality
    val sync_state: String = "SYNCED", // SYNCED, PENDING_UPLOAD, PENDING_DOWNLOAD, CONFLICTED, FAILED
    val last_synced_at: Long = 0L, // Timestamp of last successful sync
    val local_updated_at: Long = currentTimeMillis(), // When locally modified
    val server_updated_at: Long = 0L, // Server's updatedAt timestamp
    val pending_changes: String = "", // JSON of pending field changes
    val conflict_data: String = "", // JSON of conflicted fields
    val retry_count: Int = 0, // Number of sync retry attempts
)