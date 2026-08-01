package com.ampairs.ecom.data.repository

import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.ecom.api.model.AddCartItemRequest
import com.ampairs.ecom.data.api.EcomApi
import com.ampairs.ecom.data.db.dao.CartDao
import com.ampairs.ecom.data.db.dao.ListedProductDao
import com.ampairs.ecom.data.db.entity.CartEntity
import com.ampairs.ecom.data.db.entity.CartItemEntity
import com.ampairs.ecom.domain.EcomLogger
import com.ampairs.ecom.domain.toAbsoluteImageUrl
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Offline-first cart. Building the cart (add / update / remove / clear) is a pure local Room
 * write — no network — so it works fully offline. The cart is materialised to the server only at
 * checkout ([materializeToServer]); that single call is where stock / availability validation
 * happens. Cart lines are built from the locally-mirrored catalog, so a quick-add works even with
 * no connectivity.
 *
 * Active-cart identity is derived from Room (keyed by storefront), so this repository is safe to
 * inject unscoped — no in-memory state to share across ViewModels.
 */
@Inject
class CartRepository(
    private val api: EcomApi,
    private val cartDao: CartDao,
    private val listedProductDao: ListedProductDao,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeItems(storefrontId: String): Flow<List<CartItemEntity>> =
        cartDao.observeCartForStorefront(storefrontId).flatMapLatest { cart ->
            if (cart == null) flowOf(emptyList()) else cartDao.observeItems(cart.uid)
        }.orEmitOnDbClosed(emptyList())

    fun observeCart(storefrontId: String): Flow<CartEntity?> =
        cartDao.observeCartForStorefront(storefrontId).orEmitOnDbClosed(null)

    /** Get-or-create the LOCAL cart for a storefront. Never touches the network. */
    suspend fun ensureCart(storefrontId: String): CartEntity {
        cartDao.cartForStorefront(storefrontId)?.let { return it }
        val cart = CartEntity(
            uid = UidGenerator.generateUid("CRT"),
            storefront_id = storefrontId,
            // Local placeholder until checkout materialises the cart and swaps in the server token.
            session_token = UidGenerator.generateUid("CST"),
            status = "ACTIVE",
            expires_at = null,
        )
        cartDao.upsertCart(cart)
        return cart
    }

    /**
     * Set the quantity of a product in the local cart. `quantity <= 0` removes the line.
     * The line is built from the locally-mirrored catalog, so it works offline.
     */
    suspend fun setItem(storefrontId: String, listedProductId: String, quantity: Int): Result<Unit> {
        val cart = ensureCart(storefrontId)
        if (quantity <= 0) {
            cartDao.deleteItemByProduct(cart.uid, listedProductId)
            return Result.success(Unit)
        }
        val product = listedProductDao.byId(listedProductId)
            ?: return Result.failure(IllegalStateException("Product not available"))
        val existing = cartDao.itemForProduct(cart.uid, listedProductId)
        val item = CartItemEntity(
            uid = existing?.uid ?: UidGenerator.generateUid("CRI"),
            cart_id = cart.uid,
            listed_product_id = product.uid,
            management_product_id = product.management_product_id,
            product_name = product.name,
            brand = product.brand,
            unit = product.unit,
            unit_price = product.price,
            mrp_at_add = product.mrp,
            quantity = quantity,
            primary_image_url = decodeImageUrls(product.image_urls).firstOrNull()?.toAbsoluteImageUrl(),
        )
        cartDao.upsertItem(item)
        return Result.success(Unit)
    }

    /** Remove a single line (by local item uid) from the local cart. */
    suspend fun removeItem(storefrontId: String, itemId: String): Result<Unit> {
        cartDao.deleteItemByUid(itemId)
        return Result.success(Unit)
    }

    /** Empty the local cart. */
    suspend fun clear(storefrontId: String): Result<Unit> {
        val cart = cartDao.cartForStorefront(storefrontId) ?: return Result.success(Unit)
        cartDao.clearItems(cart.uid)
        return Result.success(Unit)
    }

    /** Wipe every local cart across all storefronts. Called on logout so a new user starts clean. */
    suspend fun clearAllLocalCarts(): Result<Unit> {
        cartDao.clearAll()
        return Result.success(Unit)
    }

    /** Current local session token for a storefront (used after [materializeToServer]). */
    suspend fun sessionToken(storefrontId: String): String? =
        cartDao.cartForStorefront(storefrontId)?.session_token

    /**
     * Push the local cart to the server immediately before checkout and return the server session
     * token. The server mints a fresh cart and validates every line (stock / availability) as the
     * items are pushed — this is the single point where cart validation happens. Requires network;
     * a stock/availability failure surfaces here so the user can adjust before paying.
     */
    suspend fun materializeToServer(slug: String, storefrontId: String): Result<String> {
        val cart = cartDao.cartForStorefront(storefrontId)
            ?: return Result.failure(IllegalStateException("Cart is empty"))
        val items = cartDao.itemsForCart(cart.uid)
        if (items.isEmpty()) return Result.failure(IllegalStateException("Cart is empty"))

        val server = api.createCart(slug).getOrElse {
            EcomLogger.w("Cart", "materialize: createCart failed", it)
            return Result.failure(it)
        }
        for (item in items) {
            api.setCartItem(slug, server.sessionToken, AddCartItemRequest(item.listed_product_id, item.quantity))
                .getOrElse {
                    EcomLogger.w("Cart", "materialize: setCartItem failed for ${item.listed_product_id}", it)
                    return Result.failure(it)
                }
        }
        // Remember the validated server token so checkout operates on the server cart.
        cartDao.upsertCart(cart.copy(session_token = server.sessionToken))
        return Result.success(server.sessionToken)
    }
}
