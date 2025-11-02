package com.ampairs.customer.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response DTO for customer image list endpoint.
 * Matches the backend CustomerImageListResponse structure.
 */
@Serializable
data class CustomerImageListResponse(
    @SerialName("images")
    val images: List<CustomerImage> = emptyList(),

    @SerialName("total_count")
    val totalCount: Int = 0,

    @SerialName("primary_image")
    val primaryImage: CustomerImage? = null,

    @SerialName("total_size")
    val totalSize: Long = 0L,

    @SerialName("formatted_total_size")
    val formattedTotalSize: String = "0 B"
)