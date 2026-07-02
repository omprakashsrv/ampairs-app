package com.ampairs.agent.report

import com.ampairs.common.agent.ReportPeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReportPeriodTest {

    // Fixed reference point: Wednesday, 2026-06-24 at 15:30 UTC.
    private val tz = TimeZone.UTC
    private val now = LocalDateTime(2026, 6, 24, 15, 30, 0).toInstant(tz)

    private fun dayStart(y: Int, m: Int, d: Int) = LocalDate(y, m, d).atStartOfDayIn(tz)

    @Test
    fun unknownAndAll_returnNull() {
        assertNull(ReportPeriod.parse(null, tz, now))
        assertNull(ReportPeriod.parse("", tz, now))
        assertNull(ReportPeriod.parse("all", tz, now))
        assertNull(ReportPeriod.parse("all_time", tz, now))
        assertNull(ReportPeriod.parse("overall", tz, now))
        assertNull(ReportPeriod.parse("since the beginning", tz, now)) // unrecognized → unfiltered
    }

    @Test
    fun today_isASingleDayWindow() {
        val r = ReportPeriod.parse("today", tz, now)!!
        assertEquals(dayStart(2026, 6, 24), r.startInclusive)
        assertEquals(dayStart(2026, 6, 25), r.endExclusive)
    }

    @Test
    fun yesterday_isThePriorDay() {
        val r = ReportPeriod.parse("yesterday", tz, now)!!
        assertEquals(dayStart(2026, 6, 23), r.startInclusive)
        assertEquals(dayStart(2026, 6, 24), r.endExclusive)
    }

    @Test
    fun thisWeek_startsMonday_endsTomorrow() {
        // 2026-06-24 is a Wednesday; ISO week starts Monday 2026-06-22.
        val r = ReportPeriod.parse("this_week", tz, now)!!
        assertEquals(dayStart(2026, 6, 22), r.startInclusive)
        assertEquals(dayStart(2026, 6, 25), r.endExclusive) // through end of today
    }

    @Test
    fun lastWeek_isThePriorMondaySunday() {
        val r = ReportPeriod.parse("last_week", tz, now)!!
        assertEquals(dayStart(2026, 6, 15), r.startInclusive) // Mon
        assertEquals(dayStart(2026, 6, 22), r.endExclusive)   // through Sun 06-21
    }

    @Test
    fun thisMonth_startsFirstOfMonth() {
        val r = ReportPeriod.parse("this_month", tz, now)!!
        assertEquals(dayStart(2026, 6, 1), r.startInclusive)
        assertEquals(dayStart(2026, 6, 25), r.endExclusive) // through end of today
    }

    @Test
    fun lastMonth_isMayInFull() {
        val r = ReportPeriod.parse("last_month", tz, now)!!
        assertEquals(dayStart(2026, 5, 1), r.startInclusive)
        assertEquals(dayStart(2026, 6, 1), r.endExclusive) // through 05-31
    }

    @Test
    fun thisYear_startsJan1() {
        val r = ReportPeriod.parse("this_year", tz, now)!!
        assertEquals(dayStart(2026, 1, 1), r.startInclusive)
        assertEquals(dayStart(2026, 6, 25), r.endExclusive)
    }

    @Test
    fun lastYear_isPriorCalendarYear() {
        val r = ReportPeriod.parse("last_year", tz, now)!!
        assertEquals(dayStart(2025, 1, 1), r.startInclusive)
        assertEquals(dayStart(2026, 1, 1), r.endExclusive) // through 2025-12-31
    }

    @Test
    fun keyNormalization_handlesSpacesCaseAndHyphens() {
        val expected = ReportPeriod.parse("this_month", tz, now)
        assertEquals(expected, ReportPeriod.parse("This Month", tz, now))
        assertEquals(expected, ReportPeriod.parse("this-month", tz, now))
        assertEquals(expected, ReportPeriod.parse("  THIS_MONTH  ", tz, now))
    }

    @Test
    fun rangesAreHalfOpen_andOrdered() {
        val r = ReportPeriod.parse("today", tz, now)!!
        assertTrue(r.startInclusive < r.endExclusive)
    }
}
