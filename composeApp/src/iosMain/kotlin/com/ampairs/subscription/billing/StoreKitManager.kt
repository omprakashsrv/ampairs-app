package com.ampairs.subscription.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSNumber
import kotlin.coroutines.resume

/**
 * StoreKit 2 Manager implementation for iOS.
 *
 * Integrates with StoreKitWrapper.swift for native StoreKit 2 APIs.
 *
 * Reference: https://developer.apple.com/documentation/storekit/in-app_purchase
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class StoreKitManager : BillingManager {

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Disconnected)
    override val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val _availableProducts = MutableStateFlow<List<StoreProduct>>(emptyList())
    override val availableProducts: StateFlow<List<StoreProduct>> = _availableProducts.asStateFlow()

    private val _purchases = MutableSharedFlow<List<StorePurchase>>()

    override suspend fun initialize(): BillingResult {
        return try {
            // Set up transaction listener callback
            setupTransactionCallback()

            _billingState.value = BillingState.Connected
            BillingResult.Success
        } catch (e: Exception) {
            _billingState.value = BillingState.Error(e.message ?: "Failed to initialize")
            BillingResult.Error(e.message ?: "Failed to initialize StoreKit")
        }
    }

    override suspend fun queryProducts(productIds: List<String>): BillingResult = suspendCancellableCoroutine { continuation ->
        StoreKitWrapper.shared.loadProducts(productIds) { productsMap, error ->
            if (error != null) {
                continuation.resume(BillingResult.Error(error))
            } else if (productsMap != null) {
                val products = productsMap.mapNotNull { (productId, productData) ->
                    parseProductData(productId, productData)
                }
                _availableProducts.value = products
                continuation.resume(BillingResult.Success)
            } else {
                continuation.resume(BillingResult.Error("No products found"))
            }
        }
    }

    override suspend fun launchPurchaseFlow(
        productId: String,
        offerToken: String?
    ): BillingResult = suspendCancellableCoroutine { continuation ->
        StoreKitWrapper.shared.purchase(productId) { purchaseData, error ->
            when {
                error == "USER_CANCELLED" -> {
                    continuation.resume(BillingResult.Cancelled)
                }
                error != null -> {
                    continuation.resume(BillingResult.Error(error))
                }
                purchaseData?.get("status") == "PENDING" -> {
                    continuation.resume(BillingResult.Success)
                }
                purchaseData != null -> {
                    // Transaction successful and verified
                    val purchase = parsePurchaseData(purchaseData)
                    if (purchase != null) {
                        // Emit purchase for backend verification
                        kotlinx.coroutines.GlobalScope.launch {
                            _purchases.emit(listOf(purchase))
                        }
                    }
                    continuation.resume(BillingResult.Success)
                }
                else -> {
                    continuation.resume(BillingResult.Error("Unknown purchase result"))
                }
            }
        }
    }

    override suspend fun acknowledgePurchase(purchaseToken: String): BillingResult {
        // StoreKit 2 automatically finishes transactions via transaction.finish()
        // Already handled in Swift wrapper
        return BillingResult.Success
    }

    override suspend fun consumePurchase(purchaseToken: String): BillingResult {
        // StoreKit 2 handles consumables via transaction.finish()
        // Already handled in Swift wrapper
        return BillingResult.Success
    }

    override suspend fun queryPurchases(): List<StorePurchase> = suspendCancellableCoroutine { continuation ->
        StoreKitWrapper.shared.queryPurchases { purchasesData, error ->
            if (error != null) {
                continuation.resume(emptyList())
            } else if (purchasesData != null) {
                val purchases = purchasesData.mapNotNull { parsePurchaseData(it) }
                continuation.resume(purchases)
            } else {
                continuation.resume(emptyList())
            }
        }
    }

    override suspend fun restorePurchases(): BillingResult = suspendCancellableCoroutine { continuation ->
        StoreKitWrapper.shared.restorePurchases { success, error ->
            if (success) {
                // Query current purchases after restore
                kotlinx.coroutines.GlobalScope.launch {
                    val purchases = queryPurchases()
                    if (purchases.isNotEmpty()) {
                        _purchases.emit(purchases)
                    }
                }
                continuation.resume(BillingResult.Success)
            } else {
                continuation.resume(BillingResult.Error(error ?: "Failed to restore purchases"))
            }
        }
    }

    override fun observePurchases(): Flow<List<StorePurchase>> = _purchases

    override suspend fun isBillingAvailable(): Boolean {
        // App Store is always available on iOS
        return true
    }

    override fun disconnect() {
        StoreKitWrapper.shared.stopTransactionListener()
        _billingState.value = BillingState.Disconnected
    }

    // ==================
    // Private Helper Methods
    // ==================

    private fun setupTransactionCallback() {
        StoreKitWrapper.shared.setPurchaseCallback { purchaseToken, productId, orderId ->
            // Emit transaction update to flow
            productId?.let { pId ->
                val purchase = StorePurchase(
                    purchaseToken = purchaseToken,
                    productId = pId,
                    orderId = orderId,
                    purchaseTime = System.currentTimeMillis(),
                    purchaseState = PurchaseState.PURCHASED,
                    isAcknowledged = true,
                    isAutoRenewing = true,
                    originalJson = ""
                )
                kotlinx.coroutines.GlobalScope.launch {
                    _purchases.emit(listOf(purchase))
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseProductData(productId: String, data: Map<String, Any>): StoreProduct? {
        return try {
            StoreProduct(
                productId = productId,
                type = when (data["type"] as? String) {
                    "SUBSCRIPTION" -> ProductType.SUBSCRIPTION
                    else -> ProductType.CONSUMABLE
                },
                title = data["displayName"] as? String ?: productId,
                description = data["description"] as? String ?: "",
                price = data["price"] as? String ?: "",
                priceAmountMicros = 0L, // iOS doesn't provide micros easily
                priceCurrencyCode = "USD", // Default, iOS doesn't expose easily
                subscriptionOffers = emptyList() // Parse subscription info if needed
            )
        } catch (e: Exception) {
            println("Error parsing product data: ${e.message}")
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parsePurchaseData(data: Map<String, Any>): StorePurchase? {
        return try {
            val purchaseToken = data["purchaseToken"] as? String ?: return null
            val productId = data["productId"] as? String ?: return null
            val orderId = data["orderId"] as? String
            val purchaseTime = when (val time = data["purchaseTime"]) {
                is NSNumber -> time.longValue
                is Long -> time
                is Double -> time.toLong()
                else -> System.currentTimeMillis()
            }
            val isAutoRenewing = data["isAutoRenewing"] as? Boolean ?: false

            StorePurchase(
                purchaseToken = purchaseToken,
                productId = productId,
                orderId = orderId,
                purchaseTime = purchaseTime,
                purchaseState = PurchaseState.PURCHASED,
                isAcknowledged = true, // StoreKit auto-finishes
                isAutoRenewing = isAutoRenewing,
                originalJson = data.toString()
            )
        } catch (e: Exception) {
            println("Error parsing purchase data: ${e.message}")
            null
        }
    }
}
