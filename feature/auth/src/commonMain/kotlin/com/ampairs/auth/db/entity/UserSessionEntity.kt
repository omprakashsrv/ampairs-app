package com.ampairs.auth.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.common.time.currentTimeMillis

@Entity(
    tableName = "userSessionEntity",
    indices = [Index(value = ["user_id"], unique = false)]
)
data class UserSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val user_id: String,  // Link to UserEntity.id
    val is_current: Boolean = false,  // True for the currently active user
    val workspace_id: String = "",  // Selected workspace for this user
    val last_login: Long = currentTimeMillis(),
    val login_count: Int = 1,
    val device_info: String = "",  // Device information
)