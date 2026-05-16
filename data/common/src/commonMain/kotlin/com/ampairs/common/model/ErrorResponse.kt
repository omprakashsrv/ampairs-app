package com.ampairs.network.model

import com.ampairs.common.model.Error
import com.ampairs.common.model.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    @SerialName("error")
    var error: Error,
)

fun ErrorResponse.toResponse(): Response<Any> {
    return Response(error = Error(code = error.code, message = error.message))
}