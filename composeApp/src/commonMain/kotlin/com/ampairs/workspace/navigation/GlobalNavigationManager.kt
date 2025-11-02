package com.ampairs.workspace.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.ampairs.workspace.context.WorkspaceContextManager
import com.ampairs.workspace.db.WorkspaceModuleRepository
import com.ampairs.workspace.store.InstalledModuleKey
import com.ampairs.common.concurrency.Volatile
import com.ampairs.common.concurrency.synchronized
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * Global navigation manager that observes workspace context changes
 * and manages the navigation service lifecycle automatically.
 *
 * This provides a reactive way to:
 * - Create navigation service when workspace is selected
 * - Show/hide hamburger menu based on workspace and module loading state
 * - Provide navigation service to all screens without prop drilling
 */
class GlobalNavigationManager private constructor() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val workspaceManager = WorkspaceContextManager.getInstance()

    // Navigation service state
    private val _navigationService = MutableStateFlow<DynamicModuleNavigationService?>(null)
    val navigationService: StateFlow<DynamicModuleNavigationService?> = _navigationService.asStateFlow()

    // Module loading state
    private val _isLoadingModules = MutableStateFlow(false)
    val isLoadingModules: StateFlow<Boolean> = _isLoadingModules.asStateFlow()

    // Combined state to determine when navigation should be available
    // Don't depend on loading state to prevent drawer disappearing during module loads
    val isNavigationAvailable: StateFlow<Boolean> = combine(
        workspaceManager.isWorkspaceSelected,
        navigationService
    ) { workspaceSelected, service ->
        workspaceSelected && service != null
    }.stateIn(scope, SharingStarted.Eagerly, false)

    // State to determine when hamburger menu should be visible
    val shouldShowHamburgerMenu: StateFlow<Boolean> = combine(
        isNavigationAvailable,
        // Only show on mobile platforms
        navigationService
    ) { navigationAvailable, service ->
        navigationAvailable &&
        service != null &&
        PlatformNavigationDetector.getNavigationPattern() == NavigationPattern.SIDE_DRAWER
    }.stateIn(scope, SharingStarted.Eagerly, false)

    companion object {
        @Volatile
        private var INSTANCE: GlobalNavigationManager? = null

        fun getInstance(): GlobalNavigationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GlobalNavigationManager().also { INSTANCE = it }
            }
        }
    }

    /**
     * Called when workspace is selected - creates navigation service and resets state
     */
    fun onWorkspaceSelected() {
        // Reset existing service or create new one
        val currentService = _navigationService.value
        if (currentService != null) {
            // Reset state when switching workspaces
            currentService.reset()
        } else {
            // Create new service for first workspace selection
            _navigationService.value = DynamicModuleNavigationService()
        }
    }

    /**
     * Called when workspace is cleared - destroys navigation service
     */
    fun onWorkspaceCleared() {
        // Reset the service before clearing
        _navigationService.value?.reset()
        _navigationService.value = null
        _isLoadingModules.value = false
    }

    /**
     * Called when modules start loading
     */
    fun setModuleLoading(loading: Boolean) {
        _isLoadingModules.value = loading
    }

    /**
     * Update the navigation service with loaded modules
     */
    fun updateInstalledModules(modules: List<com.ampairs.workspace.api.model.InstalledModule>) {
        _navigationService.value?.updateInstalledModules(modules)
        _isLoadingModules.value = false
    }

    /**
     * Set error state for navigation service
     */
    fun setNavigationError(error: String?) {
        _navigationService.value?.setError(error)
        _isLoadingModules.value = false
    }

    /**
     * Get current navigation service (nullable)
     */
    fun getCurrentNavigationService(): DynamicModuleNavigationService? {
        return _navigationService.value
    }
}