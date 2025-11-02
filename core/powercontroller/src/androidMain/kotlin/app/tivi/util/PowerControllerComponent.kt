// Copyright 2023, Google LLC, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.util

import app.tivi.inject.ApplicationScope
import me.tatarka.inject.annotations.Provides

actual interface PowerControllerComponent {
  @Provides
  @ApplicationScope
  fun providePowerController(bind: AndroidPowerController): PowerController = bind
}
