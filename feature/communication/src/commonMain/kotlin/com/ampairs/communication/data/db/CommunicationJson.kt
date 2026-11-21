package com.ampairs.communication.data.db

import kotlinx.serialization.json.Json

/** Shared JSON for (de)serializing embedded lists (template variants, channel lists). */
internal val communicationJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
