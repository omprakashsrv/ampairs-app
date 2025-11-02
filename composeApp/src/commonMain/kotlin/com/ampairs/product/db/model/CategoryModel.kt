package com.ampairs.product.db.model

import androidx.room.Embedded
import androidx.room.Relation
import com.ampairs.product.db.entity.CategoryEntity
import com.ampairs.product.db.entity.ImageEntity

data class CategoryModel(
    @Embedded val category: CategoryEntity,
    @Relation(
        parentColumn = "image_id",
        entityColumn = "id",
    )
    val image: ImageEntity?
)