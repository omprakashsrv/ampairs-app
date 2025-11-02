package com.ampairs.auth.db

import com.ampairs.auth.api.AuthApi
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.model.AuthComplete
import com.ampairs.auth.api.model.AuthInit
import com.ampairs.auth.api.model.AuthInitResponse
import com.ampairs.auth.api.model.FirebaseAuthRequest
import com.ampairs.auth.api.model.Token
import com.ampairs.auth.api.model.UserApiModel
import com.ampairs.auth.db.dao.UserDao
import com.ampairs.auth.db.dao.UserTokenDao
import com.ampairs.auth.db.entity.UserEntity
import com.ampairs.auth.domain.DeviceSession
import com.ampairs.auth.domain.UserToken
import com.ampairs.auth.domain.asDatabaseModel
import com.ampairs.auth.domain.asDomainModel
import com.ampairs.auth.service.RecaptchaService
import com.ampairs.common.DeviceService
import com.ampairs.common.model.GenericSuccess
import com.ampairs.common.model.Response

class UserRepository(
    val authApi: AuthApi,
    val userTokenDao: UserTokenDao,
    val userDao: UserDao,
    val deviceService: DeviceService,
    val recaptchaService: RecaptchaService,
    val tokenRepository: TokenRepository,
) {
    suspend fun initAuth(phoneNumber: String): Response<AuthInitResponse> {
        val deviceInfo = deviceService.getDeviceInfo()
        val recaptchaToken = recaptchaService.executeLogin()
        
        return authApi.initAuth(
            AuthInit(
                countryCode = 91,
                phone = phoneNumber,
                recaptchaToken = recaptchaToken,
                deviceId = deviceInfo.deviceId,
                deviceName = deviceInfo.deviceName,
                deviceType = deviceInfo.deviceType,
                platform = deviceInfo.platform,
                browser = deviceInfo.browser,
                os = deviceInfo.os
            )
        )
    }

    suspend fun completeAuth(sessionId: String, otp: String): Response<Token> {
        val deviceInfo = deviceService.getDeviceInfo()
        val recaptchaToken = recaptchaService.executeVerifyOtp()

        val completeAuth = authApi.completeAuth(
            AuthComplete(
                sessionId = sessionId,
                otp = otp,
                authMode = "SMS",
                recaptchaToken = recaptchaToken,
                deviceId = deviceInfo.deviceId,
                deviceName = deviceInfo.deviceName,
            )
        )
        authApi.clearToken()
        return completeAuth
    }

    suspend fun verifyFirebaseAuth(firebaseIdToken: String, phoneNumber: String): Response<Token> {
        val deviceInfo = deviceService.getDeviceInfo()
        val recaptchaToken = recaptchaService.executeFirebaseVerify()

        val firebaseAuth = authApi.verifyFirebaseAuth(
            FirebaseAuthRequest(
                firebaseIdToken = firebaseIdToken,
                phone = phoneNumber,
                countryCode = 91,
                recaptchaToken = recaptchaToken,
                deviceId = deviceInfo.deviceId,
                deviceName = deviceInfo.deviceName,
            )
        )
        authApi.clearToken()
        return firebaseAuth
    }

    suspend fun getUserApi(): Response<UserApiModel> {
        return authApi.getUser()
    }

    suspend fun getToken(): UserToken? {
        return userTokenDao.selectById()?.asDomainModel()
    }

    suspend fun getUser(): UserEntity? {
        // Get current user from session management, fallback to first user for legacy support
        val currentUserId = getCurrentUserId()
        return if (currentUserId != null) {
            userDao.selectById(currentUserId)
        } else {
            // Fallback to first user for legacy compatibility
            userDao.selectAll().firstOrNull()
        }
    }

    private suspend fun getCurrentUserId(): String? {
        // Use TokenRepository to get current user ID instead of parsing JWT tokens
        return tokenRepository.getCurrentUserId()
    }
    
    suspend fun getUserById(userId: String): UserEntity? {
        return userDao.selectById(userId)
    }
    
    suspend fun getAllUsers(): List<UserEntity> {
        return userDao.selectAll()
    }

    suspend fun saveUser(user: UserApiModel) {
        userDao.insert(user.asDatabaseModel())
    }
    
    suspend fun deleteUser(userId: String) {
        userDao.deleteById(userId)
    }

    suspend fun getDeviceSessions(): Response<List<DeviceSession>> {
        return authApi.getDeviceSessions()
    }

    suspend fun logoutDevice(deviceId: String): Response<GenericSuccess> {
        return authApi.logoutDevice(deviceId)
    }

    suspend fun logoutAllDevices(): Response<GenericSuccess> {
        return authApi.logoutAllDevices()
    }

    suspend fun resendOtp(phoneNumber: String): Response<AuthInitResponse> {
        val deviceInfo = deviceService.getDeviceInfo()
        val recaptchaToken = recaptchaService.executeResendOtp()

        return authApi.initAuth(
            AuthInit(
                countryCode = 91,
                phone = phoneNumber,
                recaptchaToken = recaptchaToken,
                deviceId = deviceInfo.deviceId,
                deviceName = deviceInfo.deviceName,
                deviceType = deviceInfo.deviceType,
                platform = deviceInfo.platform,
                browser = deviceInfo.browser,
                os = deviceInfo.os
            )
        )
    }

    suspend fun findExistingUser(countryCode: Long, phone: String): UserEntity? {
        return userDao.findByCountryCodeAndPhone(countryCode, phone)
    }

}