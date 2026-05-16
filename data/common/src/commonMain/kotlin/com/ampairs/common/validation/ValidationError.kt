package com.ampairs.common.validation

interface ValidationError {

    /**
     * Detailed information about what this [ValidationError] represents and what error occurred.
     */
    val details: String?
}