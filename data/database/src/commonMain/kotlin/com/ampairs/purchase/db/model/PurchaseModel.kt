package com.ampairs.purchase.db.model

import androidx.room3.Embedded
import androidx.room3.Relation
import com.ampairs.purchase.db.entity.PurchaseEntity
import com.ampairs.purchase.db.entity.PurchaseItemEntity

data class PurchaseModel(
    @Embedded val purchase: PurchaseEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["purchase_id"],
    )
    val purchaseItems: List<PurchaseItemEntity>
)
