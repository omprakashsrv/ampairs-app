package com.ampairs.product.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.product.api.model.ProductStandardCostApiModel

/**
 * Canonical `/sync` API for effective-dated standard purchase costs
 * (`GET/POST /product/v1/standard-costs/sync`). The pull feed includes soft-deleted rows; push is a
 * UID-keyed bulk upsert (deletes ride in-band as `active = false` rows). Lives in the SyncDelegate
 * layer — never injected into a repository.
 */
interface ProductStandardCostApi {

    /** Paginated pull feed — includes soft-deleted rows so deletions propagate. [lastSync] sent only when non-blank. */
    suspend fun pull(
        lastSync: String?,
        page: Int,
        size: Int,
    ): Result<PageResponse<ProductStandardCostApiModel>>

    /** Bulk upsert — send all unsynced rows (active upserts AND soft-deletes) in one request. */
    suspend fun push(models: List<ProductStandardCostApiModel>): Result<List<ProductStandardCostApiModel>>
}
