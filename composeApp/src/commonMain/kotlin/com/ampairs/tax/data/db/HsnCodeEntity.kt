package com.ampairs.tax.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.tax.domain.HsnCategory
import com.ampairs.tax.domain.HsnCode

@Entity(
    tableName = "hsn_codes",
    indices = [
        Index(value = ["hsn_code"], unique = true),
        Index(value = ["chapter"]),
        Index(value = ["category"]),
        Index(value = ["is_active"])
    ]
)
data class HsnCodeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "hsn_code")
    val hsnCode: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "chapter")
    val chapter: String,

    @ColumnInfo(name = "heading")
    val heading: String,

    @ColumnInfo(name = "parent_hsn_id")
    val parentHsnId: String? = null,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "SYNCED",

    @ColumnInfo(name = "last_sync")
    val lastSync: Long = 0
)

fun HsnCodeEntity.toDomain(): HsnCode {
    return HsnCode(
        id = id,
        hsnCode = hsnCode,
        description = description,
        chapter = chapter,
        heading = heading,
        parentHsnId = parentHsnId,
        category = try {
            HsnCategory.valueOf(category.uppercase())
        } catch (e: IllegalArgumentException) {
            HsnCategory.GENERAL
        },
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun HsnCode.toEntity(): HsnCodeEntity {
    return HsnCodeEntity(
        id = id,
        hsnCode = hsnCode,
        description = description,
        chapter = chapter,
        heading = heading,
        parentHsnId = parentHsnId,
        category = category.name,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}