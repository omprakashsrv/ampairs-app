package com.ampairs.ecom.data.repository

import com.ampairs.ecom.api.model.DistributorAccount
import com.ampairs.ecom.api.model.LinkCandidateResponse
import com.ampairs.ecom.data.api.EcomApi
import dev.zacsweers.metro.Inject

/**
 * Live, UI-invoked distributor-link flow — not part of the offline-sync pipeline. Checking for a
 * phone-match candidate and confirming a link both need an online round-trip at the moment the
 * buyer acts, so this repository holds the [EcomApi] directly (the same "UI-invoked, non-sync
 * feature" exception as customer group/type import-from-master — see `/offline-sync`).
 */
@Inject
class CustomerLinkRepository(private val api: EcomApi) {
    suspend fun getLinkCandidate(slug: String): Result<LinkCandidateResponse?> = api.getLinkCandidate(slug)
    suspend fun confirmLink(slug: String, customerId: String): Result<DistributorAccount> = api.confirmLink(slug, customerId)
}
