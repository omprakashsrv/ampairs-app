package com.ampairs.order.db.model

import androidx.room.Embedded
import androidx.room.Relation
import com.ampairs.order.db.entity.OrderEntity
import com.ampairs.order.db.entity.OrderItemEntity

data class OrderModel(
    @Embedded val order: OrderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "order_id",
    )
    val orderItems: List<OrderItemEntity>
)