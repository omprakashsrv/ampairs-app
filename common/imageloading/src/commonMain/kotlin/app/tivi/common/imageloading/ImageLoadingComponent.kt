// Copyright 2019, Google LLC, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.common.imageloading

import app.tivi.app.ApplicationInfo
import app.tivi.appinitializers.AppInitializer
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.intercept.Interceptor
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

expect interface ImageLoadingPlatformComponent

interface ImageLoadingComponent : ImageLoadingPlatformComponent {

    val imageLoader: ImageLoader

    @Provides
    fun provideImageLoader(
        context: PlatformContext,
        interceptors: Set<Interceptor>,
        info: ApplicationInfo,
    ): ImageLoader = newImageLoader(
        context = context,
        interceptors = interceptors,

        debug = info.debugBuild,
        applicationInfo = info,
    )

    @Provides
    @IntoSet
    fun bindImageLoaderCleanupInitializer(initializer: ImageLoaderCleanupInitializer): AppInitializer =
        initializer

}
