package com.ampairs.supplier.util

/**
 * Constants for Supplier domain
 */
object SupplierConstants {

    /**
     * UID prefix for Supplier entities
     * Used in UID generation to create identifiers like: SUP20250123143045A1B2C3D4E5F6G7H
     */
    const val UID_PREFIX = "SUP"

    /**
     * Supplier Status Values
     */
    const val STATUS_ACTIVE = "ACTIVE"
    const val STATUS_INACTIVE = "INACTIVE"
    const val STATUS_SUSPENDED = "SUSPENDED"

    /**
     * Default Values
     */
    const val DEFAULT_COUNTRY_CODE = 91
    const val DEFAULT_COUNTRY = "India"

    /**
     * Validation Messages
     */
    const val ERROR_VALIDATION_FIX = "Please fix the errors before saving"
    const val ERROR_INVALID_EMAIL = "Please enter a valid email address"
    const val ERROR_SUPPLIER_NOT_FOUND = "Supplier not found"
    const val ERROR_SUPPLIER_UID_REQUIRED = "Supplier UID must be set before calling createSupplier"
}
