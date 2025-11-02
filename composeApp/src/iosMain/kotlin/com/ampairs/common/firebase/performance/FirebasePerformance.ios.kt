package com.ampairs.common.firebase.performance

import cocoapods.FirebasePerformance.FIRPerformance
import cocoapods.FirebasePerformance.FIRTrace
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * iOS implementation of FirebasePerformance
 */
@OptIn(ExperimentalForeignApi::class)
actual class FirebasePerformance {
    private val performance = FIRPerformance.sharedInstance()

    actual fun setPerformanceCollectionEnabled(enabled: Boolean) {
        performance.setDataCollectionEnabled(enabled)
    }

    actual fun newTrace(traceName: String): Trace {
        val iosTrace = performance.traceWithName(traceName)
        return Trace(iosTrace)
    }
}

/**
 * iOS implementation of Trace
 */
@OptIn(ExperimentalForeignApi::class)
actual class Trace(private val iosTrace: FIRTrace) {
    actual fun start() {
        iosTrace.start()
    }

    actual fun stop() {
        iosTrace.stop()
    }

    actual fun putMetric(metricName: String, value: Long) {
        iosTrace.setIntValue(value, metricName)
    }

    actual fun incrementMetric(metricName: String, incrementBy: Long) {
        iosTrace.incrementMetric(metricName, incrementBy)
    }

    actual fun getLongMetric(metricName: String): Long {
        return iosTrace.valueForIntMetric(metricName)
    }

    actual fun putAttribute(attributeName: String, attributeValue: String) {
        iosTrace.setValue(attributeValue, attributeName)
    }

    actual fun removeAttribute(attributeName: String) {
        iosTrace.removeAttribute(attributeName)
    }

    actual fun getAttribute(attributeName: String): String? {
        return iosTrace.valueForAttribute(attributeName)
    }
}
