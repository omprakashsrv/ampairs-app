package com.ampairs.tally.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventoryEntity",
    indices = [Index(value = ["guid"], unique = true)]
)
data class TallyInventoryEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val guid: String = "",
    val name: String = "",
    val parent: String = "",
    val parent_id: String = "",
    val alterId: String = ""
)