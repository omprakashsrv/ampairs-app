package com.ampairs.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Error(
    @SerialName("code")
    val code: String = "0",
    @SerialName("message")
    val message: String = "",
    @SerialName("details")
    val details: String? = null,
) {
    /** The specific reason when the backend provides one (e.g. "No app account found with
     * phone X"), falling back to the generic category [message] (e.g. "Invalid customer data"). */
    val displayMessage: String get() = details?.takeIf { it.isNotBlank() } ?: message
}