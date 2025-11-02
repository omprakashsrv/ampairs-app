package com.ampairs.workspace.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.common.time.currentTimeMillis

/**
 * Entity for storing workspace roles locally with offline-first support
 */
@Entity(
    tableName = "workspaceRoleEntity",
    indices = [
        Index(value = ["id"], unique = false),
        Index(value = ["user_id"], unique = false),
        Index(value = ["workspace_id"], unique = false),
        Index(value = ["user_id", "workspace_id", "id"], unique = true), // Unique per user per workspace
        Index(value = ["sync_state"], unique = false),
        Index(value = ["last_synced_at"], unique = false)
    ]
)
data class WorkspaceRoleEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    
    // Core role data
    val id: String,
    val user_id: String = "", // Associate with user for multi-user support
    val workspace_id: String,
    val name: String,
    val description: String = "",
    val level: Int, // Hierarchy level (OWNER=1, ADMIN=2, MANAGER=3, MEMBER=4, VIEWER=5)
    val is_system_role: Boolean = true, // System vs custom roles
    val permissions: String = "", // JSON string of permissions list
    val can_manage_members: Boolean = false,
    val can_edit_workspace: Boolean = false,
    val can_delete_workspace: Boolean = false,
    val created_at: String = "",
    val updated_at: String = "",
    
    // Sync metadata for offline-first functionality
    val sync_state: String = "SYNCED", // SYNCED, PENDING_UPLOAD, PENDING_DOWNLOAD, CONFLICTED, FAILED
    val last_synced_at: Long = 0L, // Timestamp of last successful sync
    val local_updated_at: Long = currentTimeMillis(), // When locally modified
    val server_updated_at: Long = 0L, // Server's updatedAt timestamp
    val pending_changes: String = "", // JSON of pending field changes
    val conflict_data: String = "", // JSON of conflicted fields
    val retry_count: Int = 0, // Number of sync retry attempts
)