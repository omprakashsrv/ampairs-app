package com.ampairs.workspace.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.common.time.currentTimeMillis

/**
 * Entity for storing workspace permissions locally with offline-first support
 */
@Entity(
    tableName = "workspacePermissionEntity",
    indices = [
        Index(value = ["user_id"], unique = false),
        Index(value = ["workspace_id"], unique = false),
        Index(value = ["user_id", "workspace_id", "module"], unique = true), // Unique per user per workspace per module
        Index(value = ["sync_state"], unique = false),
        Index(value = ["last_synced_at"], unique = false)
    ]
)
data class WorkspacePermissionEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    
    // Core permission data
    val user_id: String = "", // Associate with user for multi-user support
    val workspace_id: String,
    val module: String, // Module name (e.g., "members", "products", "orders")
    val actions: String = "", // JSON string of module actions with permissions (e.g., {"create": true, "edit": true, "delete": false})
    val description: String = "", // Optional description of the module
    
    // Sync metadata for offline-first functionality
    val sync_state: String = "SYNCED", // SYNCED, PENDING_UPLOAD, PENDING_DOWNLOAD, CONFLICTED, FAILED
    val last_synced_at: Long = 0L, // Timestamp of last successful sync
    val local_updated_at: Long = currentTimeMillis(), // When locally modified
    val server_updated_at: Long = 0L, // Server's updatedAt timestamp
    val pending_changes: String = "", // JSON of pending field changes
    val conflict_data: String = "", // JSON of conflicted fields
    val retry_count: Int = 0, // Number of sync retry attempts
)