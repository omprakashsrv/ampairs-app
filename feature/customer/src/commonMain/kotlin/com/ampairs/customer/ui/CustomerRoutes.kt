package com.ampairs.customer.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation 3 routes for Customer module.
 * These routes implement NavKey for Navigation 3 compatibility.
 */

@Serializable
data object CustomerListRoute : NavKey

@Serializable
data class CustomerDetailsRoute(
    val customerId: String = ""
) : NavKey

@Serializable
data class CustomerCreateRoute(
    val customerId: String? = null
) : NavKey

@Serializable
data object StateListRoute : NavKey

@Serializable
data object CustomerTypeListRoute : NavKey

@Serializable
data class CustomerTypeCreateRoute(
    val customerTypeId: String? = null
) : NavKey

@Serializable
data object CustomerGroupListRoute : NavKey

@Serializable
data class CustomerGroupCreateRoute(
    val customerGroupId: String? = null
) : NavKey
