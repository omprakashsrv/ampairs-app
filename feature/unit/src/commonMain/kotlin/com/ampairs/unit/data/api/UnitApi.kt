package com.ampairs.unit.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.unit.domain.model.Unit

/**
 * API interface for Unit Management operations.
 *
 * Talks to the unified `/v1/units/sync` contract on the Spring Boot backend:
 *  - PULL via [getUnitsSync] (GET, snake_case params, includes soft-deleted rows)
 *  - PUSH via [bulkUpdateUnits] (POST, bulk upsert, soft-deletes sent in-band)
 *
 * [getUnitById] and [searchUnits] remain for UI / event-refresh use.
 */
interface UnitApi {

    /**
     * Incremental pull of units through the unified `/v1/units/sync` endpoint.
     *
     * The response INCLUDES soft-deleted (inactive) rows so the client can permanently
     * hard-delete them locally. Throws on a non-null API error.
     *
     * @param lastSync ISO 8601 checkpoint; only sent when non-blank
     * @param page Page number (0-indexed)
     * @param size Page size (default: 100)
     * @param sortBy Sort field (default: "updatedAt")
     * @param sortDir "ASC" or "DESC" (default: "ASC")
     */
    suspend fun getUnitsSync(
        lastSync: String,
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): PageResponse<Unit>

    /**
     * Bulk upsert units through the unified `/v1/units/sync` endpoint.
     *
     * Accepts active upserts AND soft-deleted (active = false) rows in the same body.
     * Returns the server-reconciled units. Throws on failure.
     */
    suspend fun bulkUpdateUnits(units: List<Unit>): List<Unit>

    /**
     * Search units by name.
     *
     * @param query Search query string
     * @param page Page number (0-indexed)
     * @param size Page size (default: 100)
     */
    suspend fun searchUnits(
        query: String,
        page: Int = 0,
        size: Int = 100,
    ): Response<PageResponse<Unit>>

    /**
     * Get single unit by ID (used by backend-event refresh).
     */
    suspend fun getUnitById(id: String): Response<Unit>
}
