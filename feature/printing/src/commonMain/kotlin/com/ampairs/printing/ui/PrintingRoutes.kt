package com.ampairs.printing.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation routes for the printing feature. */
@Serializable
data object PrinterListRoute : NavKey

/** A single printer's settings — status, test print, and per-document-type routing toggles. */
@Serializable
data class PrinterDetailRoute(val printerId: String) : NavKey

/** Print job spool/history with retry + mark-printed actions. */
@Serializable
data object PrintQueueRoute : NavKey

/** Print template setup — view templates per document type + printer class, restore defaults. */
@Serializable
data object TemplateListRoute : NavKey

/** Visual editor for a single template — rename, edit/reorder/add/remove layout blocks. */
@Serializable
data class TemplateEditRoute(val templateId: String) : NavKey
