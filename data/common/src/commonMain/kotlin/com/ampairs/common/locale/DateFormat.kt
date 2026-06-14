package com.ampairs.common.locale

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Timezone- and format-aware date/time rendering driven by the workspace [AppLocale].
 *
 * Timestamps are stored/synced as UTC everywhere; these helpers convert a UTC [Instant] to the
 * business timezone ([AppLocale.timeZoneId]) and render it per [AppLocale.dateFormat] /
 * [AppLocale.timeFormat]. Read the locale from `LocalAppLocale.current` in a `@Composable`.
 */

private val MONTH_ABBR = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private fun zoneOf(id: String): TimeZone = try {
    if (id.isBlank()) TimeZone.UTC else TimeZone.of(id)
} catch (_: Exception) {
    TimeZone.UTC
}

private fun Int.p2(): String = if (this in 0..9) "0$this" else this.toString()

private fun datePart(year: Int, month: Int, day: Int, format: String): String =
    when (format.trim().uppercase()) {
        "MM-DD-YYYY" -> "${month.p2()}-${day.p2()}-$year"
        "YYYY-MM-DD" -> "$year-${month.p2()}-${day.p2()}"
        "DD-MM-YYYY" -> "${day.p2()}-${month.p2()}-$year"
        else -> "${day.p2()} ${MONTH_ABBR[(month - 1).coerceIn(0, 11)]} $year" // friendly fallback
    }

private fun timePart(hour: Int, minute: Int, format: String): String =
    if (format.trim().equals("24H", ignoreCase = true)) {
        "${hour.p2()}:${minute.p2()}"
    } else {
        val isAm = hour < 12
        val h12 = ((hour + 11) % 12) + 1
        "$h12:${minute.p2()} ${if (isAm) "AM" else "PM"}"
    }

@OptIn(ExperimentalTime::class)
fun formatDate(instant: Instant, locale: AppLocale): String {
    val ldt = instant.toLocalDateTime(zoneOf(locale.timeZoneId))
    return datePart(ldt.year, ldt.monthNumber, ldt.day, locale.dateFormat)
}

@OptIn(ExperimentalTime::class)
fun formatTime(instant: Instant, locale: AppLocale): String {
    val ldt = instant.toLocalDateTime(zoneOf(locale.timeZoneId))
    return timePart(ldt.hour, ldt.minute, locale.timeFormat)
}

@OptIn(ExperimentalTime::class)
fun formatDateTime(instant: Instant, locale: AppLocale): String {
    val ldt = instant.toLocalDateTime(zoneOf(locale.timeZoneId))
    return datePart(ldt.year, ldt.monthNumber, ldt.day, locale.dateFormat) +
        ", " + timePart(ldt.hour, ldt.minute, locale.timeFormat)
}

/** ISO-8601 string convenience. Returns the raw input if it can't be parsed (matches DateTimeFormatter). */
@OptIn(ExperimentalTime::class)
fun formatDate(isoTimestamp: String?, locale: AppLocale): String {
    if (isoTimestamp.isNullOrBlank()) return ""
    return try { formatDate(Instant.parse(isoTimestamp), locale) } catch (_: Exception) { isoTimestamp }
}

@OptIn(ExperimentalTime::class)
fun formatTime(isoTimestamp: String?, locale: AppLocale): String {
    if (isoTimestamp.isNullOrBlank()) return ""
    return try { formatTime(Instant.parse(isoTimestamp), locale) } catch (_: Exception) { isoTimestamp }
}

@OptIn(ExperimentalTime::class)
fun formatDateTime(isoTimestamp: String?, locale: AppLocale): String {
    if (isoTimestamp.isNullOrBlank()) return ""
    return try { formatDateTime(Instant.parse(isoTimestamp), locale) } catch (_: Exception) { isoTimestamp }
}
