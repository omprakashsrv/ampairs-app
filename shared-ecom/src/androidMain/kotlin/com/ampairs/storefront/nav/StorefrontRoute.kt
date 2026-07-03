package com.ampairs.storefront.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The Storefront app's own navigation graph — login + the ecom storefront/order flow only. It does not
 * reuse :shared `Routes.kt` (which carries all 25 business modules). Screens themselves come from
 * feature/auth + feature/ecom and are pure callback-driven, so we host them under these keys.
 */
@Serializable
sealed interface StorefrontRoute : NavKey {

    // ── Login (feature/auth) ──────────────────────────────────────────────
    @Serializable data object Phone : StorefrontRoute

    @Serializable data class Otp(
        val sessionId: String,
        val verificationId: String,
        val phoneNumber: String,
    ) : StorefrontRoute

    @Serializable data object UserUpdate : StorefrontRoute

    @Serializable data object AccountRestore : StorefrontRoute

    // ── Ecom storefront + ordering (feature/ecom) ─────────────────────────
    @Serializable data class Storefront(val slug: String) : StorefrontRoute

    @Serializable data class DrillDown(
        val category: String? = null,
        val brand: String? = null,
        val subcategory: String? = null,
        val query: String? = null,
    ) : StorefrontRoute

    @Serializable data class ProductDetail(val productId: String) : StorefrontRoute

    @Serializable data object Cart : StorefrontRoute

    @Serializable data object Checkout : StorefrontRoute

    @Serializable data class OrderPlaced(val orderRef: String) : StorefrontRoute

    @Serializable data class OrderTracking(val orderRef: String) : StorefrontRoute

    @Serializable data object Orders : StorefrontRoute

    @Serializable data object Account : StorefrontRoute

    @Serializable data object Addresses : StorefrontRoute
}
