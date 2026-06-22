package com.ampairs.payment.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.post
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.engine.HttpClientEngine

@Inject
@ContributesBinding(AppScope::class)
class PaymentApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : PaymentApi {

    // sendDefaults: the /sync push is a full-row upsert; defaulted primitives (0.00, false, ...)
    // must be sent so the backend doesn't reject the body.
    private val client = httpClient(engine, tokenRepository, sendDefaults = true)

    private fun emptyPage(page: Int, size: Int) = PageResponse<Nothing>(
        content = emptyList(),
        pageNumber = page,
        pageSize = size,
        totalPages = 0,
        totalElements = 0L,
        hasNext = false,
        hasPrevious = false,
        first = true,
        last = true,
    )

    private fun syncParams(lastSync: String, page: Int, size: Int): MutableMap<String, Any> {
        val params = mutableMapOf<String, Any>(
            "page" to page,
            "size" to size,
            "sort_by" to "updatedAt",
            "sort_dir" to "ASC",
        )
        if (lastSync.isNotBlank()) params["last_sync"] = lastSync
        return params
    }

    // ---- ledger entries ----

    override suspend fun getLedgerEntriesSync(lastSync: String, page: Int, size: Int): PageResponse<LedgerEntryApiModel> {
        val response: Response<PageResponse<LedgerEntryApiModel>> =
            get(client, ApiUrlBuilder.paymentUrl("v1/ledger-entries/sync"), syncParams(lastSync, page, size))
        if (response.error != null) throw Exception(response.error?.message ?: "Ledger entry pull failed")
        @Suppress("UNCHECKED_CAST")
        return response.data ?: (emptyPage(page, size) as PageResponse<LedgerEntryApiModel>)
    }

    override suspend fun pushLedgerEntries(entries: List<LedgerEntryApiModel>): List<LedgerEntryApiModel> {
        val response: Response<List<LedgerEntryApiModel>> =
            post(client, ApiUrlBuilder.paymentUrl("v1/ledger-entries/sync"), entries)
        if (response.error != null) throw Exception(response.error?.message ?: "Ledger entry push failed")
        return response.data ?: emptyList()
    }

    // ---- party balances ----

    override suspend fun getPartyBalancesSync(lastSync: String, page: Int, size: Int): PageResponse<PartyBalanceApiModel> {
        val response: Response<PageResponse<PartyBalanceApiModel>> =
            get(client, ApiUrlBuilder.paymentUrl("v1/party-balances/sync"), syncParams(lastSync, page, size))
        if (response.error != null) throw Exception(response.error?.message ?: "Party balance pull failed")
        @Suppress("UNCHECKED_CAST")
        return response.data ?: (emptyPage(page, size) as PageResponse<PartyBalanceApiModel>)
    }

    override suspend fun pushPartyBalances(balances: List<PartyBalanceApiModel>): List<PartyBalanceApiModel> {
        val response: Response<List<PartyBalanceApiModel>> =
            post(client, ApiUrlBuilder.paymentUrl("v1/party-balances/sync"), balances)
        if (response.error != null) throw Exception(response.error?.message ?: "Party balance push failed")
        return response.data ?: emptyList()
    }

    // ---- payment vouchers ----

    override suspend fun getVouchersSync(lastSync: String, page: Int, size: Int): PageResponse<PaymentVoucherApiModel> {
        val response: Response<PageResponse<PaymentVoucherApiModel>> =
            get(client, ApiUrlBuilder.paymentUrl("v1/vouchers/sync"), syncParams(lastSync, page, size))
        if (response.error != null) throw Exception(response.error?.message ?: "Voucher pull failed")
        @Suppress("UNCHECKED_CAST")
        return response.data ?: (emptyPage(page, size) as PageResponse<PaymentVoucherApiModel>)
    }

    override suspend fun pushVouchers(vouchers: List<PaymentVoucherApiModel>): List<PaymentVoucherApiModel> {
        val response: Response<List<PaymentVoucherApiModel>> =
            post(client, ApiUrlBuilder.paymentUrl("v1/vouchers/sync"), vouchers)
        if (response.error != null) throw Exception(response.error?.message ?: "Voucher push failed")
        return response.data ?: emptyList()
    }

    // ---- payment allocations ----

    override suspend fun getAllocationsSync(lastSync: String, page: Int, size: Int): PageResponse<PaymentAllocationApiModel> {
        val response: Response<PageResponse<PaymentAllocationApiModel>> =
            get(client, ApiUrlBuilder.paymentUrl("v1/allocations/sync"), syncParams(lastSync, page, size))
        if (response.error != null) throw Exception(response.error?.message ?: "Allocation pull failed")
        @Suppress("UNCHECKED_CAST")
        return response.data ?: (emptyPage(page, size) as PageResponse<PaymentAllocationApiModel>)
    }

    override suspend fun pushAllocations(allocations: List<PaymentAllocationApiModel>): List<PaymentAllocationApiModel> {
        val response: Response<List<PaymentAllocationApiModel>> =
            post(client, ApiUrlBuilder.paymentUrl("v1/allocations/sync"), allocations)
        if (response.error != null) throw Exception(response.error?.message ?: "Allocation push failed")
        return response.data ?: emptyList()
    }

    // ---- adjustments ----

    override suspend fun getAdjustmentsSync(lastSync: String, page: Int, size: Int): PageResponse<AdjustmentVoucherApiModel> {
        val response: Response<PageResponse<AdjustmentVoucherApiModel>> =
            get(client, ApiUrlBuilder.paymentUrl("v1/adjustments/sync"), syncParams(lastSync, page, size))
        if (response.error != null) throw Exception(response.error?.message ?: "Adjustment pull failed")
        @Suppress("UNCHECKED_CAST")
        return response.data ?: (emptyPage(page, size) as PageResponse<AdjustmentVoucherApiModel>)
    }

    override suspend fun pushAdjustments(adjustments: List<AdjustmentVoucherApiModel>): List<AdjustmentVoucherApiModel> {
        val response: Response<List<AdjustmentVoucherApiModel>> =
            post(client, ApiUrlBuilder.paymentUrl("v1/adjustments/sync"), adjustments)
        if (response.error != null) throw Exception(response.error?.message ?: "Adjustment push failed")
        return response.data ?: emptyList()
    }
}
