package com.ampairs.imagesearch.domain

/**
 * One composable part of the search query, rendered as a toggle chip.
 *
 * The effective query is every [enabled] keyword's [value] joined by a space (plus any free-text the
 * user typed). Toggling a chip re-runs the search.
 *
 * @property label short chip label (e.g. "Brand").
 * @property value the search term contributed when [enabled] (e.g. the brand name).
 */
data class SearchKeyword(
    val label: String,
    val value: String,
    val enabled: Boolean = true,
)

/** Join the enabled keywords into a single query string. */
fun List<SearchKeyword>.toQuery(): String =
    filter { it.enabled && it.value.isNotBlank() }.joinToString(" ") { it.value.trim() }
