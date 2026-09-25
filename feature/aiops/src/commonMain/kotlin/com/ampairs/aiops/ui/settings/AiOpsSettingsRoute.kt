package com.ampairs.aiops.ui.settings

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** AI Ops (data-quality assistant) settings — currently the workspace autonomy level. */
@Serializable
data object AiOpsSettingsRoute : NavKey
