package com.ampairs.purchase.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation3 sub-routes for the purchase module. The parent `Route.Purchase` (in shared/Routes.kt)
 * is the module root; these are the in-module destinations resolved by `purchaseEntryProvider`.
 */
@Serializable
sealed interface PurchaseRoute : NavKey {
    /** Purchase list. */
    @Serializable
    data object PurchaseListRoute : PurchaseRoute

    /** Read-only details of one purchase. */
    @Serializable
    data class PurchaseDetailsRoute(val purchaseId: String) : PurchaseRoute

    /** Create (purchaseId == null) or edit an existing purchase. */
    @Serializable
    data class PurchaseCreateRoute(val purchaseId: String? = null) : PurchaseRoute
}
