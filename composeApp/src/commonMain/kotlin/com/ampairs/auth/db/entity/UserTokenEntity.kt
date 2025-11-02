package com.ampairs.auth.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.common.time.currentTimeMillis

@Entity(
    tableName = "userTokenEntity",
    indices = [
        Index(value = ["user_id"], unique = true),
        Index(value = ["id"], unique = false)  // Remove unique constraint to allow multiple entries
    ]
)
data class UserTokenEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,  // Keep for backward compatibility, but will use user_id as primary identifier
    val user_id: String,  // Link to UserEntity.id
    val refresh_token: String,
    val access_token: String,
    val access_token_expires_at: Long? = null,
    val refresh_token_expires_at: Long? = null,
    val is_active: Boolean = true,  // Track if this user session is active
    val last_used: Long = currentTimeMillis(),
    @Deprecated("Use access_token_expires_at instead")
    val expires_at: Long? = null,
)