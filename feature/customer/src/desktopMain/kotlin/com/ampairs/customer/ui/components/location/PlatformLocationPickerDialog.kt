package com.ampairs.customer.ui.components.location

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Desktop actual implementation using OpenStreetMap integration
 */
@Composable
actual fun PlatformLocationPickerDialog(
    showDialog: Boolean,
    currentLocation: LocationData?,
    onLocationSelected: (LocationData, AddressData?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier,
    locationService: LocationService
) {
    DesktopLocationPickerDialog(
        showDialog = showDialog,
        currentLocation = currentLocation,
        onLocationSelected = onLocationSelected,
        onDismiss = onDismiss,
        modifier = modifier,
        locationService = locationService
    )
}