package com.ampairs.subscription.billing

interface StoreKitBridgeApi {
    fun loadProducts(productIds: List<String>, completion: (Map<String, Map<String, Any>>?, String?) -> Unit)
    fun purchase(productId: String, completion: (Map<String, Any>?, String?) -> Unit)
    fun queryPurchases(completion: (List<Map<String, Any>>?, String?) -> Unit)
    fun restorePurchases(completion: (Boolean, String?) -> Unit)
    fun setPurchaseCallback(callback: (String, String?, String?) -> Unit)
    fun stopTransactionListener()
}
