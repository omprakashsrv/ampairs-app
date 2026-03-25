package com.ampairs.unit.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.unit.domain.model.Unit

/**
 * API interface for Unit Management operations
 *
 * Communicates with Spring Boot backend for unit synchronization
 */
interface UnitApi {

    /**
     * Get paginated list of units
     *
     * @param page Page number (0-indexed)
     * @param size Page size (default: 100)
     * @param lastSyncTime ISO 8601 timestamp for incremental sync (optional)
     * @param sortBy Sort field (default: "name")
     * @param sortDirection "ASC" or "DESC" (default: "ASC")
     * @return Paginated response with unit list
     */
    suspend fun getUnits(
        page: Int = 0,
        size: Int = 100,
        lastSyncTime: String? = null,
        sortBy: String = "name",
        sortDirection: String = "ASC"
    ): Response<PageResponse<Unit>>

    /**
     * Search units by name
     *
     * @param query Search query string
     * @param page Page number (0-indexed)
     * @param size Page size (default: 100)
     * @return Paginated response with matching units
     */
    suspend fun searchUnits(
        query: String,
        page: Int = 0,
        size: Int = 100
    ): Response<PageResponse<Unit>>

    /**
     * Get single unit by ID
     *
     * @param id Unit UID
     * @return Unit details
     */
    suspend fun getUnitById(id: String): Response<Unit>

    /**
     * Create a new unit
     *
     * @param unit Unit to create
     * @return Created unit with server-generated metadata
     */
    suspend fun createUnit(unit: Unit): Response<Unit>

    /**
     * Update an existing unit
     *
     * @param id Unit UID to update
     * @param unit Updated unit data
     * @return Updated unit
     */
    suspend fun updateUnit(id: String, unit: Unit): Response<Unit>

    /**
     * Delete a unit
     *
     * @param id Unit UID to delete
     * @return Success response
     */
    suspend fun deleteUnit(id: String): Response<kotlin.Unit>
}
