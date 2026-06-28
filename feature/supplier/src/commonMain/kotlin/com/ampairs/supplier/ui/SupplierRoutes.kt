package com.ampairs.supplier.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation 3 routes for the Supplier module.
 * These routes implement NavKey for Navigation 3 compatibility.
 */

@Serializable
data object SupplierListRoute : NavKey

@Serializable
data class SupplierDetailsRoute(
    val supplierId: String = ""
) : NavKey

@Serializable
data class SupplierCreateRoute(
    val supplierId: String? = null
) : NavKey
