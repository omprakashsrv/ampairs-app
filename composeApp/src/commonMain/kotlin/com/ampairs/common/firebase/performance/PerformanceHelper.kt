package com.ampairs.common.firebase.performance

import com.ampairs.common.model.Response
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Track API request performance with Firebase Performance Monitoring
 *
 * Usage in repository:
 * ```kotlin
 * suspend fun getCustomers(): Response<List<Customer>> {
 *     return trackApiPerformance(
 *         performance = performance,
 *         endpoint = "/api/v1/customers",
 *         method = "GET"
 *     ) {
 *         api.getCustomers()
 *     }
 * }
 * ```
 */
suspend fun <T> trackApiPerformance(
    performance: FirebasePerformance,
    endpoint: String,
    method: String,
    block: suspend () -> Response<T>
): Response<T> {
    val trace = performance.newTrace(PerformanceTraces.API_REQUEST)
    trace.putAttribute(PerformanceAttributes.API_ENDPOINT, endpoint)
    trace.putAttribute(PerformanceAttributes.HTTP_METHOD, method)
    trace.start()

    return try {
        val response = block()

        // Track success/failure
        val isSuccess = response.data != null && response.error == null
        trace.putAttribute(PerformanceAttributes.SUCCESS, isSuccess.toString())

        if (isSuccess) {
            trace.putAttribute(PerformanceAttributes.STATUS_CODE, "200")
        } else {
            trace.putAttribute(PerformanceAttributes.STATUS_CODE, "error")
            response.error?.let { error ->
                trace.putAttribute("error_message", error.message)
            }
        }

        trace.stop()
        response
    } catch (e: Exception) {
        trace.putAttribute(PerformanceAttributes.SUCCESS, "false")
        trace.putAttribute(PerformanceAttributes.STATUS_CODE, "exception")
        trace.putAttribute("exception_type", e::class.simpleName ?: "Unknown")
        trace.stop()
        throw e
    }
}

/**
 * Track custom operation performance
 *
 * Usage:
 * ```kotlin
 * val result = trackPerformance(
 *     performance = performance,
 *     traceName = PerformanceTraces.DATABASE_QUERY,
 *     attributes = mapOf(
 *         PerformanceAttributes.OPERATION_TYPE to "insert",
 *         PerformanceAttributes.ENTITY_NAME to "Customer"
 *     )
 * ) {
 *     database.customerDao().insert(customer)
 * }
 * ```
 */
suspend fun <T> trackPerformance(
    performance: FirebasePerformance,
    traceName: String,
    attributes: Map<String, String> = emptyMap(),
    block: suspend () -> T
): T {
    val trace = performance.newTrace(traceName)
    attributes.forEach { (key, value) ->
        trace.putAttribute(key, value)
    }
    trace.start()

    return try {
        val result = block()
        trace.putAttribute(PerformanceAttributes.SUCCESS, "true")
        trace.stop()
        result
    } catch (e: Exception) {
        trace.putAttribute(PerformanceAttributes.SUCCESS, "false")
        trace.putAttribute("exception_type", e::class.simpleName ?: "Unknown")
        trace.stop()
        throw e
    }
}

/**
 * Measure execution time and add as metric to trace
 */
@OptIn(ExperimentalTime::class)
suspend fun <T> Trace.measureAndRecord(
    metricName: String,
    block: suspend () -> T
): T {
    val startTime = Clock.System.now().toEpochMilliseconds()
    return try {
        block()
    } finally {
        val endTime = Clock.System.now().toEpochMilliseconds()
        val duration = endTime - startTime
        putMetric(metricName, duration)
    }
}
