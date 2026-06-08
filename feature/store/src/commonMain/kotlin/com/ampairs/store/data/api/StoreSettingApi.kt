package com.ampairs.store.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.store.domain.model.StoreSetting

/**
 * Bulk-sync API for store settings. Only pull (incremental list) and push (bulk upsert) — no
 * per-record CRUD; the backend mirrors this offline-first contract.
 */
interface StoreSettingApi {

    /** Incremental pull. [lastSync] is an ISO-8601 instant; null fetches a full snapshot. */
    suspend fun pull(
        page: Int = 0,
        size: Int = 100,
        lastSync: String? = null,
    ): Response<PageResponse<StoreSetting>>

    /** Bulk upsert (and soft-delete via active=false) of setting overrides. */
    suspend fun push(settings: List<StoreSetting>): Response<List<StoreSetting>>
}
