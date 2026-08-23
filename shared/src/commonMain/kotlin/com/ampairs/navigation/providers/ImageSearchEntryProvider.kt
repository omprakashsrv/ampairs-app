package com.ampairs.navigation.providers

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.imagesearch.ImageSearchRoute
import com.ampairs.imagesearch.ui.BulkImageMatchScreen
import com.ampairs.imagesearch.ui.ImageSearchScreen

/**
 * Entry provider for the internet image-search picker. Renders [ImageSearchScreen], which writes the
 * chosen image into the file pipeline itself and pops back on success.
 */
fun imageSearchEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is ImageSearchRoute.Search -> NavEntry(key) {
        ImageSearchScreen(
            entityType = key.entityType,
            entityUid = key.entityUid,
            keywords = key.keywords,
            onNavigateBack = { backStack.removeLastOrNull() },
        )
    }

    is ImageSearchRoute.BulkMatch -> NavEntry(key) {
        BulkImageMatchScreen(
            entityType = key.entityType,
            targets = key.targets,
            onNavigateBack = { backStack.removeLastOrNull() },
        )
    }

    else -> null
}
