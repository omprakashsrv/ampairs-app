package com.ampairs.business.data.api

import com.ampairs.business.domain.*

/**
 * Business Management API interface.
 * Maps to backend endpoints at /api/v1/business
 *
 * Backend uses unified endpoint - all updates go through single PUT endpoint
 */
interface BusinessApi {
    // Main unified endpoints (matches backend BusinessController)
    suspend fun getBusiness(): Result<Business>
    suspend fun updateBusiness(request: BusinessPayload): Result<Business>
    suspend fun createBusiness(request: BusinessPayload): Result<Business>

    // Dashboard overview (optimized for performance)
    suspend fun getBusinessOverview(): Result<BusinessOverview>

    // Utility
    suspend fun checkBusinessExists(): Result<Boolean>
}
