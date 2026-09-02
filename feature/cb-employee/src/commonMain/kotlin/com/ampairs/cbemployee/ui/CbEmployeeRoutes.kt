package com.ampairs.cbemployee.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation 3 routes for the cb-employee feature. */

@Serializable
data object CbEmployeeListRoute : NavKey

@Serializable
data class CbEmployeeFormRoute(val employeeId: String? = null) : NavKey
