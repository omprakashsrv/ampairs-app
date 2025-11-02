package com.ampairs.workspace.store

import com.ampairs.common.time.currentTimeMillis
import com.ampairs.workspace.api.WorkspaceModuleApi
import com.ampairs.workspace.api.model.AvailableModule
import com.ampairs.workspace.api.model.InstalledModule
import com.ampairs.workspace.db.dao.WorkspaceModuleDao
import com.ampairs.workspace.db.entity.AvailableModuleEntity
import com.ampairs.workspace.db.entity.InstalledModuleEntity
import com.ampairs.workspace.db.entity.InstalledModuleWithMenuItems
import com.ampairs.workspace.db.entity.ModuleMenuItemEntity
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

/**
 * Store5 Factory for Module data management following existing patterns
 */

// Type aliases for clarity
typealias InstalledModuleStore = Store<InstalledModuleKey, List<InstalledModule>>
typealias AvailableModuleStore = Store<AvailableModuleKey, List<AvailableModule>>

/**
 * Keys for Store5 operations
 */
data class InstalledModuleKey(
    val workspaceId: String,
    val refresh: Boolean = false
) {
    companion object {
        fun all(workspaceId: String) = InstalledModuleKey(workspaceId = workspaceId, refresh = false)
        fun refresh(workspaceId: String) = InstalledModuleKey(workspaceId = workspaceId, refresh = true)
    }
}

data class AvailableModuleKey(
    val category: String? = null,
    val featured: Boolean = false
) {
    companion object {
        fun all() = AvailableModuleKey()
        fun featured() = AvailableModuleKey(featured = true)
        fun category(category: String) = AvailableModuleKey(category = category)
    }
}

class WorkspaceModuleStoreFactory(
    private val moduleApi: WorkspaceModuleApi,
    private val moduleDao: WorkspaceModuleDao,
) {

    fun createInstalledModuleStore(): InstalledModuleStore {
        return StoreBuilder
            .from(
                fetcher = createInstalledModuleFetcher(),
                sourceOfTruth = createInstalledModuleSourceOfTruth()
            )
            .build()
    }

    fun createAvailableModuleStore(): AvailableModuleStore {
        return StoreBuilder
            .from(
                fetcher = createAvailableModuleFetcher(),
                sourceOfTruth = createAvailableModuleSourceOfTruth()
            )
            .build()
    }

    private fun createInstalledModuleFetcher(): Fetcher<InstalledModuleKey, List<InstalledModule>> {
        return Fetcher.of { key ->
            val result = moduleApi.getInstalledModules(key.workspaceId)
            result.getOrThrow()
        }
    }

    private fun createInstalledModuleSourceOfTruth(): SourceOfTruth<InstalledModuleKey, List<InstalledModule>, List<InstalledModule>> {
        return SourceOfTruth.of(
            reader = { key ->
                moduleDao.getInstalledModulesWithMenuItemsFlow(key.workspaceId).map { entitiesWithMenuItems ->
                    entitiesWithMenuItems.map { entityWithMenuItems ->
                        // Convert to InstalledModule using the repository extension function
                        entityWithMenuItems.toInstalledModule()
                    }
                }
            },
            writer = { key, modules ->
                val entities = modules.map { it.toEntity(key.workspaceId) }
                moduleDao.insertInstalledModules(entities)

                // Insert menu items for each module
                modules.forEach { module ->
                    // Delete old menu items first
                    moduleDao.deleteMenuItemsByModuleId(module.id)

                    // Insert new menu items
                    val menuItems = module.routeInfo.menuItems.map { menuItem ->
                        ModuleMenuItemEntity(
                            id = "${module.id}_${menuItem.id}",
                            moduleId = module.id,
                            label = menuItem.label,
                            routePath = menuItem.routePath,
                            icon = menuItem.icon,
                            order = menuItem.order,
                            isDefault = menuItem.isDefault
                        )
                    }
                    if (menuItems.isNotEmpty()) {
                        moduleDao.insertMenuItems(menuItems)
                    }
                }

                // Mark all as synced
                entities.forEach { entity ->
                    moduleDao.updateInstalledModuleSyncState(
                        entity.id,
                        entity.workspaceId,
                        "SYNCED",
                        currentTimeMillis()
                    )
                }
            }
        )
    }

    private fun createAvailableModuleFetcher(): Fetcher<AvailableModuleKey, List<AvailableModule>> {
        return Fetcher.of { key ->
            val result = moduleApi.getAvailableModules(
                category = key.category,
                featured = key.featured
            )
            result.getOrThrow()
        }
    }

    private fun createAvailableModuleSourceOfTruth(): SourceOfTruth<AvailableModuleKey, List<AvailableModule>, List<AvailableModule>> {
        return SourceOfTruth.of(
            reader = { key ->
                kotlinx.coroutines.flow.flow {
                    val entities = when {
                        key.featured -> moduleDao.getFeaturedModules()
                        key.category != null -> moduleDao.getAvailableModulesByCategory(key.category!!)
                        else -> moduleDao.getAvailableModules()
                    }
                    emit(entities.map { it.toApiModel() })
                }
            },
            writer = { key, modules ->
                val entities = modules.map { it.toEntity() }
                moduleDao.insertAvailableModules(entities)
                
                // Mark all as synced
                entities.forEach { entity ->
                    moduleDao.updateAvailableModuleSyncState(
                        entity.moduleCode, 
                        "SYNCED", 
                        currentTimeMillis()
                    )
                }
            }
        )
    }
}

// Extension functions to convert between API models and entities
private fun InstalledModule.toEntity(workspaceId: String): InstalledModuleEntity {
    return InstalledModuleEntity(
        id = id,
        workspaceId = workspaceId, // Use the workspaceId from the Store key context
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
        navigationIndex = navigationIndex,
        routeBasePath = routeInfo.basePath,
        routeDisplayName = routeInfo.displayName,
        routeIconName = routeInfo.iconName,
        sync_state = "SYNCED",
        created_at = currentTimeMillis(),
        updated_at = currentTimeMillis(),
        last_synced_at = currentTimeMillis()
    )
}

private fun InstalledModuleEntity.toApiModel(): InstalledModule {
    return InstalledModule(
        id = id,
        workspaceId = workspaceId, // Use the workspaceId from the entity
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
            menuItems = emptyList() // Menu items will be loaded separately by relations
        ),
        navigationIndex = navigationIndex
    )
}

// Extension function for complete entity with menu items
private fun InstalledModuleWithMenuItems.toInstalledModule(): InstalledModule {
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
            menuItems = menuItems.sortedBy { it.order }.map { menuItem ->
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

private fun AvailableModule.toEntity(): AvailableModuleEntity {
    return AvailableModuleEntity(
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
        sizeMb = sizeMb,
        sync_state = "SYNCED",
        created_at = currentTimeMillis(),
        updated_at = currentTimeMillis(),
        last_synced_at = currentTimeMillis()
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