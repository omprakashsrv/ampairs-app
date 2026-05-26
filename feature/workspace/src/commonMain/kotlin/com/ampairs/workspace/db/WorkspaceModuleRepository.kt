package com.ampairs.workspace.db

import com.ampairs.common.di.AppScope
import com.ampairs.workspace.api.WorkspaceModuleApi
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import com.ampairs.workspace.api.model.InstalledModule
import com.ampairs.workspace.api.model.AvailableModule
import com.ampairs.workspace.api.model.ModuleInstallationResponse
import com.ampairs.workspace.api.model.ModuleUninstallationResponse
import com.ampairs.workspace.api.model.ModuleDetailResponse
import com.ampairs.workspace.db.dao.WorkspaceModuleDao
import com.ampairs.workspace.db.entity.AvailableModuleEntity
import com.ampairs.workspace.db.entity.InstalledModuleEntity
import com.ampairs.workspace.db.entity.InstalledModuleWithMenuItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject
@SingleIn(AppScope::class)
class WorkspaceModuleRepository(
    private val moduleApi: WorkspaceModuleApi,
    private val moduleDao: WorkspaceModuleDao
) {

    fun getInstalledModulesFlow(workspaceId: String): Flow<List<InstalledModule>> =
        moduleDao.getInstalledModulesWithMenuItemsFlow(workspaceId)
            .map { items -> items.map { it.toApiModel() } }

    suspend fun syncInstalledModules(workspaceId: String) {
        try {
            val result = moduleApi.getInstalledModules(workspaceId)
            result.onSuccess { modules ->
                val entities = modules.map { it.toInstalledModuleEntity(workspaceId) }
                moduleDao.deleteAllInstalledModules(workspaceId)
                moduleDao.insertInstalledModules(entities)
            }
        } catch (_: Exception) {
            // Graceful failure — UI continues with cached data
        }
    }

    suspend fun getAvailableModules(
        category: String? = null,
        featured: Boolean = false,
        refresh: Boolean = false
    ): List<AvailableModule> {
        if (!refresh) {
            val cached = moduleDao.getAvailableModules()
            if (cached.isNotEmpty()) {
                val filtered = when {
                    featured -> cached.filter { it.featured }.map { it.toApiModel() }
                    category != null -> cached.filter { it.category == category }.map { it.toApiModel() }
                    else -> cached.map { it.toApiModel() }
                }
                if (filtered.isNotEmpty()) return filtered
            }
        }
        return try {
            val result = moduleApi.getAvailableModules(category, featured)
            if (result.isSuccess) {
                val modules = result.getOrThrow()
                val entities = modules.map { it.toAvailableModuleEntity() }
                moduleDao.deleteAllAvailableModules()
                moduleDao.insertAvailableModules(entities)
                modules
            } else {
                moduleDao.getAvailableModules().map { it.toApiModel() }
            }
        } catch (_: Exception) {
            moduleDao.getAvailableModules().map { it.toApiModel() }
        }
    }

    suspend fun installModule(workspaceId: String, moduleCode: String): Result<ModuleInstallationResponse> {
        return try {
            moduleApi.installModule(workspaceId, moduleCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uninstallModule(workspaceId: String, moduleId: String): Result<ModuleUninstallationResponse> {
        return try {
            val result = moduleApi.uninstallModule(workspaceId, moduleId)
            if (result.isSuccess) {
                moduleDao.deleteInstalledModuleById(moduleId, workspaceId)
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isModuleInstalled(workspaceId: String, moduleCode: String): Boolean =
        moduleDao.isModuleInstalled(workspaceId, moduleCode)

    suspend fun getModuleByCode(workspaceId: String, moduleCode: String): InstalledModule? =
        moduleDao.getInstalledModulesWithMenuItems(workspaceId)
            .find { it.module.moduleCode == moduleCode }
            ?.toApiModel()

    suspend fun getActiveModules(workspaceId: String): List<InstalledModule> =
        moduleDao.getInstalledModulesWithMenuItems(workspaceId)
            .filter { it.module.status == "ACTIVE" && it.module.enabled }
            .map { it.toApiModel() }

    suspend fun getInactiveModules(workspaceId: String): List<InstalledModule> =
        moduleDao.getInstalledModulesWithMenuItems(workspaceId)
            .filter { it.module.status != "ACTIVE" || !it.module.enabled }
            .map { it.toApiModel() }

    suspend fun getModuleDetails(workspaceId: String, moduleId: String): Result<ModuleDetailResponse> {
        return try {
            moduleApi.getModuleDetails(workspaceId, moduleId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearCache(workspaceId: String) {
        moduleDao.deleteAllInstalledModules(workspaceId)
        moduleDao.deleteAllAvailableModules()
    }
}

private fun InstalledModuleWithMenuItems.toApiModel(): InstalledModule = InstalledModule(
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

private fun InstalledModuleEntity.toApiModel(): InstalledModule = InstalledModule(
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
        menuItems = emptyList()
    ),
    navigationIndex = navigationIndex
)

private fun AvailableModuleEntity.toApiModel(): AvailableModule = AvailableModule(
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

private fun InstalledModule.toInstalledModuleEntity(wsId: String): InstalledModuleEntity = InstalledModuleEntity(
    id = id,
    workspaceId = wsId,
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
    routeBasePath = routeInfo?.basePath ?: "",
    routeDisplayName = routeInfo?.displayName ?: "",
    routeIconName = routeInfo?.iconName ?: "",
    navigationIndex = navigationIndex
)

private fun AvailableModule.toAvailableModuleEntity(): AvailableModuleEntity = AvailableModuleEntity(
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
