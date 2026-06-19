package com.ampairs.formwidgets.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ampairsapp.feature.formwidgets.generated.resources.Res
import ampairsapp.feature.formwidgets.generated.resources.widget_delivery_location
import ampairsapp.feature.formwidgets.generated.resources.widget_open_in_maps
import org.jetbrains.compose.resources.stringResource

/**
 * Shows the delivery drop point for a set of coordinates.
 *
 * - Android renders an embedded, non-interactive Google map with a pin (actual).
 * - iOS / Desktop render a compact card with the location label and an "Open in Maps" action that
 *   launches the platform maps app (the shared [OpenInMapsCard] below).
 *
 * Only call this when both coordinates are present.
 */
@Composable
expect fun DeliveryLocationMap(
    latitude: Double,
    longitude: Double,
    label: String?,
    modifier: Modifier = Modifier,
)

/** External maps deep link (works with Google Maps, Apple Maps, and most map apps via the OS). */
fun deliveryMapsUrl(latitude: Double, longitude: Double): String =
    "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"

/**
 * Cross-platform fallback used by the iOS/Desktop actuals: a card that opens the device's maps app
 * at the pin. Also reused as the tap target on Android.
 */
@Composable
fun OpenInMapsCard(
    latitude: Double,
    longitude: Double,
    label: String?,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.widget_delivery_location), style = MaterialTheme.typography.titleSmall)
                Text(
                    label ?: "${latitude.toString().take(9)}, ${longitude.toString().take(9)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(onClick = { uriHandler.openUri(deliveryMapsUrl(latitude, longitude)) }) {
                Icon(Icons.Filled.Map, contentDescription = null)
                Text(stringResource(Res.string.widget_open_in_maps), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}
