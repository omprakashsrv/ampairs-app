package com.ampairs.common

import co.touchlab.kermit.Logger
import com.ampairs.network.model.ErrorResponse
import com.ampairs.common.model.Error
import com.ampairs.common.model.Response
import com.ampairs.network.model.toResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.content.PartData
import io.ktor.http.isSuccess

private val apiLog = Logger.withTag("KtorApiClient")

suspend inline fun <reified T> post(client: HttpClient, url: String, body: Any?): T {
    return try {
        handleResponse(client.post(url) { if (body != null) setBody(body) })
    } catch (e: NetworkException) {
        createNetworkErrorResponse(e)
    } catch (e: Exception) {
        createNetworkErrorResponse(e)
    }
}

suspend inline fun <reified E, reified T> postList(client: HttpClient, url: String, body: List<E>): T {
    return try {
        handleResponse(client.post(url) { setBody<List<E>>(body) })
    } catch (e: NetworkException) {
        createNetworkErrorResponse(e)
    } catch (e: Exception) {
        createNetworkErrorResponse(e)
    }
}

suspend inline fun <reified T> put(client: HttpClient, url: String, body: Any?): T {
    return try {
        handleResponse(client.put(url) { if (body != null) setBody(body) })
    } catch (e: NetworkException) {
        createNetworkErrorResponse(e)
    } catch (e: Exception) {
        createNetworkErrorResponse(e)
    }
}

suspend inline fun <reified T> patch(client: HttpClient, url: String, body: Any?): T {
    return try {
        handleResponse(client.patch(url) { if (body != null) setBody(body) })
    } catch (e: NetworkException) {
        createNetworkErrorResponse(e)
    } catch (e: Exception) {
        createNetworkErrorResponse(e)
    }
}

suspend inline fun <reified T> delete(client: HttpClient, url: String): T = delete(client, url, null)

suspend inline fun <reified T> delete(
    client: HttpClient,
    url: String,
    parameters: Map<String, Any>?
): T {
    return try {
        handleResponse(client.delete(url) {
            parameters?.forEach { (key, value) -> parameter(key, value) }
        })
    } catch (e: NetworkException) {
        createNetworkErrorResponse(e)
    } catch (e: Exception) {
        createNetworkErrorResponse(e)
    }
}

suspend inline fun <reified T> get(client: HttpClient, url: String): T = get(client, url, null)

suspend inline fun <reified T> get(
    client: HttpClient,
    url: String,
    parameters: Map<String, Any>?
): T {
    return try {
        handleResponse(client.get(url) {
            parameters?.forEach { (key, value) -> parameter(key, value) }
        })
    } catch (e: NetworkException) {
        createNetworkErrorResponse(e)
    } catch (e: Exception) {
        createNetworkErrorResponse(e)
    }
}

suspend inline fun <reified T> postMultiPart(
    client: HttpClient,
    url: String,
    parts: List<PartData>,
    requestTimeoutMillis: Long? = null,
): T {
    return try {
        handleResponse(client.submitFormWithBinaryData(url = url, formData = parts) {
            if (requestTimeoutMillis != null) {
                timeout { this.requestTimeoutMillis = requestTimeoutMillis }
            }
        })
    } catch (e: NetworkException) {
        createNetworkErrorResponse(e)
    } catch (e: Exception) {
        createNetworkErrorResponse(e)
    }
}

suspend inline fun <reified T> handleResponse(response: HttpResponse): T {
    return if (response.status.isSuccess()) {
        response.body<T>()
    } else {
        val errorResponse = try {
            response.body<ErrorResponse>().toResponse()
        } catch (_: Exception) {
            Response(
                error = Error(
                    code = response.status.value.toString(),
                    message = "HTTP ${response.status.value}: ${response.status.description}"
                ),
                data = null
            )
        }
        // Safe: all API call sites declare T as Response<*>
        @Suppress("UNCHECKED_CAST")
        errorResponse as T
    }
}

// Safe: all API call sites declare T as Response<*>. Non-inline: no reified needed for an unchecked cast.
@Suppress("UNCHECKED_CAST")
fun <T> createNetworkErrorResponse(exception: Exception): T {
    apiLog.e(exception) { "Network request failed" }
    return Response(
        error = Error(
            code = "NETWORK_ERROR",
            message = when {
                exception is NetworkException -> exception.message ?: "Network connection failed"
                exception::class.simpleName == "ConnectException" -> "Unable to connect to server. Please check your network connection."
                exception::class.simpleName == "UnknownHostException" -> "Server not found. Please check your network connection."
                exception::class.simpleName == "SocketTimeoutException" -> "Request timed out. Please try again."
                else -> "Network error: ${exception.message ?: "Unknown error"}"
            }
        ),
        data = null
    ) as T
}
