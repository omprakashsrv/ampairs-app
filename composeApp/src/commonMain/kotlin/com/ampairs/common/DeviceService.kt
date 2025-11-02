package com.ampairs.common

import com.ampairs.auth.domain.DeviceInfo

interface DeviceService {
    fun getDeviceInfo(): DeviceInfo
    fun getDeviceId(): String
    fun generateDeviceId(): String
    fun clearDeviceId()
}