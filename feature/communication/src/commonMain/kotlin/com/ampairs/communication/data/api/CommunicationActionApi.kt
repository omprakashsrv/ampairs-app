package com.ampairs.communication.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.delete
import com.ampairs.common.di.AppScope
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.common.put
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

/**
 * Non-sync, UI-invoked communication endpoints: provider credentials (write-only secrets, never on
 * `/sync`), the usage/billing report, template preview (server-rendered), and campaign lifecycle
 * actions. No secret is ever persisted on the device.
 */
interface CommunicationActionApi {
    suspend fun listCredentials(): Response<List<CredentialResponse>>
    suspend fun createCredential(request: CredentialRequest): Response<CredentialResponse>
    suspend fun updateCredential(uid: String, request: CredentialRequest): Response<CredentialResponse>
    suspend fun deleteCredential(uid: String): Response<Unit>
    suspend fun validateCredential(uid: String): Response<CredentialResponse>

    suspend fun usageReport(from: String?, to: String?): Response<UsageReportResponse>

    suspend fun previewTemplate(code: String, request: PreviewRequest): Response<PreviewResponse>

    suspend fun startCampaign(uid: String): Response<CampaignDto>
    suspend fun pauseCampaign(uid: String): Response<CampaignDto>
    suspend fun resumeCampaign(uid: String): Response<CampaignDto>
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CommunicationActionApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : CommunicationActionApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun listCredentials(): Response<List<CredentialResponse>> =
        get(client, ApiUrlBuilder.communicationUrl("v1/credentials"))

    override suspend fun createCredential(request: CredentialRequest): Response<CredentialResponse> =
        post(client, ApiUrlBuilder.communicationUrl("v1/credentials"), request)

    override suspend fun updateCredential(uid: String, request: CredentialRequest): Response<CredentialResponse> =
        put(client, ApiUrlBuilder.communicationUrl("v1/credentials/$uid"), request)

    override suspend fun deleteCredential(uid: String): Response<Unit> =
        delete(client, ApiUrlBuilder.communicationUrl("v1/credentials/$uid"))

    override suspend fun validateCredential(uid: String): Response<CredentialResponse> =
        post(client, ApiUrlBuilder.communicationUrl("v1/credentials/$uid/validate"), null)

    override suspend fun usageReport(from: String?, to: String?): Response<UsageReportResponse> {
        val params = mutableMapOf<String, Any>("group_by" to "channel,credential,billing_mode")
        from?.takeIf { it.isNotBlank() }?.let { params["from"] = it }
        to?.takeIf { it.isNotBlank() }?.let { params["to"] = it }
        return get(client, ApiUrlBuilder.communicationUrl("v1/usage"), params)
    }

    override suspend fun previewTemplate(code: String, request: PreviewRequest): Response<PreviewResponse> =
        post(client, ApiUrlBuilder.communicationUrl("v1/templates/$code/preview"), request)

    override suspend fun startCampaign(uid: String): Response<CampaignDto> =
        post(client, ApiUrlBuilder.communicationUrl("v1/campaigns/$uid/start"), null)

    override suspend fun pauseCampaign(uid: String): Response<CampaignDto> =
        post(client, ApiUrlBuilder.communicationUrl("v1/campaigns/$uid/pause"), null)

    override suspend fun resumeCampaign(uid: String): Response<CampaignDto> =
        post(client, ApiUrlBuilder.communicationUrl("v1/campaigns/$uid/resume"), null)
}
