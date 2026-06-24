package com.ampairs.agent.data.api

import com.ampairs.common.model.Response

/**
 * Pull-only access to the backend on-device model manifest (`GET /api/agent/v1/models`). Global
 * reference data; the app shows what it gets and gates locally by device RAM. No admin/setup UI.
 */
interface ModelCatalogApi {
    suspend fun catalog(): Response<List<AiModelResponse>>
}
