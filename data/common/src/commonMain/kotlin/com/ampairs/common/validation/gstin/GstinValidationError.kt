package com.ampairs.common.validation.gstin

import com.ampairs.common.validation.ValidationError

sealed class GstinValidationError(override val details: String? = null) : ValidationError {
    data object InputIsNull : GstinValidationError("Input is not a valid GSTIN because it is null.")
    data object Empty : GstinValidationError("Input is not a valid GSTIN because it is empty.")
    data object InvalidLength : GstinValidationError("GSTIN must be exactly 15 characters.")
    data object InvalidFormat : GstinValidationError("Input is not in a valid GSTIN format.")
    data object InvalidStateCode : GstinValidationError("Input contains an invalid state code.")
    data object InvalidChecksum : GstinValidationError("Input contains an invalid checksum.")

    val message: String
        get() = when (this) {
            is InputIsNull -> "GSTIN cannot be null"
            is Empty -> "GSTIN cannot be empty"
            is InvalidLength -> "GSTIN must be exactly 15 characters"
            is InvalidFormat -> "Invalid GSTIN format"
            is InvalidStateCode -> "Invalid state code in GSTIN"
            is InvalidChecksum -> "Invalid GSTIN checksum"
        }
}