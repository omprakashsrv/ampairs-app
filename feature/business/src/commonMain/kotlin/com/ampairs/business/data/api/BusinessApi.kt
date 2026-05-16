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

    // ==================== Logo Endpoints ====================

    /**
     * Upload business logo (max 10MB, JPEG/PNG/WebP)
     * POST /api/v1/business/logo
     */
    suspend fun uploadLogo(
        imageData: ByteArray,
        fileName: String,
        contentType: String
    ): Result<Business>

    /**
     * Delete business logo
     * DELETE /api/v1/business/logo
     */
    suspend fun deleteLogo(): Result<Business>

    // ==================== Gallery Image Endpoints ====================

    /**
     * Upload gallery image (max 20 images per business)
     * POST /api/v1/business/images
     */
    suspend fun uploadImage(
        imageData: ByteArray,
        fileName: String,
        contentType: String,
        imageType: BusinessImageType = BusinessImageType.GALLERY,
        title: String? = null,
        description: String? = null,
        altText: String? = null,
        isPrimary: Boolean = false
    ): Result<BusinessImage>

    /**
     * Get all business images
     * GET /api/v1/business/images
     */
    suspend fun getImages(imageType: BusinessImageType? = null): Result<List<BusinessImage>>

    /**
     * Get image details
     * GET /api/v1/business/images/{imageUid}
     */
    suspend fun getImageDetails(imageUid: String): Result<BusinessImage>

    /**
     * Update image metadata
     * PUT /api/v1/business/images/{imageUid}
     */
    suspend fun updateImage(imageUid: String, request: UpdateBusinessImageRequest): Result<BusinessImage>

    /**
     * Set image as primary
     * POST /api/v1/business/images/{imageUid}/set-primary
     */
    suspend fun setImageAsPrimary(imageUid: String): Result<BusinessImage>

    /**
     * Reorder images
     * POST /api/v1/business/images/reorder
     */
    suspend fun reorderImages(imageUids: List<String>): Result<Unit>

    /**
     * Delete image
     * DELETE /api/v1/business/images/{imageUid}
     */
    suspend fun deleteImage(imageUid: String): Result<Unit>
}
