package com.ampairs.agent.data.api

import com.ampairs.common.model.Response

/**
 * Pull-only access to the server-seeded LLM model catalog (global; the app filters by device RAM +
 * role locally). Mirrors the module catalog pull — no admin/setup UI; the backend seeds the models.
 */
interface ModelCatalogApi {
    suspend fun catalog(): Response<List<RemoteModelDescriptor>>
}
