// Copyright 2023, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0

package app.tivi.util

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
expect class TiviDateFormatter {
  fun formatShortDate(instant: Instant): String
  fun formatShortDate(date: LocalDate): String
  fun formatMediumDate(instant: Instant): String
  fun formatMediumDateTime(instant: Instant): String
  fun formatShortTime(localTime: LocalTime): String
  fun formatShortRelativeTime(date: Instant, reference: Instant = Clock.System.now()): String
  fun formatDayOfWeek(dayOfWeek: DayOfWeek): String
}
