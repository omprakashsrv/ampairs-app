package com.ampairs.customer.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "states",
    indices = [
        Index(value = ["id"], unique = true),
        Index(value = ["name"])
    ]
)
data class StateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val syncStatus: String = "SYNCED", // PENDING, SYNCING, SYNCED, FAILED
    val createdAt: Long,
    val updatedAt: Long
)
