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
}
