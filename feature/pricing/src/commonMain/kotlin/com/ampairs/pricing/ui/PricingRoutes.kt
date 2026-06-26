package com.ampairs.pricing.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object PricingShellRoute : NavKey

@Serializable
data object PricingHomeRoute : NavKey

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

@Serializable
data object PriceListWizardRoute : NavKey

@Serializable
data class ItemEditorRoute(val priceListId: String, val itemId: String? = null) : NavKey

@Serializable
data object OffersListRoute : NavKey

@Serializable
data class OfferBuilderRoute(val offerId: String? = null) : NavKey

@Serializable
data class OfferPreviewRoute(val offerId: String) : NavKey
