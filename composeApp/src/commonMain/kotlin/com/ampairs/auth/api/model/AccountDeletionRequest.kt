package com.ampairs.auth.api.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountDeletionRequest(
    val confirmed: Boolean,
    val reason: String? = null
)
