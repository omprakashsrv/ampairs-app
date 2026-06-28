package com.ampairs.notification.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.notification.domain.model.AppNotification
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

/**
 * Ktor implementation of [NotificationApi].
 *
 * Pull and push share the canonical `/v1/notifications/feed/sync` URL (GET / POST). Mirrors the
 * UnitApiImpl bulk `/sync` style.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class NotificationApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : NotificationApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getFeedSync(
        lastSync: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): PageResponse<AppNotification> {
        val params = mutableMapOf<String, Any>(
            "page" to page,
            "size" to size,
            "sort_by" to sortBy,
            "sort_dir" to sortDir,
        )
        if (lastSync.isNotBlank()) {
            params["last_sync"] = lastSync
        }

        val response: Response<PageResponse<AppNotification>> = get(
            client,
            ApiUrlBuilder.notificationUrl("v1/notifications/feed/sync"),
            params,
        )
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: PageResponse(
            content = emptyList(),
            pageNumber = page,
            pageSize = size,
            totalPages = 0,
            totalElements = 0L,
            hasNext = false,
            hasPrevious = false,
            first = true,
            last = true,
        )
    }

    override suspend fun pushFeedSync(notifications: List<AppNotification>): List<AppNotification> {
        val response: Response<List<AppNotification>> = post(
            client,
            ApiUrlBuilder.notificationUrl("v1/notifications/feed/sync"),
            notifications,
        )
        return response.data ?: throw Exception("Failed to push notification feed")
    }
}
