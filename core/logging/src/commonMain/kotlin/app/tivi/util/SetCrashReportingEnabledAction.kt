// Copyright 2023, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.util

fun interface SetCrashReportingEnabledAction {
  operator fun invoke(enabled: Boolean)
}
