package com.ampairs.workspace.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.common.time.currentTimeMillis

@Entity(
    tableName = "workspaceMemberEntity",
    indices = [
        Index(value = ["id"], unique = true),
        Index(value = ["user_id"], unique = false),
        Index(value = ["workspace_id"], unique = false),
        Index(value = ["sync_state"], unique = false),
        Index(value = ["last_synced_at"], unique = false)
    ]
)
data class WorkspaceMemberEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val user_id: String, // Current logged-in user ID
    val member_user_id: String, // The actual member's user ID
    val workspace_id: String,
    val email: String,
    val name: String,
    val role: String,
    val status: String,
    val joined_at: String,
    val last_activity: String? = null,
    val permissions: String = "", // JSON string of permissions map
    val avatar_url: String? = null,
    val phone: String? = null,
    
    // Sync metadata for offline-first functionality (following WorkspaceEntity pattern)
    val sync_state: String = "SYNCED", // SYNCED, PENDING_UPLOAD, PENDING_DOWNLOAD, CONFLICTED, FAILED
    val last_synced_at: Long = 0L, // Timestamp of last successful sync
    val local_updated_at: Long = currentTimeMillis(), // When locally modified
    val server_updated_at: Long = 0L, // Server's updatedAt timestamp
    val pending_changes: String = "", // JSON of pending field changes
    val conflict_data: String = "", // JSON of conflicted fields
    val retry_count: Int = 0, // Number of sync retry attempts
)