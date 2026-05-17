package com.ampairs.common.firebase.performance

/**
 * Desktop stub implementation of FirebasePerformance
 * Note: Firebase is not supported on Desktop platforms
 * All methods are no-ops
 */
actual class FirebasePerformance {
    actual fun setPerformanceCollectionEnabled(enabled: Boolean) {
        // No-op: Firebase not supported on Desktop
        println("FirebasePerformance (Desktop stub): setPerformanceCollectionEnabled($enabled)")
    }

    actual fun newTrace(traceName: String): Trace {
        return Trace(traceName)
    }
}

/**
 * Desktop stub implementation of Trace
 * Note: Firebase is not supported on Desktop platforms
 * All methods are no-ops
 */
actual class Trace(private val traceName: String) {
    actual fun start() {
        // No-op: Firebase not supported on Desktop
        println("Trace (Desktop stub): start($traceName)")
    }

    actual fun stop() {
        // No-op: Firebase not supported on Desktop
        println("Trace (Desktop stub): stop($traceName)")
    }

    actual fun putMetric(metricName: String, value: Long) {
        // No-op: Firebase not supported on Desktop
    }

    actual fun incrementMetric(metricName: String, incrementBy: Long) {
        // No-op: Firebase not supported on Desktop
    }

    actual fun getLongMetric(metricName: String): Long {
        // No-op: Firebase not supported on Desktop
        return 0L
    }

    actual fun putAttribute(attributeName: String, attributeValue: String) {
        // No-op: Firebase not supported on Desktop
    }

    actual fun removeAttribute(attributeName: String) {
        // No-op: Firebase not supported on Desktop
    }

    actual fun getAttribute(attributeName: String): String? {
        // No-op: Firebase not supported on Desktop
        return null
    }
}
