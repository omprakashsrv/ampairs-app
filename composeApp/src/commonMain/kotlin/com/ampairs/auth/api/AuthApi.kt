package com.ampairs.auth.api

import com.ampairs.auth.api.model.AuthComplete
import com.ampairs.auth.api.model.AuthInit
import com.ampairs.auth.api.model.AuthInitResponse
import com.ampairs.auth.api.model.FirebaseAuthRequest
import com.ampairs.auth.api.model.Token
import com.ampairs.auth.api.model.UserApiModel
import com.ampairs.auth.api.model.UserUpdateRequest
import com.ampairs.auth.domain.DeviceSession
import com.ampairs.common.model.GenericSuccess
import com.ampairs.common.model.Response

interface AuthApi {

    suspend fun initAuth(authInit: AuthInit): Response<AuthInitResponse>

    suspend fun completeAuth(authComplete: AuthComplete): Response<Token>

    suspend fun verifyFirebaseAuth(firebaseAuthRequest: FirebaseAuthRequest): Response<Token>

    suspend fun refreshToken(deviceId: String? = null): Response<Token>

    suspend fun getUser(): Response<UserApiModel>

    suspend fun updateUser(userUpdateRequest: UserUpdateRequest): Response<UserApiModel>

    suspend fun getDeviceSessions(): Response<List<DeviceSession>>

    suspend fun logoutDevice(deviceId: String): Response<GenericSuccess>

    suspend fun logoutAllDevices(): Response<GenericSuccess>

    fun clearToken()

}