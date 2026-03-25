package com.ampairs.customer.data.api

import com.ampairs.customer.domain.CustomerImage
import com.ampairs.customer.domain.CustomerImageUploadRequest
import com.ampairs.customer.domain.CustomerImageUploadResponse
import com.ampairs.customer.domain.CustomerImageUpdateRequest
import com.ampairs.customer.domain.CustomerImageBulkRequest
import com.ampairs.customer.domain.ThumbnailResponse

/**
 * API interface for customer image operations.
 * Provides REST endpoints for image upload, download, management, and bulk operations.
 */
interface CustomerImageApi {

    // Image CRUD operations

    /**
     * Get customer images with synchronization support.
     */
    suspend fun getCustomerImages(
        customerId: String,
        lastSync: String = ""
    ): List<CustomerImage>

    /**
     * Get all customer images for sync (without pagination).
     */
    suspend fun getAllCustomerImages(
        customerId: String,
        lastSync: String = ""
    ): List<CustomerImage>

    /**
     * Get a specific customer image by ID.
     */
    suspend fun getCustomerImage(customerId: String, imageId: String): CustomerImage?

    /**
     * Initiate image upload and get presigned URL.
     */
    suspend fun uploadCustomerImage(request: CustomerImageUploadRequest): CustomerImageUploadResponse

    /**
     * Upload customer image using multipart form data.
     */
    suspend fun uploadCustomerImageMultipart(
        uid: String,
        customerId: String,
        fileName: String,
        contentType: String,
        imageData: ByteArray,
        description: String? = null,
        isPrimary: Boolean = false,
        displayOrder: Int? = null
    ): CustomerImageUploadResponse

    /**
     * Upload image file to presigned URL.
     */
    suspend fun uploadImageFile(uploadUrl: String, imageData: ByteArray, contentType: String): Boolean

    /**
     * Update customer image metadata.
     */
    suspend fun updateCustomerImage(customerId: String, imageId: String, request: CustomerImageUpdateRequest): CustomerImage

    /**
     * Delete a customer image.
     */
    suspend fun deleteCustomerImage(customerId: String, imageId: String)

    /**
     * Set image as primary for customer.
     */
    suspend fun setPrimaryImage(customerId: String, imageId: String): CustomerImage

    // Thumbnail operations

    /**
     * Get thumbnail URL for specific size.
     */
    suspend fun getThumbnail(customerId: String, imageId: String, size: String = "150"): ThumbnailResponse

    /**
     * Get multiple thumbnail sizes for an image.
     */
    suspend fun getThumbnails(customerId: String, imageId: String, sizes: List<String> = listOf("150", "300", "500")): List<ThumbnailResponse>

    // Bulk operations

    /**
     * Perform bulk operations on multiple images.
     */
    suspend fun bulkOperation(request: CustomerImageBulkRequest): List<CustomerImage>

    /**
     * Delete multiple images.
     */
    suspend fun deleteMultipleImages(imageIds: List<String>)

    /**
     * Reorder multiple images.
     */
    suspend fun reorderImages(imageIds: List<String>, sortOrders: List<Int>): List<CustomerImage>

    // Customer-level operations

    /**
     * Get primary image for a customer.
     */
    suspend fun getPrimaryImage(customerId: String): CustomerImage?

    /**
     * Get customer images count.
     */
    suspend fun getCustomerImagesCount(customerId: String): Int

    /**
     * Delete all images for a customer.
     */
    suspend fun deleteAllCustomerImages(customerId: String)

    // Download operations

    /**
     * Get download URL for full-size image.
     */
    suspend fun getDownloadUrl(customerId: String, imageId: String): String

    /**
     * Download image data.
     */
    suspend fun downloadImage(imageUrl: String): ByteArray

    /**
     * Download thumbnail image data.
     */
    suspend fun downloadThumbnail(thumbnailUrl: String): ByteArray
}