package com.ampairs.auth.api

import com.ampairs.auth.api.model.AuthComplete
import com.ampairs.auth.api.model.AuthInit
import com.ampairs.auth.api.model.AuthInitResponse
import com.ampairs.auth.api.model.FirebaseAuthRequest
import com.ampairs.auth.api.model.RefreshToken
import com.ampairs.auth.api.model.Token
import com.ampairs.auth.api.model.UserApiModel
import com.ampairs.auth.api.model.UserUpdateRequest
import com.ampairs.auth.domain.DeviceSession
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.model.GenericSuccess
import com.ampairs.common.model.Response
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider

class AuthApiImpl(engine: HttpClientEngine, private val tokenRepository: TokenRepository) : AuthApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun initAuth(authInit: AuthInit): Response<AuthInitResponse> {
        return post(
            client,
            ApiUrlBuilder.authUrl("auth/v1/init"),
            authInit
        )
    }

    override suspend fun completeAuth(authComplete: AuthComplete): Response<Token> {
        return post(
            client,
            ApiUrlBuilder.authUrl("auth/v1/verify"),
            authComplete
        )
    }

    override suspend fun verifyFirebaseAuth(firebaseAuthRequest: FirebaseAuthRequest): Response<Token> {
        return post(
            client,
            ApiUrlBuilder.authUrl("auth/v1/verify/firebase"),
            firebaseAuthRequest
        )
    }

    override suspend fun refreshToken(deviceId: String?): Response<Token> {
        val refreshTokenModel = RefreshToken(
            refreshToken = tokenRepository.getRefreshToken(),
            deviceId = deviceId
        )
        return post(client, ApiUrlBuilder.authUrl("auth/v1/refresh_token"), refreshTokenModel)
    }

    override suspend fun getUser(): Response<UserApiModel> {
        return get(client, ApiUrlBuilder.userUrl("v1"))
    }

    override suspend fun updateUser(userUpdateRequest: UserUpdateRequest): Response<UserApiModel> {
        return post(client, ApiUrlBuilder.userUrl("v1/update"), userUpdateRequest)
    }

    override suspend fun getDeviceSessions(): Response<List<DeviceSession>> {
        return get(client, ApiUrlBuilder.authUrl("auth/v1/devices"))
    }

    override suspend fun logoutDevice(deviceId: String): Response<GenericSuccess> {
        return post(client, ApiUrlBuilder.authUrl("auth/v1/devices/$deviceId/logout"), null)
    }

    override suspend fun logoutAllDevices(): Response<GenericSuccess> {
        return post(client, ApiUrlBuilder.authUrl("auth/v1/logout/all"), null)
    }

    override fun clearToken() {
        client.authProvider<BearerAuthProvider>()?.clearToken()
    }
}