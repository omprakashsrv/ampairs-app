package com.ampairs.payment.data.api

import com.ampairs.common.model.PageResponse

/**
 * The single network surface for the payment module (spec 013 contracts/payment-sync.md).
 * Only the [com.ampairs.payment.sync] delegates touch this — repositories are local-only.
 *
 * Every resource is on the canonical `/sync` contract: pull = `GET v1/{resource}/sync`
 * (paginated, includes soft-deleted), push = `POST v1/{resource}/sync` (UID-keyed bulk upsert).
 */
interface PaymentApi {

    // ---- ledger entries ----
    suspend fun getLedgerEntriesSync(lastSync: String = "", page: Int = 0, size: Int = 100): PageResponse<LedgerEntryApiModel>
    suspend fun pushLedgerEntries(entries: List<LedgerEntryApiModel>): List<LedgerEntryApiModel>

    // ---- party balances (pull-authoritative; push carries only opening fields) ----
    suspend fun getPartyBalancesSync(lastSync: String = "", page: Int = 0, size: Int = 100): PageResponse<PartyBalanceApiModel>
    suspend fun pushPartyBalances(balances: List<PartyBalanceApiModel>): List<PartyBalanceApiModel>

    // ---- payment vouchers ----
    suspend fun getVouchersSync(lastSync: String = "", page: Int = 0, size: Int = 100): PageResponse<PaymentVoucherApiModel>
    suspend fun pushVouchers(vouchers: List<PaymentVoucherApiModel>): List<PaymentVoucherApiModel>

    // ---- payment allocations ----
    suspend fun getAllocationsSync(lastSync: String = "", page: Int = 0, size: Int = 100): PageResponse<PaymentAllocationApiModel>
    suspend fun pushAllocations(allocations: List<PaymentAllocationApiModel>): List<PaymentAllocationApiModel>

    // ---- adjustments ----
    suspend fun getAdjustmentsSync(lastSync: String = "", page: Int = 0, size: Int = 100): PageResponse<AdjustmentVoucherApiModel>
    suspend fun pushAdjustments(adjustments: List<AdjustmentVoucherApiModel>): List<AdjustmentVoucherApiModel>
}
