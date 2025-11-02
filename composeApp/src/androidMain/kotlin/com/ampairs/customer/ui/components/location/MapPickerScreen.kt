package com.ampairs.customer.ui.components.location

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

/**
 * Google Maps Compose screen for location selection
 * Allows users to pick a location by moving the map
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    initialLocation: LocationData?,
    onLocationSelected: (LocationData) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Initial camera position
    val initialLatLng = initialLocation?.let {
        LatLng(it.latitude, it.longitude)
    } ?: LatLng(12.9716, 77.5946) // Default to Bangalore

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 15f)
    }

    // Track center of map as user moves it
    var selectedLocation by remember {
        mutableStateOf(initialLocation ?: LocationData(initialLatLng.latitude, initialLatLng.longitude))
    }

    // Update selected location when map moves
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val center = cameraPositionState.position.target
            selectedLocation = LocationData(
                latitude = center.latitude,
                longitude = center.longitude
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Location") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onLocationSelected(selectedLocation) }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Confirm location")
                    }
                }
            )
        },
        floatingActionButton = {
            // My Location button
            FloatingActionButton(
                onClick = {
                    // This should trigger getCurrentLocation from LocationService
                    // and animate camera to that position
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "My location",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = false, // We'll handle this manually
                    mapType = MapType.NORMAL
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = false, // Using custom FAB
                    compassEnabled = true,
                    mapToolbarEnabled = false
                )
            )

            // Center marker (pin)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Selected location",
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )
            }

            // Location info card at bottom
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Selected Location",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Lat: ${String.format("%.6f", selectedLocation.latitude)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Lng: ${String.format("%.6f", selectedLocation.longitude)}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    selectedLocation.address?.let { address ->
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { onLocationSelected(selectedLocation) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Confirm Location")
                    }
                }
            }
        }
    }
}
