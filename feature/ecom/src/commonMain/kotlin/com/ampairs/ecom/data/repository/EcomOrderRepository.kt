package com.ampairs.ecom.data.repository

import com.ampairs.ecom.api.model.CheckoutRequest
import com.ampairs.ecom.api.model.EcomOrderResponse
import com.ampairs.ecom.data.api.EcomApi
import com.ampairs.ecom.data.db.dao.EcomOrderDao
import com.ampairs.ecom.data.db.entity.EcomOrderEntity
import com.ampairs.ecom.data.db.entity.EcomOrderLineItemEntity
import com.ampairs.ecom.domain.EcomLogger
import com.ampairs.ecom.domain.EcomSession
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Order history cache + checkout. Orders are placed via the live checkout call (not push-sync);
 * [pushPendingToServer] is therefore a no-op. Pull refreshes the per-storefront history (contract §7).
 */
@Inject
class EcomOrderRepository(
    private val api: EcomApi,
    private val orderDao: EcomOrderDao,
    private val session: EcomSession,
) {
    fun observeOrders(storefrontId: String): Flow<List<EcomOrderEntity>> =
        orderDao.observeForStorefront(storefrontId).orEmitOnDbClosed(emptyList())

    fun observeOrder(ref: String): Flow<EcomOrderEntity?> =
        orderDao.observeByRef(ref).orEmitOnDbClosed(null)

    fun observeLineItems(orderUid: String): Flow<List<EcomOrderLineItemEntity>> =
        orderDao.observeLineItems(orderUid).orEmitOnDbClosed(emptyList())

    /** Place the order. On success, cache it locally and return the response. */
    suspend fun checkout(slug: String, sessionToken: String, request: CheckoutRequest): Result<EcomOrderResponse> {
        val result = api.checkout(slug, sessionToken, request)
        result.onSuccess { cache(it) }.onFailure { EcomLogger.w("Order", "checkout failed", it) }
        return result
    }

    suspend fun refreshOrder(slug: String, ref: String): Result<EcomOrderResponse> {
        val result = api.getOrder(slug, ref)
        result.onSuccess { cache(it) }
        return result
    }

    suspend fun pullFromServer(): Result<Int> {
        val slug = session.activeSlug ?: return Result.success(0)
        val result = api.getOrders(slug, page = 0, size = 50)
        return result.fold(
            onSuccess = { page ->
                page.content.forEach { cache(it) }
                Result.success(page.content.size)
            },
            onFailure = {
                EcomLogger.w("Order", "pull failed", it)
                Result.failure(it)
            },
        )
    }

    /** Orders are created via checkout, never pushed from local. */
    suspend fun pushPendingToServer(): Result<Int> = Result.success(0)

    private suspend fun cache(order: EcomOrderResponse) {
        orderDao.upsertWithItems(
            order.toEntity(),
            order.lineItems.map { it.toEntity(order.uid) },
        )
    }
}
