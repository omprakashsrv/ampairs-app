package com.ampairs.event.domain

import kotlinx.serialization.Serializable

/**
 * Event types that can be published and consumed across devices.
 * Matches backend EventType enum in event module.
 */
@Serializable
enum class EventType {
    // Customer events
    CUSTOMER_CREATED,
    CUSTOMER_UPDATED,
    CUSTOMER_DELETED,

    // Product events
    PRODUCT_CREATED,
    PRODUCT_UPDATED,
    PRODUCT_DELETED,
    PRODUCT_STOCK_CHANGED,

    // Order events
    ORDER_CREATED,
    ORDER_UPDATED,
    ORDER_DELETED,
    ORDER_STATUS_CHANGED,
    ORDER_CONFIRMED,
    ORDER_FULFILLED,
    ORDER_CANCELLED,

    // Invoice events
    INVOICE_CREATED,
    INVOICE_UPDATED,
    INVOICE_DELETED,
    INVOICE_SENT,
    INVOICE_PAID,
    INVOICE_PARTIAL_PAID,
    INVOICE_OVERDUE,

    // Device/User status events
    USER_STATUS_CHANGED,
    DEVICE_CONNECTED,
    DEVICE_DISCONNECTED
}
