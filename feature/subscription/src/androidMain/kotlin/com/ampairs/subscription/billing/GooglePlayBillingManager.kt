package com.ampairs.subscription.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google Play Billing Manager implementation using Billing Library v7.
 *
 * Handles:
 * - Product queries (subscription plans)
 * - Purchase flows
 * - Purchase acknowledgement
 * - Purchase restoration
 *
 * Reference: https://developer.android.com/google/play/billing/integrate
 */
class GooglePlayBillingManager(
    private val context: Context
) : BillingManager, PurchasesUpdatedListener, BillingClientStateListener {

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Disconnected)
    override val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val _availableProducts = MutableStateFlow<List<StoreProduct>>(emptyList())
    override val availableProducts: StateFlow<List<StoreProduct>> = _availableProducts.asStateFlow()

    private val _purchases = MutableSharedFlow<List<StorePurchase>>()
    private var currentActivity: Activity? = null

    private var billingClient: BillingClient? = null
    private var cachedProductDetails = mapOf<String, ProductDetails>()

    // Coroutine scope for background operations
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Set the current activity for purchase flows
     */
    fun setActivity(activity: Activity?) {
        currentActivity = activity
    }

    override suspend fun initialize(): BillingResult {
        return try {
            // Build and start connection
            billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .enablePrepaidPlans()
                        .build()
                )
                .build()

            startConnection()
        } catch (e: Exception) {
            _billingState.value = BillingState.Error(e.message ?: "Failed to initialize")
            BillingResult.Error(e.message ?: "Failed to initialize billing")
        }
    }

    private suspend fun startConnection(): BillingResult = suspendCancellableCoroutine { continuation ->
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: com.android.billingclient.api.BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _billingState.value = BillingState.Connected
                    continuation.resume(BillingResult.Success)
                } else {
                    val errorMsg = "Billing setup failed: ${billingResult.debugMessage}"
                    _billingState.value = BillingState.Error(errorMsg, billingResult.responseCode)
                    continuation.resume(BillingResult.Error(errorMsg, billingResult.responseCode))
                }
            }

            override fun onBillingServiceDisconnected() {
                _billingState.value = BillingState.Disconnected
                // Retry connection
                billingClient?.startConnection(this)
            }
        })
    }

    override fun onBillingSetupFinished(billingResult: com.android.billingclient.api.BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _billingState.value = BillingState.Connected
        } else {
            _billingState.value = BillingState.Error(billingResult.debugMessage, billingResult.responseCode)
        }
    }

    override fun onBillingServiceDisconnected() {
        _billingState.value = BillingState.Disconnected
        // Auto-reconnect
        billingClient?.startConnection(this)
    }

    override suspend fun queryProducts(productIds: List<String>): BillingResult {
        val client = billingClient ?: return BillingResult.Error("Billing not initialized")

        if (!client.isReady) {
            return BillingResult.Error("Billing client not ready")
        }

        return try {
            val productList = productIds.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            val result = client.queryProductDetails(params)

            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = result.productDetailsList ?: emptyList()

                // Cache product details for purchase flow
                cachedProductDetails = details.associateBy { it.productId }

                // Convert to StoreProduct
                _availableProducts.value = details.map { it.toStoreProduct() }

                BillingResult.Success
            } else {
                BillingResult.Error(
                    "Failed to query products: ${result.billingResult.debugMessage}",
                    result.billingResult.responseCode
                )
            }
        } catch (e: Exception) {
            BillingResult.Error("Exception querying products: ${e.message}")
        }
    }

    override suspend fun launchPurchaseFlow(
        productId: String,
        offerToken: String?
    ): BillingResult {
        val activity = currentActivity
            ?: return BillingResult.Error("Activity not available")

        val client = billingClient
            ?: return BillingResult.Error("Billing not initialized")

        val productDetails = cachedProductDetails[productId]
            ?: return BillingResult.Error("Product not found: $productId")

        // Get subscription offer token
        val subscriptionOfferToken = offerToken
            ?: productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return BillingResult.Error("No subscription offer available")

        return try {
            val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(subscriptionOfferToken)
                .build()

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build()

            val responseCode = client.launchBillingFlow(activity, billingFlowParams)

            when (responseCode.responseCode) {
                BillingClient.BillingResponseCode.OK -> BillingResult.Success
                BillingClient.BillingResponseCode.USER_CANCELED -> BillingResult.Cancelled
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> BillingResult.Error(
                    "Item already owned",
                    responseCode.responseCode
                )
                else -> BillingResult.Error(
                    "Purchase failed: ${responseCode.debugMessage}",
                    responseCode.responseCode
                )
            }
        } catch (e: Exception) {
            BillingResult.Error("Exception launching purchase: ${e.message}")
        }
    }

    override fun onPurchasesUpdated(
        billingResult: com.android.billingclient.api.BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { purchaseList ->
                    val storePurchases = purchaseList.map { it.toStorePurchase() }
                    // Emit to flow
                    scope.launch {
                        _purchases.emit(storePurchases)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                // User cancelled, no action needed
            }
            else -> {
                // Error occurred
                _billingState.value = BillingState.Error(billingResult.debugMessage, billingResult.responseCode)
            }
        }
    }

    override suspend fun acknowledgePurchase(purchaseToken: String): BillingResult {
        val client = billingClient ?: return BillingResult.Error("Billing not initialized")

        return try {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()

            val result = client.acknowledgePurchase(params)

            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                BillingResult.Success
            } else {
                BillingResult.Error("Acknowledge failed: ${result.debugMessage}", result.responseCode)
            }
        } catch (e: Exception) {
            BillingResult.Error("Exception acknowledging: ${e.message}")
        }
    }

    override suspend fun consumePurchase(purchaseToken: String): BillingResult {
        val client = billingClient ?: return BillingResult.Error("Billing not initialized")

        return try {
            val params = ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()

            val result = client.consumePurchase(params)

            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                BillingResult.Success
            } else {
                BillingResult.Error(
                    "Consume failed: ${result.billingResult.debugMessage}",
                    result.billingResult.responseCode
                )
            }
        } catch (e: Exception) {
            BillingResult.Error("Exception consuming: ${e.message}")
        }
    }

    override suspend fun queryPurchases(): List<StorePurchase> {
        val client = billingClient ?: return emptyList()

        return try {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            val result = client.queryPurchasesAsync(params)

            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                result.purchasesList.map { it.toStorePurchase() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun restorePurchases(): BillingResult {
        val purchases = queryPurchases()
        return if (purchases.isNotEmpty()) {
            scope.launch {
                _purchases.emit(purchases)
            }
            BillingResult.Success
        } else {
            BillingResult.Success
        }
    }

    override fun observePurchases(): Flow<List<StorePurchase>> = _purchases

    override suspend fun isBillingAvailable(): Boolean {
        return billingClient?.isReady == true
    }

    override fun disconnect() {
        billingClient?.endConnection()
        billingClient = null
        _billingState.value = BillingState.Disconnected
    }

    // ==================
    // Extension Functions
    // ==================

    private fun ProductDetails.toStoreProduct(): StoreProduct {
        val subscriptionOffer = subscriptionOfferDetails?.firstOrNull()

        return StoreProduct(
            productId = productId,
            type = ProductType.SUBSCRIPTION,
            title = title,
            description = description,
            price = subscriptionOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "",
            priceAmountMicros = subscriptionOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceAmountMicros ?: 0L,
            priceCurrencyCode = subscriptionOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceCurrencyCode ?: "USD",
            subscriptionOffers = subscriptionOfferDetails?.map { it.toSubscriptionOffer() } ?: emptyList()
        )
    }

    private fun ProductDetails.SubscriptionOfferDetails.toSubscriptionOffer(): SubscriptionOffer {
        return SubscriptionOffer(
            offerId = offerId ?: basePlanId,
            offerToken = offerToken,
            pricingPhases = pricingPhases.pricingPhaseList.map { it.toPricingPhase() }
        )
    }

    private fun ProductDetails.PricingPhase.toPricingPhase(): PricingPhase {
        return PricingPhase(
            priceAmountMicros = this.priceAmountMicros,
            priceCurrencyCode = this.priceCurrencyCode,
            billingPeriod = this.billingPeriod,
            billingCycleCount = this.billingCycleCount,
            recurrenceMode = when (this.recurrenceMode) {
                1 -> RecurrenceMode.INFINITE_RECURRING
                2 -> RecurrenceMode.FINITE_RECURRING
                else -> RecurrenceMode.NON_RECURRING
            }
        )
    }

    private fun Purchase.toStorePurchase(): StorePurchase {
        return StorePurchase(
            purchaseToken = purchaseToken,
            productId = products.firstOrNull() ?: "",
            orderId = orderId,
            purchaseTime = purchaseTime,
            purchaseState = when (purchaseState) {
                Purchase.PurchaseState.PURCHASED -> PurchaseState.PURCHASED
                Purchase.PurchaseState.PENDING -> PurchaseState.PENDING
                else -> PurchaseState.UNSPECIFIED
            },
            isAcknowledged = isAcknowledged,
            isAutoRenewing = isAutoRenewing,
            originalJson = originalJson
        )
    }
}
