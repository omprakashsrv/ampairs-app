package com.ampairs.ecom.api.model

/** Preserves the backend's `error.code` (e.g. "ECOM_NOT_LINKED") so callers can branch on it. */
class EcomApiException(val code: String, message: String) : Exception(message)

/** Buyer-account error code returned (HTTP 403) when the login isn't linked to a CRM account in the store. */
const val ECOM_NOT_LINKED_CODE = "ECOM_NOT_LINKED"

/**
 * True when [this] failure is the "not linked to this store" case (server 403 `ECOM_NOT_LINKED`),
 * as opposed to a transient/network error — lets the UI show a "link your account" hint only then.
 */
fun Throwable.isNotLinked(): Boolean = this is EcomApiException && code == ECOM_NOT_LINKED_CODE
