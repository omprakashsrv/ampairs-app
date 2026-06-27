package com.ampairs.notification.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.notification.domain.model.AppNotification

/**
 * API for the in-app notification feed.
 *
 * Canonical `/v1/notifications/feed/sync` contract:
 *  - PULL via [getFeedSync] (GET, snake_case params, includes dismissed rows)
 *  - PUSH via [pushFeedSync] (POST, bulk upsert of read/dismiss flag changes; dismiss in-band)
 */
interface NotificationApi {

    /**
     * Incremental pull of the notification feed.
     *
     * The response INCLUDES dismissed (inactive) rows so the client can permanently hard-delete
     * them locally. Throws on a non-null API error.
     *
     * @param lastSync ISO 8601 checkpoint; only sent when non-blank
     * @param page Page number (0-indexed)
     * @param size Page size (default 100)
     * @param sortBy Sort field (default "updatedAt")
     * @param sortDir "ASC" or "DESC" (default "ASC")
     */
    suspend fun getFeedSync(
        lastSync: String,
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): PageResponse<AppNotification>

    /**
     * Bulk push of locally unsynced notifications (read / dismiss flag changes). Dismissed rows
     * (active = false) ride along in-band. Returns the server-reconciled rows. Throws on failure.
     */
    suspend fun pushFeedSync(notifications: List<AppNotification>): List<AppNotification>
}
