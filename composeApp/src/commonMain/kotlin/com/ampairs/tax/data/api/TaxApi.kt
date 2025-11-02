package com.ampairs.tax.data.api

import com.ampairs.tax.domain.HsnCode
import com.ampairs.tax.domain.TaxCalculationRequest
import com.ampairs.tax.domain.TaxCalculationResult
import com.ampairs.tax.domain.TaxRate

interface TaxApi {

    suspend fun getHsnCodes(
        workspaceId: String,
        page: Int = 0,
        size: Int = 100,
        search: String? = null,
        category: String? = null
    ): Result<List<HsnCode>>

    suspend fun getHsnCode(
        workspaceId: String,
        hsnCodeId: String
    ): Result<HsnCode>

    suspend fun createHsnCode(
        workspaceId: String,
        hsnCode: HsnCode
    ): Result<HsnCode>

    suspend fun updateHsnCode(
        workspaceId: String,
        hsnCodeId: String,
        hsnCode: HsnCode
    ): Result<HsnCode>

    suspend fun deleteHsnCode(
        workspaceId: String,
        hsnCodeId: String
    ): Result<Unit>

    suspend fun searchHsnCodes(
        workspaceId: String,
        query: String
    ): Result<List<HsnCode>>

    suspend fun getTaxRates(
        workspaceId: String,
        hsnCode: String? = null,
        businessType: String? = null,
        effectiveDate: Long? = null
    ): Result<List<TaxRate>>

    suspend fun getTaxRate(
        workspaceId: String,
        taxRateId: String
    ): Result<TaxRate>

    suspend fun createTaxRate(
        workspaceId: String,
        taxRate: TaxRate
    ): Result<TaxRate>

    suspend fun updateTaxRate(
        workspaceId: String,
        taxRateId: String,
        taxRate: TaxRate
    ): Result<TaxRate>

    suspend fun deleteTaxRate(
        workspaceId: String,
        taxRateId: String
    ): Result<Unit>

    suspend fun calculateTax(
        workspaceId: String,
        request: TaxCalculationRequest
    ): Result<TaxCalculationResult>

    suspend fun getEffectiveTaxRate(
        workspaceId: String,
        hsnCode: String,
        businessType: String,
        effectiveDate: Long
    ): Result<TaxRate>
}