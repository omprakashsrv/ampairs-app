package com.ampairs.unit.domain.model

/**
 * Key for Store5 caching and pagination
 *
 * Used to uniquely identify different data requests in the Store5 cache
 */
data class UnitKey(
    val searchQuery: String = "",
    val page: Int = 0,
    val size: Int = 100
)
