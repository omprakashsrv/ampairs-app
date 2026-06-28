package com.ampairs.communication.util

/** UID prefixes (mirroring the backend `communication/config/Constants.kt`). */
object CommunicationConstants {
    const val TEMPLATE_UID_PREFIX = "CTPL"
    const val VARIANT_UID_PREFIX = "CTPV"
    const val BINDING_UID_PREFIX = "CETB"
    const val SCHEDULE_UID_PREFIX = "CSCH"
    const val CAMPAIGN_UID_PREFIX = "CCMP"
    const val PREFERENCE_UID_PREFIX = "CPRF"
}

/** Channels supported by the communication engine (mirrors backend `Channel`). */
enum class CommChannel { EMAIL, SMS, WHATSAPP, PUSH }

/** Message categories (mirrors backend `MessageCategory`). */
enum class CommCategory { TRANSACTIONAL, RECURRING, PROMOTIONAL }
