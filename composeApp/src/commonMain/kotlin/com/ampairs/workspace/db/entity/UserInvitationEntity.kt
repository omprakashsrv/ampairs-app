package com.ampairs.workspace.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.common.time.currentTimeMillis

@Entity(
    tableName = "userInvitationEntity",
    indices = [
        Index(value = ["id"], unique = true),
        Index(value = ["user_id"], unique = false),
        Index(value = ["workspace_id"], unique = false),
        Index(value = ["sync_state"], unique = false),
        Index(value = ["expires_at"], unique = false),
        Index(value = ["created_at"], unique = false)
    ]
)
data class UserInvitationEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val user_id: String, // Current logged-in user ID
    val workspace_id: String,
    val workspace_name: String,
    val workspace_description: String? = null,
    val workspace_type: String? = null,
    val role: String,
    val message: String? = null,
    val invited_by: String? = null,
    val inviter_name: String? = null,
    val expires_at: String,
    val created_at: String,
    val days_until_expiry: Long? = null,

    // Sync metadata for offline-first functionality
    val sync_state: String = "SYNCED", // SYNCED, PENDING_UPLOAD, PENDING_DOWNLOAD, CONFLICTED, FAILED
    val last_synced_at: Long = 0L, // Timestamp of last successful sync
    val local_updated_at: Long = currentTimeMillis(), // When locally modified
    val server_updated_at: Long = 0L, // Server's updatedAt timestamp
    val pending_changes: String = "", // JSON of pending field changes
    val conflict_data: String = "", // JSON of conflicted fields
    val retry_count: Int = 0, // Number of sync retry attempts
)