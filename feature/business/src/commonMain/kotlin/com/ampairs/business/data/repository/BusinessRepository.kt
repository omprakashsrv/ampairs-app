package com.ampairs.business.data.repository

import com.ampairs.business.data.api.BusinessApi
import com.ampairs.business.data.db.BusinessDao
import com.ampairs.business.data.db.BusinessEntity
import com.ampairs.business.data.db.toDomain
import com.ampairs.business.data.db.toEntity
import com.ampairs.business.domain.Business
import com.ampairs.business.domain.toPayload
import com.ampairs.business.util.BusinessConstants
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.workspace.context.WorkspaceContextManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class BusinessRepository(
    private val businessDao: BusinessDao,
    private val businessApi: BusinessApi,
    private val workspaceContextManager: WorkspaceContextManager
) {

    fun observeBusiness(): Flow<Business?> = businessDao.observeBusiness().map { it?.toDomain() }

    suspend fun getCachedBusiness(): Business? = businessDao.getBusiness()?.toDomain()

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

    suspend fun createBusinessProfile(request: com.ampairs.business.domain.BusinessCreateRequest): Result<com.ampairs.business.domain.BusinessProfile> {
        val workspaceId = workspaceContextManager.getCurrentWorkspaceId()
            ?: return Result.failure(IllegalStateException("Workspace not selected"))

        // Convert BusinessCreateRequest to BusinessPayload for unified endpoint
        val payload = com.ampairs.business.domain.BusinessPayload(
            name = request.name,
            businessType = com.ampairs.business.domain.BusinessType.valueOf(request.businessType),
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

        return businessApi.createBusiness(payload).map { business ->
            com.ampairs.business.domain.BusinessProfile(
                uid = business.id,
                seqId = business.seqId ?: "",
                name = business.name,
                businessType = business.businessType.name,
                description = business.description,
                ownerName = business.ownerName,
                addressLine1 = business.addressLine1,
                addressLine2 = business.addressLine2,
                city = business.city,
                state = business.state,
                postalCode = business.postalCode,
                country = business.country,
                latitude = business.latitude,
                longitude = business.longitude,
                phone = business.phone,
                email = business.email,
                website = business.website,
                taxId = business.taxId,
                registrationNumber = business.registrationNumber,
                active = business.active,
                customAttributes = business.customAttributes,
                createdAt = business.createdAt,
                updatedAt = business.updatedAt
            )
        }
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
    suspend fun fetchBusinessOverview(): Result<com.ampairs.business.domain.BusinessOverview> {
        return businessApi.getBusinessOverview()
    }

    /**
     * Get business profile from remote.
     * Maps unified Business response to BusinessProfile DTO for UI.
     */
    suspend fun fetchBusinessProfile(): Result<com.ampairs.business.domain.BusinessProfile> {
        return businessApi.getBusiness().map { business ->
            com.ampairs.business.domain.BusinessProfile(
                uid = business.id,
                seqId = business.seqId ?: "",
                name = business.name,
                businessType = business.businessType.name,
                description = business.description,
                ownerName = business.ownerName,
                addressLine1 = business.addressLine1,
                addressLine2 = business.addressLine2,
                city = business.city,
                state = business.state,
                postalCode = business.postalCode,
                country = business.country,
                latitude = business.latitude,
                longitude = business.longitude,
                phone = business.phone,
                email = business.email,
                website = business.website,
                taxId = business.taxId,
                registrationNumber = business.registrationNumber,
                active = business.active,
                customAttributes = business.customAttributes,
                createdAt = business.createdAt,
                updatedAt = business.updatedAt
            )
        }
    }

    /**
     * Update business profile.
     * Uses unified update endpoint with all business fields.
     */
    suspend fun updateBusinessProfile(
        request: com.ampairs.business.domain.BusinessProfileUpdateRequest
    ): Result<com.ampairs.business.domain.BusinessProfile> {
        // Convert ProfileUpdateRequest to full BusinessPayload
        val payload = com.ampairs.business.domain.BusinessPayload(
            name = request.name,
            businessType = com.ampairs.business.domain.BusinessType.valueOf(request.businessType),
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

        return businessApi.updateBusiness(payload).map { business ->
            com.ampairs.business.domain.BusinessProfile(
                uid = business.id,
                seqId = business.seqId ?: "",
                name = business.name,
                businessType = business.businessType.name,
                description = business.description,
                ownerName = business.ownerName,
                addressLine1 = business.addressLine1,
                addressLine2 = business.addressLine2,
                city = business.city,
                state = business.state,
                postalCode = business.postalCode,
                country = business.country,
                latitude = business.latitude,
                longitude = business.longitude,
                phone = business.phone,
                email = business.email,
                website = business.website,
                taxId = business.taxId,
                registrationNumber = business.registrationNumber,
                active = business.active,
                customAttributes = business.customAttributes,
                createdAt = business.createdAt,
                updatedAt = business.updatedAt
            )
        }
    }

    /**
     * Get business operations from remote.
     * Maps unified Business response to BusinessOperations DTO for UI.
     */
    suspend fun fetchBusinessOperations(): Result<com.ampairs.business.domain.BusinessOperations> {
        return businessApi.getBusiness().map { business ->
            com.ampairs.business.domain.BusinessOperations(
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
    suspend fun updateBusinessOperations(
        request: com.ampairs.business.domain.BusinessOperationsUpdateRequest
    ): Result<com.ampairs.business.domain.BusinessOperations> {
        // First get current business to preserve other fields
        val currentResult = businessApi.getBusiness()
        if (currentResult.isFailure) {
            return Result.failure(currentResult.exceptionOrNull() ?: Exception("Failed to get current business"))
        }

        val current = currentResult.getOrThrow()

        // Create payload with updated operations fields
        val payload = com.ampairs.business.domain.BusinessPayload(
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
            com.ampairs.business.domain.BusinessOperations(
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
    suspend fun fetchTaxConfiguration(): Result<com.ampairs.business.domain.TaxConfiguration> {
        return businessApi.getBusiness().map { business ->
            com.ampairs.business.domain.TaxConfiguration(
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
    suspend fun updateTaxConfiguration(
        request: com.ampairs.business.domain.TaxConfigurationUpdateRequest
    ): Result<com.ampairs.business.domain.TaxConfiguration> {
        // First get current business to preserve other fields
        val currentResult = businessApi.getBusiness()
        if (currentResult.isFailure) {
            return Result.failure(currentResult.exceptionOrNull() ?: Exception("Failed to get current business"))
        }

        val current = currentResult.getOrThrow()

        // Create payload with updated tax fields
        val payload = com.ampairs.business.domain.BusinessPayload(
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
            com.ampairs.business.domain.TaxConfiguration(
                uid = business.id,
                taxId = business.taxId,
                registrationNumber = business.registrationNumber,
                taxSettings = business.taxSettings ?: emptyMap()
            )
        }
    }
}
