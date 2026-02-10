package com.ampairs.tax.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation 3 routes for Tax module.
 * These routes implement NavKey for Navigation 3 compatibility.
 */

@Serializable
data object TaxListRoute : NavKey

@Serializable
data object TaxConfigurationRoute : NavKey

@Serializable
data object TaxCalculatorRoute : NavKey

@Serializable
data object MyTaxCodesRoute : NavKey

@Serializable
data class TaxCodeSearchRoute(
    val query: String = ""
) : NavKey

@Serializable
data class TaxCodeDetailRoute(
    val taxCodeId: String = ""
) : NavKey
