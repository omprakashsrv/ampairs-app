package com.ampairs.customer.domain

import kotlinx.serialization.Serializable

@Serializable
data class StateKey(
    val workspaceId: String = "default"
)