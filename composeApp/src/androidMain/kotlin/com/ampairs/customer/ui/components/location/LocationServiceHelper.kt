package com.ampairs.customer.ui.components.location

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Composable that listens for location service requests (map selection and permissions)
 * and handles them with appropriate UI components.
 *
 * Place this at the root of your Android app to enable:
 * - selectLocationFromMap() functionality
 * - Automatic permission request when getCurrentLocation() is called
 *
 * Example:
 * ```
 * @Composable
 * fun MyApp() {
 *     LocationServiceMapHandler()
 *     // ... rest of your app
 * }
 * ```
 */
@Composable
fun LocationServiceMapHandler() {
    val locationService: LocationService = koinInject()
    val scope = rememberCoroutineScope()

    // Map selection state
    var showMapPicker by remember { mutableStateOf(false) }
    var currentMapRequest by remember { mutableStateOf<MapSelectionRequest?>(null) }

    // Permission request state
    var showPermissionDialog by remember { mutableStateOf(false) }
    var currentPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }

    // Collect map selection requests
    LaunchedEffect(Unit) {
        scope.launch {
            locationService.mapSelectionRequests.collectLatest { request ->
                currentMapRequest = request
                showMapPicker = true
            }
        }
    }

    // Collect permission requests
    LaunchedEffect(Unit) {
        scope.launch {
            locationService.permissionRequests.collectLatest { request ->
                currentPermissionRequest = request
                showPermissionDialog = true
            }
        }
    }

    // Show permission handler when requested
    if (showPermissionDialog && currentPermissionRequest != null) {
        PermissionRequestDialog(
            onPermissionResult = { granted ->
                locationService.completePermissionRequest(granted)
                showPermissionDialog = false
                currentPermissionRequest = null
            },
            onDismiss = {
                locationService.cancelPermissionRequest()
                showPermissionDialog = false
                currentPermissionRequest = null
            }
        )
    }

    // Show map picker when requested
    if (showMapPicker && currentMapRequest != null) {
        Dialog(
            onDismissRequest = {
                locationService.cancelMapSelection()
                showMapPicker = false
                currentMapRequest = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MapPickerScreen(
                initialLocation = currentMapRequest?.initialLocation,
                onLocationSelected = { location ->
                    locationService.completeMapSelection(location)
                    showMapPicker = false
                    currentMapRequest = null
                },
                onDismiss = {
                    locationService.cancelMapSelection()
                    showMapPicker = false
                    currentMapRequest = null
                }
            )
        }
    }
}

/**
 * Internal composable that automatically launches system permission dialog
 * and completes when user grants/denies permission
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionRequestDialog(
    onPermissionResult: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Track if we've launched and if we've already reported result
    var hasLaunched by remember { mutableStateOf(false) }
    var hasReportedResult by remember { mutableStateOf(false) }

    // Automatically launch permission request when composable shows
    LaunchedEffect(Unit) {
        if (locationPermissionState.allPermissionsGranted) {
            // Already granted, complete immediately
            hasReportedResult = true
            onPermissionResult(true)
        } else if (!hasLaunched) {
            hasLaunched = true
            locationPermissionState.launchMultiplePermissionRequest()
        }
    }

    // Monitor ONLY when permission is granted
    LaunchedEffect(locationPermissionState.allPermissionsGranted) {
        if (locationPermissionState.allPermissionsGranted && hasLaunched && !hasReportedResult) {
            // Permission granted - automatically continue location fetch
            hasReportedResult = true
            onPermissionResult(true)
        }
    }

    // Monitor for permission denial - separate effect
    LaunchedEffect(locationPermissionState.permissions.map { it.status }) {
        if (hasLaunched && !hasReportedResult && !locationPermissionState.allPermissionsGranted) {
            // Check if any permission was explicitly denied
            val anyDenied = locationPermissionState.permissions.any { perm ->
                perm.status is com.google.accompanist.permissions.PermissionStatus.Denied
            }

            if (anyDenied) {
                // User denied permission
                hasReportedResult = true
                onPermissionResult(false)
            }
        }
    }

    // Show minimal loading UI while waiting for permission dialog
    // Don't show the full LocationPermissionHandler cards
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
