package com.ampairs.common.util

import com.ampairs.common.model.formatDate
import kotlin.time.Instant

/**
 * Centralized date/time formatting utilities for the application.
 * Uses business-defined formats for consistent display across the app.
 */
object DateTimeFormatter {

    /**
     * Formats an ISO 8601 timestamp string to a human-readable format.
     * Input: "2025-01-15T10:30:45Z" or "2025-01-15T10:30:45"
     * Output: "15 Jan 2025, 10:30 AM"
     */
    @OptIn(kotlin.time.ExperimentalTime::class)
    fun formatTimestamp(isoTimestamp: String): String {
        return try {
            val instant = Instant.parse(isoTimestamp)
            instant.toEpochMilliseconds().formatDate("dd MMM yyyy, hh:mm a", isoTimestamp)
        } catch (_: Exception) {
            isoTimestamp
        }
    }

    /**
     * Formats an ISO 8601 timestamp string to date only format.
     * Input: "2025-01-15T10:30:45Z"
     * Output: "15 Jan 2025"
     */
    @OptIn(kotlin.time.ExperimentalTime::class)
    fun formatDate(isoTimestamp: String): String {
        return try {
            val instant = Instant.parse(isoTimestamp)
            instant.toEpochMilliseconds().formatDate("dd MMM yyyy", isoTimestamp)
        } catch (_: Exception) {
            isoTimestamp
        }
    }

    /**
     * Formats an ISO 8601 timestamp string to time only format.
     * Input: "2025-01-15T10:30:45Z"
     * Output: "10:30 AM"
     */
    @OptIn(kotlin.time.ExperimentalTime::class)
    fun formatTime(isoTimestamp: String): String {
        return try {
            val instant = Instant.parse(isoTimestamp)
            instant.toEpochMilliseconds().formatDate("hh:mm a", isoTimestamp)
        } catch (_: Exception) {
            isoTimestamp
        }
    }
}
