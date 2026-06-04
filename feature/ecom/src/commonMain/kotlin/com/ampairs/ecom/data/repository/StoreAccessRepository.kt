package com.ampairs.ecom.data.repository

import com.ampairs.ecom.api.model.StoreAccessResponse
import com.ampairs.ecom.api.model.StoreAccessStatus
import com.ampairs.ecom.data.api.EcomApi
import dev.zacsweers.metro.Inject

/**
 * Store-access gate (LOGIN_FIRST). The backend endpoint is a future addition (plan §11.1);
 * until it ships, [USE_REAL_API] = false routes through a demo stub that auto-grants.
 * The ViewModel/UI is identical for both paths — flip the flag when the API lands.
 */
@Inject
class StoreAccessRepository(
    private val api: EcomApi,
) {
    suspend fun requestAccess(slug: String): Result<StoreAccessResponse> =
        if (USE_REAL_API) api.requestStoreAccess(slug)
        else Result.success(StoreAccessResponse(status = StoreAccessStatus.PENDING.name))

    suspend fun checkAccess(slug: String): Result<StoreAccessResponse> =
        if (USE_REAL_API) api.getStoreAccess(slug)
        else Result.success(StoreAccessResponse(status = StoreAccessStatus.GRANTED.name))

    companion object {
        /** Flip to true once the backend exposes /ecom/account/store-access (plan §11.1). */
        const val USE_REAL_API = false
    }
}
