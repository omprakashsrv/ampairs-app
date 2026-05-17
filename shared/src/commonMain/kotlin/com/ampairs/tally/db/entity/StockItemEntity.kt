package com.ampairs.tally.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stockItemEntity",
    indices = [Index(value = ["guid"], unique = true)]
)
data class StockItemEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val guid: String = "",
    val unitName: String = "",
    val alias: String = "",
    val reservedName: String = "",
    val parent: String = "",
    val parentGuid: String = "",
    val category: String = "",
    val gstApplicable: String = "",
    val tcsApplicable: String = "",
    val tcsCategory: String = "",
    val gstTypeOfSupply: String = "",
    val baseUnits: String = "",
    val additionalUnits: String = "",
    val gstRepUOM: String = "",
    val denominator: String = "",
    val conversion: String = "",
    val standardCost: String = "",
    val standardPrice: String = "",
    val gstDetailList: String = "",
    val taxability: String = "",
    val tcsCategoryDetailList: String = "",
    val alterId: String = ""
)