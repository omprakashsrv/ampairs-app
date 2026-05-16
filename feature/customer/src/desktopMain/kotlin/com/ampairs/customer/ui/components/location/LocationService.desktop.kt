package com.ampairs.customer.ui.components.location

import kotlinx.coroutines.delay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import javax.swing.SwingUtilities

/**
 * Desktop implementation of LocationService
 * Features: Enhanced mock selection with intelligent location variation
 * Future: Will integrate with OpenStreetMap when library becomes available
 * Current: Dynamic mock implementation with multiple Indian cities and contextual selection
 */
@OptIn(ExperimentalTime::class)
actual class LocationService {

    actual suspend fun getCurrentLocation(): Result<LocationData> {
        return try {
            // Simulate GPS fetch delay
            delay(1.seconds)

            // Mock current location (Bangalore with some randomness)
            val baseLatitude = 12.9716
            val baseLongitude = 77.5946
            val randomOffset = 0.01

            val location = LocationData(
                latitude = baseLatitude + (Random.nextDouble() - 0.5) * randomOffset,
                longitude = baseLongitude + (Random.nextDouble() - 0.5) * randomOffset,
                address = "Current Location, Bangalore, Karnataka, India",
                accuracy = Random.nextDouble(5.0, 50.0),
                timestamp = Clock.System.now().toEpochMilliseconds()
            )

            Result.success(location)
        } catch (e: Exception) {
            Result.failure(LocationError.LocationUnavailable)
        }
    }

    actual suspend fun selectLocationFromMap(initialLocation: LocationData?): Result<LocationData> {
        return try {
            // Simulate map selection delay
            delay(2.seconds)

            // Mock map selection - vary location based on initial location or use random major city
            // Future: Will integrate with OpenStreetMap when library becomes available
            val selectedLocation = when {
                // If initial location provided, simulate selection near it
                initialLocation != null -> LocationData(
                    latitude = initialLocation.latitude + (Random.nextDouble() - 0.5) * 0.02,
                    longitude = initialLocation.longitude + (Random.nextDouble() - 0.5) * 0.02,
                    address = "Selected near ${initialLocation.address ?: "previous location"}",
                    accuracy = 5.0,
                    timestamp = Clock.System.now().toEpochMilliseconds()
                )
                // Otherwise, simulate selection from random major Indian cities
                else -> {
                    val cities = listOf(
                        Triple(12.9716, 77.5946, "Selected from Map: Bangalore, Karnataka"),
                        Triple(19.0760, 72.8777, "Selected from Map: Mumbai, Maharashtra"),
                        Triple(28.6139, 77.2090, "Selected from Map: New Delhi, Delhi"),
                        Triple(22.5726, 88.3639, "Selected from Map: Kolkata, West Bengal"),
                        Triple(13.0827, 80.2707, "Selected from Map: Chennai, Tamil Nadu")
                    )
                    val randomCity = cities.random()
                    LocationData(
                        latitude = randomCity.first + (Random.nextDouble() - 0.5) * 0.01,
                        longitude = randomCity.second + (Random.nextDouble() - 0.5) * 0.01,
                        address = randomCity.third,
                        accuracy = 8.0,
                        timestamp = Clock.System.now().toEpochMilliseconds()
                    )
                }
            }

            Result.success(selectedLocation)
        } catch (e: Exception) {
            Result.failure(LocationError.ServiceUnavailable)
        }
    }

    actual suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<AddressData> {
        return try {
            delay(1.seconds)

            // Mock geocoding based on coordinate ranges
            val address = when {
                // Bangalore area
                latitude in 12.8..13.1 && longitude in 77.4..77.8 -> AddressData(
                    formattedAddress = "Sample Street, Bangalore, Karnataka 560001, India",
                    street = "Sample Street",
                    streetNumber = "${Random.nextInt(1, 999)}",
                    city = "Bangalore",
                    state = "Karnataka",
                    pincode = "560001",
                    country = "India",
                    countryCode = "IN",
                    locality = "Indiranagar"
                )
                // Mumbai area
                latitude in 19.0..19.3 && longitude in 72.7..73.0 -> AddressData(
                    formattedAddress = "Sample Road, Mumbai, Maharashtra 400001, India",
                    street = "Sample Road",
                    streetNumber = "${Random.nextInt(1, 999)}",
                    city = "Mumbai",
                    state = "Maharashtra",
                    pincode = "400001",
                    country = "India",
                    countryCode = "IN",
                    locality = "Bandra"
                )
                // Delhi area
                latitude in 28.4..28.9 && longitude in 76.8..77.5 -> AddressData(
                    formattedAddress = "Sample Lane, New Delhi, Delhi 110001, India",
                    street = "Sample Lane",
                    streetNumber = "${Random.nextInt(1, 999)}",
                    city = "New Delhi",
                    state = "Delhi",
                    pincode = "110001",
                    country = "India",
                    countryCode = "IN",
                    locality = "Connaught Place"
                )
                else -> AddressData(
                    formattedAddress = "Unknown Location, India",
                    street = "Unknown Street",
                    city = "Unknown City",
                    state = "Unknown State",
                    pincode = "000000",
                    country = "India",
                    countryCode = "IN"
                )
            }

            Result.success(address)
        } catch (e: Exception) {
            Result.failure(LocationError.NetworkError)
        }
    }

    actual suspend fun searchLocations(query: String): Result<List<LocationSearchResult>> {
        return try {
            delay(1.seconds)

            // Mock search results
            val mockResults = listOf(
                LocationSearchResult(
                    location = LocationData(12.9716, 77.5946, "Bangalore"),
                    address = AddressData("Bangalore, Karnataka, India", city = "Bangalore", state = "Karnataka", country = "India"),
                    displayName = "Bangalore, Karnataka",
                    type = LocationType.CITY
                ),
                LocationSearchResult(
                    location = LocationData(19.0760, 72.8777, "Mumbai"),
                    address = AddressData("Mumbai, Maharashtra, India", city = "Mumbai", state = "Maharashtra", country = "India"),
                    displayName = "Mumbai, Maharashtra",
                    type = LocationType.CITY
                ),
                LocationSearchResult(
                    location = LocationData(28.6139, 77.2090, "Delhi"),
                    address = AddressData("New Delhi, Delhi, India", city = "New Delhi", state = "Delhi", country = "India"),
                    displayName = "New Delhi, Delhi",
                    type = LocationType.CITY
                )
            ).filter { it.displayName.contains(query, ignoreCase = true) }

            Result.success(mockResults)
        } catch (e: Exception) {
            Result.failure(LocationError.NetworkError)
        }
    }

    actual suspend fun hasLocationPermission(): Boolean {
        // Desktop doesn't typically require location permissions like mobile
        return true
    }

    actual suspend fun requestLocationPermission(): Boolean {
        // Desktop doesn't require permission request
        return true
    }
}