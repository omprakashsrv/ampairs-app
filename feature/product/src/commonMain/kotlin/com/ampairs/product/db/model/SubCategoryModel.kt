package com.ampairs.product.db.model

import androidx.room.Embedded
import androidx.room.Relation
import com.ampairs.product.db.entity.ImageEntity
import com.ampairs.product.db.entity.SubCategoryEntity

data class SubCategoryModel(
    @Embedded val subCategory: SubCategoryEntity,
    @Relation(
        parentColumn = "image_id",
        entityColumn = "id",
    )
    val image: ImageEntity?
)