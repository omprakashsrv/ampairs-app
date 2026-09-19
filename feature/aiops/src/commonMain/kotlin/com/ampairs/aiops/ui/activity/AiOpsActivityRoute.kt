package com.ampairs.aiops.ui.activity

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** AI Ops activity feed — the audit trail of fixes the assistant applied, with undo. */
@Serializable
data object AiOpsActivityRoute : NavKey
