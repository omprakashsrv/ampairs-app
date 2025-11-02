// Copyright 2022, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.util

import app.tivi.appinitializers.AppInitializer
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

expect interface LoggerPlatformComponent

interface LoggerComponent : LoggerPlatformComponent {
  @Provides
  @IntoSet
  fun provideCrashReportingInitializer(impl: CrashReportingInitializer): AppInitializer = impl

  @Provides
  @IntoSet
  fun provideKermitInitializer(impl: KermitInitializer): AppInitializer = impl
}
