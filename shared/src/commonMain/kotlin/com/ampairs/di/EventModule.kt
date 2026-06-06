package com.ampairs.di

import com.ampairs.auth.api.AuthApi
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.DeviceService
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.event.EventManager
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets

@ContributesTo(WorkspaceScope::class)
interface EventModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideEventManager(
            config: WorkspaceConfig,
            deviceService: DeviceService,
            engine: HttpClientEngine,
            tokenRepository: TokenRepository,
            authApi: AuthApi,
            closableRegistry: WorkspaceClosableRegistry,
        ): EventManager {
            val wsHttpClient = HttpClient(engine) {
                install(WebSockets)
                // The shared engine has socketTimeout = 15s for regular API calls. WebSocket
                // sessions are long-lived and receive no frames when idle, so we must override
                // the socket timeout to prevent the 15s disconnect cycle.
                install(HttpTimeout) {
                    socketTimeoutMillis = Long.MAX_VALUE
                }
            }
            return EventManager(
                workspaceId = config.workspaceId,
                deviceId = deviceService.getDeviceId(),
                httpClient = wsHttpClient,
                tokenProvider = { tokenRepository.getAccessToken() ?: "" },
                tokenRefresher = { authApi.refreshToken().data != null },
            ).also { manager ->
                closableRegistry.register(manager)
            }
        }
    }
}
