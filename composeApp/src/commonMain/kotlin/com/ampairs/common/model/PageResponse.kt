package com.ampairs.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PageResponse<T>(
    val content: List<T>,
    @SerialName("page_number")
    val pageNumber: Int,
    @SerialName("page_size")
    val pageSize: Int,
    @SerialName("total_pages")
    val totalPages: Int,
    @SerialName("total_elements")
    val totalElements: Long,
    @SerialName("has_next")
    val hasNext: Boolean,
    @SerialName("has_previous")
    val hasPrevious: Boolean,
    @SerialName("first")
    val first: Boolean,
    @SerialName("last")
    val last: Boolean
)