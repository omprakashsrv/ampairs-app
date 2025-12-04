package com.ampairs.tax.data.api

import com.ampairs.tax.domain.HsnCode
import com.ampairs.tax.domain.TaxCalculationRequest
import com.ampairs.tax.domain.TaxCalculationResult
import com.ampairs.tax.domain.TaxRate

interface TaxApi {

    suspend fun getHsnCodes(
        page: Int = 0,
        size: Int = 100,
        search: String? = null,
        category: String? = null
    ): Result<List<HsnCode>>

    suspend fun getHsnCode(
        hsnCodeId: String
    ): Result<HsnCode>

    suspend fun createHsnCode(
        hsnCode: HsnCode
    ): Result<HsnCode>

    suspend fun updateHsnCode(
        hsnCodeId: String,
        hsnCode: HsnCode
    ): Result<HsnCode>

    suspend fun deleteHsnCode(
        hsnCodeId: String
    ): Result<Unit>

    suspend fun searchHsnCodes(
        query: String
    ): Result<List<HsnCode>>

    suspend fun getTaxRates(
        hsnCode: String? = null,
        businessType: String? = null,
        effectiveDate: Long? = null
    ): Result<List<TaxRate>>

    suspend fun getTaxRate(
        taxRateId: String
    ): Result<TaxRate>

    suspend fun createTaxRate(
        taxRate: TaxRate
    ): Result<TaxRate>

    suspend fun updateTaxRate(
        taxRateId: String,
        taxRate: TaxRate
    ): Result<TaxRate>

    suspend fun deleteTaxRate(
        taxRateId: String
    ): Result<Unit>

    suspend fun calculateTax(
        request: TaxCalculationRequest
    ): Result<TaxCalculationResult>

    suspend fun getEffectiveTaxRate(
        hsnCode: String,
        businessType: String,
        effectiveDate: Long
    ): Result<TaxRate>
}