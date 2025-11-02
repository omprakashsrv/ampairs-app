package com.ampairs.tally.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "unitEntity",
    indices = [Index(value = ["guid"], unique = true)]
)
data class TallyUnitEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val guid: String = "",
    val unitName: String = "",
    val reservedName: String = "",
    val gstRepUOM: String = "",
    val decimalPlaces: String = "",
    val isSimpleUnit: String = "",
    val alterId: String = ""
)