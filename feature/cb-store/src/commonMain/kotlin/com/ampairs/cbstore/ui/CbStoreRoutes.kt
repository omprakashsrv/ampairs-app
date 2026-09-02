package com.ampairs.cbstore.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation 3 routes for the cb-store feature. */

@Serializable
data object CbStoreListRoute : NavKey

@Serializable
data class CbStoreFormRoute(val storeId: String? = null) : NavKey
// Zonal offices are managed server-side / seeded for v1; no in-app CRUD screen yet.
