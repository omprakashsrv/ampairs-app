package com.ampairs.business.data.repository

import com.ampairs.business.data.api.BusinessApi
import com.ampairs.business.data.db.BusinessDao
import com.ampairs.business.data.db.BusinessEntity
import com.ampairs.business.data.db.toDomain
import com.ampairs.business.data.db.toEntity
import com.ampairs.business.domain.*
import com.ampairs.business.util.BusinessConstants
import com.ampairs.common.di.AppScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.workspace.context.WorkspaceContextManager
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Inject
class BusinessRepository(
    private val businessDao: BusinessDao,
    private val businessApi: BusinessApi,
    private val workspaceContextManager: WorkspaceContextManager
) {

    fun observeBusiness(): Flow<Business?> = businessDao.observeBusiness().map { it?.toDomain() }

    suspend fun getCachedBusiness(): Business? = businessDao.getBusiness()?.toDomain()

    @OptIn(ExperimentalTime::class)
    suspend fun saveLocal(business: Business, markSynced: Boolean) {
        val workspaceId = business.workspaceId ?: workspaceContextManager.getCurrentWorkspaceId()
        val existing = businessDao.getBusiness()

        var entity = business
            .ensureId(existing)
            .toEntity(markSynced = markSynced, workspaceId = workspaceId)

        entity = entity.copy(
            seqId = entity.seqId ?: existing?.seqId,
            workspaceId = entity.workspaceId ?: existing?.workspaceId,
            localCreatedAt = existing?.localCreatedAt ?: entity.localCreatedAt
        )

        businessDao.upsertBusiness(entity)
        if (markSynced) {
            val now = Clock.System.now().toEpochMilliseconds()
            businessDao.updateSyncStatus(entity.uid, true, now)
        }
    }

    suspend fun clearLocal() {
        businessDao.clearAll()
    }

    suspend fun checkBusinessExists(): Result<Boolean> {
        return businessApi.checkBusinessExists()
    }

    suspend fun createBusinessProfile(request: BusinessCreateRequest): Result<BusinessProfile> {
        val workspaceId = workspaceContextManager.getCurrentWorkspaceId()
            ?: return Result.failure(IllegalStateException("Workspace not selected"))

        val payload = BusinessPayload(
            name = request.name,
            businessType = BusinessType.valueOf(request.businessType),
            description = request.description,
            ownerName = request.ownerName,
            addressLine1 = request.addressLine1,
            addressLine2 = request.addressLine2,
            city = request.city,
            state = request.state,
            postalCode = request.postalCode,
            country = request.country,
            latitude = request.latitude,
            longitude = request.longitude,
            phone = request.phone,
            email = request.email,
            website = request.website,
            taxId = request.taxId,
            registrationNumber = request.registrationNumber,
            active = true,
            customAttributes = null
        )

        return businessApi.createBusiness(payload).map { it.toProfile() }
    }

    suspend fun fetchFromRemote(): Result<Business> {
        val workspaceId = workspaceContextManager.getCurrentWorkspaceId()
            ?: return Result.failure(IllegalStateException("Workspace not selected"))

        val result = businessApi.getBusiness()
        result.onSuccess { remote ->
            saveLocal(remote.copy(workspaceId = workspaceId), markSynced = true)
        }
        return result
    }

    suspend fun upsertBusiness(business: Business): Result<Business> {
        val workspaceId = workspaceContextManager.getCurrentWorkspaceId()
        val existing = businessDao.getBusiness()
        val ensuredBusiness = business.ensureId(existing)

        // Offline-first: persist immediately with synced=false
        saveLocal(ensuredBusiness.copy(workspaceId = workspaceId), markSynced = false)

        if (workspaceId.isNullOrEmpty()) {
            // No workspace context yet; rely on later sync
            return Result.success(ensuredBusiness)
        }

        val payload = ensuredBusiness.toPayload()

        val apiResult = if (existing == null || ensuredBusiness.id.startsWith(BusinessConstants.LOCAL_ID_PREFIX)) {
            businessApi.createBusiness(payload)
        } else {
            businessApi.updateBusiness(payload)
        }

        return apiResult.fold(
            onSuccess = { remote ->
                saveLocal(remote.copy(workspaceId = workspaceId), markSynced = true)
                Result.success(remote)
            },
            onFailure = {
                // Keep local unsynced entity; background sync will retry
                Result.success(ensuredBusiness)
            }
        )
    }

    suspend fun syncPending(): Result<Boolean> {
        val pending = businessDao.getPendingBusiness() ?: return Result.success(false)
        val workspaceId = workspaceContextManager.getCurrentWorkspaceId() ?: return Result.success(false)

        val domain = pending.toDomain().copy(workspaceId = workspaceId)
        val payload = domain.toPayload()
        val apiResult = if (pending.uid.startsWith(BusinessConstants.LOCAL_ID_PREFIX)) {
            businessApi.createBusiness(payload)
        } else {
            businessApi.updateBusiness(payload)
        }

        return apiResult.map { remote ->
            saveLocal(remote.copy(workspaceId = workspaceId), markSynced = true)
            true
        }
    }

    private fun Business.ensureId(existing: BusinessEntity?): Business {
        if (id.isNotBlank()) {
            return this
        }
        if (existing != null) {
            return copy(id = existing.uid)
        }
        val generatedId = UidGenerator.generateUid(BusinessConstants.UID_PREFIX)
        return copy(id = generatedId)
    }

    // ==================== Specific Section Methods ====================
    // Note: Backend uses unified endpoint - all sections are part of Business entity
    // These methods provide convenience wrappers for UI screens

    /**
     * Get business overview from remote.
     */
    suspend fun fetchBusinessOverview(): Result<BusinessOverview> {
        return businessApi.getBusinessOverview()
    }

    /**
     * Get business profile from remote.
     * Maps unified Business response to BusinessProfile DTO for UI.
     */
    suspend fun fetchBusinessProfile(): Result<BusinessProfile> {
        return businessApi.getBusiness().map { it.toProfile() }
    }

    /**
     * Update business profile.
     * Uses unified update endpoint with all business fields.
     */
    suspend fun updateBusinessProfile(request: BusinessProfileUpdateRequest): Result<BusinessProfile> {
        val payload = BusinessPayload(
            name = request.name,
            businessType = BusinessType.valueOf(request.businessType),
            description = request.description,
            ownerName = request.ownerName,
            addressLine1 = request.addressLine1,
            addressLine2 = request.addressLine2,
            city = request.city,
            state = request.state,
            postalCode = request.postalCode,
            country = request.country,
            latitude = request.latitude,
            longitude = request.longitude,
            phone = request.phone,
            email = request.email,
            website = request.website,
            taxId = request.taxId,
            registrationNumber = request.registrationNumber,
            active = request.active,
            customAttributes = request.customAttributes
        )

        return businessApi.updateBusiness(payload).map { it.toProfile() }
    }

    /**
     * Get business operations from remote.
     * Maps unified Business response to BusinessOperations DTO for UI.
     */
    suspend fun fetchBusinessOperations(): Result<BusinessOperations> {
        return businessApi.getBusiness().map { business ->
            BusinessOperations(
                uid = business.id,
                timezone = business.timezone,
                currency = business.currency,
                language = business.language,
                dateFormat = business.dateFormat,
                timeFormat = business.timeFormat,
                openingHours = business.openingHours,
                closingHours = business.closingHours,
                operatingDays = business.operatingDays
            )
        }
    }

    /**
     * Update business operations.
     * Uses unified update endpoint with all business fields.
     */
    suspend fun updateBusinessOperations(request: BusinessOperationsUpdateRequest): Result<BusinessOperations> {
        val currentResult = businessApi.getBusiness()
        if (currentResult.isFailure) {
            return Result.failure(currentResult.exceptionOrNull() ?: Exception("Failed to get current business"))
        }

        val current = currentResult.getOrThrow()

        val payload = BusinessPayload(
            name = current.name,
            businessType = current.businessType,
            description = current.description,
            ownerName = current.ownerName,
            addressLine1 = current.addressLine1,
            addressLine2 = current.addressLine2,
            city = current.city,
            state = current.state,
            postalCode = current.postalCode,
            country = current.country,
            latitude = current.latitude,
            longitude = current.longitude,
            phone = current.phone,
            email = current.email,
            website = current.website,
            taxId = current.taxId,
            registrationNumber = current.registrationNumber,
            timezone = request.timezone,
            currency = request.currency,
            language = request.language,
            dateFormat = request.dateFormat,
            timeFormat = request.timeFormat,
            openingHours = request.openingHours,
            closingHours = request.closingHours,
            operatingDays = request.operatingDays,
            active = current.active,
            customAttributes = current.customAttributes
        )

        return businessApi.updateBusiness(payload).map { business ->
            BusinessOperations(
                uid = business.id,
                timezone = business.timezone,
                currency = business.currency,
                language = business.language,
                dateFormat = business.dateFormat,
                timeFormat = business.timeFormat,
                openingHours = business.openingHours,
                closingHours = business.closingHours,
                operatingDays = business.operatingDays
            )
        }
    }

    /**
     * Get tax configuration from remote.
     * Maps unified Business response to TaxConfiguration DTO for UI.
     */
    suspend fun fetchTaxConfiguration(): Result<TaxConfiguration> {
        return businessApi.getBusiness().map { business ->
            TaxConfiguration(
                uid = business.id,
                taxId = business.taxId,
                registrationNumber = business.registrationNumber,
                taxSettings = business.taxSettings ?: emptyMap()
            )
        }
    }

    /**
     * Update tax configuration.
     * Uses unified update endpoint with all business fields.
     */
    suspend fun updateTaxConfiguration(request: TaxConfigurationUpdateRequest): Result<TaxConfiguration> {
        val currentResult = businessApi.getBusiness()
        if (currentResult.isFailure) {
            return Result.failure(currentResult.exceptionOrNull() ?: Exception("Failed to get current business"))
        }

        val current = currentResult.getOrThrow()

        val payload = BusinessPayload(
            name = current.name,
            businessType = current.businessType,
            description = current.description,
            ownerName = current.ownerName,
            addressLine1 = current.addressLine1,
            addressLine2 = current.addressLine2,
            city = current.city,
            state = current.state,
            postalCode = current.postalCode,
            country = current.country,
            latitude = current.latitude,
            longitude = current.longitude,
            phone = current.phone,
            email = current.email,
            website = current.website,
            taxId = request.taxId,
            registrationNumber = request.registrationNumber,
            taxSettings = request.taxSettings,
            timezone = current.timezone,
            currency = current.currency,
            language = current.language,
            dateFormat = current.dateFormat,
            timeFormat = current.timeFormat,
            openingHours = current.openingHours,
            closingHours = current.closingHours,
            operatingDays = current.operatingDays,
            active = current.active,
            customAttributes = current.customAttributes
        )

        return businessApi.updateBusiness(payload).map { business ->
            TaxConfiguration(
                uid = business.id,
                taxId = business.taxId,
                registrationNumber = business.registrationNumber,
                taxSettings = business.taxSettings ?: emptyMap()
            )
        }
    }
}

private fun Business.toProfile() = BusinessProfile(
    uid = id,
    seqId = seqId ?: "",
    name = name,
    businessType = businessType.name,
    description = description,
    ownerName = ownerName,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    city = city,
    state = state,
    postalCode = postalCode,
    country = country,
    latitude = latitude,
    longitude = longitude,
    phone = phone,
    email = email,
    website = website,
    taxId = taxId,
    registrationNumber = registrationNumber,
    active = active,
    customAttributes = customAttributes,
    createdAt = createdAt,
    updatedAt = updatedAt
)
