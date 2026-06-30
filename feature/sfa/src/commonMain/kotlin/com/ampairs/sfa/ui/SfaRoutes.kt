package com.ampairs.sfa.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation 3 routes for the SFA module. */
@Serializable
data object SfaBeatListRoute : NavKey

@Serializable
data class BeatFormRoute(val beatId: String? = null) : NavKey

@Serializable
data object SfaPlannedVisitListRoute : NavKey
