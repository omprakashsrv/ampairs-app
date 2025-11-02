package com.ampairs.customer.ui.components.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Location picker state using LocationService architecture
 */
sealed class LocationDialogState {
    object Idle : LocationDialogState()
    object LoadingLocation : LocationDialogState()
    object LoadingAddress : LocationDialogState()
    data class LocationSelected(val location: LocationData) : LocationDialogState()
    data class AddressResolved(val location: LocationData, val address: AddressData) : LocationDialogState()
    data class Error(val message: String) : LocationDialogState()
}

/**
 * Platform-specific location picker dialog
 */
@Composable
expect fun PlatformLocationPickerDialog(
    showDialog: Boolean,
    currentLocation: LocationData? = null,
    onLocationSelected: (LocationData, AddressData?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    locationService: LocationService
)

/**
 * Main location picker dialog - delegates to platform-specific implementation
 */
@Composable
fun LocationPickerDialog(
    showDialog: Boolean,
    currentLocation: LocationData? = null,
    onLocationSelected: (LocationData, AddressData?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    locationService: LocationService = koinInject()
) {
    PlatformLocationPickerDialog(
        showDialog = showDialog,
        currentLocation = currentLocation,
        onLocationSelected = onLocationSelected,
        onDismiss = onDismiss,
        modifier = modifier,
        locationService = locationService
    )
}

/**
 * Common location picker dialog implementation using LocationService architecture
 */
@Composable
fun CommonLocationPickerDialog(
    showDialog: Boolean,
    currentLocation: LocationData? = null,
    onLocationSelected: (LocationData, AddressData?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    locationService: LocationService = koinInject()
) {
    if (!showDialog) return

    var dialogState by remember(currentLocation) {
        mutableStateOf<LocationDialogState>(
            if (currentLocation != null)
                LocationDialogState.LocationSelected(currentLocation)
            else
                LocationDialogState.Idle
        )
    }

    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Location")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (dialogState) {
                    is LocationDialogState.Idle -> {
                        LocationIdleContent(
                            onGetCurrentLocation = {
                                dialogState = LocationDialogState.LoadingLocation
                                scope.launch {
                                    val result = locationService.getCurrentLocation()
                                    dialogState = if (result.isSuccess) {
                                        LocationDialogState.LocationSelected(result.getOrThrow())
                                    } else {
                                        LocationDialogState.Error(
                                            result.exceptionOrNull()?.message ?: "Failed to get current location"
                                        )
                                    }
                                }
                            },
                            onSelectFromMap = {
                                dialogState = LocationDialogState.LoadingLocation
                                scope.launch {
                                    val result = locationService.selectLocationFromMap()
                                    dialogState = if (result.isSuccess) {
                                        LocationDialogState.LocationSelected(result.getOrThrow())
                                    } else {
                                        LocationDialogState.Error(
                                            result.exceptionOrNull()?.message ?: "Failed to select location from map"
                                        )
                                    }
                                }
                            }
                        )
                    }

                    is LocationDialogState.LoadingLocation -> {
                        LoadingContent("Getting your location...")
                    }

                    is LocationDialogState.LoadingAddress -> {
                        LoadingContent("Resolving address...")
                    }

                    is LocationDialogState.LocationSelected -> {
                        val selectedState = dialogState as LocationDialogState.LocationSelected
                        LocationSelectedContent(
                            location = selectedState.location,
                            onLocationOnly = {
                                onLocationSelected(selectedState.location, null)
                            },
                            onGetAddress = {
                                dialogState = LocationDialogState.LoadingAddress
                                scope.launch {
                                    val geocodeResult = locationService.reverseGeocode(
                                        selectedState.location.latitude,
                                        selectedState.location.longitude
                                    )

                                    if (geocodeResult.isSuccess) {
                                        val address = geocodeResult.getOrThrow()
                                        dialogState = LocationDialogState.AddressResolved(
                                            selectedState.location,
                                            address
                                        )
                                    } else {
                                        // Even if geocoding fails, we can still use the location
                                        onLocationSelected(selectedState.location, null)
                                    }
                                }
                            },
                            onSelectDifferent = {
                                dialogState = LocationDialogState.Idle
                            }
                        )
                    }

                    is LocationDialogState.AddressResolved -> {
                        val resolvedState = dialogState as LocationDialogState.AddressResolved
                        AddressResolvedContent(
                            location = resolvedState.location,
                            address = resolvedState.address,
                            onLocationOnly = {
                                onLocationSelected(resolvedState.location, null)
                            },
                            onLocationAndAddress = {
                                onLocationSelected(resolvedState.location, resolvedState.address)
                            },
                            onSelectDifferent = {
                                dialogState = LocationDialogState.Idle
                            }
                        )
                    }

                    is LocationDialogState.Error -> {
                        val errorState = dialogState as LocationDialogState.Error
                        ErrorContent(
                            message = errorState.message,
                            onRetry = {
                                dialogState = LocationDialogState.Idle
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            // Confirm button is handled within each state's content
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LocationIdleContent(
    onGetCurrentLocation: () -> Unit,
    onSelectFromMap: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Select customer location to auto-populate address details",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onGetCurrentLocation,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Use Current Location")
        }

        OutlinedButton(
            onClick = onSelectFromMap,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select from Map")
        }
    }
}

@Composable
private fun LoadingContent(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator()
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LocationSelectedContent(
    location: LocationData,
    onLocationOnly: () -> Unit,
    onGetAddress: () -> Unit,
    onSelectDifferent: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Location Selected",
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

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onLocationOnly,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Use Location Only", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onGetAddress,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Get Address", style = MaterialTheme.typography.labelMedium)
                }
            }

            OutlinedButton(
                onClick = onSelectDifferent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Different Location", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun AddressResolvedContent(
    location: LocationData,
    address: AddressData,
    onLocationOnly: () -> Unit,
    onLocationAndAddress: () -> Unit,
    onSelectDifferent: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Address Found",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = address.formattedAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                if (address.city != null || address.state != null || address.pincode != null) {
                    Text(
                        text = buildString {
                            address.city?.let { append("$it") }
                            address.state?.let {
                                if (isNotEmpty()) append(", ")
                                append(it)
                            }
                            address.pincode?.let {
                                if (isNotEmpty()) append(" - ")
                                append(it)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onLocationOnly,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Location Only", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onLocationAndAddress,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Location & Address", style = MaterialTheme.typography.labelMedium)
                }
            }

            OutlinedButton(
                onClick = onSelectDifferent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Different Location", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Location Error",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Try Again")
        }
    }
}