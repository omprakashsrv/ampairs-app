package com.ampairs.ecom.data.api

import com.ampairs.ecom.api.model.AddCartItemRequest
import com.ampairs.ecom.api.model.AddressRequest
import com.ampairs.ecom.api.model.AddressResponse
import com.ampairs.ecom.api.model.CartResponse
import com.ampairs.ecom.api.model.CatalogMeta
import com.ampairs.ecom.api.model.CheckoutRequest
import com.ampairs.ecom.api.model.DistributorAccount
import com.ampairs.ecom.api.model.EcomOrderResponse
import com.ampairs.ecom.api.model.LinkCandidateResponse
import com.ampairs.ecom.api.model.ListedProduct
import com.ampairs.ecom.api.model.ManagedStorefront
import com.ampairs.ecom.api.model.PageResponse
import com.ampairs.ecom.api.model.ProductSyncPage
import com.ampairs.ecom.api.model.StoreAccessResponse
import com.ampairs.ecom.api.model.Storefront
import com.ampairs.ecom.api.model.StorefrontCreateRequest
import com.ampairs.ecom.api.model.StorefrontUpdateRequest

/**
 * Network surface for the ecom storefront. Public storefront endpoints are slug-scoped;
 * account endpoints require auth (handled transparently by the shared bearer client).
 *
 * All methods return [Result] — callers null-check / fold; no exceptions bubble to UI.
 */
interface EcomApi {

    // ── Storefront directory (public discovery — common multi-store app) ──
    /** Lists published storefronts for the directory/picker. `q` filters by name/slug when non-null. */
    suspend fun listStorefronts(q: String? = null, page: Int = 0, size: Int = 20): Result<PageResponse<Storefront>>

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

    // ── Distributor link (auth) ──
    /** A CRM account matching the buyer's own phone in this workspace, or `null` when there's no match. */
    suspend fun getLinkCandidate(slug: String): Result<LinkCandidateResponse?>
    /** Commits a candidate from [getLinkCandidate]; the server re-validates the phone match. */
    suspend fun confirmLink(slug: String, customerId: String): Result<DistributorAccount>

    // ── Store access gate (auth, future API — see plan §11.1) ──
    suspend fun requestStoreAccess(slug: String): Result<StoreAccessResponse>
    suspend fun getStoreAccess(slug: String): Result<StoreAccessResponse>

    // ── Merchant storefront management (auth, workspace-scoped via X-Workspace-ID) ──
    /** The workspace's storefront, or `null` when none has been created yet (server 404 NOT_FOUND). */
    suspend fun getMyStorefront(): Result<ManagedStorefront?>
    suspend fun createStorefront(request: StorefrontCreateRequest): Result<ManagedStorefront>
    suspend fun updateStorefront(request: StorefrontUpdateRequest): Result<ManagedStorefront>
    suspend fun publishStorefront(): Result<ManagedStorefront>
    suspend fun unpublishStorefront(): Result<ManagedStorefront>
}
