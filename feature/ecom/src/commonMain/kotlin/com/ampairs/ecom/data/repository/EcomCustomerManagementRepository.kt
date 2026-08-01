package com.ampairs.ecom.data.repository

import com.ampairs.ecom.api.model.EcomContactResponse
import com.ampairs.ecom.data.api.EcomApi
import dev.zacsweers.metro.Inject

/**
 * Owner-facing "ecom users" list: every buyer linked to any of this workspace's CRM customers.
 * Online-only, UI-invoked, no central-sync path — same allowed exception as
 * [StorefrontManagementRepository] (see /offline-sync). Delegates straight to [EcomApi]; no Room mirror.
 */
@Inject
class EcomCustomerManagementRepository(
    private val api: EcomApi,
) {
    suspend fun getContacts(): Result<List<EcomContactResponse>> = api.getEcomContacts()

    suspend fun setContactActive(contactUid: String, active: Boolean): Result<EcomContactResponse> =
        api.setEcomContactActive(contactUid, active)
}
