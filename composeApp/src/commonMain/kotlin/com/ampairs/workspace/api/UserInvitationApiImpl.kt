package com.ampairs.workspace.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.model.Response
import com.ampairs.workspace.api.model.UserInvitationResponse
import com.ampairs.workspace.api.model.InvitationActionResponse
import io.ktor.client.engine.HttpClientEngine

/**
 * Implementation of UserInvitationApi using Ktor HTTP client
 */
class UserInvitationApiImpl(
    engine: HttpClientEngine,
    private val tokenRepository: TokenRepository,
) : UserInvitationApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getPendingInvitations(): Response<List<UserInvitationResponse>> {
        return get(client, ApiUrlBuilder.userUrl("v1/invitation/pending"))
    }

    override suspend fun acceptInvitation(invitationId: String): Response<InvitationActionResponse> {
        return post(client, ApiUrlBuilder.userUrl("v1/invitation/$invitationId/accept"), null)
    }

    override suspend fun rejectInvitation(invitationId: String): Response<InvitationActionResponse> {
        return post(client, ApiUrlBuilder.userUrl("v1/invitation/$invitationId/reject"), null)
    }
}