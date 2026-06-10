package com.ampairs.formwidgets.contact

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Contacts.CNAuthorizationStatus
import platform.Contacts.CNContactStore
import platform.Contacts.CNEntityType
import platform.Contacts.CNLabeledValue
import platform.Contacts.CNPhoneNumber
import platform.Contacts.CNPostalAddress
import platform.ContactsUI.CNContactPickerDelegateProtocol
import platform.ContactsUI.CNContactPickerViewController
import platform.Foundation.NSBundle
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual class ContactPickerService {
    private val contactStore = CNContactStore()
    private var contactDeferred: CompletableDeferred<Result<ContactData>>? = null

    actual fun isAvailable(): Boolean = true

    actual suspend fun pickContact(): Result<ContactData> = withContext(Dispatchers.Main) {
        if (!hasContactPermission()) {
            val granted = requestContactPermission()
            if (!granted) {
                return@withContext Result.failure(ContactPickerError.PermissionDenied)
            }
        }

        contactDeferred = CompletableDeferred()

        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            ?: return@withContext Result.failure(ContactPickerError.NotAvailable)

        val picker = CNContactPickerViewController()
        val delegate = ContactPickerDelegate { contact ->
            if (contact != null) {
                contactDeferred?.complete(Result.success(contact))
            } else {
                contactDeferred?.complete(Result.failure(ContactPickerError.Cancelled))
            }
        }

        picker.delegate = delegate

        rootViewController.presentViewController(picker, animated = true, completion = null)

        contactDeferred?.await() ?: Result.failure(ContactPickerError.NoContactSelected)
    }

    actual suspend fun hasContactPermission(): Boolean {
        val status = CNContactStore.authorizationStatusForEntityType(CNEntityType.CNEntityTypeContacts)
        return status == 3L // CNAuthorizationStatusAuthorized
    }

    actual suspend fun requestContactPermission(): Boolean = withContext(Dispatchers.Main) {
        val currentStatus = CNContactStore.authorizationStatusForEntityType(CNEntityType.CNEntityTypeContacts)

        when (currentStatus) {
            3L -> return@withContext true // CNAuthorizationStatusAuthorized
            2L, 1L -> return@withContext false // CNAuthorizationStatusDenied, CNAuthorizationStatusRestricted
            else -> {
                // Not determined, request permission
                val permissionDeferred = CompletableDeferred<Boolean>()
                contactStore.requestAccessForEntityType(CNEntityType.CNEntityTypeContacts) { granted, _ ->
                    permissionDeferred.complete(granted)
                }
                permissionDeferred.await()
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class ContactPickerDelegate(
    private val onContactSelected: (ContactData?) -> Unit
) : NSObject(), CNContactPickerDelegateProtocol {

    override fun contactPickerDidCancel(picker: CNContactPickerViewController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onContactSelected(null)
    }

    override fun contactPicker(picker: CNContactPickerViewController, didSelectContact: platform.Contacts.CNContact) {
        picker.dismissViewControllerAnimated(true, completion = null)

        val contact = didSelectContact
        val name = "${contact.givenName} ${contact.familyName}".trim()

        // Get phone
        var phone = ""
        var countryCode = "+91"
        val phones = contact.phoneNumbers as? List<*>
        if (phones?.isNotEmpty() == true) {
            val labeledValue = phones.first() as? CNLabeledValue
            val phoneNumber = labeledValue?.value as? CNPhoneNumber
            val fullPhone = phoneNumber?.stringValue ?: ""
            if (fullPhone.isNotEmpty()) {
                val (code, number) = parsePhoneNumber(fullPhone)
                countryCode = code
                phone = number
            }
        }

        // Get email
        var email = ""
        val emails = contact.emailAddresses as? List<*>
        if (emails?.isNotEmpty() == true) {
            val labeledValue = emails.first() as? CNLabeledValue
            email = labeledValue?.value as? String ?: ""
        }

        // Get organization
        val organization = contact.organizationName ?: ""

        // Get address
        var street = ""
        var city = ""
        var state = ""
        var pincode = ""
        var country = ""

        val addresses = contact.postalAddresses as? List<*>
        if (addresses?.isNotEmpty() == true) {
            val labeledValue = addresses.first() as? CNLabeledValue
            val postalAddress = labeledValue?.value as? CNPostalAddress
            street = postalAddress?.street ?: ""
            city = postalAddress?.city ?: ""
            state = postalAddress?.state ?: ""
            pincode = postalAddress?.postalCode ?: ""
            country = postalAddress?.country ?: ""
        }

        val contactData = ContactData(
            name = name,
            email = email,
            phone = phone,
            countryCode = countryCode,
            organization = organization,
            address = street,
            street = street,
            city = city,
            state = state,
            pincode = pincode,
            country = country
        )

        onContactSelected(contactData)
    }

    private fun parsePhoneNumber(fullPhone: String): Pair<String, String> {
        val cleaned = fullPhone.replace(Regex("[^+\\d]"), "")

        return when {
            cleaned.startsWith("+91") && cleaned.length > 3 -> {
                Pair("+91", cleaned.substring(3))
            }
            cleaned.startsWith("+") && cleaned.length > 2 -> {
                val possibleCodes = listOf(
                    cleaned.substring(0, minOf(4, cleaned.length)),
                    cleaned.substring(0, minOf(3, cleaned.length)),
                    cleaned.substring(0, minOf(2, cleaned.length))
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
                Pair("+91", cleaned)
            }
            else -> {
                Pair("+91", cleaned)
            }
        }
    }
}
