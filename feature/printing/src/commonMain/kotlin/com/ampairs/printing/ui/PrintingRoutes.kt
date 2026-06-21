package com.ampairs.printing.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation routes for the printing feature. */
@Serializable
data object PrinterListRoute : NavKey

/** Print job spool/history with retry + mark-printed actions. */
@Serializable
data object PrintQueueRoute : NavKey

/** Print template setup — view templates per document type + printer class, restore defaults. */
@Serializable
data object TemplateListRoute : NavKey

/** Visual editor for a single template — rename, edit/reorder/add/remove layout blocks. */
@Serializable
data class TemplateEditRoute(val templateId: String) : NavKey
