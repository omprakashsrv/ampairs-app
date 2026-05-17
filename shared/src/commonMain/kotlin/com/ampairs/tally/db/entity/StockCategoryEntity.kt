package com.ampairs.tally.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stockCategoryEntity",
    indices = [Index(value = ["guid"], unique = true)]
)
data class StockCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val guid: String = "",
    val unitName: String = "",
    val reservedName: String = "",
    val parent: String = "",
    val parentGuid: String = "",
    val alterId: String = ""
)