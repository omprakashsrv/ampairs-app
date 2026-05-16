package com.ampairs.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Response<T>(
    @SerialName("data") var data: T? = null,
    @SerialName("error") var error: Error? = null,
)


inline fun <T> Response<T>.onSuccess(
    crossinline onResult: T.() -> Unit,
): Response<T> {
    if (this.data != null) {
        onResult(this.data!!)
    }
    return this
}

inline fun <T> Response<T>.onError(
    crossinline onResult: Error.() -> Unit,
): Response<T> {
    if (this.error != null || this.data == null) {
        onResult(this.error ?: Error())
    }
    return this
}

