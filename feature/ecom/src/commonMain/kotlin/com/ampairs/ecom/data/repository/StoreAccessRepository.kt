package com.ampairs.ecom.data.repository

import com.ampairs.ecom.api.model.StoreAccessResponse
import com.ampairs.ecom.data.api.EcomApi
import dev.zacsweers.metro.Inject

/** Store-access gate (LOGIN_FIRST). See the ecom storefront management API, plan §11.1. */
@Inject
class StoreAccessRepository(
    private val api: EcomApi,
) {
    suspend fun requestAccess(slug: String): Result<StoreAccessResponse> = api.requestStoreAccess(slug)

    suspend fun checkAccess(slug: String): Result<StoreAccessResponse> = api.getStoreAccess(slug)
}
