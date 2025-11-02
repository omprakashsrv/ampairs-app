package com.ampairs.tax.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.delete
import com.ampairs.common.model.Response
import com.ampairs.tax.domain.HsnCode
import com.ampairs.tax.domain.TaxCalculationRequest
import com.ampairs.tax.domain.TaxCalculationResult
import com.ampairs.tax.domain.TaxRate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.ktor.client.engine.HttpClientEngine

const val TAX_ENDPOINT = "http://localhost:8080"

@Serializable
data class HsnCodeUpsertRequest(
    @SerialName("uid")
    val uid: String? = null,
    @SerialName("hsn_code")
    val hsnCode: String,
    @SerialName("hsn_description")
    val hsnDescription: String,
    @SerialName("hsn_chapter")
    val hsnChapter: String? = null,
    @SerialName("hsn_heading")
    val hsnHeading: String? = null,
    @SerialName("parent_hsn_id")
    val parentHsnId: Long? = null,
    @SerialName("level")
    val level: Int = 1,
    @SerialName("unit_of_measurement")
    val unitOfMeasurement: String? = null,
    @SerialName("exemption_available")
    val exemptionAvailable: Boolean = false,
    @SerialName("business_category_rules")
    val businessCategoryRules: Map<String, String> = emptyMap(),
    @SerialName("attributes")
    val attributes: Map<String, String> = emptyMap(),
    @SerialName("effective_from")
    val effectiveFrom: String? = null,
    @SerialName("effective_to")
    val effectiveTo: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true
)

@Serializable
data class TaxRateUpsertRequest(
    @SerialName("uid")
    val uid: String? = null,
    @SerialName("hsn_code_id")
    val hsnCodeId: Long,
    @SerialName("tax_component_type")
    val taxComponentType: String,
    @SerialName("rate_percentage")
    val ratePercentage: String,
    @SerialName("fixed_amount_per_unit")
    val fixedAmountPerUnit: String? = null,
    @SerialName("minimum_amount")
    val minimumAmount: String? = null,
    @SerialName("maximum_amount")
    val maximumAmount: String? = null,
    @SerialName("business_type")
    val businessType: String,
    @SerialName("geographical_zone")
    val geographicalZone: String? = null,
    @SerialName("effective_from")
    val effectiveFrom: String,
    @SerialName("effective_to")
    val effectiveTo: String? = null,
    @SerialName("version_number")
    val versionNumber: Int = 1,
    @SerialName("notification_number")
    val notificationNumber: String? = null,
    @SerialName("notification_date")
    val notificationDate: String? = null,
    @SerialName("conditions")
    val conditions: Map<String, String> = emptyMap(),
    @SerialName("exemption_rules")
    val exemptionRules: Map<String, String> = emptyMap(),
    @SerialName("is_reverse_charge_applicable")
    val isReverseChargeApplicable: Boolean = false,
    @SerialName("is_composition_scheme_applicable")
    val isCompositionSchemeApplicable: Boolean = true,
    @SerialName("description")
    val description: String? = null,
    @SerialName("source_reference")
    val sourceReference: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true
)

class TaxApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : TaxApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getHsnCodes(
        workspaceId: String,
        page: Int,
        size: Int,
        search: String?,
        category: String?
    ): Result<List<HsnCode>> {
        return try {
            val params = mutableMapOf<String, Any>(
                "page" to page,
                "size" to size
            )
            search?.let { params["searchTerm"] = it }
            category?.let { params["chapter"] = it }

            val response: Response<List<HsnCode>> = get(
                client,
                "$TAX_ENDPOINT/api/v1/hsn-codes",
                params
            )
            Result.success(response.data ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHsnCode(workspaceId: String, hsnCodeId: String): Result<HsnCode> {
        return try {
            val response: Response<HsnCode> = get(
                client,
                "$TAX_ENDPOINT/api/v1/hsn-codes/$hsnCodeId"
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("HSN Code not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createHsnCode(workspaceId: String, hsnCode: HsnCode): Result<HsnCode> {
        return try {
            // Use single POST UPSERT endpoint
            val requestDto = HsnCodeUpsertRequest(
                uid = null, // No UID for new creation
                hsnCode = hsnCode.hsnCode,
                hsnDescription = hsnCode.description,
                hsnChapter = hsnCode.chapter.ifEmpty { null },
                hsnHeading = hsnCode.heading.ifEmpty { null },
                parentHsnId = hsnCode.parentHsnId?.toLongOrNull(),
                level = 1,
                unitOfMeasurement = null,
                exemptionAvailable = false,
                businessCategoryRules = emptyMap(),
                attributes = emptyMap(),
                effectiveFrom = null,
                effectiveTo = null,
                isActive = hsnCode.isActive
            )
            val response: Response<HsnCode> = post(
                client,
                "$TAX_ENDPOINT/api/v1/hsn-codes",
                requestDto
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to create HSN Code"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateHsnCode(
        workspaceId: String,
        hsnCodeId: String,
        hsnCode: HsnCode
    ): Result<HsnCode> {
        return try {
            // Use single POST UPSERT endpoint with UID included
            val requestDto = HsnCodeUpsertRequest(
                uid = hsnCodeId, // Include UID for update operation
                hsnCode = hsnCode.hsnCode,
                hsnDescription = hsnCode.description,
                hsnChapter = hsnCode.chapter.ifEmpty { null },
                hsnHeading = hsnCode.heading.ifEmpty { null },
                parentHsnId = hsnCode.parentHsnId?.toLongOrNull(),
                level = 1,
                unitOfMeasurement = null,
                exemptionAvailable = false,
                businessCategoryRules = emptyMap(),
                attributes = emptyMap(),
                effectiveFrom = null,
                effectiveTo = null,
                isActive = hsnCode.isActive
            )
            val response: Response<HsnCode> = post(
                client,
                "$TAX_ENDPOINT/api/v1/hsn-codes",
                requestDto
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to update HSN Code"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHsnCode(workspaceId: String, hsnCodeId: String): Result<Unit> {
        return try {
            delete<Unit>(
                client,
                "$TAX_ENDPOINT/api/v1/hsn-codes/$hsnCodeId"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchHsnCodes(workspaceId: String, query: String): Result<List<HsnCode>> {
        return try {
            val params = mapOf("searchTerm" to query)
            val response: Response<List<HsnCode>> = get(
                client,
                "$TAX_ENDPOINT/api/v1/hsn-codes",
                params
            )
            Result.success(response.data ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTaxRates(
        workspaceId: String,
        hsnCode: String?,
        businessType: String?,
        effectiveDate: Long?
    ): Result<List<TaxRate>> {
        return try {
            val params = mutableMapOf<String, Any>()
            hsnCode?.let { params["hsnCode"] = it }
            businessType?.let { params["businessType"] = it }
            effectiveDate?.let { params["effectiveDate"] = it }

            val response: Response<List<TaxRate>> = get(
                client,
                "$TAX_ENDPOINT/api/v1/tax-rates",
                params
            )
            Result.success(response.data ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEffectiveTaxRate(
        workspaceId: String,
        hsnCode: String,
        businessType: String,
        effectiveDate: Long
    ): Result<TaxRate> {
        return try {
            val params = mapOf(
                "hsnCode" to hsnCode,
                "businessType" to businessType,
                "effectiveDate" to effectiveDate
            )
            val response: Response<TaxRate> = get(
                client,
                "$TAX_ENDPOINT/api/v1/tax-rates/effective",
                params
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("Effective tax rate not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTaxRate(workspaceId: String, taxRateId: String): Result<TaxRate> {
        return try {
            val response: Response<TaxRate> = get(
                client,
                "$TAX_ENDPOINT/api/v1/tax-rates/$taxRateId"
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("Tax Rate not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createTaxRate(workspaceId: String, taxRate: TaxRate): Result<TaxRate> {
        return try {
            // Use single POST UPSERT endpoint
            val requestDto = TaxRateUpsertRequest(
                uid = null, // No UID for new creation
                hsnCodeId = 1L, // Default value, should be mapped appropriately
                taxComponentType = taxRate.taxType.name,
                ratePercentage = taxRate.ratePercentage.toString(),
                fixedAmountPerUnit = taxRate.cessAmountPerUnit?.toString(),
                minimumAmount = null,
                maximumAmount = null,
                businessType = taxRate.businessType.name,
                geographicalZone = taxRate.geographicalZone,
                effectiveFrom = "2024-01-01", // Default effective date
                effectiveTo = null,
                versionNumber = taxRate.versionNumber,
                notificationNumber = null,
                notificationDate = null,
                conditions = emptyMap(),
                exemptionRules = emptyMap(),
                isReverseChargeApplicable = false,
                isCompositionSchemeApplicable = true,
                description = null, // Not available in current TaxRate domain
                sourceReference = null,
                isActive = taxRate.isActive
            )
            val response: Response<TaxRate> = post(
                client,
                "$TAX_ENDPOINT/api/v1/tax-rates",
                requestDto
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to create Tax Rate"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTaxRate(
        workspaceId: String,
        taxRateId: String,
        taxRate: TaxRate
    ): Result<TaxRate> {
        return try {
            // Use single POST UPSERT endpoint with UID included
            val requestDto = TaxRateUpsertRequest(
                uid = taxRateId, // Include UID for update operation
                hsnCodeId = 1L, // Default value, should be mapped appropriately
                taxComponentType = taxRate.taxType.name,
                ratePercentage = taxRate.ratePercentage.toString(),
                fixedAmountPerUnit = taxRate.cessAmountPerUnit?.toString(),
                minimumAmount = null,
                maximumAmount = null,
                businessType = taxRate.businessType.name,
                geographicalZone = taxRate.geographicalZone,
                effectiveFrom = "2024-01-01", // Default effective date
                effectiveTo = null,
                versionNumber = taxRate.versionNumber,
                notificationNumber = null,
                notificationDate = null,
                conditions = emptyMap(),
                exemptionRules = emptyMap(),
                isReverseChargeApplicable = false,
                isCompositionSchemeApplicable = true,
                description = null, // Not available in current TaxRate domain
                sourceReference = null,
                isActive = taxRate.isActive
            )

            val response: Response<TaxRate> = post(
                client,
                "$TAX_ENDPOINT/api/v1/tax-rates",
                requestDto
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to update Tax Rate"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTaxRate(workspaceId: String, taxRateId: String): Result<Unit> {
        return try {
            delete<Unit>(
                client,
                "$TAX_ENDPOINT/api/v1/tax-rates/$taxRateId"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun calculateTax(
        workspaceId: String,
        request: TaxCalculationRequest
    ): Result<TaxCalculationResult> {
        return try {
            val response: Response<TaxCalculationResult> = post(
                client,
                "$TAX_ENDPOINT/api/v1/tax/calculate",
                request
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to calculate tax"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}