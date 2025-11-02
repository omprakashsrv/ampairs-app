package com.ampairs.workspace.navigation

import com.ampairs.workspace.api.model.InstalledModule
import com.ampairs.workspace.api.model.ModuleMenuItem
import com.ampairs.workspace.api.model.ModuleRouteInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * Dynamic module navigation service for KMP applications
 * Manages platform-specific navigation patterns:
 * - Desktop: Menu bar integration
 * - Mobile: Side navigation drawer
 */
class DynamicModuleNavigationService {

    // Service scope for state management
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    // Internal state
    private val _installedModules = MutableStateFlow<List<InstalledModule>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    // Public state
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()

    // Navigation routes computed from installed modules, filtered by availability
    val navigationRoutes: StateFlow<List<DynamicModuleRoute>> = _installedModules
        .map { modules ->
            modules
                .filter { it.status == "ACTIVE" && it.enabled }
                .map { module -> mapToNavigationRoute(module) }
                .filter { route -> isModuleImplementationAvailable(route.moduleCode) }
                .sortedBy { it.navigationIndex }
        }
        .stateIn(serviceScope, SharingStarted.Eagerly, emptyList())

    // Unavailable modules (installed but not implemented locally)
    val unavailableModules: StateFlow<List<DynamicModuleRoute>> = _installedModules
        .map { modules ->
            modules
                .filter { it.status == "ACTIVE" && it.enabled }
                .map { module -> mapToNavigationRoute(module) }
                .filter { route -> !isModuleImplementationAvailable(route.moduleCode) }
                .sortedBy { it.navigationIndex }
        }
        .stateIn(serviceScope, SharingStarted.Eagerly, emptyList())

    /**
     * Update the installed modules list (called from WorkspaceModulesViewModel)
     */
    fun updateInstalledModules(modules: List<InstalledModule>) {
        _installedModules.value = modules
        _error.value = null
    }

    /**
     * Set loading state
     */
    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    /**
     * Set error state
     */
    fun setError(error: String?) {
        _error.value = error
    }

    /**
     * Reset the service state when switching workspaces or logging out
     * This clears the installed modules to prevent stale navigation data
     */
    fun reset() {
        _installedModules.value = emptyList()
        _isLoading.value = false
        _error.value = null
    }

    /**
     * Get navigation route by module code
     */
    fun getModuleRoute(moduleCode: String): DynamicModuleRoute? {
        return navigationRoutes.value.find { it.moduleCode == moduleCode }
    }

    /**
     * Get full route path for a menu item
     */
    fun getFullRoutePath(moduleCode: String, menuItem: ModuleMenuItem): String {
        return "/workspace/modules/${moduleCode}${menuItem.routePath}"
    }

    /**
     * Navigate to a specific module (default menu item)
     */
    fun getDefaultModuleRoute(moduleCode: String): String? {
        val module = getModuleRoute(moduleCode)
        val defaultMenuItem = module?.menuItems?.find { it.isDefault }
            ?: module?.menuItems?.firstOrNull()

        return defaultMenuItem?.let { getFullRoutePath(moduleCode, it) }
    }

    /**
     * Check if a module has a local implementation available
     */
    private fun isModuleImplementationAvailable(moduleCode: String): Boolean {
        return try {
            // Check if the module has a registered navigation provider
            when (moduleCode) {
                "business-profile", "customer-management", "product-management",
                "order-management", "invoice-management", "tax-code-management" -> true
                "inventory-management" -> false // Not implemented yet
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Map installed module to navigation route
     */
    private fun mapToNavigationRoute(module: InstalledModule): DynamicModuleRoute {
        return DynamicModuleRoute(
            moduleCode = module.moduleCode,
            displayName = module.routeInfo.displayName,
            icon = module.routeInfo.iconName,
            primaryColor = module.primaryColor,
            basePath = module.routeInfo.basePath,
            menuItems = module.routeInfo.menuItems.sortedBy { it.order },
            navigationIndex = module.navigationIndex,
            enabled = module.enabled,
            status = module.status
        )
    }
}

/**
 * Navigation route data for dynamic module routing
 */
data class DynamicModuleRoute(
    val moduleCode: String,
    val displayName: String,
    val icon: String,
    val primaryColor: String,
    val basePath: String,
    val menuItems: List<ModuleMenuItem>,
    val navigationIndex: Int,
    val enabled: Boolean,
    val status: String
)

/**
 * Platform-specific navigation pattern
 */
enum class NavigationPattern {
    MENU_BAR,     // Desktop: Top menu bar
    SIDE_DRAWER   // Mobile: Side navigation drawer
}

/**
 * Platform detection for navigation patterns
 */
expect object PlatformNavigationDetector {
    fun getNavigationPattern(): NavigationPattern

    /**
     * Returns true if the platform requires a back button in the UI
     * (e.g., iOS has no hardware back button)
     * Returns false if the platform has hardware/gesture navigation
     * (e.g., Android has hardware back button or gesture navigation)
     */
    fun requiresBackButton(): Boolean
}