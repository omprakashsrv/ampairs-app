package com.ampairs.auth.db.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "userEntity",
    indices = [Index(value = ["id"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val first_name: String,
    val last_name: String,
    val user_name: String,
    val country_code: Long,
    val phone: String,
    val profile_picture_url: String? = null,
    val profile_picture_thumbnail_url: String? = null
)