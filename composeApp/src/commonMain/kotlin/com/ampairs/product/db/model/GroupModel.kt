package com.ampairs.product.db.model

import androidx.room.Embedded
import androidx.room.Relation
import com.ampairs.product.db.entity.GroupEntity
import com.ampairs.product.db.entity.ImageEntity

data class GroupModel(
    @Embedded val group: GroupEntity,
    @Relation(
        parentColumn = "image_id",
        entityColumn = "id",
    )
    val image: ImageEntity?
)