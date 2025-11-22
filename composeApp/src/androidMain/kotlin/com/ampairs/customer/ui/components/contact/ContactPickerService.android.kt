package com.ampairs.customer.ui.components.contact

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.ampairs.common.ActivityProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

actual class ContactPickerService {

    actual fun isAvailable(): Boolean = true

    actual suspend fun pickContact(): Result<ContactData> = withContext(Dispatchers.Main) {
        val activity = ActivityProvider.getActivity()
            ?: return@withContext Result.failure(ContactPickerError.NotAvailable)

        // Use phone picker which grants temporary permission to the selected phone contact
        // This doesn't require READ_CONTACTS permission
        suspendCancellableCoroutine { continuation ->
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
            }

            ContactPickerResultHolder.setCallback { uri ->
                if (uri != null) {
                    try {
                        val contactData = parsePhoneContactUri(activity.contentResolver, uri)
                        continuation.resume(Result.success(contactData))
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(ContactPickerError.Unknown(e)))
                    }
                } else {
                    continuation.resume(Result.failure(ContactPickerError.Cancelled))
                }
            }

            activity.startActivityForResult(intent, CONTACT_PICKER_REQUEST_CODE)
        }
    }

    // Permission not required for system contact picker
    actual suspend fun hasContactPermission(): Boolean = true

    // Permission not required for system contact picker
    actual suspend fun requestContactPermission(): Boolean = true

    companion object {
        const val CONTACT_PICKER_REQUEST_CODE = 9001
        const val CONTACT_PERMISSION_REQUEST_CODE = 9002
    }

    private fun parsePhoneContactUri(contentResolver: ContentResolver, phoneUri: Uri): ContactData {
        var name = ""
        var phone = ""
        var countryCode = "+91"

        // Query the specific phone URI that was picked - this has temporary permission
        contentResolver.query(
            phoneUri,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                if (nameIndex != -1) {
                    name = cursor.getString(nameIndex) ?: ""
                }

                if (numberIndex != -1) {
                    val fullPhone = cursor.getString(numberIndex) ?: ""
                    if (fullPhone.isNotBlank()) {
                        val (code, number) = parsePhoneNumber(fullPhone)
                        countryCode = code
                        phone = number

                        // Debug log
                        android.util.Log.d("ContactPicker", "Full phone: $fullPhone, Code: $code, Number: $number")
                    }
                }
            }
        }

        // Note: Email and address data are not available via phone picker
        // without READ_CONTACTS permission. Users can manually add those fields.
        return ContactData(
            name = name,
            phone = phone,
            countryCode = countryCode
        )
    }

    private fun parsePhoneNumber(fullPhone: String): Pair<String, String> {
        val cleaned = fullPhone.replace("[^+\\d]".toRegex(), "")

        return when {
            cleaned.startsWith("+91") && cleaned.length > 3 -> {
                Pair("+91", cleaned.substring(3))
            }
            cleaned.startsWith("+") && cleaned.length > 2 -> {
                // Try to extract country code (1-4 digits after +)
                val possibleCodes = listOf(
                    cleaned.substring(0, 4), // +XXX
                    cleaned.substring(0, 3), // +XX
                    cleaned.substring(0, 2)  // +X
                )
                var code = "+91"
                var number = cleaned
                for (c in possibleCodes) {
                    if (cleaned.length > c.length) {
                        code = c
                        number = cleaned.substring(c.length)
                        break
                    }
                }
                Pair(code, number)
            }
            cleaned.length == 10 -> {
                // Indian mobile number without country code
                Pair("+91", cleaned)
            }
            else -> {
                Pair("+91", cleaned)
            }
        }
    }
}
