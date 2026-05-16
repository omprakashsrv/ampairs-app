package com.ampairs.auth.domain

/**
 * Defines the authentication method used for login
 */
enum class AuthMethod {
    /**
     * Backend API authentication using phone + OTP
     */
    BACKEND_API,

    /**
     * Firebase authentication using phone number
     * Supported on Android and iOS
     */
    FIREBASE
}
