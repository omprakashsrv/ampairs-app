package com.ampairs.customer.data.repository

import com.ampairs.customer.data.api.CustomerApi
import com.ampairs.customer.domain.CustomerContactResponse
import com.ampairs.customer.domain.LinkContactRequest
import dev.zacsweers.metro.Inject

/**
 * Live, UI-invoked "linked accounts" management for a CRM customer — not part of the offline-sync
 * pipeline. Listing/linking/restricting an ecom account needs an online round trip at the moment
 * the owner acts, so this repository holds the [CustomerApi] directly (the same "UI-invoked,
 * non-sync feature" exception used by `feature/ecom`'s `CustomerLinkRepository` — see `/offline-sync`).
 */
@Inject
class CustomerContactRepository(private val api: CustomerApi) {
    suspend fun getContacts(customerId: String): Result<List<CustomerContactResponse>> =
        runCatching { api.getContacts(customerId) }

    suspend fun linkContact(customerId: String, phone: String, name: String?): Result<CustomerContactResponse> =
        runCatching { api.linkContact(customerId, LinkContactRequest(phone = phone, name = name)) }

    suspend fun setContactActive(customerId: String, contactUid: String, active: Boolean): Result<CustomerContactResponse> =
        runCatching { api.setContactActive(customerId, contactUid, active) }
}
