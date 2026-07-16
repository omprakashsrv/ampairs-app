package com.ampairs.notification.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Room entity for an in-app notification.
 *
 * Table: notifications
 * `synced = false` flags the row for an automatic bulk push (read/dismiss flag changes only).
 * `active = false` is a dismiss (soft-delete) — sent in-band on push, then hard-deleted locally.
 */
@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["uid"], unique = true, name = "notification_uid_idx"),
        Index(value = ["updated_at"], name = "notification_updated_at_idx"),
    ]
)
data class NotificationLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "uid")
    val uid: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "body")
    val body: String,

    @ColumnInfo(name = "data_payload")
    val dataPayload: String? = null,

    @ColumnInfo(name = "read")
    val read: Boolean = false,

    @ColumnInfo(name = "active")
    val active: Boolean = true,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null,

    @ColumnInfo(name = "synced")
    val synced: Boolean = false,
)
