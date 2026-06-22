package com.ampairs.payment.domain

import com.ampairs.store.domain.StoreSettingsProvider
import dev.zacsweers.metro.Inject

/**
 * Reads the workspace payment-collection settings (FR-027, R11) via [StoreSettingsProvider], with
 * caller-owned defaults so the flow works offline before any override has synced. Mirrors how
 * invoice/order consume `StoreSetting`.
 */
@Inject
class PaymentSettings(
    private val provider: StoreSettingsProvider,
) {
    private val module = "payment-collection"

    suspend fun enabledPaymentModes(): List<PaymentMode> {
        val raw = provider.getString(module, "enabledPaymentModes", null)
        if (raw.isNullOrBlank()) return DEFAULT_MODES
        return raw.split(",")
            .mapNotNull { token ->
                PaymentMode.entries.firstOrNull { it.name.equals(token.trim(), ignoreCase = true) }
            }
            .ifEmpty { DEFAULT_MODES }
    }

    suspend fun defaultPaymentMode(): PaymentMode {
        val raw = provider.getString(module, "defaultPaymentMode", null)
        return PaymentMode.entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) }
            ?: PaymentMode.CASH
    }

    suspend fun chequeRequiresClearance(): Boolean =
        provider.getBoolean(module, "chequeRequiresClearance", default = true)

    suspend fun allowOnAccountReceipts(): Boolean =
        provider.getBoolean(module, "allowOnAccountReceipts", default = true)

    suspend fun enforceCreditLimit(): Boolean =
        provider.getBoolean(module, "enforceCreditLimit", default = true)

    /** Aging bucket day boundaries, e.g. "30,60,90". */
    suspend fun agingBuckets(): List<Int> {
        val raw = provider.getString(module, "agingBuckets", null)
        if (raw.isNullOrBlank()) return OutstandingService.DEFAULT_BUCKETS
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }
            .ifEmpty { OutstandingService.DEFAULT_BUCKETS }
    }

    companion object {
        val DEFAULT_MODES = listOf(
            PaymentMode.CASH,
            PaymentMode.CHEQUE,
            PaymentMode.UPI,
            PaymentMode.NEFT,
            PaymentMode.RTGS,
            PaymentMode.IMPS,
            PaymentMode.NET_BANKING,
            PaymentMode.CARD,
            PaymentMode.BANK_TRANSFER,
        )
    }
}
