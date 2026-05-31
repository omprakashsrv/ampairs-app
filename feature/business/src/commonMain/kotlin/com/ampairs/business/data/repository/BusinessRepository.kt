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
            timezone = request.timezone,
            currency = request.currency,
            language = request.language,
            dateFormat = request.dateFormat,
            timeFormat = request.timeFormat,
            openingHours = request.openingHours,
            closingHours = request.closingHours,
            operatingDays = request.operatingDays,
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

    suspend fun fetchBusinessOverview(): Result<BusinessOverview> {
        val cached = getCachedBusiness()
        if (cached != null) return Result.success(cached.toOverview())
        return businessApi.getBusinessOverview()
    }

    suspend fun fetchBusinessProfile(): Result<BusinessProfile> {
        val cached = getCachedBusiness()
        if (cached != null) return Result.success(cached.toProfile())
        return fetchFromRemote().map { business -> business.toProfile() }
    }

    suspend fun updateBusinessProfile(request: BusinessProfileUpdateRequest): Result<BusinessProfile> {
        val base = getCachedBusiness()
            ?: fetchFromRemote().getOrNull()
            ?: return Result.failure(Exception("No business data available offline"))

        val updated = base.copy(
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

        return upsertBusiness(updated).map { business -> business.toProfile() }
    }

    suspend fun fetchBusinessOperations(): Result<BusinessOperations> {
        val cached = getCachedBusiness()
        if (cached != null) return Result.success(cached.toOperations())
        return fetchFromRemote().map { business -> business.toOperations() }
    }

    suspend fun updateBusinessOperations(request: BusinessOperationsUpdateRequest): Result<BusinessOperations> {
        val base = getCachedBusiness()
            ?: fetchFromRemote().getOrNull()
            ?: return Result.failure(Exception("No business data available offline"))

        val updated = base.copy(
            timezone = request.timezone,
            currency = request.currency,
            language = request.language,
            dateFormat = request.dateFormat,
            timeFormat = request.timeFormat,
            openingHours = request.openingHours,
            closingHours = request.closingHours,
            operatingDays = request.operatingDays
        )

        return upsertBusiness(updated).map { business -> business.toOperations() }
    }

    suspend fun fetchTaxConfiguration(): Result<TaxConfiguration> {
        val cached = getCachedBusiness()
        if (cached != null) return Result.success(cached.toTaxConfiguration())
        return fetchFromRemote().map { business -> business.toTaxConfiguration() }
    }

    suspend fun updateTaxConfiguration(request: TaxConfigurationUpdateRequest): Result<TaxConfiguration> {
        val base = getCachedBusiness()
            ?: fetchFromRemote().getOrNull()
            ?: return Result.failure(Exception("No business data available offline"))

        val updated = base.copy(
            taxId = request.taxId,
            registrationNumber = request.registrationNumber,
            taxSettings = request.taxSettings
        )

        return upsertBusiness(updated).map { business -> business.toTaxConfiguration() }
    }

    suspend fun syncFromRemote(): Result<Business> = fetchFromRemote()
}

private fun Business.toOperations() = BusinessOperations(
    uid = id,
    timezone = timezone,
    currency = currency,
    language = language,
    dateFormat = dateFormat,
    timeFormat = timeFormat,
    openingHours = openingHours,
    closingHours = closingHours,
    operatingDays = operatingDays
)

private fun Business.toTaxConfiguration() = TaxConfiguration(
    uid = id,
    taxId = taxId,
    registrationNumber = registrationNumber,
    taxSettings = taxSettings ?: emptyMap()
)

private fun Business.toOverview() = BusinessOverview(
    uid = id,
    seqId = seqId ?: "",
    name = name,
    businessType = businessType.name,
    currency = currency,
    timezone = timezone,
    email = email,
    phone = phone,
    address = listOfNotNull(addressLine1, city, state).joinToString(", "),
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt
)

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
