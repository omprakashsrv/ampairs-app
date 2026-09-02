package com.ampairs.cbmaintenance.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation 3 routes for the cb-maintenance feature. */

@Serializable
data object CbPmDueListRoute : NavKey

@Serializable
data object CbTicketListRoute : NavKey

@Serializable
data object CbRaiseTicketRoute : NavKey

@Serializable
data object CbPmScheduleListRoute : NavKey
