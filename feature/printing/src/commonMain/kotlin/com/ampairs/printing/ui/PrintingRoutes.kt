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
