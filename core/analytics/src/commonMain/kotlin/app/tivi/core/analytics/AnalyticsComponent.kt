// Copyright 2022, Google LLC, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.core.analytics

import app.tivi.appinitializers.AppInitializer
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

expect interface AnalyticsPlatformComponent

interface AnalyticsComponent : AnalyticsPlatformComponent {
  @Provides
  @IntoSet
  fun provideAnalyticsInitializer(impl: AnalyticsInitializer): AppInitializer = impl
}
