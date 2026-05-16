package com.ampairs.unit.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation 3 routes for Unit module.
 * These routes implement NavKey for Navigation 3 compatibility.
 */

@Serializable
data object UnitListRoute : NavKey

@Serializable
data class UnitFormRoute(
    val unitId: String? = null
) : NavKey
