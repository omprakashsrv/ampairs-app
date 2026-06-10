package com.ampairs.formwidgets.contact

/**
 * Platform-agnostic contact picker service interface.
 * Provides contact selection capabilities across Android and iOS platforms.
 * Desktop platform does not support contact picker.
 */
expect class ContactPickerService {
    /**
     * Check if contact picker is available on this platform
     */
    fun isAvailable(): Boolean

    /**
     * Pick a contact from the device's contact list
     * @return Result containing ContactData or an error
     */
    suspend fun pickContact(): Result<ContactData>

    /**
     * Check if contact read permission is granted
     */
    suspend fun hasContactPermission(): Boolean

    /**
     * Request contact read permission (platform-specific)
     */
    suspend fun requestContactPermission(): Boolean
}

/**
 * Contact information extracted from device contacts
 */
data class ContactData(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val countryCode: String = "+91", // Default to India
    val organization: String = "",
    val address: String = "",
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val country: String = ""
)

/**
 * Contact picker errors
 */
sealed class ContactPickerError(message: String) : Exception(message) {
    object PermissionDenied : ContactPickerError("Contact permission was denied. Please grant contact access in settings.")
    object Cancelled : ContactPickerError("Contact selection was cancelled.")
    object NotAvailable : ContactPickerError("Contact picker is not available on this platform.")
    object NoContactSelected : ContactPickerError("No contact was selected.")
    data class Unknown(override val cause: Throwable) : ContactPickerError("An unknown error occurred: ${cause.message}")
}
