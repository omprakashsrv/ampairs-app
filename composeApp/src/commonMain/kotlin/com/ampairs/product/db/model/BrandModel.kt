package com.ampairs.product.db.model

import androidx.room.Embedded
import androidx.room.Relation
import com.ampairs.product.db.entity.BrandEntity
import com.ampairs.product.db.entity.ImageEntity

data class BrandModel(
    @Embedded val brand: BrandEntity,
    @Relation(
        parentColumn = "image_id",
        entityColumn = "id",
    )
    val image: ImageEntity?
)