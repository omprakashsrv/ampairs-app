package com.ampairs.workspace.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.delete
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.model.Response
import com.ampairs.workspace.api.model.AcceptInvitationResponse
import com.ampairs.workspace.api.model.CreateInvitationRequest
import com.ampairs.workspace.api.model.InvitationApiModel
import com.ampairs.workspace.api.model.PagedInvitationResponse
import com.ampairs.workspace.api.model.ResendInvitationRequest
import io.ktor.client.engine.HttpClientEngine

/**
 * Implementation of WorkspaceInvitationApi using Ktor HTTP client
 */
class WorkspaceInvitationApiImpl(
    engine: HttpClientEngine,
    private val tokenRepository: TokenRepository,
) : WorkspaceInvitationApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getWorkspaceInvitations(
        workspaceId: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): Response<PagedInvitationResponse> {
        val params = mapOf(
            "page" to page,
            "size" to size,
            "sortBy" to sortBy,
            "sortDir" to sortDir
        )
        return get(client, ApiUrlBuilder.workspaceUrl("v1/invitation"), params)
    }

    override suspend fun createInvitation(
        workspaceId: String,
        request: CreateInvitationRequest,
    ): Response<InvitationApiModel> {
        return post(client, ApiUrlBuilder.workspaceUrl("v1/invitation"), request)
    }

    override suspend fun acceptInvitation(token: String): Response<AcceptInvitationResponse> {
        return post(client, ApiUrlBuilder.workspaceUrl("v1/invitation/$token/accept"), null)
    }

    override suspend fun resendInvitation(
        workspaceId: String,
        invitationId: String,
        request: ResendInvitationRequest,
    ): Response<InvitationApiModel> {
        return post(client, ApiUrlBuilder.workspaceUrl("v1/invitation/$invitationId/resend"), request)
    }

    override suspend fun cancelInvitation(
        workspaceId: String,
        invitationId: String,
    ): Response<String> {
        return delete(client, ApiUrlBuilder.workspaceUrl("v1/invitation/$invitationId"))
    }
}