package com.ampairs.workspace.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.delete
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.workspace.api.model.InstalledModule
import com.ampairs.workspace.api.model.AvailableModule
import com.ampairs.workspace.api.model.ModuleInstallationResponse
import com.ampairs.workspace.api.model.ModuleUninstallationResponse
import com.ampairs.workspace.api.model.ModuleDetailResponse
import io.ktor.client.engine.HttpClientEngine

/**
 * Implementation that exactly matches the web service calls
 */
class WorkspaceModuleApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : WorkspaceModuleApi {

    private val client = httpClient(engine, tokenRepository)

    companion object {
        private const val BASE_URL = "v1/modules"
    }

    override suspend fun getInstalledModules(workspaceId: String): Result<List<InstalledModule>> {
        return try {
            val response: Response<List<InstalledModule>> = get(
                client,
                ApiUrlBuilder.workspaceUrl(BASE_URL)
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAvailableModules(
        category: String?,
        featured: Boolean
    ): Result<List<AvailableModule>> {
        return try {
            val parameters = buildMap<String, Any> {
                category?.let { put("category", it) }
                if (featured) put("featured", featured)
            }

            val response: Response<List<AvailableModule>> = get(
                client,
                ApiUrlBuilder.workspaceUrl("$BASE_URL/available"),
                if (parameters.isNotEmpty()) parameters else null
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun installModule(
        workspaceId: String,
        moduleCode: String
    ): Result<ModuleInstallationResponse> {
        return try {
            val response: Response<ModuleInstallationResponse> = post(
                client,
                ApiUrlBuilder.workspaceUrl("$BASE_URL/install/$moduleCode"),
                null // No body, like other working POST endpoints
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uninstallModule(
        workspaceId: String,
        moduleId: String
    ): Result<ModuleUninstallationResponse> {
        return try {
            val response: Response<ModuleUninstallationResponse> = delete(
                client,
                ApiUrlBuilder.workspaceUrl("$BASE_URL/$moduleId"),
                null // No body needed, workspace context sent via X-Workspace-ID header
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getModuleDetails(
        workspaceId: String,
        moduleId: String
    ): Result<ModuleDetailResponse> {
        return try {
            val response: Response<ModuleDetailResponse> = get(
                client,
                ApiUrlBuilder.workspaceUrl("$BASE_URL/$moduleId")
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}