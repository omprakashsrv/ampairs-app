package com.ampairs.store.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.postList
import com.ampairs.store.domain.definition.StoreSettingDefinition
import com.ampairs.store.domain.model.StoreSetting
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)
class StoreSettingApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : StoreSettingApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun pull(
        page: Int,
        size: Int,
        lastSync: String?,
    ): Response<PageResponse<StoreSetting>> {
        val params = mutableMapOf(
            "page" to page.toString(),
            "size" to size.toString(),
        )
        lastSync?.let { params["last_sync"] = it }
        return get(client, ApiUrlBuilder.settingUrl("v1/settings"), params)
    }

    override suspend fun push(settings: List<StoreSetting>): Response<List<StoreSetting>> {
        return postList(client, ApiUrlBuilder.settingUrl("v1/settings"), settings)
    }

    override suspend fun definitions(): Response<List<StoreSettingDefinition>> {
        return get(client, ApiUrlBuilder.settingUrl("v1/settings/definitions"))
    }
}
