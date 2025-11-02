@file:OptIn(ExperimentalTime::class)

package com.ampairs.common.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


@OptIn(ExperimentalTime::class)
class DateTimeAdapter {

    companion object {
        val DATE_TIME_FORMATTER: String = "yyyy-MM-dd HH:mm:ss"

        val DATE_FORMATTER: String = "yyyy-MM-dd"


        fun toDateTimeString(value: Instant?): String {
            return value?.formatDate(DATE_TIME_FORMATTER) ?: ""
        }

        fun toDateString(value: Instant?): String {
            return value?.formatDate(DATE_FORMATTER) ?: ""
        }

        fun toDateString(value: LocalDate?): String {
            return value?.atStartOfDayIn(TimeZone.currentSystemDefault())?.toEpochMilliseconds()
                ?.formatDate(DATE_FORMATTER)
                ?: ""
        }

        fun fromDateTimeString(value: String?): Instant? {
            return if (value.isNullOrEmpty()) null else Instant.fromEpochMilliseconds(
                value.parseDate(DATE_TIME_FORMATTER)
            )
        }

        fun fromDateString(value: String?): Instant? {
            return if (value.isNullOrEmpty()) null else Instant.fromEpochMilliseconds(
                value.parseDate(DATE_FORMATTER)
            )
        }
    }
}

expect fun Instant.formatDate(pattern: String, defValue: String = ""): String

expect fun String.parseDate(pattern: String, defValue: Long = 0L): Long

fun Long.formatDate(pattern: String, defValue: String = ""): String {
    return Instant.fromEpochMilliseconds(this).formatDate(pattern, defValue)
}