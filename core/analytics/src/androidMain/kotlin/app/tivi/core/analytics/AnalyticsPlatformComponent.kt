// Copyright 2022, Google LLC, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.core.analytics

import app.tivi.inject.ApplicationScope
import me.tatarka.inject.annotations.Provides

actual interface AnalyticsPlatformComponent {
  @ApplicationScope
  @Provides
  fun provideTiviFirebaseAnalytics(bind: TiviFirebaseAnalytics): Analytics = bind
}
