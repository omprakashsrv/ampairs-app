package com.ampairs.pricing.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object PriceListListRoute : NavKey

@Serializable
data class PriceListFormRoute(val priceListId: String? = null) : NavKey
