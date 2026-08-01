package com.ampairs.ecom.data.repository

import com.ampairs.ecom.api.model.ManagedStorefront
import com.ampairs.ecom.api.model.StorefrontCreateRequest
import com.ampairs.ecom.api.model.StorefrontUpdateRequest
import com.ampairs.ecom.data.api.EcomApi
import dev.zacsweers.metro.Inject

/**
 * Merchant-side storefront configuration. This is an online-only, UI-invoked feature with no
 * central-sync path (a single workspace-scoped config record minted server-side) — the allowed
 * exception to the "repository never touches the API" rule (see /offline-sync). It delegates
 * straight to [EcomApi]; no Room mirror.
 */
@Inject
class StorefrontManagementRepository(
    private val api: EcomApi,
) {
    /** The workspace's storefront, or `null` when none exists yet. */
    suspend fun getStorefront(): Result<ManagedStorefront?> = api.getMyStorefront()

    suspend fun createStorefront(request: StorefrontCreateRequest): Result<ManagedStorefront> =
        api.createStorefront(request)

    suspend fun updateStorefront(request: StorefrontUpdateRequest): Result<ManagedStorefront> =
        api.updateStorefront(request)

    suspend fun publish(): Result<ManagedStorefront> = api.publishStorefront()

    suspend fun unpublish(): Result<ManagedStorefront> = api.unpublishStorefront()
}
