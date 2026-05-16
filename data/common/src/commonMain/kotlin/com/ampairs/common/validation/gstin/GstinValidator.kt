package com.ampairs.common.validation.gstin

import com.ampairs.common.validation.Invalid
import com.ampairs.common.validation.Valid
import com.ampairs.common.validation.ValidationResult
import com.ampairs.common.validation.Validator

class GstinValidator : Validator<String?, String> {

    companion object {
        // GSTIN format: 15 characters - 2 digits (state code) + 10 alphanumeric (PAN) + 1 digit (entity code) + 1 alphabet (default Z) + 1 digit (checksum)
        private val GSTIN_REGEX = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}[Z]{1}[0-9A-Z]{1}\$")

        // Valid state codes (1-38 for states/UTs)
        private val VALID_STATE_CODES = (1..38).map { it.toString().padStart(2, '0') }.toSet()
    }

    override fun validate(input: String?): ValidationResult<String> {
        if (input == null) return Invalid(GstinValidationError.InputIsNull)

        val cleanInput = input.trim().uppercase()

        if (cleanInput.isEmpty()) return Invalid(GstinValidationError.Empty)

        if (cleanInput.length != 15) return Invalid(GstinValidationError.InvalidLength)

        if (!cleanInput.matches(GSTIN_REGEX)) return Invalid(GstinValidationError.InvalidFormat)

        // Validate state code
        val stateCode = cleanInput.substring(0, 2)
        if (stateCode !in VALID_STATE_CODES) return Invalid(GstinValidationError.InvalidStateCode)

        // Validate checksum (simplified - in real implementation you'd calculate the actual checksum)
        if (!isValidChecksum(cleanInput)) return Invalid(GstinValidationError.InvalidChecksum)

        return Valid(cleanInput)
    }

    private fun isValidChecksum(gstin: String): Boolean {
        // Simplified checksum validation
        // In a real implementation, you would calculate the GSTIN checksum algorithm
        // For now, we'll just check that the last character is alphanumeric
        val lastChar = gstin.last()
        return lastChar.isLetterOrDigit()
    }
}