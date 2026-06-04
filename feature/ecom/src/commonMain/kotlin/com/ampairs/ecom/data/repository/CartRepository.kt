package com.ampairs.ecom.data.repository

import com.ampairs.ecom.api.model.AddCartItemRequest
import com.ampairs.ecom.api.model.CartResponse
import com.ampairs.ecom.data.api.EcomApi
import com.ampairs.ecom.data.db.dao.CartDao
import com.ampairs.ecom.data.db.entity.CartItemEntity
import com.ampairs.ecom.domain.EcomLogger
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Online cart with an optimistic local Room mirror (contract §9 — cart is online-only).
 * Server is authoritative for quantities/stock caps; every successful call replaces the mirror.
 *
 * Active-cart identity is derived from Room (keyed by storefront), so this repository is safe
 * to inject unscoped — no in-memory state to share across ViewModels.
 */
@Inject
class CartRepository(
    private val api: EcomApi,
    private val cartDao: CartDao,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeItems(storefrontId: String): Flow<List<CartItemEntity>> =
        cartDao.observeCartForStorefront(storefrontId).flatMapLatest { cart ->
            if (cart == null) flowOf(emptyList()) else cartDao.observeItems(cart.uid)
        }

    fun observeCart(storefrontId: String): Flow<com.ampairs.ecom.data.db.entity.CartEntity?> =
        cartDao.observeCartForStorefront(storefrontId)

    /** Get-or-create the cart for a storefront, returning the live server cart. */
    suspend fun ensureCart(slug: String, storefrontId: String): Result<CartResponse> {
        val local = cartDao.cartForStorefront(storefrontId)
        val result = if (local != null) api.getCart(slug, local.session_token) else api.createCart(slug)
        return result.onSuccess { persist(storefrontId, it) }
            .onFailure { EcomLogger.w("Cart", "ensureCart failed", it) }
    }

    suspend fun setItem(slug: String, storefrontId: String, listedProductId: String, quantity: Int): Result<CartResponse> {
        val token = cartDao.cartForStorefront(storefrontId)?.session_token
            ?: return Result.failure(IllegalStateException("No active cart"))
        return api.setCartItem(slug, token, AddCartItemRequest(listedProductId, quantity))
            .onSuccess { persist(storefrontId, it) }
    }

    suspend fun removeItem(slug: String, storefrontId: String, itemId: String): Result<CartResponse> {
        val token = cartDao.cartForStorefront(storefrontId)?.session_token
            ?: return Result.failure(IllegalStateException("No active cart"))
        return api.removeCartItem(slug, token, itemId).onSuccess { persist(storefrontId, it) }
    }

    suspend fun clear(slug: String, storefrontId: String): Result<CartResponse> {
        val token = cartDao.cartForStorefront(storefrontId)?.session_token
            ?: return Result.failure(IllegalStateException("No active cart"))
        return api.clearCart(slug, token).onSuccess { persist(storefrontId, it) }
    }

    /** After login: merge the guest cart into the authenticated customer cart. */
    suspend fun claim(slug: String, storefrontId: String): Result<CartResponse> {
        val token = cartDao.cartForStorefront(storefrontId)?.session_token
            ?: return Result.failure(IllegalStateException("No active cart"))
        return api.claimCart(slug, token).onSuccess { persist(storefrontId, it) }
    }

    /** Current session token for a storefront (needed by checkout). */
    suspend fun sessionToken(storefrontId: String): String? =
        cartDao.cartForStorefront(storefrontId)?.session_token

    private suspend fun persist(storefrontId: String, cart: CartResponse) {
        cartDao.upsertCart(cart.toCartEntity(storefrontId))
        cartDao.replaceItems(cart.uid, cart.items.map { it.toEntity(cart.uid) })
    }
}
