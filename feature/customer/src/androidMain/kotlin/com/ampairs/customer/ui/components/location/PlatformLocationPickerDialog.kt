package com.ampairs.customer.ui.components.location

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Android actual implementation - uses common implementation for now
 * Future: Can be enhanced with Google Maps Compose integration
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
    // Use the common implementation for Android
    CommonLocationPickerDialog(
        showDialog = showDialog,
        currentLocation = currentLocation,
        onLocationSelected = onLocationSelected,
        onDismiss = onDismiss,
        modifier = modifier,
        locationService = locationService
    )
}