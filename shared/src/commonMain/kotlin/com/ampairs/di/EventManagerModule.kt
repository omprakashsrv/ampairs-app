package com.ampairs.di

import com.ampairs.auth.api.AuthApi
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.di.AppScope
import com.ampairs.event.EventManagerFactory
import com.ampairs.workspace.EventManagerProvider
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine

@ContributesTo(AppScope::class)
interface EventManagerModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideEventManagerProvider(
            engine: HttpClientEngine,
            tokenRepository: TokenRepository,
            authApi: AuthApi
        ): EventManagerProvider {
            val httpClient = HttpClient(engine)
            return EventManagerProvider { workspaceId, userId, deviceId ->
                EventManagerFactory.getOrCreate(
                    workspaceId = workspaceId,
                    userId = userId,
                    deviceId = deviceId,
                    httpClient = httpClient,
                    tokenProvider = { tokenRepository.getAccessToken() ?: "" },
                    tokenRefresher = { authApi.refreshToken().data != null }
                )
            }
        }
    }
}
