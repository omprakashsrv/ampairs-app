package com.ampairs.ecom.data.api

import com.ampairs.ecom.api.model.AddCartItemRequest
import com.ampairs.ecom.api.model.AddressRequest
import com.ampairs.ecom.api.model.AddressResponse
import com.ampairs.ecom.api.model.CartResponse
import com.ampairs.ecom.api.model.CatalogMeta
import com.ampairs.ecom.api.model.CheckoutRequest
import com.ampairs.ecom.api.model.EcomOrderResponse
import com.ampairs.ecom.api.model.ListedProduct
import com.ampairs.ecom.api.model.PageResponse
import com.ampairs.ecom.api.model.ProductSyncPage
import com.ampairs.ecom.api.model.StoreAccessResponse
import com.ampairs.ecom.api.model.Storefront

/**
 * Network surface for the ecom storefront. Public storefront endpoints are slug-scoped;
 * account endpoints require auth (handled transparently by the shared bearer client).
 *
 * All methods return [Result] — callers null-check / fold; no exceptions bubble to UI.
 */
interface EcomApi {

    // ── Storefront bootstrap & catalog (public) ──
    suspend fun getStorefront(slug: String): Result<Storefront>
    suspend fun getCatalogMeta(slug: String): Result<CatalogMeta>
    suspend fun getProducts(
        slug: String,
        page: Int = 0,
        size: Int = 20,
        category: String? = null,
        subcategory: String? = null,
        brand: String? = null,
    ): Result<PageResponse<ListedProduct>>
    suspend fun searchProducts(slug: String, query: String, page: Int = 0, size: Int = 20): Result<PageResponse<ListedProduct>>
    suspend fun getProduct(slug: String, productId: String): Result<ListedProduct>
    suspend fun syncProducts(slug: String, since: String, page: Int = 0, size: Int = 100): Result<ProductSyncPage>

    // ── Cart (session-based, public) ──
    suspend fun createCart(slug: String): Result<CartResponse>
    suspend fun getCart(slug: String, sessionToken: String): Result<CartResponse>
    suspend fun setCartItem(slug: String, sessionToken: String, request: AddCartItemRequest): Result<CartResponse>
    suspend fun removeCartItem(slug: String, sessionToken: String, itemId: String): Result<CartResponse>
    suspend fun clearCart(slug: String, sessionToken: String): Result<CartResponse>
    suspend fun claimCart(slug: String, sessionToken: String): Result<CartResponse>

    // ── Checkout (auth) ──
    suspend fun checkout(slug: String, sessionToken: String, request: CheckoutRequest): Result<EcomOrderResponse>

    // ── Account: addresses (auth) ──
    suspend fun getAddresses(): Result<List<AddressResponse>>
    suspend fun createAddress(request: AddressRequest): Result<AddressResponse>
    suspend fun updateAddress(addressId: String, request: AddressRequest): Result<AddressResponse>
    suspend fun deleteAddress(addressId: String): Result<Unit>

    // ── Account: orders (auth) ──
    suspend fun getOrders(slug: String, page: Int = 0, size: Int = 20): Result<PageResponse<EcomOrderResponse>>
    suspend fun getOrder(slug: String, ecomOrderRef: String): Result<EcomOrderResponse>

    // ── Store access gate (auth, future API — see plan §11.1) ──
    suspend fun requestStoreAccess(slug: String): Result<StoreAccessResponse>
    suspend fun getStoreAccess(slug: String): Result<StoreAccessResponse>
}
