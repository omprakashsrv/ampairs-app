package com.ampairs.order.domain

enum class OrderStatus {
    DRAFT, NEW, ORDERED, CONFIRMED, PROCESSING, SHIPPED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED,
    REFUNDED, PENDING_MERCHANT_REVIEW, INVOICED
}

/** Falls back to [OrderStatus.NEW] instead of throwing on a status value the app doesn't know yet
 * (e.g. the backend ships a new lifecycle value ahead of an app release). */
fun parseOrderStatus(value: String): OrderStatus =
    try {
        OrderStatus.valueOf(value.uppercase())
    } catch (_: IllegalArgumentException) {
        OrderStatus.NEW
    }