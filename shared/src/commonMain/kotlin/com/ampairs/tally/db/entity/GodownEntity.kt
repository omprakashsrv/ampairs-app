package com.ampairs.tally.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "godownEntity",
    indices = [Index(value = ["guid"], unique = true)]
)
data class GodownEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val guid: String = "",
    val name: String = "",
    val parent: String = "",
    val alterId: String = ""
)