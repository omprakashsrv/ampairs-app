package com.ampairs.formwidgets.location

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Desktop: no embedded map SDK wired — show the location with an "Open in Maps" action. */
@Composable
actual fun DeliveryLocationMap(
    latitude: Double,
    longitude: Double,
    label: String?,
    modifier: Modifier,
) {
    OpenInMapsCard(latitude = latitude, longitude = longitude, label = label, modifier = modifier)
}
