package com.ampairs.customer.domain

import kotlinx.serialization.Serializable

@Serializable
data class CustomerTypeKey(
    val page: Int = 0,
    val size: Int = 100,
    val searchQuery: String = ""
)