package com.ampairs.workspace.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.common.time.currentTimeMillis

@Entity(
    tableName = "workspaceInvitationEntity",
    indices = [
        Index(value = ["id"], unique = false),
        Index(value = ["user_id"], unique = false),
        Index(value = ["workspace_id"], unique = false),
        Index(value = ["user_id", "workspace_id"], unique = false),
        Index(value = ["user_id", "id"], unique = true), // Unique per user
        Index(value = ["phone"], unique = false),
        Index(value = ["status"], unique = false),
        Index(value = ["sync_state"], unique = false),
        Index(value = ["last_synced_at"], unique = false)
    ]
)
data class WorkspaceInvitationEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val user_id: String = "", // Associate invitation with user who can see it
    val workspace_id: String,
    val country_code: Int,
    val phone: String,
    val recipient_name: String? = null,
    val invited_role: String,
    val status: String, // PENDING, ACCEPTED, EXPIRED, CANCELLED, DECLINED
    val created_at: String,
    val expires_at: String,
    val sent_by_name: String,
    val sent_by_email: String,
    val email_sent: Boolean = false,
    val email_delivered: Boolean = false,
    val email_opened: Boolean = false,
    val link_clicked: Boolean = false,
    val resend_count: Int = 0,
    val invitation_message: String? = null,
    
    // Sync metadata for offline-first functionality
    val sync_state: String = "SYNCED", // SYNCED, PENDING_UPLOAD, PENDING_DOWNLOAD, CONFLICTED, FAILED
    val last_synced_at: Long = 0L, // Timestamp of last successful sync
    val local_updated_at: Long = currentTimeMillis(), // When locally modified
    val server_updated_at: Long = 0L, // Server's updatedAt timestamp
)