package com.ampairs.order.db.model

import androidx.room3.Embedded
import androidx.room3.Relation
import com.ampairs.order.db.entity.OrderEntity
import com.ampairs.order.db.entity.OrderItemEntity

data class OrderModel(
    @Embedded val order: OrderEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["order_id"],
    )
    val orderItems: List<OrderItemEntity>
)