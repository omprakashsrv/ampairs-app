package com.ampairs.tax.testutil

import com.ampairs.common.model.PageResponse
import com.ampairs.tax.data.api.BulkImportResult
import com.ampairs.tax.data.api.BulkSubscribeResult
import com.ampairs.tax.data.api.TaxConfigurationApi
import com.ampairs.tax.data.db.dao.TaxCodeDao
import com.ampairs.tax.data.db.dao.TaxComponentDao
import com.ampairs.tax.data.db.dao.TaxComponentTypeDao
import com.ampairs.tax.data.db.dao.TaxConfigurationDao
import com.ampairs.tax.data.db.dao.TaxRuleDao
import com.ampairs.tax.data.db.entity.TaxCodeEntity
import com.ampairs.tax.data.db.entity.TaxComponentEntity
import com.ampairs.tax.data.db.entity.TaxComponentTypeEntity
import com.ampairs.tax.data.db.entity.TaxConfigurationEntity
import com.ampairs.tax.data.db.entity.TaxRuleEntity
import com.ampairs.tax.data.db.entity.toEntity
import com.ampairs.tax.domain.model.ComponentComposition
import com.ampairs.tax.domain.model.ComponentReference
import com.ampairs.tax.domain.model.JurisdictionLevel
import com.ampairs.tax.domain.model.MasterTaxCode
import com.ampairs.tax.domain.model.TaxCalculationMethod
import com.ampairs.tax.domain.model.TaxCode
import com.ampairs.tax.domain.model.TaxComponentCategory
import com.ampairs.tax.domain.model.TaxComponentType
import com.ampairs.tax.domain.model.TaxConfiguration
import com.ampairs.tax.domain.model.TaxRule
import com.ampairs.tax.domain.model.WorkspaceTaxComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/**
 * Hand-written KMP-safe fakes for the tax feature's DAOs + the network API.
 *
 * MockK/Mockito are JVM-only and won't compile for the iOS/native test targets, so every collaborator
 * is faked by hand here (mirrors the approach in `feature/unit`'s `UnitRepositoryTest`). The fakes are
 * shared across the strategy, engine and repository tests so the maths-heavy cases can build a real
 * repository over an in-memory store and a network that never gets touched on the offline paths.
 */

private val EPOCH = Instant.fromEpochMilliseconds(0)

// ---------------------------------------------------------------------------
// Builders — keep the verbose entity constructors out of the test bodies.
// ---------------------------------------------------------------------------

/** A single component within a [ComponentComposition]. */
fun componentRef(id: String, name: String, rate: Double, order: Int = 0) =
    ComponentReference(id = id, name = name, rate = rate, order = order)

/** Build a [TaxRule] (domain) whose composition has the given scenario keys. */
fun taxRule(
    taxCode: String,
    jurisdiction: String,
    countryCode: String = "",
    composition: Map<String, ComponentComposition>,
    id: String = "rule-$taxCode-$jurisdiction",
): TaxRule = TaxRule(
    id = id,
    countryCode = countryCode,
    taxCode = taxCode,
    jurisdiction = jurisdiction,
    componentComposition = composition,
)

/** Convenience: a composition for one scenario key from a list of components. */
fun composition(
    scenario: String,
    components: List<ComponentReference>,
    totalRate: Double = components.sumOf { it.rate },
) = ComponentComposition(scenario = scenario, components = components, totalRate = totalRate)

/** A workspace tax component (domain) — only the fields the strategies read are meaningful. */
fun workspaceComponent(
    id: String,
    componentTypeId: String,
    jurisdiction: String = "",
    rate: Double = 0.0,
    order: Int = 0,
): WorkspaceTaxComponent = WorkspaceTaxComponent(
    id = id,
    componentTypeId = componentTypeId,
    jurisdiction = jurisdiction,
    jurisdictionLevel = JurisdictionLevel.STATE,
    ratePercentage = rate,
    calculationOrder = order,
    effectiveFrom = EPOCH,
    createdAt = EPOCH,
    updatedAt = EPOCH,
)

/** A component type (domain). [componentCode] is what strategies key off (GST/PST/STATE_TAX...). */
fun componentType(
    id: String,
    componentCode: String,
    countryCode: String = "",
): TaxComponentType = TaxComponentType(
    id = id,
    countryCode = countryCode,
    componentCode = componentCode,
    componentName = componentCode,
    shortName = componentCode,
    displayName = componentCode,
    componentCategory = TaxComponentCategory.FEDERAL,
    calculationMethod = TaxCalculationMethod.PERCENTAGE,
)

fun taxCode(
    id: String,
    code: String = "CODE",
    isFavorite: Boolean = false,
    notes: String? = null,
    isActive: Boolean = true,
    usageCount: Int = 0,
    syncStatus: String = "SYNCED",
): TaxCode = TaxCode(
    id = id,
    masterTaxCodeId = "master-$id",
    code = code,
    codeType = com.ampairs.tax.domain.model.TaxCodeType.HSN_CODE,
    description = "desc",
    shortDescription = "short",
    isFavorite = isFavorite,
    notes = notes,
    isActive = isActive,
    usageCount = usageCount,
    addedAt = EPOCH,
    updatedAt = EPOCH,
    syncStatus = syncStatus,
)

// ---------------------------------------------------------------------------
// Fake DAOs
// ---------------------------------------------------------------------------

/** In-memory [TaxRuleDao]. Seed with [seedRule]; the strategies only read via getEffectiveRule. */
class FakeTaxRuleDao : TaxRuleDao {
    private val rows = MutableStateFlow<Map<String, TaxRuleEntity>>(emptyMap())

    fun seedRule(rule: TaxRule) {
        val e = rule.toEntity()
        rows.value = rows.value + (e.id to e)
    }

    override fun observeTaxRules(): Flow<List<TaxRuleEntity>> =
        rows.map { m -> m.values.filter { it.isActive }.sortedByDescending { it.createdAt } }

    override suspend fun getById(id: String): TaxRuleEntity? = rows.value[id]

    override suspend fun getEffectiveRule(taxCode: String, jurisdiction: String): TaxRuleEntity? =
        rows.value.values.firstOrNull {
            it.taxCode == taxCode && it.jurisdiction == jurisdiction && it.isActive
        }

    override fun observeRulesByCodeString(taxCode: String): Flow<List<TaxRuleEntity>> =
        rows.map { m -> m.values.filter { it.taxCode == taxCode && it.isActive } }

    override fun observeRulesByTaxCode(taxCodeId: String): Flow<List<TaxRuleEntity>> =
        rows.map { m -> m.values.filter { it.taxCodeId == taxCodeId && it.isActive } }

    override suspend fun getRulesByTaxCode(taxCodeId: String): List<TaxRuleEntity> =
        rows.value.values.filter { it.taxCodeId == taxCodeId && it.isActive }

    override suspend fun getUnsyncedRules(): List<TaxRuleEntity> =
        rows.value.values.filter { it.syncStatus != "SYNCED" }

    override suspend fun getModifiedAfter(modifiedAfter: Instant): List<TaxRuleEntity> =
        rows.value.values.filter { it.updatedAt > modifiedAfter }

    override suspend fun insert(rule: TaxRuleEntity) {
        rows.value = rows.value + (rule.id to rule)
    }

    override suspend fun insertAll(rules: List<TaxRuleEntity>) {
        rows.value = rows.value + rules.associateBy { it.id }
    }

    override suspend fun update(rule: TaxRuleEntity) {
        rows.value = rows.value + (rule.id to rule)
    }

    override suspend fun updateSyncStatus(id: String, status: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(syncStatus = status)) }
    }

    override suspend fun deactivate(id: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(isActive = false)) }
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value - id
    }

    override suspend fun deleteAll() {
        rows.value = emptyMap()
    }
}

/** In-memory [TaxComponentDao]. */
class FakeTaxComponentDao : TaxComponentDao {
    private val rows = MutableStateFlow<Map<String, TaxComponentEntity>>(emptyMap())

    fun seed(component: WorkspaceTaxComponent) {
        val e = component.toEntity()
        rows.value = rows.value + (e.id to e)
    }

    override fun observeTaxComponents(): Flow<List<TaxComponentEntity>> =
        rows.map { m -> m.values.filter { it.isActive }.sortedBy { it.calculationOrder } }

    override suspend fun getComponentsByJurisdiction(
        jurisdiction: String,
        effectiveDate: Instant,
    ): List<TaxComponentEntity> =
        rows.value.values.filter {
            it.jurisdiction == jurisdiction && it.isActive &&
                it.effectiveFrom <= effectiveDate &&
                (it.effectiveTo == null || it.effectiveTo!! >= effectiveDate)
        }.sortedBy { it.calculationOrder }

    override suspend fun getById(id: String): TaxComponentEntity? = rows.value[id]

    override suspend fun getByTypeAndJurisdiction(
        componentTypeId: String,
        jurisdiction: String,
    ): TaxComponentEntity? =
        rows.value.values.firstOrNull {
            it.componentTypeId == componentTypeId && it.jurisdiction == jurisdiction && it.isActive
        }

    override suspend fun getUnsyncedComponents(): List<TaxComponentEntity> =
        rows.value.values.filter { it.syncStatus != "SYNCED" }

    override suspend fun insert(component: TaxComponentEntity) {
        rows.value = rows.value + (component.id to component)
    }

    override suspend fun insertAll(components: List<TaxComponentEntity>) {
        rows.value = rows.value + components.associateBy { it.id }
    }

    override suspend fun update(component: TaxComponentEntity) {
        rows.value = rows.value + (component.id to component)
    }

    override suspend fun updateSyncStatus(id: String, status: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(syncStatus = status)) }
    }

    override suspend fun deactivate(id: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(isActive = false)) }
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value - id
    }

    override suspend fun deleteAll() {
        rows.value = emptyMap()
    }
}

/** In-memory [TaxComponentTypeDao]. */
class FakeTaxComponentTypeDao : TaxComponentTypeDao {
    private val rows = MutableStateFlow<Map<String, TaxComponentTypeEntity>>(emptyMap())

    fun seed(type: TaxComponentType) {
        val e = type.toEntity()
        rows.value = rows.value + (e.id to e)
    }

    override fun observeAllComponentTypes(): Flow<List<TaxComponentTypeEntity>> =
        rows.map { m -> m.values.filter { it.isActive }.sortedBy { it.sortOrder } }

    override fun observeComponentTypesByCountry(countryCode: String): Flow<List<TaxComponentTypeEntity>> =
        rows.map { m -> m.values.filter { it.countryCode == countryCode && it.isActive }.sortedBy { it.sortOrder } }

    override suspend fun getById(id: String): TaxComponentTypeEntity? = rows.value[id]

    override suspend fun getByCode(countryCode: String, componentCode: String): TaxComponentTypeEntity? =
        rows.value.values.firstOrNull {
            it.countryCode == countryCode && it.componentCode == componentCode && it.isActive
        }

    override suspend fun insert(componentType: TaxComponentTypeEntity) {
        rows.value = rows.value + (componentType.id to componentType)
    }

    override suspend fun insertAll(componentTypes: List<TaxComponentTypeEntity>) {
        rows.value = rows.value + componentTypes.associateBy { it.id }
    }

    override suspend fun deleteByCountry(countryCode: String) {
        rows.value = rows.value.filterValues { it.countryCode != countryCode }
    }
}

/** In-memory [TaxConfigurationDao] (single-row table). */
class FakeTaxConfigurationDao : TaxConfigurationDao {
    private val row = MutableStateFlow<TaxConfigurationEntity?>(null)

    fun seed(config: TaxConfiguration) {
        row.value = config.toEntity()
    }

    override fun observeConfiguration(): Flow<TaxConfigurationEntity?> = row

    override suspend fun getConfiguration(): TaxConfigurationEntity? = row.value

    override suspend fun insert(config: TaxConfigurationEntity) {
        row.value = config
    }

    override suspend fun update(config: TaxConfigurationEntity) {
        row.value = config
    }

    override suspend fun updateSyncTime(timestamp: Instant) {
        row.value = row.value?.copy(syncedAt = timestamp)
    }

    override suspend fun delete() {
        row.value = null
    }
}

/** In-memory [TaxCodeDao]. */
class FakeTaxCodeDao : TaxCodeDao {
    val rows = MutableStateFlow<Map<String, TaxCodeEntity>>(emptyMap())

    fun seed(code: TaxCode) {
        val e = code.toEntity()
        rows.value = rows.value + (e.id to e)
    }

    override fun observeTaxCodes(): Flow<List<TaxCodeEntity>> =
        rows.map { m -> m.values.filter { it.isActive }.sortedByDescending { it.usageCount } }

    override suspend fun getById(id: String): TaxCodeEntity? = rows.value[id]

    override suspend fun getByCode(code: String): TaxCodeEntity? =
        rows.value.values.firstOrNull { it.code == code && it.isActive }

    override fun observeFavorites(): Flow<List<TaxCodeEntity>> =
        rows.map { m -> m.values.filter { it.isFavorite && it.isActive }.sortedByDescending { it.usageCount } }

    override suspend fun searchTaxCodes(query: String, limit: Int): List<TaxCodeEntity> =
        rows.value.values.filter {
            it.isActive && (it.code.contains(query) || it.description.contains(query) || it.shortDescription.contains(query))
        }.sortedByDescending { it.usageCount }.take(limit)

    override suspend fun getUnsyncedCodes(): List<TaxCodeEntity> =
        rows.value.values.filter { it.syncStatus != "SYNCED" }

    override suspend fun getModifiedAfter(modifiedAfter: Instant): List<TaxCodeEntity> =
        rows.value.values.filter { it.updatedAt > modifiedAfter }

    override suspend fun insert(code: TaxCodeEntity) {
        rows.value = rows.value + (code.id to code)
    }

    override suspend fun insertAll(codes: List<TaxCodeEntity>) {
        rows.value = rows.value + codes.associateBy { it.id }
    }

    override suspend fun update(code: TaxCodeEntity) {
        rows.value = rows.value + (code.id to code)
    }

    override suspend fun incrementUsageCount(id: String, timestamp: Instant) {
        rows.value[id]?.let {
            rows.value = rows.value + (id to it.copy(usageCount = it.usageCount + 1, lastUsedAt = timestamp))
        }
    }

    override suspend fun setFavorite(id: String, isFavorite: Boolean) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(isFavorite = isFavorite)) }
    }

    override suspend fun updateSyncStatus(id: String, status: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(syncStatus = status)) }
    }

    override suspend fun deactivate(id: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(isActive = false)) }
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value - id
    }

    override suspend fun deleteAll() {
        rows.value = emptyMap()
    }
}

// ---------------------------------------------------------------------------
// Fake network API
// ---------------------------------------------------------------------------

/**
 * [TaxConfigurationApi] that records calls and returns configurable results. Every method defaults to
 * a failure so any unexpected network call in an offline-first test is obvious. Override the relevant
 * `xxxResult` to opt a method into success.
 */
class FakeTaxConfigurationApi : TaxConfigurationApi {

    // Recording
    val subscribedMasterCodes = mutableListOf<String>()
    val unsubscribedCodes = mutableListOf<String>()
    var subscribeCallCount = 0
    val bulkSubscribeBatches = mutableListOf<List<String>>()

    // Configurable results
    var subscribeResult: (String) -> Result<TaxCode> =
        { Result.failure(Exception("not configured")) }
    var unsubscribeResult: Result<Unit> = Result.failure(Exception("not configured"))
    var bulkSubscribeResult: (List<String>) -> Result<BulkSubscribeResult> =
        { Result.failure(Exception("not configured")) }
    var searchMasterResult: Result<PageResponse<MasterTaxCode>> = Result.failure(Exception("not configured"))
    var popularResult: Result<PageResponse<MasterTaxCode>> = Result.failure(Exception("not configured"))

    private fun fail(): Nothing = throw NotImplementedError("FakeTaxConfigurationApi: not used in this test")

    override suspend fun getWorkspaceConfiguration(): Result<TaxConfiguration> =
        Result.failure(Exception("no network"))

    override suspend fun createWorkspaceConfiguration(config: TaxConfiguration): Result<TaxConfiguration> =
        Result.failure(Exception("no network"))

    override suspend fun updateWorkspaceConfiguration(config: TaxConfiguration): Result<TaxConfiguration> =
        Result.failure(Exception("no network"))

    override suspend fun searchMasterTaxCodes(
        query: String,
        countryCode: String,
        codeType: String?,
        category: String?,
        page: Int,
        size: Int,
    ): Result<PageResponse<MasterTaxCode>> = searchMasterResult

    override suspend fun getMasterTaxCode(codeId: String): Result<MasterTaxCode> = fail()

    override suspend fun getPopularTaxCodes(
        countryCode: String,
        industry: String?,
        limit: Int,
    ): Result<PageResponse<MasterTaxCode>> = popularResult

    override suspend fun getTaxCodes(
        modifiedAfter: Instant?,
        page: Int,
        size: Int,
    ): Result<PageResponse<TaxCode>> = Result.failure(Exception("no network"))

    override suspend fun subscribeToTaxCode(
        masterTaxCodeId: String,
        customTaxRuleId: String?,
        isFavorite: Boolean,
        notes: String?,
    ): Result<TaxCode> {
        subscribeCallCount++
        subscribedMasterCodes += masterTaxCodeId
        return subscribeResult(masterTaxCodeId)
    }

    override suspend fun unsubscribeFromTaxCode(workspaceTaxCodeId: String): Result<Unit> {
        unsubscribedCodes += workspaceTaxCodeId
        return unsubscribeResult
    }

    override suspend fun updateTaxCodeConfig(
        taxCodeId: String,
        isFavorite: Boolean?,
        notes: String?,
        customName: String?,
    ): Result<TaxCode> = fail()

    override suspend fun bulkSubscribeTaxCodes(
        masterTaxCodeIds: List<String>,
        applyDefaultRules: Boolean,
    ): Result<BulkSubscribeResult> {
        bulkSubscribeBatches += masterTaxCodeIds
        return bulkSubscribeResult(masterTaxCodeIds)
    }

    override suspend fun getComponentTypes(countryCode: String): Result<List<TaxComponentType>> = fail()

    override suspend fun getWorkspaceComponents(
        modifiedAfter: Instant?,
        page: Int,
        size: Int,
    ): Result<PageResponse<WorkspaceTaxComponent>> = Result.failure(Exception("no network"))

    override suspend fun getTaxRules(
        modifiedAfter: Instant?,
        page: Int,
        size: Int,
    ): Result<PageResponse<TaxRule>> = Result.failure(Exception("no network"))

    override suspend fun getTaxRulesByTaxCode(taxCodeId: String): Result<List<TaxRule>> =
        Result.failure(Exception("no network"))

    override suspend fun updateTaxRule(ruleId: String, rule: TaxRule): Result<TaxRule> =
        Result.failure(Exception("no network"))

    override suspend fun deleteTaxRule(ruleId: String): Result<Unit> = fail()

    override suspend fun createTaxRule(rule: TaxRule): Result<TaxRule> =
        Result.failure(Exception("no network"))

    override suspend fun bulkImportTaxRules(rules: List<TaxRule>): Result<BulkImportResult> =
        Result.failure(Exception("no network"))
}
