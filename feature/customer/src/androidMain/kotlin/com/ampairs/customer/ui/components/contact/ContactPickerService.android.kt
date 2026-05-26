package com.ampairs.customer.ui.components.contact

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.ampairs.common.CurrentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

actual class ContactPickerService {

    actual fun isAvailable(): Boolean = true

    actual suspend fun pickContact(): Result<ContactData> = withContext(Dispatchers.Main) {
        val activity = CurrentActivity.get()
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

        // Default country code if we can't extract one
        val defaultCode = "+91"

        return when {
            cleaned.startsWith("+91") && cleaned.length > 3 -> {
                Pair("+91", cleaned.substring(3))
            }
            cleaned.startsWith("+") && cleaned.length > 2 -> {
                // Try to extract country code (1-4 digits after +)
                val possibleCodes = listOf(
                    cleaned.substring(0, minOf(4, cleaned.length)), // +XXX
                    cleaned.substring(0, minOf(3, cleaned.length)), // +XX
                    cleaned.substring(0, minOf(2, cleaned.length))  // +X
                )
                var code = defaultCode
                var number = cleaned
                for (c in possibleCodes) {
                    // Validate that the code has at least one digit after +
                    val codeDigits = c.removePrefix("+")
                    if (codeDigits.isNotEmpty() && codeDigits.all { it.isDigit() } && cleaned.length > c.length) {
                        code = c
                        number = cleaned.substring(c.length)
                        break
                    }
                }
                // If code is still invalid (no digits after +), use default
                val finalCodeDigits = code.removePrefix("+")
                if (finalCodeDigits.isEmpty() || !finalCodeDigits.all { it.isDigit() }) {
                    Pair(defaultCode, cleaned.removePrefix("+"))
                } else {
                    Pair(code, number)
                }
            }
            cleaned.length == 10 -> {
                // Indian mobile number without country code
                Pair(defaultCode, cleaned)
            }
            cleaned.startsWith("0") && cleaned.length == 11 -> {
                // Indian number with leading 0 (e.g., 09876543210)
                Pair(defaultCode, cleaned.substring(1))
            }
            else -> {
                // Default to +91 for any other format
                Pair(defaultCode, cleaned)
            }
        }
    }
}
