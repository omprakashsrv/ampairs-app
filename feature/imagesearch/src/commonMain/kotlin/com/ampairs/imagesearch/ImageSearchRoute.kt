package com.ampairs.imagesearch

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation route for the internet image-search picker.
 *
 * The route carries the target file owner ([entityType] / [entityUid]) so the picked image can be
 * written straight into the existing file pipeline, plus the composable [keywords] (product name,
 * brand, category, group, sub-group) used to seed the search chips.
 *
 * [entityType] mirrors `com.ampairs.file.api.FileEntityType.name` (e.g. "PRODUCT"); it is passed as a
 * plain String so this route stays free of a hard file-api coupling at the navigation layer.
 */
@Serializable
sealed interface ImageSearchRoute : NavKey {
    @Serializable
    data class Search(
        val entityType: String = "",
        val entityUid: String = "",
        val keywords: List<String> = emptyList(),
    ) : ImageSearchRoute

    /**
     * Bulk auto-match: scrape candidate images for many entities at once (e.g. every product missing
     * an image) and let the user pick/confirm per row, then save all in one go.
     */
    @Serializable
    data class BulkMatch(
        val entityType: String = "",
        val targets: List<BulkTarget> = emptyList(),
    ) : ImageSearchRoute
}

/** One entity to auto-match in a [ImageSearchRoute.BulkMatch] run. */
@Serializable
data class BulkTarget(
    val entityUid: String,
    val name: String,
    val keywords: List<String> = emptyList(),
)
