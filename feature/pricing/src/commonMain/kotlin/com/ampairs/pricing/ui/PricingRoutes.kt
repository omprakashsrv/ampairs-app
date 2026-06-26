package com.ampairs.pricing.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object PriceListListRoute : NavKey

@Serializable
data class PriceListDetailRoute(val priceListId: String) : NavKey

@Serializable
data class PriceListFormRoute(val priceListId: String? = null) : NavKey

@Serializable
data object GeoZoneListRoute : NavKey

@Serializable
data class GeoZoneFormRoute(val geoZoneId: String? = null) : NavKey

@Serializable
data object PriceTesterRoute : NavKey
