package com.ampairs.event.di

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.config.ConfigurationManager
import com.ampairs.common.httpClient
import com.ampairs.common.refreshTokens
import com.ampairs.event.EventManagerFactory
import io.ktor.client.engine.HttpClientEngine
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for Event synchronization system.
 * Provides workspace-scoped EventManager instances.
 *
 * Usage in repository or ViewModel:
 * ```kotlin
 * // Inject EventManager with workspace parameters
 * val eventManager: EventManager = koinInject {
 *     parametersOf(workspaceId, userId, deviceId)
 * }
 * ```
 *
 * The EventManager can also be retrieved without parameters if workspace context is already set:
 * ```kotlin
 * val eventManager: EventManager? = getOrNull()  // Returns null if not connected
 * ```
 */
val eventModule: Module = module {
    /**
     * Provide EventManager as factory with workspace-specific parameters.
     * Creates or retrieves existing EventManager from EventManagerFactory.
     *
     * Parameters (in order):
     * - workspaceId: String - Workspace identifier
     * - userId: String - Current user identifier
     * - deviceId: String - Current device identifier
     */
    factory { params ->
        val workspaceId = params.get<String>(0)
        val userId = params.get<String>(1)
        val deviceId = params.get<String>(2)

        val tokenRepository = get<TokenRepository>()

        EventManagerFactory.getOrCreate(
            workspaceId = workspaceId,
            userId = userId,
            deviceId = deviceId,
            httpClient = httpClient(
                engine = get<HttpClientEngine>(),
                tokenRepository = tokenRepository,
                withTimeout = false,
            ),
            tokenProvider = { tokenRepository.getAccessToken() ?: "" },
            tokenRefresher = { refreshTokens(tokenRepository) },
            baseUrl = ConfigurationManager.apiBaseUrl
        )
    }
}

/**
 * Helper function to expose the module (for consistency with other modules)
 */
fun eventModule() = eventModule
