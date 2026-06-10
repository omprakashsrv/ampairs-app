package com.ampairs.formwidgets.contact

/**
 * Desktop implementation of ContactPickerService.
 * Contact picker is not available on Desktop platform.
 */
actual class ContactPickerService {

    actual fun isAvailable(): Boolean = false

    actual suspend fun pickContact(): Result<ContactData> {
        return Result.failure(ContactPickerError.NotAvailable)
    }

    actual suspend fun hasContactPermission(): Boolean = false

    actual suspend fun requestContactPermission(): Boolean = false
}
