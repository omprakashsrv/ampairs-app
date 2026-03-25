package com.ampairs.customer.ui.components.location

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.WindowPosition
import org.openstreetmap.gui.jmapviewer.JMapViewer
import org.openstreetmap.gui.jmapviewer.Coordinate
import org.openstreetmap.gui.jmapviewer.MapMarkerDot
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Real OpenStreetMap dialog for desktop location selection using JMapViewer
 */
@OptIn(ExperimentalTime::class)
@Composable
fun OpenStreetMapDialog(
    showDialog: Boolean,
    initialLocation: LocationData? = null,
    onLocationSelected: (LocationData) -> Unit,
    onDismiss: () -> Unit
) {
    if (!showDialog) return

    var selectedLocation by remember(initialLocation) {
        mutableStateOf(initialLocation)
    }

    DialogWindow(
        onCloseRequest = onDismiss,
        state = DialogState(
            position = WindowPosition.Aligned(Alignment.Center),
            width = 800.dp,
            height = 600.dp
        ),
        title = "Select Location from OpenStreetMap",
        resizable = true
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Info banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "🗺️ Click anywhere on the OpenStreetMap to select a location",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Location info card
            selectedLocation?.let { location ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Selected Location",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Lat: ${location.latitude.toString().take(8)}, Lng: ${location.longitude.toString().take(8)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        location.address?.let { address ->
                            Text(
                                text = address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Real OpenStreetMap viewer
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                SwingPanel(
                    factory = {
                        val mapViewer = JMapViewer().apply {
                            // Set OpenStreetMap tile source
                            setTileSource(OsmTileSource.Mapnik())

                            // Initial map position
                            // Note: JMapViewer Coordinate constructor expects (latitude, longitude)
                            val startCoordinate = initialLocation?.let {
                                Coordinate(it.latitude, it.longitude)
                            } ?: Coordinate(12.9716, 77.5946) // Default to Bangalore

                            setDisplayPosition(startCoordinate, 13)

                            // Add initial marker if location provided
                            initialLocation?.let { location ->
                                // MapMarkerDot constructor: (color, latitude, longitude) - standard geographic order
                                val marker = MapMarkerDot(Color.RED, location.latitude, location.longitude)
                                addMapMarker(marker)
                            }

                            // Add click listener for location selection
                            addMouseListener(object : MouseAdapter() {
                                override fun mouseClicked(e: MouseEvent) {
                                    if (e.clickCount == 1) {
                                        val coordinate = getPosition(e.point)

                                        // Clear existing markers
                                        mapMarkerList.clear()

                                        // Add new marker - MapMarkerDot constructor: (color, latitude, longitude)
                                        val marker = MapMarkerDot(Color.RED, coordinate.lat, coordinate.lon)
                                        addMapMarker(marker)

                                        // Update selected location
                                        selectedLocation = LocationData(
                                            latitude = coordinate.lat,
                                            longitude = coordinate.lon,
                                            address = "Selected from OpenStreetMap: ${coordinate.lat.toString().take(6)}, ${coordinate.lon.toString().take(6)}",
                                            accuracy = 5.0,
                                            timestamp = Clock.System.now().toEpochMilliseconds()
                                        )

                                        repaint()
                                    }
                                }
                            })
                        }
                        mapViewer
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        selectedLocation?.let { location ->
                            onLocationSelected(location)
                        }
                        onDismiss()
                    },
                    enabled = selectedLocation != null
                ) {
                    Text("Select Location")
                }
            }
        }
    }
}