package com.ampairs.common.validation.phone

import com.ampairs.common.validation.ValidationError

sealed class PhoneNumberValidationError(override val details: String? = null) : ValidationError {

    /**
     * The provided input value to the [PhoneNumberValidator] was null. A null value is not a valid Phone Number.
     */
    data object InputIsNull :
        PhoneNumberValidationError(details = "Input is not a valid Phone Number because it is null.")

    /**
     * The provided input value to the [PhoneNumberValidator] was empty. An empty value is not a valid Phone Number.
     */
    data object Empty :
        PhoneNumberValidationError(details = "Input is not a valid Phone Number because it is empty.")

    /**
     * The provided input value to the [PhoneNumberValidator] was not in a valid Phone Number format.
     */
    data object InvalidFormat :
        PhoneNumberValidationError(details = "Input is not in a valid Phone Number Format.")

    /**
     * The provided input value contains an invalid country code.
     */
    data object InvalidCountryCode :
        PhoneNumberValidationError(details = "Input contains an invalid country code.")

    /**
     * The provided input value has an invalid length for a phone number.
     */
    data object InvalidLength :
        PhoneNumberValidationError(details = "Input has an invalid length for a phone number.")

    val message: String
        get() = when (this) {
            is InputIsNull -> "Phone number is required"
            is Empty -> "Phone number cannot be empty"
            is InvalidFormat -> "Invalid phone number format"
            is InvalidCountryCode -> "Invalid country code"
            is InvalidLength -> "Phone number length is invalid"
        }
}