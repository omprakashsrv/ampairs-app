package com.ampairs.customer.util

/**
 * Constants for Customer domain
 */
object CustomerConstants {

    /**
     * UID prefix for Customer entities
     * Used in UID generation to create identifiers like: CUS20250123143045A1B2C3D4E5F6G7H
     */
    const val UID_PREFIX = "CUS"

    /**
     * UID prefix for Customer Image entities
     * Used in UID generation to create identifiers like: IMG20250123143045A1B2C3D4E5F6G7H
     */
    const val CUSTOMER_IMAGE_UID_PREFIX = "IMG"

    /**
     * Customer Status Values
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
     * UI Labels
     */
    const val LABEL_CUSTOMER_TYPE = "Customer Type"
    const val LABEL_CUSTOMER_GROUP = "Customer Group"
    const val LABEL_STATUS = "Status"
    const val TITLE_CUSTOMERS = "Customers"
    const val TITLE_CUSTOMER_DETAILS = "Customer Details"

    /**
     * Validation Messages
     */
    const val ERROR_VALIDATION_FIX = "Please fix the errors before saving"
    const val ERROR_INVALID_EMAIL = "Please enter a valid email address"
    const val ERROR_INVALID_LANDLINE = "Please enter a valid landline number"
    const val ERROR_CUSTOMER_NOT_FOUND = "Customer not found"
    const val ERROR_CUSTOMER_UID_REQUIRED = "Customer UID must be set before calling createCustomer"
    const val ERROR_CUSTOMER_IMAGE_UID_REQUIRED = "Customer image UID must be set before calling createImage"
}