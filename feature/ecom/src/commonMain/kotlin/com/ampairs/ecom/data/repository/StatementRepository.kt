package com.ampairs.ecom.data.repository

import com.ampairs.ecom.api.model.BuyerOutstanding
import com.ampairs.ecom.api.model.BuyerStatement
import com.ampairs.ecom.data.api.EcomApi
import com.ampairs.ecom.domain.EcomLogger
import com.ampairs.ecom.domain.EcomSession
import dev.zacsweers.metro.Inject

/**
 * Spec 029 — buyer money position: current outstanding (+ open bills, aging) and the running
 * account statement. Live, UI-invoked reads scoped to the active storefront; link-gated server-side.
 */
@Inject
class StatementRepository(
    private val api: EcomApi,
    private val session: EcomSession,
) {
    suspend fun getOutstanding(): Result<BuyerOutstanding> {
        val slug = session.activeSlug ?: return Result.failure(IllegalStateException("No active storefront"))
        return api.getOutstanding(slug).onFailure { EcomLogger.w("Statement", "outstanding failed", it) }
    }

    suspend fun getStatement(from: String? = null, to: String? = null): Result<BuyerStatement> {
        val slug = session.activeSlug ?: return Result.failure(IllegalStateException("No active storefront"))
        return api.getStatement(slug, from = from, to = to).onFailure { EcomLogger.w("Statement", "statement failed", it) }
    }
}
