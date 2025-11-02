package com.ampairs.auth.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseAuthRequest(
    @SerialName("firebase_id_token")
    val firebaseIdToken: String,
    @SerialName("phone")
    val phone: String,
    @SerialName("country_code")
    val countryCode: Int = 91,
    @SerialName("recaptcha_token")
    val recaptchaToken: String? = null,
    @SerialName("device_id")
    val deviceId: String? = null,
    @SerialName("device_name")
    val deviceName: String? = null,
)
