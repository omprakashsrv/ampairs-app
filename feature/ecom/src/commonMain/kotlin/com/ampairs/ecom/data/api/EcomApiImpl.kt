package com.ampairs.ecom.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.delete
import com.ampairs.common.di.AppScope
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.common.put
import com.ampairs.ecom.api.model.AddCartItemRequest
import com.ampairs.ecom.api.model.AddressRequest
import com.ampairs.ecom.api.model.AddressResponse
import com.ampairs.ecom.api.model.CartResponse
import com.ampairs.ecom.api.model.CatalogMeta
import com.ampairs.ecom.api.model.CheckoutRequest
import com.ampairs.ecom.api.model.EcomOrderResponse
import com.ampairs.ecom.api.model.ListedProduct
import com.ampairs.ecom.api.model.ManagedStorefront
import com.ampairs.ecom.api.model.PageResponse
import com.ampairs.ecom.api.model.ProductSyncPage
import com.ampairs.ecom.api.model.StoreAccessRequest
import com.ampairs.ecom.api.model.StoreAccessResponse
import com.ampairs.ecom.api.model.Storefront
import com.ampairs.ecom.api.model.StorefrontCreateRequest
import com.ampairs.ecom.api.model.StorefrontUpdateRequest
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class EcomApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : EcomApi {

    private val client = httpClient(engine, tokenRepository)

    // ── Storefront bootstrap & catalog ──

    override suspend fun getStorefront(slug: String): Result<Storefront> = call {
        get<Response<Storefront>>(client, ApiUrlBuilder.storeUrl(slug))
    }

    override suspend fun getCatalogMeta(slug: String): Result<CatalogMeta> = call {
        get<Response<CatalogMeta>>(client, ApiUrlBuilder.storeUrl(slug, "catalog-meta"))
    }

    override suspend fun getProducts(
        slug: String,
        page: Int,
        size: Int,
        category: String?,
        subcategory: String?,
        brand: String?,
    ): Result<PageResponse<ListedProduct>> = call {
        val params = buildMap<String, Any> {
            put("page", page); put("size", size)
            category?.let { put("category", it) }
            subcategory?.let { put("subcategory", it) }
            brand?.let { put("brand", it) }
        }
        get<Response<PageResponse<ListedProduct>>>(client, ApiUrlBuilder.storeUrl(slug, "products"), params)
    }

    override suspend fun searchProducts(slug: String, query: String, page: Int, size: Int): Result<PageResponse<ListedProduct>> = call {
        val params = mapOf<String, Any>("q" to query, "page" to page, "size" to size)
        get<Response<PageResponse<ListedProduct>>>(client, ApiUrlBuilder.storeUrl(slug, "products/search"), params)
    }

    override suspend fun getProduct(slug: String, productId: String): Result<ListedProduct> = call {
        get<Response<ListedProduct>>(client, ApiUrlBuilder.storeUrl(slug, "products/$productId"))
    }

    override suspend fun syncProducts(slug: String, since: String, page: Int, size: Int): Result<ProductSyncPage> = call {
        val params = mapOf<String, Any>("since" to since, "page" to page, "size" to size)
        get<Response<ProductSyncPage>>(client, ApiUrlBuilder.storeUrl(slug, "products/sync"), params)
    }

    // ── Cart ──

    override suspend fun createCart(slug: String): Result<CartResponse> = call {
        post<Response<CartResponse>>(client, ApiUrlBuilder.storeUrl(slug, "cart"), null)
    }

    override suspend fun getCart(slug: String, sessionToken: String): Result<CartResponse> = call {
        get<Response<CartResponse>>(client, ApiUrlBuilder.storeUrl(slug, "cart/$sessionToken"))
    }

    override suspend fun setCartItem(slug: String, sessionToken: String, request: AddCartItemRequest): Result<CartResponse> = call {
        post<Response<CartResponse>>(client, ApiUrlBuilder.storeUrl(slug, "cart/$sessionToken/items"), request)
    }

    override suspend fun removeCartItem(slug: String, sessionToken: String, itemId: String): Result<CartResponse> = call {
        delete<Response<CartResponse>>(client, ApiUrlBuilder.storeUrl(slug, "cart/$sessionToken/items/$itemId"))
    }

    override suspend fun clearCart(slug: String, sessionToken: String): Result<CartResponse> = call {
        delete<Response<CartResponse>>(client, ApiUrlBuilder.storeUrl(slug, "cart/$sessionToken"))
    }

    override suspend fun claimCart(slug: String, sessionToken: String): Result<CartResponse> = call {
        post<Response<CartResponse>>(client, ApiUrlBuilder.storeUrl(slug, "cart/$sessionToken/claim"), null)
    }

    // ── Checkout ──

    override suspend fun checkout(slug: String, sessionToken: String, request: CheckoutRequest): Result<EcomOrderResponse> = call {
        post<Response<EcomOrderResponse>>(client, ApiUrlBuilder.storeUrl(slug, "cart/$sessionToken/checkout"), request)
    }

    // ── Account: addresses ──

    override suspend fun getAddresses(): Result<List<AddressResponse>> = call {
        get<Response<List<AddressResponse>>>(client, ApiUrlBuilder.ecomUrl("account/addresses"))
    }

    override suspend fun createAddress(request: AddressRequest): Result<AddressResponse> = call {
        post<Response<AddressResponse>>(client, ApiUrlBuilder.ecomUrl("account/addresses"), request)
    }

    override suspend fun updateAddress(addressId: String, request: AddressRequest): Result<AddressResponse> = call {
        put<Response<AddressResponse>>(client, ApiUrlBuilder.ecomUrl("account/addresses/$addressId"), request)
    }

    override suspend fun deleteAddress(addressId: String): Result<Unit> = try {
        // DELETE returns 204 No Content — check status directly rather than parsing an empty body.
        // (Use request{} with an explicit method to avoid an empty-body deserialization failure.)
        val response = client.request(ApiUrlBuilder.ecomUrl("account/addresses/$addressId")) {
            method = HttpMethod.Delete
        }
        if (response.status.isSuccess()) Result.success(Unit)
        else Result.failure(Exception("HTTP ${response.status.value}: ${response.status.description}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Account: orders ──

    override suspend fun getOrders(slug: String, page: Int, size: Int): Result<PageResponse<EcomOrderResponse>> = call {
        val params = mapOf<String, Any>("storefront_slug" to slug, "page" to page, "size" to size)
        get<Response<PageResponse<EcomOrderResponse>>>(client, ApiUrlBuilder.ecomUrl("account/orders"), params)
    }

    override suspend fun getOrder(slug: String, ecomOrderRef: String): Result<EcomOrderResponse> = call {
        val params = mapOf<String, Any>("storefront_slug" to slug)
        get<Response<EcomOrderResponse>>(client, ApiUrlBuilder.ecomUrl("account/orders/$ecomOrderRef"), params)
    }

    // ── Store access gate (future API — see plan §11.1) ──

    override suspend fun requestStoreAccess(slug: String): Result<StoreAccessResponse> = call {
        post<Response<StoreAccessResponse>>(client, ApiUrlBuilder.ecomUrl("account/store-access"), StoreAccessRequest(slug))
    }

    override suspend fun getStoreAccess(slug: String): Result<StoreAccessResponse> = call {
        get<Response<StoreAccessResponse>>(client, ApiUrlBuilder.ecomUrl("account/store-access/$slug"))
    }

    // ── Merchant storefront management ──

    override suspend fun getMyStorefront(): Result<ManagedStorefront?> = try {
        val response = get<Response<ManagedStorefront>>(client, ApiUrlBuilder.ecomUrl("management/storefront"))
        when {
            response.data != null -> Result.success(response.data)
            // No storefront created yet — distinguish 404 NOT_FOUND from a real error.
            response.error?.code == "NOT_FOUND" -> Result.success(null)
            else -> Result.failure(Exception(response.error?.message?.ifBlank { "Failed to load storefront" } ?: "Failed to load storefront"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createStorefront(request: StorefrontCreateRequest): Result<ManagedStorefront> = call {
        post<Response<ManagedStorefront>>(client, ApiUrlBuilder.ecomUrl("management/storefront"), request)
    }

    override suspend fun updateStorefront(request: StorefrontUpdateRequest): Result<ManagedStorefront> = call {
        put<Response<ManagedStorefront>>(client, ApiUrlBuilder.ecomUrl("management/storefront"), request)
    }

    override suspend fun publishStorefront(): Result<ManagedStorefront> = call {
        put<Response<ManagedStorefront>>(client, ApiUrlBuilder.ecomUrl("management/storefront/publish"), null)
    }

    override suspend fun unpublishStorefront(): Result<ManagedStorefront> = call {
        put<Response<ManagedStorefront>>(client, ApiUrlBuilder.ecomUrl("management/storefront/unpublish"), null)
    }

    /** Shared Response<T> → Result<T> mapping; no exceptions reach the UI layer. */
    private inline fun <T> call(block: () -> Response<T>): Result<T> = try {
        val response = block()
        if (response.data != null && response.error == null) {
            Result.success(response.data!!)
        } else {
            Result.failure(Exception(response.error?.message?.ifBlank { "Server returned no data" } ?: "Server returned no data"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
