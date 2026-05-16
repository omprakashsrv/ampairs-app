package com.ampairs.customer.ui.components.location

import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Platform-agnostic location service interface
 * Provides location selection and geocoding capabilities across all platforms
 */
expect class LocationService {
    /**
     * Get current device location using GPS
     */
    suspend fun getCurrentLocation(): Result<LocationData>

    /**
     * Open platform-specific map for location selection
     * @param initialLocation Optional initial location to center map
     */
    suspend fun selectLocationFromMap(initialLocation: LocationData? = null): Result<LocationData>

    /**
     * Convert coordinates to human-readable address
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<AddressData>

    /**
     * Search for locations by query string
     */
    suspend fun searchLocations(query: String): Result<List<LocationSearchResult>>

    /**
     * Check if location permissions are granted
     */
    suspend fun hasLocationPermission(): Boolean

    /**
     * Request location permissions (platform-specific)
     */
    suspend fun requestLocationPermission(): Boolean
}

/**
 * Enhanced location data with additional metadata
 */
@OptIn(ExperimentalTime::class)
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val accuracy: Double? = null, // Accuracy in meters
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * Comprehensive address information
 */
data class AddressData(
    val formattedAddress: String,
    val street: String? = null,
    val streetNumber: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val locality: String? = null,
    val subLocality: String? = null
)

/**
 * Location search result
 */
data class LocationSearchResult(
    val location: LocationData,
    val address: AddressData,
    val displayName: String,
    val type: LocationType = LocationType.UNKNOWN
)

/**
 * Type of location for search results
 */
enum class LocationType {
    ADDRESS,
    BUSINESS,
    LANDMARK,
    CITY,
    STATE,
    COUNTRY,
    UNKNOWN
}

/**
 * Location service errors
 */
sealed class LocationError(message: String) : Exception(message) {
    object PermissionDenied : LocationError("Location permission was denied. Please grant location access in settings.")
    object LocationUnavailable : LocationError("Unable to get current location. Please ensure GPS/Location Services are enabled.")
    object NetworkError : LocationError("Network error occurred while fetching location data.")
    object ServiceUnavailable : LocationError("Location service is currently unavailable.")
    data class Unknown(override val cause: Throwable) : LocationError("An unknown error occurred: ${cause.message}")
}