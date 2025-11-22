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
     * UID prefix for Customer Type entities
     * Used in UID generation to create identifiers like: CT20250123143045A1B2C3D4E5F6G7H8
     */
    const val CUSTOMER_TYPE_UID_PREFIX = "CT"

    /**
     * UID prefix for Customer Group entities
     * Used in UID generation to create identifiers like: CG20250123143045A1B2C3D4E5F6G7H8
     */
    const val CUSTOMER_GROUP_UID_PREFIX = "CG"

    /**
     * UID prefix for Customer Image entities
     * Used in UID generation to create identifiers like: CIMG20250123143045A1B2C3D4E5F6G
     */
    const val CUSTOMER_IMAGE_UID_PREFIX = "CIMG"

    /**
     * UID prefix for State entities
     * Used in UID generation to create identifiers like: STA20250123143045A1B2C3D4E5F6G7
     */
    const val STATE_UID_PREFIX = "STA"

    /**
     * UID prefix for Customer Field Config entities
     * Used in UID generation to create identifiers like: CFC20250123143045A1B2C3D4E5F6G7
     */
    const val CUSTOMER_FIELD_CONFIG_UID_PREFIX = "CFC"

    /**
     * UID prefix for Customer Attribute Definition entities
     * Used in UID generation to create identifiers like: CAD20250123143045A1B2C3D4E5F6G7
     */
    const val CUSTOMER_ATTRIBUTE_DEF_UID_PREFIX = "CAD"

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