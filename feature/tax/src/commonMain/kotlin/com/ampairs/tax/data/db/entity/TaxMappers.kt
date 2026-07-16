package com.ampairs.tax.data.db.entity

import com.ampairs.tax.domain.model.ComponentComposition
import com.ampairs.tax.domain.model.JurisdictionLevel
import com.ampairs.tax.domain.model.RateTier
import com.ampairs.tax.domain.model.RateType
import com.ampairs.tax.domain.model.TaxCalculationMethod
import com.ampairs.tax.domain.model.TaxCode
import com.ampairs.tax.domain.model.TaxCodeType
import com.ampairs.tax.domain.model.TaxComponentCategory
import com.ampairs.tax.domain.model.TaxComponentType
import com.ampairs.tax.domain.model.TaxConfiguration
import com.ampairs.tax.domain.model.TaxRule
import com.ampairs.tax.domain.model.TaxStrategy
import com.ampairs.tax.domain.model.WorkspaceTaxComponent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Entity <-> domain mappers. The @Entity classes live in :data:database (same package); these
// mappers stay in the feature module because they reference the tax domain models/enums.

fun TaxCodeEntity.toDomain(): TaxCode = TaxCode(
    id = id,
    masterTaxCodeId = masterTaxCodeId,
    code = code,
    codeType = TaxCodeType.valueOf(codeType),
    description = description,
    shortDescription = shortDescription,
    customName = customName,
    customTaxRuleId = customTaxRuleId,
    usageCount = usageCount,
    lastUsedAt = lastUsedAt,
    isFavorite = isFavorite,
    notes = notes,
    isActive = isActive,
    addedAt = addedAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

fun TaxCode.toEntity(): TaxCodeEntity = TaxCodeEntity(
    id = id,
    masterTaxCodeId = masterTaxCodeId,
    code = code,
    codeType = codeType.name,
    description = description,
    shortDescription = shortDescription,
    customName = customName,
    customTaxRuleId = customTaxRuleId,
    usageCount = usageCount,
    lastUsedAt = lastUsedAt,
    isFavorite = isFavorite,
    notes = notes,
    isActive = isActive,
    addedAt = addedAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

fun TaxComponentEntity.toDomain(): WorkspaceTaxComponent = WorkspaceTaxComponent(
    id = id,
    componentTypeId = componentTypeId,
    jurisdiction = jurisdiction,
    jurisdictionLevel = JurisdictionLevel.valueOf(jurisdictionLevel),
    ratePercentage = ratePercentage,
    rateType = RateType.valueOf(rateType),
    rateTiers = rateTiers?.let { Json.decodeFromString<List<RateTier>>(it) },
    calculationOrder = calculationOrder,
    isCompoundTax = isCompoundTax,
    compoundOnComponents = compoundOnComponents?.let { Json.decodeFromString(it) },
    applicableFor = Json.decodeFromString(applicableFor),
    exemptions = Json.decodeFromString(exemptions),
    effectiveFrom = effectiveFrom,
    effectiveTo = effectiveTo,
    glAccountCode = glAccountCode,
    taxAuthorityCode = taxAuthorityCode,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

fun WorkspaceTaxComponent.toEntity(): TaxComponentEntity = TaxComponentEntity(
    id = id,
    componentTypeId = componentTypeId,
    jurisdiction = jurisdiction,
    jurisdictionLevel = jurisdictionLevel.name,
    ratePercentage = ratePercentage,
    rateType = rateType.name,
    rateTiers = rateTiers?.let { Json.encodeToString(it) },
    calculationOrder = calculationOrder,
    isCompoundTax = isCompoundTax,
    compoundOnComponents = compoundOnComponents?.let { Json.encodeToString(it) },
    applicableFor = Json.encodeToString(applicableFor),
    exemptions = Json.encodeToString(exemptions),
    effectiveFrom = effectiveFrom,
    effectiveTo = effectiveTo,
    glAccountCode = glAccountCode,
    taxAuthorityCode = taxAuthorityCode,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

fun TaxComponentTypeEntity.toDomain(): TaxComponentType = TaxComponentType(
    id = id,
    countryCode = countryCode,
    componentCode = componentCode,
    componentName = componentName,
    shortName = shortName,
    displayName = displayName,
    componentCategory = TaxComponentCategory.valueOf(componentCategory),
    calculationMethod = TaxCalculationMethod.valueOf(calculationMethod),
    isMandatory = isMandatory,
    defaultRate = defaultRate,
    sortOrder = sortOrder,
    metadata = try {
        Json.decodeFromString(metadata)
    } catch (e: Exception) {
        emptyMap()
    },
    isActive = isActive
)

fun TaxComponentType.toEntity(): TaxComponentTypeEntity = TaxComponentTypeEntity(
    id = id,
    countryCode = countryCode,
    componentCode = componentCode,
    componentName = componentName,
    shortName = shortName,
    displayName = displayName,
    componentCategory = componentCategory.name,
    calculationMethod = calculationMethod.name,
    isMandatory = isMandatory,
    defaultRate = defaultRate,
    sortOrder = sortOrder,
    metadata = Json.encodeToString(metadata),
    isActive = isActive
)

fun TaxConfigurationEntity.toDomain(): TaxConfiguration = TaxConfiguration(
    id = id,
    countryCode = countryCode,
    taxStrategy = TaxStrategy.valueOf(taxStrategy),
    defaultTaxCodeSystem = TaxCodeType.valueOf(defaultTaxCodeSystem),
    taxJurisdictions = Json.decodeFromString(taxJurisdictions),
    industry = industry,
    autoSubscribeNewCodes = autoSubscribeNewCodes,
    syncedAt = syncedAt,
    metadata = try {
        Json.decodeFromString(metadata)
    } catch (e: Exception) {
        emptyMap()
    }
)

fun TaxConfiguration.toEntity(): TaxConfigurationEntity = TaxConfigurationEntity(
    id = id,
    countryCode = countryCode,
    taxStrategy = taxStrategy.name,
    defaultTaxCodeSystem = defaultTaxCodeSystem.name,
    taxJurisdictions = Json.encodeToString(taxJurisdictions),
    industry = industry,
    autoSubscribeNewCodes = autoSubscribeNewCodes,
    syncedAt = syncedAt,
    metadata = Json.encodeToString(metadata)
)

fun TaxRuleEntity.toDomain(): TaxRule = TaxRule(
    id = id,
    countryCode = countryCode,
    taxCodeId = taxCodeId,
    taxCode = taxCode,
    taxCodeType = taxCodeType,
    taxCodeDescription = taxCodeDescription,
    jurisdiction = jurisdiction,
    jurisdictionLevel = jurisdictionLevel,
    componentComposition = Json.decodeFromString(componentComposition),
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

fun TaxRule.toEntity(): TaxRuleEntity = TaxRuleEntity(
    id = id,
    countryCode = countryCode,
    taxCodeId = taxCodeId,
    taxCode = taxCode,
    taxCodeType = taxCodeType,
    taxCodeDescription = taxCodeDescription,
    jurisdiction = jurisdiction,
    jurisdictionLevel = jurisdictionLevel,
    componentComposition = Json.encodeToString(componentComposition),
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)
