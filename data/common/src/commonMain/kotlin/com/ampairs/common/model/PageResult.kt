package com.ampairs.common.model

/**
 * Generic pagination result wrapper that matches Spring Boot Page structure
 * This can be reused across all modules that need paginated data
 * 
 * @param T The type of content being paginated
 */
data class PageResult<T>(
    val content: List<T>,
    val totalElements: Int,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val isFirst: Boolean,
    val isLast: Boolean,
    val isEmpty: Boolean,
)