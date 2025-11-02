package com.ampairs.common.security

/**
 * Multiplatform SecurityException for common security-related errors
 */
class SecurityException(message: String, cause: Throwable? = null) : Exception(message, cause)