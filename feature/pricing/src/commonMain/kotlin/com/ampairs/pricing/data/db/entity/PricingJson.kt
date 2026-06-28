package com.ampairs.pricing.data.db.entity

import kotlinx.serialization.json.Json

/** Shared lenient JSON for encoding/decoding the JSON-column payloads (tiers, members, predicates). */
internal val PricingJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
