package com.ampairs.common.firebase.performance

import com.google.firebase.perf.FirebasePerformance as AndroidFirebasePerformance
import com.google.firebase.perf.metrics.Trace as AndroidTrace

/**
 * Android implementation of FirebasePerformance
 */
actual class FirebasePerformance {
    private val performance = AndroidFirebasePerformance.getInstance()

    actual fun setPerformanceCollectionEnabled(enabled: Boolean) {
        performance.isPerformanceCollectionEnabled = enabled
    }

    actual fun newTrace(traceName: String): Trace {
        return Trace(performance.newTrace(traceName))
    }
}

/**
 * Android implementation of Trace
 */
actual class Trace(private val androidTrace: AndroidTrace) {
    actual fun start() {
        androidTrace.start()
    }

    actual fun stop() {
        androidTrace.stop()
    }

    actual fun putMetric(metricName: String, value: Long) {
        androidTrace.putMetric(metricName, value)
    }

    actual fun incrementMetric(metricName: String, incrementBy: Long) {
        androidTrace.incrementMetric(metricName, incrementBy)
    }

    actual fun getLongMetric(metricName: String): Long {
        return androidTrace.getLongMetric(metricName)
    }

    actual fun putAttribute(attributeName: String, attributeValue: String) {
        androidTrace.putAttribute(attributeName, attributeValue)
    }

    actual fun removeAttribute(attributeName: String) {
        androidTrace.removeAttribute(attributeName)
    }

    actual fun getAttribute(attributeName: String): String? {
        return androidTrace.getAttribute(attributeName)
    }
}
