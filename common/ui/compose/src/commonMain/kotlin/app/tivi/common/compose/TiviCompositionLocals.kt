// Copyright 2020, Google LLC, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.common.compose

import androidx.compose.runtime.staticCompositionLocalOf
import app.tivi.util.TiviDateFormatter

val LocalTiviDateFormatter = staticCompositionLocalOf<TiviDateFormatter> {
  error("TiviDateFormatter not provided")
}

val LocalPreferences = staticCompositionLocalOf<String> {
  error("LocalPreferences not provided")
}
