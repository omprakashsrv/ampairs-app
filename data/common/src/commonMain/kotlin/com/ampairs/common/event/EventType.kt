package com.ampairs.common.event

import kotlinx.serialization.Serializable

@Serializable
enum class EventType {
    CUSTOMER_CREATED,
    CUSTOMER_UPDATED,
    CUSTOMER_DELETED,

    PRODUCT_CREATED,
    PRODUCT_UPDATED,
    PRODUCT_DELETED,
    PRODUCT_STOCK_CHANGED,

    ORDER_CREATED,
    ORDER_UPDATED,
    ORDER_DELETED,
    ORDER_STATUS_CHANGED,
    ORDER_CONFIRMED,
    ORDER_FULFILLED,
    ORDER_CANCELLED,

    INVOICE_CREATED,
    INVOICE_UPDATED,
    INVOICE_DELETED,
    INVOICE_SENT,
    INVOICE_PAID,
    INVOICE_PARTIAL_PAID,
    INVOICE_OVERDUE,

    USER_STATUS_CHANGED,
    DEVICE_CONNECTED,
    DEVICE_DISCONNECTED,

    // Generic entity change events (backend EntityChangePublisher — any entity type)
    ENTITY_CREATED,
    ENTITY_UPDATED,
    ENTITY_DELETED,

    // Fallback for any event type the client doesn't recognise (decoded via coerceInputValues).
    UNKNOWN,
}
