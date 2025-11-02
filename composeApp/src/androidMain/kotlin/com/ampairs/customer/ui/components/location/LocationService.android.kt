package com.ampairs.customer.ui.components.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Request to show map picker - emitted by LocationService when selectLocationFromMap is called
 */
data class MapSelectionRequest(
    val initialLocation: LocationData?,
    val deferred: CompletableDeferred<LocationData>
)

/**
 * Request to show permission dialog - emitted when location permission is needed
 */
data class PermissionRequest(
    val deferred: CompletableDeferred<Boolean>
)

/**
 * Android implementation of LocationService using Google Play Services
 * Provides real GPS location, Google Maps integration, and Geocoding
 */
actual class LocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val geocoder: Geocoder? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Modern Android (API 33+) - callback-based geocoder
        Geocoder(context)
    } else if (Geocoder.isPresent()) {
        // Legacy Android with geocoder support
        Geocoder(context)
    } else {
        null
    }

    // Event flow for map selection requests - UI layer should collect this
    private val _mapSelectionRequests = MutableSharedFlow<MapSelectionRequest>(replay = 0)
    val mapSelectionRequests: SharedFlow<MapSelectionRequest> = _mapSelectionRequests.asSharedFlow()

    // Event flow for permission requests - UI layer should collect this
    private val _permissionRequests = MutableSharedFlow<PermissionRequest>(replay = 0)
    val permissionRequests: SharedFlow<PermissionRequest> = _permissionRequests.asSharedFlow()

    // Holds pending map selection deferred result
    private var pendingMapSelection: CompletableDeferred<LocationData>? = null

    // Holds pending permission request deferred result
    private var pendingPermissionRequest: CompletableDeferred<Boolean>? = null

    actual suspend fun getCurrentLocation(): Result<LocationData> {
        return withContext(Dispatchers.IO) {
            try {
                // Check permissions - request if not granted
                if (!hasLocationPermissionInternal()) {
                    val granted = requestPermissionFromUI()
                    if (!granted) {
                        return@withContext Result.failure(LocationError.PermissionDenied)
                    }
                }

                // Request current location with high accuracy and 30-second timeout
                val cancellationTokenSource = CancellationTokenSource()
                val location = try {
                    withTimeout(30000L) { // 30 second timeout for GPS fix
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            cancellationTokenSource.token
                        ).await()
                    }
                } catch (e: TimeoutCancellationException) {
                    // Cancel the location request
                    cancellationTokenSource.cancel()
                    throw e
                }

                if (location != null) {
                    // Get address from coordinates
                    val address = try {
                        val addressData = reverseGeocode(location.latitude, location.longitude).getOrNull()
                        addressData?.formattedAddress
                    } catch (e: Exception) {
                        null
                    }

                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = address,
                        accuracy = location.accuracy.toDouble(),
                        timestamp = location.time
                    )
                    Result.success(locationData)
                } else {
                    Result.failure(LocationError.LocationUnavailable)
                }
            } catch (e: TimeoutCancellationException) {
                // GPS location fetch timed out - location services might be disabled
                Result.failure(LocationError.LocationUnavailable)
            } catch (e: SecurityException) {
                Result.failure(LocationError.PermissionDenied)
            } catch (e: Exception) {
                Result.failure(LocationError.LocationUnavailable)
            }
        }
    }

    actual suspend fun selectLocationFromMap(initialLocation: LocationData?): Result<LocationData> {
        return withContext(Dispatchers.Main) {
            try {
                // Create a deferred result that will be completed by the UI
                val deferred = CompletableDeferred<LocationData>()
                pendingMapSelection = deferred

                // Emit request for UI to show map picker
                _mapSelectionRequests.emit(MapSelectionRequest(initialLocation, deferred))

                // Wait for UI to complete the selection
                val selectedLocation = deferred.await()
                pendingMapSelection = null

                Result.success(selectedLocation)
            } catch (e: Exception) {
                pendingMapSelection = null
                Result.failure(LocationError.ServiceUnavailable)
            }
        }
    }

    /**
     * Internal method to request permission from UI layer
     * Timeout after 60 seconds to prevent infinite waiting
     */
    private suspend fun requestPermissionFromUI(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val deferred = CompletableDeferred<Boolean>()
                pendingPermissionRequest = deferred

                // Emit request for UI to show permission dialog
                _permissionRequests.emit(PermissionRequest(deferred))

                // Wait for UI to complete the permission request with timeout
                val granted = withTimeout(60000L) { // 60 second timeout
                    deferred.await()
                }
                pendingPermissionRequest = null

                granted
            } catch (e: TimeoutCancellationException) {
                // Permission request timed out - user didn't respond
                pendingPermissionRequest?.cancel()
                pendingPermissionRequest = null
                false
            } catch (e: Exception) {
                pendingPermissionRequest = null
                false
            }
        }
    }

    /**
     * Call this from UI when user selects a location from the map
     * This completes the pending map selection request
     */
    fun completeMapSelection(location: LocationData) {
        pendingMapSelection?.complete(location)
    }

    /**
     * Call this from UI when user cancels map selection
     */
    fun cancelMapSelection() {
        pendingMapSelection?.cancel()
        pendingMapSelection = null
    }

    /**
     * Call this from UI when permission is granted
     */
    fun completePermissionRequest(granted: Boolean) {
        pendingPermissionRequest?.complete(granted)
    }

    /**
     * Call this from UI when permission request is cancelled
     */
    fun cancelPermissionRequest() {
        pendingPermissionRequest?.complete(false)
        pendingPermissionRequest = null
    }

    actual suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<AddressData> {
        return withContext(Dispatchers.IO) {
            try {
                if (geocoder == null) {
                    return@withContext Result.failure(LocationError.ServiceUnavailable)
                }

                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Modern Android (API 33+) - use callback-based API
                    callbackFlow {
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            trySend(addresses)
                        }
                        awaitClose {}
                    }.first()
                } else {
                    // Legacy Android - use synchronous API
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)
                }

                if (addresses.isNullOrEmpty()) {
                    return@withContext Result.failure(LocationError.ServiceUnavailable)
                }

                val address = addresses.first()
                val addressData = address.toAddressData()
                Result.success(addressData)
            } catch (e: Exception) {
                Result.failure(LocationError.NetworkError)
            }
        }
    }

    actual suspend fun searchLocations(query: String): Result<List<LocationSearchResult>> {
        return withContext(Dispatchers.IO) {
            try {
                if (geocoder == null) {
                    return@withContext Result.failure(LocationError.ServiceUnavailable)
                }

                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Modern Android (API 33+) - use callback-based API
                    callbackFlow {
                        geocoder.getFromLocationName(query, 5) { addresses ->
                            trySend(addresses)
                        }
                        awaitClose {}
                    }.first()
                } else {
                    // Legacy Android - use synchronous API
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(query, 5)
                }

                if (addresses.isNullOrEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                val results = addresses.map { address ->
                    LocationSearchResult(
                        location = LocationData(
                            latitude = address.latitude,
                            longitude = address.longitude,
                            address = address.getAddressLine(0)
                        ),
                        address = address.toAddressData(),
                        displayName = address.getAddressLine(0) ?: query,
                        type = determineLocationType(address)
                    )
                }

                Result.success(results)
            } catch (e: Exception) {
                Result.failure(LocationError.NetworkError)
            }
        }
    }

    actual suspend fun hasLocationPermission(): Boolean {
        return hasLocationPermissionInternal()
    }

    actual suspend fun requestLocationPermission(): Boolean {
        // Permission requests must be done from Activity/Fragment context
        // This will be handled by the UI layer using Accompanist Permissions
        // Return current permission status
        return hasLocationPermissionInternal()
    }

    private fun hasLocationPermissionInternal(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    private fun Address.toAddressData(): AddressData {
        return AddressData(
            formattedAddress = getAddressLine(0) ?: "",
            street = thoroughfare,
            streetNumber = subThoroughfare,
            city = locality,
            state = adminArea,
            pincode = postalCode,
            country = countryName,
            countryCode = countryCode,
            locality = subLocality,
            subLocality = featureName
        )
    }

    private fun determineLocationType(address: Address): LocationType {
        return when {
            address.featureName != null && address.thoroughfare != null -> LocationType.ADDRESS
            address.locality != null && address.adminArea != null -> LocationType.CITY
            address.adminArea != null -> LocationType.STATE
            address.countryName != null -> LocationType.COUNTRY
            else -> LocationType.UNKNOWN
        }
    }
}
