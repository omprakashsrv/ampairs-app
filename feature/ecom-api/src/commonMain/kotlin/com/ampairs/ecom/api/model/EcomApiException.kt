package com.ampairs.ecom.api.model

/** Preserves the backend's `error.code` (e.g. "ECOM_NOT_LINKED") so callers can branch on it. */
class EcomApiException(val code: String, message: String) : Exception(message)
