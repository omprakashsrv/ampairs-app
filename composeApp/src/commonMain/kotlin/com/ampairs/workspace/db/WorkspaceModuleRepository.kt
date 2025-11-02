package com.ampairs.workspace.db

import com.ampairs.workspace.api.WorkspaceModuleApi
import com.ampairs.workspace.api.model.InstalledModule
import com.ampairs.workspace.api.model.AvailableModule
import com.ampairs.workspace.api.model.ModuleInstallationResponse
import com.ampairs.workspace.api.model.ModuleUninstallationResponse
import com.ampairs.workspace.api.model.ModuleDetailResponse
import com.ampairs.workspace.db.dao.WorkspaceModuleDao
import com.ampairs.workspace.db.entity.AvailableModuleEntity
import com.ampairs.workspace.db.entity.InstalledModuleEntity
import com.ampairs.workspace.db.entity.InstalledModuleWithMenuItems
import com.ampairs.workspace.store.WorkspaceModuleStoreFactory
import com.ampairs.workspace.store.InstalledModuleKey
import com.ampairs.workspace.store.AvailableModuleKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * Offline-first repository for workspace modules
 * Follows the existing Store5 patterns used by other workspace repositories
 */
class WorkspaceModuleRepository(
    private val moduleApi: WorkspaceModuleApi,
    private val moduleDao: WorkspaceModuleDao,
    storeFactory: WorkspaceModuleStoreFactory
) {

    private val installedModuleStore = storeFactory.createInstalledModuleStore()
    private val availableModuleStore = storeFactory.createAvailableModuleStore()

    // Expose store for direct access like StateStore
    val moduleStore get() = installedModuleStore

    /**
     * Get installed modules as Flow for reactive UI updates
     * Matches web: this.installedModules = signal<InstalledModule[]>([])
     */
    fun getInstalledModulesFlow(
        workspaceId: String,
        refresh: Boolean = false
    ): Flow<List<InstalledModule>> {
        // Use different keys for cache vs fresh requests to ensure Store5 triggers API calls
        val key = if (refresh) {
            InstalledModuleKey.refresh(workspaceId) // Key with refresh=true
        } else {
            InstalledModuleKey.all(workspaceId) // Key with refresh=false (default)
        }

        val request = if (refresh) {
            StoreReadRequest.fresh(key) // Force API call
        } else {
            StoreReadRequest.cached(key, refresh = false) // Use cache first
        }

        return installedModuleStore.stream(request)
            .map { response ->
                when (response) {
                    is StoreReadResponse.Data -> response.value
                    is StoreReadResponse.Loading -> response.dataOrNull() ?: emptyList()
                    is StoreReadResponse.Error -> response.dataOrNull() ?: emptyList()
                    is StoreReadResponse.NoNewData -> response.dataOrNull() ?: emptyList()
                    is StoreReadResponse.Initial -> emptyList()
                }
            }
    }

    /**
     * Clear installed modules cache to force fresh API call
     */
    @OptIn(ExperimentalStoreApi::class)
    suspend fun clearInstalledModulesCache() {
        installedModuleStore.clear()
    }

    /**
     * Get available modules
     * Matches web: async getAvailableModules(category?: string, featured = false): Promise<AvailableModule[]>
     */
    suspend fun getAvailableModules(
        category: String? = null,
        featured: Boolean = false,
        refresh: Boolean = false
    ): List<AvailableModule> {
        println("WorkspaceModuleRepository: getAvailableModules - category=$category, featured=$featured, refresh=$refresh")
        val key = when {
            featured -> AvailableModuleKey.featured()
            category != null -> AvailableModuleKey.category(category)
            else -> AvailableModuleKey.all()
        }
        println("WorkspaceModuleRepository: Using key: $key")

        val request = if (refresh) {
            println("WorkspaceModuleRepository: Creating FRESH request")
            StoreReadRequest.fresh(key)
        } else {
            println("WorkspaceModuleRepository: Creating CACHED request")
            StoreReadRequest.cached(key, refresh = false)
        }

        // Wait for first Data or Error response from Store5 (skip Loading/Initial states)
        return availableModuleStore.stream(request)
            .map { response ->
                println("WorkspaceModuleRepository: Store response type: ${response::class.simpleName}")
                when (response) {
                    is StoreReadResponse.Data -> {
                        println("WorkspaceModuleRepository: Data received with ${response.value.size} modules")
                        response.value
                    }
                    is StoreReadResponse.Loading -> {
                        println("WorkspaceModuleRepository: Loading... continuing to wait")
                        null // Return null to skip this emission
                    }
                    is StoreReadResponse.Error -> {
                        val data: List<AvailableModule>? = response.dataOrNull()
                        println("WorkspaceModuleRepository: Error response - returning ${data?.size ?: 0} modules")
                        data ?: emptyList() // Return empty on error
                    }
                    is StoreReadResponse.NoNewData -> {
                        val data: List<AvailableModule>? = response.dataOrNull()
                        println("WorkspaceModuleRepository: NoNewData - returning ${data?.size ?: 0} modules")
                        data ?: emptyList()
                    }
                    is StoreReadResponse.Initial -> {
                        println("WorkspaceModuleRepository: Initial... continuing to wait")
                        null // Return null to skip this emission
                    }
                }
            }
            .filterNotNull() // Skip null values (Loading and Initial states)
            .first() // Get first non-null response (Data, Error, or NoNewData)
    }

    /**
     * Install a module
     * Matches web: async installModule(moduleCode: string): Promise<ModuleInstallationResponse>
     */
    suspend fun installModule(
        workspaceId: String,
        moduleCode: String
    ): Result<ModuleInstallationResponse> {
        return try {
            val result = moduleApi.installModule(workspaceId, moduleCode)

            if (result.isSuccess) {
                // Store5 will automatically refresh via SourceOfTruth
                // ViewModel's flow will pick up the changes automatically
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uninstall a module
     * Matches web: async uninstallModule(moduleId: string): Promise<ModuleUninstallationResponse>
     */
    suspend fun uninstallModule(
        workspaceId: String,
        moduleId: String
    ): Result<ModuleUninstallationResponse> {
        return try {
            val result = moduleApi.uninstallModule(workspaceId, moduleId)

            if (result.isSuccess) {
                // Remove from local database - Store5 will handle sync automatically
                moduleDao.deleteInstalledModuleById(moduleId, workspaceId)
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if module is installed (matches web service method)
     * Matches web: isModuleInstalled(moduleCode: string): boolean
     */
    suspend fun isModuleInstalled(workspaceId: String, moduleCode: String): Boolean {
        return moduleDao.isModuleInstalled(workspaceId, moduleCode)
    }

    /**
     * Get module by code (matches web service method)
     */
    suspend fun getModuleByCode(workspaceId: String, moduleCode: String): InstalledModule? {
        return moduleDao.getInstalledModulesWithMenuItems(workspaceId)
            .find { it.module.moduleCode == moduleCode }
            ?.toApiModel()
    }

    /**
     * Get active modules (matches web computed property)
     * Matches web: get activeModules() { return this.installedModules().filter(m => m.status === 'ACTIVE' && m.enabled); }
     */
    suspend fun getActiveModules(workspaceId: String): List<InstalledModule> {
        return moduleDao.getInstalledModulesWithMenuItems(workspaceId)
            .filter { it.module.status == "ACTIVE" && it.module.enabled }
            .map { it.toApiModel() }
    }

    /**
     * Get inactive modules (matches web computed property)
     * Matches web: get inactiveModules() { return this.installedModules().filter(m => m.status !== 'ACTIVE' || !m.enabled); }
     */
    suspend fun getInactiveModules(workspaceId: String): List<InstalledModule> {
        return moduleDao.getInstalledModulesWithMenuItems(workspaceId)
            .filter { it.module.status != "ACTIVE" || !it.module.enabled }
            .map { it.toApiModel() }
    }

    /**
     * Get detailed information about a specific module
     * Matches backend: GET /workspace/v1/modules/{moduleId}
     * Note: Module details are not cached as they contain real-time analytics
     */
    suspend fun getModuleDetails(
        workspaceId: String,
        moduleId: String
    ): Result<ModuleDetailResponse> {
        return try {
            moduleApi.getModuleDetails(workspaceId, moduleId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clear all caches and refresh from server
     */
    @OptIn(ExperimentalStoreApi::class)
    suspend fun refresh() {
        installedModuleStore.clear()
        availableModuleStore.clear()
    }

    /**
     * Clear all cached data
     */
    @OptIn(ExperimentalStoreApi::class)
    suspend fun clearCache(workspaceId: String) {
        moduleDao.deleteAllInstalledModules(workspaceId)
        moduleDao.deleteAllAvailableModules()
        installedModuleStore.clear()
        availableModuleStore.clear()
    }

}

// Extension functions for entity-API model conversion
private fun InstalledModuleEntity.toApiModel(): InstalledModule {
    return InstalledModule(
        id = id,
        workspaceId = workspaceId,
        moduleCode = moduleCode,
        name = name,
        category = category,
        version = version,
        status = status,
        enabled = enabled,
        installedAt = installedAt,
        icon = icon,
        primaryColor = primaryColor,
        healthScore = healthScore,
        needsAttention = needsAttention,
        description = description,
        routeInfo = com.ampairs.workspace.api.model.ModuleRouteInfo(
            basePath = routeBasePath,
            displayName = routeDisplayName,
            iconName = routeIconName,
            menuItems = emptyList() // Will be populated by DAO with relations
        ),
        navigationIndex = navigationIndex
    )
}

// Extension function for complete entity with menu items
private fun InstalledModuleWithMenuItems.toApiModel(): InstalledModule {
    return InstalledModule(
        id = module.id,
        workspaceId = module.workspaceId,
        moduleCode = module.moduleCode,
        name = module.name,
        category = module.category,
        version = module.version,
        status = module.status,
        enabled = module.enabled,
        installedAt = module.installedAt,
        icon = module.icon,
        primaryColor = module.primaryColor,
        healthScore = module.healthScore,
        needsAttention = module.needsAttention,
        description = module.description,
        routeInfo = com.ampairs.workspace.api.model.ModuleRouteInfo(
            basePath = module.routeBasePath,
            displayName = module.routeDisplayName,
            iconName = module.routeIconName,
            menuItems = menuItems.map { menuItem ->
                com.ampairs.workspace.api.model.ModuleMenuItem(
                    id = menuItem.id,
                    label = menuItem.label,
                    routePath = menuItem.routePath,
                    icon = menuItem.icon,
                    order = menuItem.order,
                    isDefault = menuItem.isDefault
                )
            }
        ),
        navigationIndex = module.navigationIndex
    )
}

private fun AvailableModuleEntity.toApiModel(): AvailableModule {
    return AvailableModule(
        moduleCode = moduleCode,
        name = name,
        description = description,
        category = category,
        version = version,
        rating = rating,
        installCount = installCount,
        complexity = complexity,
        icon = icon,
        primaryColor = primaryColor,
        featured = featured,
        requiredTier = requiredTier,
        sizeMb = sizeMb
    )
}