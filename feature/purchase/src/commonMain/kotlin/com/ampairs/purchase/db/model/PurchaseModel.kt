package com.ampairs.purchase.db.model

import androidx.room.Embedded
import androidx.room.Relation
import com.ampairs.purchase.db.entity.PurchaseEntity
import com.ampairs.purchase.db.entity.PurchaseItemEntity

data class PurchaseModel(
    @Embedded val purchase: PurchaseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "purchase_id",
    )
    val purchaseItems: List<PurchaseItemEntity>
)
