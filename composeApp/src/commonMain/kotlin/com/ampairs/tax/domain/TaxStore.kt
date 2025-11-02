package com.ampairs.tax.domain

import com.ampairs.tax.data.repository.TaxRepository
import kotlinx.serialization.Serializable
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Serializable
data class HsnCodeKey(val id: String)

@Serializable
data class TaxRateKey(val hsnCode: String, val businessType: BusinessType)

@Serializable
data class TaxRateByIdKey(val id: String)

@Serializable
data class TaxCalculationKey(val request: TaxCalculationRequest)

class TaxStore(
    private val taxRepository: TaxRepository
) {

    // HSN Code Store
    val hsnCodeStore: Store<HsnCodeKey, HsnCode> = StoreBuilder
        .from<HsnCodeKey, HsnCode>(
            fetcher = Fetcher.ofFlow { key ->
                flow {
                    val hsnCode = taxRepository.getHsnCodeById(key.id)
                    if (hsnCode != null) {
                        emit(hsnCode)
                    } else {
                        throw Exception("HSN Code not found: ${key.id}")
                    }
                }
            }
        )
        .build()

    // All HSN Codes Store
    val allHsnCodesStore: Store<Unit, List<HsnCode>> = StoreBuilder
        .from<Unit, List<HsnCode>>(
            fetcher = Fetcher.ofFlow {
                taxRepository.getAllHsnCodes()
            }
        )
        .build()

    // Tax Rate Store
    val taxRateStore: Store<TaxRateKey, TaxRate> = StoreBuilder
        .from<TaxRateKey, TaxRate>(
            fetcher = Fetcher.ofFlow { key ->
                flow {
                    val taxRate = taxRepository.getEffectiveTaxRate(
                        hsnCode = key.hsnCode,
                        businessType = key.businessType
                    )
                    if (taxRate != null) {
                        emit(taxRate)
                    } else {
                        throw Exception("Tax rate not found for HSN: ${key.hsnCode}, Business Type: ${key.businessType}")
                    }
                }
            }
        )
        .build()

    // Tax Rate by ID Store
    val taxRateByIdStore: Store<TaxRateByIdKey, TaxRate> = StoreBuilder
        .from<TaxRateByIdKey, TaxRate>(
            fetcher = Fetcher.ofFlow { key ->
                flow {
                    val taxRate = taxRepository.getTaxRateById(key.id)
                    if (taxRate != null) {
                        emit(taxRate)
                    } else {
                        throw Exception("Tax rate not found with ID: ${key.id}")
                    }
                }
            }
        )
        .build()

    // All Tax Rates Store
    val allTaxRatesStore: Store<Unit, List<TaxRate>> = StoreBuilder
        .from<Unit, List<TaxRate>>(
            fetcher = Fetcher.ofFlow {
                taxRepository.getAllTaxRates()
            }
        )
        .build()

    // Tax Calculation Store
    val taxCalculationStore: Store<TaxCalculationKey, TaxCalculationResult> = StoreBuilder
        .from<TaxCalculationKey, TaxCalculationResult>(
            fetcher = Fetcher.of { key ->
                taxRepository.calculateTax(key.request).getOrThrow()
            }
        )
        .build()

    // Repository methods for operations
    suspend fun createHsnCode(hsnCode: HsnCode): Result<HsnCode> {
        return taxRepository.createHsnCode(hsnCode)
    }

    suspend fun updateHsnCode(hsnCode: HsnCode): Result<HsnCode> {
        return taxRepository.updateHsnCode(hsnCode)
    }

    suspend fun deleteHsnCode(id: String): Result<Unit> {
        return taxRepository.deleteHsnCode(id)
    }

    suspend fun searchHsnCodes(query: String, limit: Int = 50): List<HsnCode> {
        return taxRepository.searchHsnCodes(query, limit)
    }

    suspend fun getHsnCodesByCategory(category: String): List<HsnCode> {
        return taxRepository.getHsnCodesByCategory(category)
    }

    suspend fun createTaxRate(taxRate: TaxRate): Result<TaxRate> {
        return taxRepository.createTaxRate(taxRate)
    }

    suspend fun updateTaxRate(taxRate: TaxRate): Result<TaxRate> {
        // For now, use createTaxRate which handles both create and update
        // In the future, add a specific updateTaxRate method to the repository
        return taxRepository.createTaxRate(taxRate)
    }

    suspend fun getTaxRatesByHsnCode(hsnCode: String): List<TaxRate> {
        return taxRepository.getTaxRatesByHsnCode(hsnCode)
    }

    suspend fun getEffectiveTaxRate(hsnCode: String, businessType: BusinessType): TaxRate? {
        return taxRepository.getEffectiveTaxRate(hsnCode, businessType)
    }

    suspend fun calculateTax(request: TaxCalculationRequest): Result<TaxCalculationResult> {
        return taxRepository.calculateTax(request)
    }

    suspend fun syncData(): Result<Unit> {
        return taxRepository.syncData()
    }
}