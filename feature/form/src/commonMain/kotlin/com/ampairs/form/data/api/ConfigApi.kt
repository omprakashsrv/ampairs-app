package com.ampairs.form.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.form.domain.FormSchema

/**
 * Form configuration API — the single aggregate `/config/schema/sync` feed (spec 011). One record per
 * entityType. The old two-feed endpoints (field-configs/attribute-definitions) were removed.
 *
 * NOTE (spec 011): drafted, compile-gate on device.
 */
interface ConfigApi {

    /** Pull: aggregates changed at/after [lastSync] (full feed when blank). */
    suspend fun getSchemaSync(
        lastSync: String,
        page: Int,
        size: Int,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): PageResponse<FormSchema>

    /** Push: replace-aggregate (server upserts present members, deletes absent). Returns resolved. */
    suspend fun pushSchema(schemas: List<FormSchema>): List<FormSchema>

    /** UI read: the aggregate for one entityType (seeds defaults server-side on first access). */
    suspend fun getConfigSchema(entityType: String): FormSchema
}
