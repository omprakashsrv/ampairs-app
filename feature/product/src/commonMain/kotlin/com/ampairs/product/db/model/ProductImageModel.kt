package com.ampairs.product.db.model

import androidx.room.Embedded
import androidx.room.Relation
import com.ampairs.product.db.entity.ImageEntity
import com.ampairs.product.db.entity.ProductImageEntity

data class ProductImageModel(
    @Embedded val productImage: ProductImageEntity,
    @Relation(
        parentColumn = "image_id",
        entityColumn = "id",
    )
    val image: ImageEntity

)