package com.ampairs.purchase.domain

/**
 * Buy-side purchase document status. RECEIVED is what the backend uses to increase stock; the app
 * just sends the chosen status. CANCELLED is the server delete signal (treated like a soft-delete
 * on pull).
 */
enum class PurchaseStatus {
    DRAFT, ORDERED, RECEIVED, CANCELLED
}
