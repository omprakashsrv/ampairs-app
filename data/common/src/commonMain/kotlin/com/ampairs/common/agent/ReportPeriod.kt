package com.ampairs.common.agent

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** A half-open instant range `[startInclusive, endExclusive)` for a report's time filter. */
data class DateRange(val startInclusive: Instant, val endExclusive: Instant)

/**
 * Parses a natural-language period keyword from a report request into a concrete [DateRange],
 * computed in the **business** time zone (pass `AppLocale.timeZoneId` resolved to a [TimeZone];
 * never the device zone — see `/cmp-practices` §12). Returns `null` for "all time" (no filter) and
 * for any keyword it doesn't recognize, so callers treat an unknown period as unfiltered rather than
 * inventing a wrong window.
 *
 * Pure and dependency-free so it is unit-tested; the caller supplies `now` (e.g. `Clock.System.now()`).
 */
object ReportPeriod {

    /**
     * Convenience for non-composable callers (action handlers): resolves [now] from the system clock
     * and defaults to the device time zone. Use the device zone because syncable date columns are
     * stored device-local (`DateTimeAdapter.toDateTimeString`), so filtering must use the same zone.
     */
    @OptIn(ExperimentalTime::class)
    fun current(raw: String?, timeZone: TimeZone = TimeZone.currentSystemDefault()): DateRange? =
        parse(raw, timeZone, Clock.System.now())

    fun parse(raw: String?, timeZone: TimeZone, now: Instant): DateRange? {
        val key = raw?.trim()?.lowercase()?.replace(' ', '_')?.replace('-', '_') ?: return null
        val today = now.toLocalDateTime(timeZone).date
        return when (key) {
            "", "all", "all_time", "alltime", "ever", "any", "total", "overall" -> null
            "today" -> dayRange(today, today, timeZone)
            "yesterday" -> today.minus(1, DateTimeUnit.DAY).let { dayRange(it, it, timeZone) }
            "this_week", "week", "current_week" ->
                dayRange(weekStart(today), today, timeZone)
            "last_week", "previous_week" -> {
                val end = weekStart(today).minus(1, DateTimeUnit.DAY)
                dayRange(weekStart(end), end, timeZone)
            }
            "this_month", "month", "current_month" ->
                dayRange(monthStart(today), today, timeZone)
            "last_month", "previous_month" -> {
                val end = monthStart(today).minus(1, DateTimeUnit.DAY)
                dayRange(monthStart(end), end, timeZone)
            }
            "this_year", "year", "current_year", "ytd" ->
                dayRange(LocalDate(today.year, 1, 1), today, timeZone)
            "last_year", "previous_year" ->
                dayRange(LocalDate(today.year - 1, 1, 1), LocalDate(today.year - 1, 12, 31), timeZone)
            else -> null
        }
    }

    /** Monday of [date]'s ISO week. */
    private fun weekStart(date: LocalDate): LocalDate =
        date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

    private fun monthStart(date: LocalDate): LocalDate = LocalDate(date.year, date.monthNumber, 1)

    /** `[startDay 00:00, endDay+1 00:00)` in [tz] — inclusive of the whole end day. */
    private fun dayRange(startDay: LocalDate, endDay: LocalDate, tz: TimeZone): DateRange =
        DateRange(
            startInclusive = startDay.atStartOfDayIn(tz),
            endExclusive = endDay.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz),
        )
}
