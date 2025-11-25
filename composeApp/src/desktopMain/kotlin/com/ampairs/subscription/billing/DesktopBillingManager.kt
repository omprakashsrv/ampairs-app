package com.ampairs.subscription.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import java.awt.Desktop
import java.net.URI

/**
 * Desktop Billing Manager implementation.
 *
 * Desktop apps use web-based checkout (Razorpay/Stripe hosted checkout pages).
 * The actual payment is handled by the browser, and verification is done
 * via backend webhooks.
 *
 * Flow:
 * 1. App requests checkout URL from backend
 * 2. Browser opens with checkout page
 * 3. User completes payment
 * 4. Backend receives webhook and updates subscription
 * 5. App polls or receives push notification to sync
 */
class DesktopBillingManager : BillingManager {

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Disconnected)
    override val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val _availableProducts = MutableStateFlow<List<StoreProduct>>(emptyList())
    override val availableProducts: StateFlow<List<StoreProduct>> = _availableProducts.asStateFlow()

    override suspend fun initialize(): BillingResult {
        _billingState.value = BillingState.Connected
        return BillingResult.Success
    }

    override suspend fun queryProducts(productIds: List<String>): BillingResult {
        // Desktop doesn't query local products
        // Products are fetched from backend API
        return BillingResult.Success
    }

    override suspend fun launchPurchaseFlow(
        productId: String,
        offerToken: String?
    ): BillingResult {
        // For desktop, offerToken is the checkout URL
        val checkoutUrl = offerToken
            ?: return BillingResult.Error("Checkout URL required for desktop purchase")

        return try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(checkoutUrl))
                BillingResult.Pending(checkoutUrl)
            } else {
                BillingResult.Error("Desktop browser not available")
            }
        } catch (e: Exception) {
            BillingResult.Error("Failed to open checkout: ${e.message}")
        }
    }

    /**
     * Open checkout URL in default browser
     */
    fun openCheckoutUrl(url: String): BillingResult {
        return try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
                BillingResult.Success
            } else {
                BillingResult.Error("Desktop browser not available")
            }
        } catch (e: Exception) {
            BillingResult.Error("Failed to open checkout: ${e.message}")
        }
    }

    override suspend fun acknowledgePurchase(purchaseToken: String): BillingResult {
        // Not applicable for desktop - handled by backend webhooks
        return BillingResult.Success
    }

    override suspend fun consumePurchase(purchaseToken: String): BillingResult {
        // Not applicable for desktop
        return BillingResult.Success
    }

    override suspend fun queryPurchases(): List<StorePurchase> {
        // Desktop purchases are tracked server-side
        // App should sync with backend to get current subscription
        return emptyList()
    }

    override suspend fun restorePurchases(): BillingResult {
        // For desktop, sync with backend to restore
        return BillingResult.Success
    }

    override fun observePurchases(): Flow<List<StorePurchase>> {
        // Desktop relies on backend sync, not local purchase observation
        return flowOf(emptyList())
    }

    override suspend fun isBillingAvailable(): Boolean {
        // Desktop billing is always "available" since it uses web checkout
        return true
    }

    override fun disconnect() {
        _billingState.value = BillingState.Disconnected
    }
}
