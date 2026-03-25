package com.ampairs.workspace.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.delete
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.put
import com.ampairs.common.model.Response
import com.ampairs.workspace.api.model.MemberDetailsResponse
import com.ampairs.workspace.api.model.PagedMemberResponse
import com.ampairs.workspace.api.model.UpdateMemberRequest
import com.ampairs.workspace.api.model.UserRoleResponse
import com.ampairs.workspace.api.model.WorkspaceRole
import com.ampairs.workspace.api.model.WorkspacePermissionResponse
import io.ktor.client.engine.HttpClientEngine

/**
 * Implementation of WorkspaceMemberApi using Ktor HTTP client
 */
class WorkspaceMemberApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : WorkspaceMemberApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getWorkspaceMembers(
        workspaceId: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): Response<PagedMemberResponse> {
        val params = mapOf(
            "page" to page,
            "size" to size,
            "sortBy" to sortBy,
            "sortDir" to sortDir
        )
        return get(client, ApiUrlBuilder.workspaceUrl("v1/member"), params)
    }

    override suspend fun getMemberDetails(
        workspaceId: String,
        memberId: String,
    ): Response<MemberDetailsResponse> {
        return get(client, ApiUrlBuilder.workspaceUrl("v1/member/$memberId"))
    }

    override suspend fun updateMember(
        workspaceId: String,
        memberId: String,
        request: UpdateMemberRequest,
    ): Response<MemberDetailsResponse> {
        return put(client, ApiUrlBuilder.workspaceUrl("v1/member/$memberId"), request)
    }

    override suspend fun removeMember(
        workspaceId: String,
        memberId: String,
    ): Response<String> {
        return delete(client, ApiUrlBuilder.workspaceUrl("v1/member/$memberId"))
    }

    override suspend fun getMyRole(workspaceId: String): Response<UserRoleResponse> {
        return get(client, ApiUrlBuilder.workspaceUrl("v1/member/my-role"))
    }

    override suspend fun getAvailableRoles(workspaceId: String): Response<List<WorkspaceRole>> {
        return get(client, ApiUrlBuilder.workspaceUrl("v1/member/roles"))
    }

    override suspend fun getAvailablePermissions(workspaceId: String): Response<List<WorkspacePermissionResponse>> {
        return get(client, ApiUrlBuilder.workspaceUrl("v1/member/permissions"))
    }
}