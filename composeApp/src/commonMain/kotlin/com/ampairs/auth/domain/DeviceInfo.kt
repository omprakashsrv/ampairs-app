package com.ampairs.auth.domain

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val platform: String,
    val browser: String,
    val os: String,
    val userAgent: String
)

@Serializable
data class DeviceSession(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val platform: String,
    val browser: String,
    val os: String,
    val ipAddress: String,
    val location: String? = null,
    val lastActivity: String,
    val loginTime: String,
    val isCurrentDevice: Boolean
)