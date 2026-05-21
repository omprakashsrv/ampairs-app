package com.ampairs.workspace.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import com.ampairs.common.delete
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.postMultiPart
import com.ampairs.common.put
import com.ampairs.common.model.Response
import com.ampairs.workspace.api.model.CreateWorkspaceRequest
import com.ampairs.workspace.api.model.UpdateWorkspaceRequest
import com.ampairs.workspace.api.model.WorkspaceApiModel
import com.ampairs.workspace.api.model.PagedWorkspaceResponse
import io.ktor.client.engine.HttpClientEngine
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.utils.io.ByteReadChannel

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class WorkspaceApiImpl(engine: HttpClientEngine, private val tokenRepository: TokenRepository) :
    WorkspaceApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getUserWorkspaces(
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): Response<PagedWorkspaceResponse> {
        val params = mapOf(
            "page" to page,
            "size" to size,
            "sortBy" to sortBy,
            "sortDir" to sortDir
        )
        return get(client, ApiUrlBuilder.workspaceUrl("v1"), params)
    }

    override suspend fun getWorkspace(workspaceId: String): Response<WorkspaceApiModel> {
        return get(client, ApiUrlBuilder.workspaceUrl("v1/$workspaceId"))
    }

    override suspend fun getWorkspaceBySlug(slug: String): Response<WorkspaceApiModel> {
        return get(client, ApiUrlBuilder.workspaceUrl("v1/by-slug/$slug"))
    }

    override suspend fun createWorkspace(request: CreateWorkspaceRequest): Response<WorkspaceApiModel> {
        return post(client, ApiUrlBuilder.workspaceUrl("v1"), request)
    }

    override suspend fun updateWorkspace(
        workspaceId: String,
        request: UpdateWorkspaceRequest,
    ): Response<WorkspaceApiModel> {
        return put(client, ApiUrlBuilder.workspaceUrl("v1/$workspaceId"), request)
    }

    override suspend fun checkSlugAvailability(slug: String): Response<Map<String, Boolean>> {
        return get(client, ApiUrlBuilder.workspaceUrl("v1/check-slug/$slug"))
    }

    override suspend fun archiveWorkspace(workspaceId: String): Response<String> {
        return post(client, ApiUrlBuilder.workspaceUrl("v1/$workspaceId/archive"), null)
    }

    override suspend fun restoreWorkspace(workspaceId: String): Response<String> {
        return post(client, ApiUrlBuilder.workspaceUrl("v1/$workspaceId/restore"), null)
    }

    override suspend fun getMyRole(workspaceId: String): Response<Map<String, Any>> {
        return get(client, ApiUrlBuilder.workspaceUrl("v1/$workspaceId/my-role"))
    }

    override suspend fun uploadAvatar(
        workspaceId: String,
        imageData: ByteArray,
        fileName: String,
        contentType: String
    ): Response<WorkspaceApiModel> {
        val parts = listOf(
            PartData.FileItem(
                provider = { ByteReadChannel(imageData) },
                dispose = {},
                partHeaders = Headers.build {
                    append(HttpHeaders.ContentType, contentType)
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$fileName\"")
                }
            )
        )
        return postMultiPart(client, ApiUrlBuilder.workspaceUrl("v1/$workspaceId/avatar"), parts)
    }

    override suspend fun deleteAvatar(workspaceId: String): Response<WorkspaceApiModel> {
        return delete(client, ApiUrlBuilder.workspaceUrl("v1/$workspaceId/avatar"))
    }
}