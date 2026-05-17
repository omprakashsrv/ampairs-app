package com.ampairs.common.firebase.performance

/**
 * Common interface for Firebase Performance Monitoring across platforms
 */
expect class FirebasePerformance {
    /**
     * Enable or disable performance monitoring
     * @param enabled Whether performance monitoring should be enabled
     */
    fun setPerformanceCollectionEnabled(enabled: Boolean)

    /**
     * Create a new trace
     * @param traceName The name of the trace
     * @return A Trace object
     */
    fun newTrace(traceName: String): Trace
}

/**
 * Common interface for Performance Trace
 */
expect class Trace {
    /**
     * Start the trace
     */
    fun start()

    /**
     * Stop the trace
     */
    fun stop()

    /**
     * Put a metric into the trace
     * @param metricName The name of the metric
     * @param value The value of the metric
     */
    fun putMetric(metricName: String, value: Long)

    /**
     * Increment a metric in the trace
     * @param metricName The name of the metric
     * @param incrementBy The amount to increment by
     */
    fun incrementMetric(metricName: String, incrementBy: Long)

    /**
     * Get the current value of a metric
     * @param metricName The name of the metric
     * @return The current value of the metric
     */
    fun getLongMetric(metricName: String): Long

    /**
     * Put an attribute into the trace
     * @param attributeName The name of the attribute
     * @param attributeValue The value of the attribute
     */
    fun putAttribute(attributeName: String, attributeValue: String)

    /**
     * Remove an attribute from the trace
     * @param attributeName The name of the attribute to remove
     */
    fun removeAttribute(attributeName: String)

    /**
     * Get an attribute value from the trace
     * @param attributeName The name of the attribute
     * @return The value of the attribute or null if not found
     */
    fun getAttribute(attributeName: String): String?
}

/**
 * Common Performance Trace Names
 */
object PerformanceTraces {
    const val APP_START = "app_start"
    const val LOGIN_FLOW = "login_flow"
    const val DATA_SYNC = "data_sync"
    const val SCREEN_LOAD = "screen_load"
    const val API_REQUEST = "api_request"
    const val DATABASE_QUERY = "database_query"
    const val DATABASE_READ = "database_read"
    const val DATABASE_WRITE = "database_write"
    const val IMAGE_LOAD = "image_load"
    const val USER_FLOW = "user_flow"
    const val SYNC_OPERATION = "sync_operation"
}

/**
 * Common Performance Metric Names
 */
object PerformanceMetrics {
    const val ITEMS_COUNT = "items_count"
    const val RESPONSE_TIME = "response_time"
    const val RETRY_COUNT = "retry_count"
    const val CACHE_HIT = "cache_hit"
    const val CACHE_MISS = "cache_miss"
}

/**
 * Common Performance Attribute Names
 */
object PerformanceAttributes {
    const val SCREEN_NAME = "screen_name"
    const val SCREEN_CLASS = "screen_class"
    const val API_ENDPOINT = "api_endpoint"
    const val HTTP_METHOD = "http_method"
    const val STATUS_CODE = "status_code"
    const val NETWORK_TYPE = "network_type"
    const val SUCCESS = "success"
    const val OPERATION_TYPE = "operation_type"
    const val ENTITY_NAME = "entity_name"
}
