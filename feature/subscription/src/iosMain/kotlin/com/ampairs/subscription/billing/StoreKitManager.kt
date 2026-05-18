package com.ampairs.subscription.billing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSNumber
import kotlin.coroutines.resume

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class StoreKitManager(private val bridge: StoreKitBridgeApi) : BillingManager {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Disconnected)
    override val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val _availableProducts = MutableStateFlow<List<StoreProduct>>(emptyList())
    override val availableProducts: StateFlow<List<StoreProduct>> = _availableProducts.asStateFlow()

    private val _purchases = MutableSharedFlow<List<StorePurchase>>()

    override suspend fun initialize(): BillingResult {
        return try {
            setupTransactionCallback()
            _billingState.value = BillingState.Connected
            BillingResult.Success
        } catch (e: Exception) {
            _billingState.value = BillingState.Error(e.message ?: "Failed to initialize")
            BillingResult.Error(e.message ?: "Failed to initialize StoreKit")
        }
    }

    override suspend fun queryProducts(productIds: List<String>): BillingResult =
        suspendCancellableCoroutine { continuation ->
            bridge.loadProducts(productIds) { productsMap, error ->
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

    override suspend fun launchPurchaseFlow(productId: String, offerToken: String?): BillingResult =
        suspendCancellableCoroutine { continuation ->
            bridge.purchase(productId) { purchaseData, error ->
                when {
                    error == "USER_CANCELLED" -> continuation.resume(BillingResult.Cancelled)
                    error != null -> continuation.resume(BillingResult.Error(error))
                    purchaseData?.get("status") as? String == "PENDING" -> continuation.resume(BillingResult.Success)
                    purchaseData != null -> {
                        val purchase = parsePurchaseData(purchaseData)
                        if (purchase != null) {
                            scope.launch { _purchases.emit(listOf(purchase)) }
                        }
                        continuation.resume(BillingResult.Success)
                    }
                    else -> continuation.resume(BillingResult.Error("Unknown purchase result"))
                }
            }
        }

    override suspend fun acknowledgePurchase(purchaseToken: String): BillingResult = BillingResult.Success

    override suspend fun consumePurchase(purchaseToken: String): BillingResult = BillingResult.Success

    override suspend fun queryPurchases(): List<StorePurchase> =
        suspendCancellableCoroutine { continuation ->
            bridge.queryPurchases { purchasesData, error ->
                if (error != null) {
                    continuation.resume(emptyList())
                } else if (purchasesData != null) {
                    continuation.resume(purchasesData.mapNotNull { parsePurchaseData(it) })
                } else {
                    continuation.resume(emptyList())
                }
            }
        }

    override suspend fun restorePurchases(): BillingResult =
        suspendCancellableCoroutine { continuation ->
            bridge.restorePurchases { success, error ->
                if (success) {
                    scope.launch {
                        val purchases = queryPurchases()
                        if (purchases.isNotEmpty()) _purchases.emit(purchases)
                    }
                    continuation.resume(BillingResult.Success)
                } else {
                    continuation.resume(BillingResult.Error(error ?: "Failed to restore purchases"))
                }
            }
        }

    override fun observePurchases(): Flow<List<StorePurchase>> = _purchases

    override suspend fun isBillingAvailable(): Boolean = true

    override fun disconnect() {
        bridge.stopTransactionListener()
        _billingState.value = BillingState.Disconnected
    }

    private fun setupTransactionCallback() {
        bridge.setPurchaseCallback { purchaseToken, productId, orderId ->
            productId?.let { pId ->
                val purchase = StorePurchase(
                    purchaseToken = purchaseToken,
                    productId = pId,
                    orderId = orderId,
                    purchaseTime = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                    purchaseState = PurchaseState.PURCHASED,
                    isAcknowledged = true,
                    isAutoRenewing = true,
                    originalJson = ""
                )
                scope.launch { _purchases.emit(listOf(purchase)) }
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
                    else -> ProductType.IN_APP_PRODUCT
                },
                title = data["displayName"] as? String ?: productId,
                description = data["description"] as? String ?: "",
                price = data["price"] as? String ?: "",
                priceAmountMicros = 0L,
                priceCurrencyCode = "USD",
                subscriptionOffers = emptyList()
            )
        } catch (e: Exception) {
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
                else -> kotlin.time.Clock.System.now().toEpochMilliseconds()
            }
            StorePurchase(
                purchaseToken = purchaseToken,
                productId = productId,
                orderId = orderId,
                purchaseTime = purchaseTime,
                purchaseState = PurchaseState.PURCHASED,
                isAcknowledged = true,
                isAutoRenewing = data["isAutoRenewing"] as? Boolean ?: false,
                originalJson = data.toString()
            )
        } catch (e: Exception) {
            null
        }
    }
}
