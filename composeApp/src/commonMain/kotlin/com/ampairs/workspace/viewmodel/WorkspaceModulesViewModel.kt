package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.api.model.InstalledModule
import com.ampairs.workspace.api.model.AvailableModule
import com.ampairs.workspace.api.model.ModuleInstallationResponse
import com.ampairs.workspace.api.model.ModuleUninstallationResponse
import com.ampairs.workspace.api.model.ModuleDetailResponse
import com.ampairs.workspace.db.WorkspaceModuleRepository
import com.ampairs.workspace.navigation.DynamicModuleNavigationService
import com.ampairs.workspace.navigation.GlobalNavigationManager
import com.ampairs.workspace.store.InstalledModuleKey
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * ViewModel for Workspace Modules matching web implementation
 * Follows web: workspace-modules.component.ts with signals pattern
 */
class WorkspaceModulesViewModel(
    private val moduleRepository: WorkspaceModuleRepository,
    private val workspaceId: String? = null // Optional workspace context
) : ViewModel() {

    // Use global navigation manager instead of local service
    private val globalNavigationManager = GlobalNavigationManager.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Installed modules - using UI state pattern like StateListViewModel
    private val _installedModules = MutableStateFlow<List<InstalledModule>>(emptyList())
    val installedModules: StateFlow<List<InstalledModule>> = _installedModules.asStateFlow()

    // Active modules - matches web: get activeModules()
    val activeModules: StateFlow<List<InstalledModule>> = installedModules
        .map { modules -> modules.filter { it.status == "ACTIVE" && it.enabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Available modules for store - matches web: availableModules
    private val _availableModules = MutableStateFlow<List<AvailableModule>>(emptyList())
    val availableModules: StateFlow<List<AvailableModule>> = _availableModules.asStateFlow()

    // Featured modules - matches web: get featuredModules()
    val featuredModules: StateFlow<List<AvailableModule>> = availableModules
        .map { modules -> modules.filter { it.featured } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Note: Data loading is handled by the UI screen with explicit refresh=true
    // This ensures fresh data is fetched from the backend via Store5

    init {
        // Sync to global navigation manager is now enabled in loadInstalledModules()
        // Auto-loading is disabled to prevent infinite loops - only load when explicitly called
    }

    /**
     * Load installed modules - exact StateListViewModel pattern
     */
    fun loadInstalledModules() {
        val wsId = workspaceId ?: run {
            println("WorkspaceModulesViewModel: loadInstalledModules - workspaceId is null!")
            return
        }
        println("WorkspaceModulesViewModel: loadInstalledModules called for workspaceId: $wsId")
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            // Set loading state in global navigation manager
            globalNavigationManager.setModuleLoading(true)

            try {
                val key = InstalledModuleKey.refresh(wsId)
                println("WorkspaceModulesViewModel: Streaming from moduleStore with key: $key")
                moduleRepository.moduleStore
                    .stream(StoreReadRequest.cached(key, refresh = true))
                    .collect { response ->
                        println("WorkspaceModulesViewModel: Store response type: ${response::class.simpleName}")
                        when (response) {
                            is StoreReadResponse.Data -> {
                                println("WorkspaceModulesViewModel: Received ${response.value.size} installed modules")
                                _installedModules.value = response.value
                                _isLoading.value = false
                                _errorMessage.value = null
                                // Sync modules to global navigation manager
                                globalNavigationManager.updateInstalledModules(response.value)
                            }

                            is StoreReadResponse.Loading -> {
                                println("WorkspaceModulesViewModel: Loading...")
                                _isLoading.value = true
                            }

                            is StoreReadResponse.Error.Exception -> {
                                println("WorkspaceModulesViewModel: Error.Exception: ${response.error.message}")
                                response.error.printStackTrace()
                                _isLoading.value = false
                                _errorMessage.value = response.error.message ?: "Failed to load modules"
                                // Set error in global navigation manager
                                globalNavigationManager.setNavigationError(_errorMessage.value)
                            }

                            is StoreReadResponse.Error.Message -> {
                                println("WorkspaceModulesViewModel: Error.Message: ${response.message}")
                                _isLoading.value = false
                                _errorMessage.value = response.message
                                // Set error in global navigation manager
                                globalNavigationManager.setNavigationError(_errorMessage.value)
                            }

                            else -> {
                                println("WorkspaceModulesViewModel: Other response type: ${response::class.simpleName}")
                                // Handle other response types if needed
                            }
                        }
                    }
            } catch (e: Exception) {
                println("WorkspaceModulesViewModel: Exception in loadInstalledModules: ${e.message}")
                e.printStackTrace()
                _isLoading.value = false
                _errorMessage.value = e.message ?: "Failed to load modules"
                // Set error in global navigation manager
                globalNavigationManager.setNavigationError(_errorMessage.value)
            }
        }
    }

    /**
     * Load available modules - matches web: async getAvailableModules()
     * Uses offline-first approach with fallback to cached data or mock data
     */
    fun loadAvailableModules(
        category: String? = null,
        featured: Boolean = false,
        refresh: Boolean = false
    ) {
        println("WorkspaceModulesViewModel: loadAvailableModules called - category=$category, featured=$featured, refresh=$refresh")
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                println("WorkspaceModulesViewModel: Calling repository.getAvailableModules...")
                // Call actual API through repository (Store5 handles caching automatically)
                val modules = moduleRepository.getAvailableModules(category, featured, refresh)
                println("WorkspaceModulesViewModel: Received ${modules.size} available modules from repository")
                _availableModules.value = modules

            } catch (e: Exception) {
                println("WorkspaceModulesViewModel: Exception loading available modules: ${e.message}")
                e.printStackTrace()
                // Check if we have existing cached data
                val currentModules = _availableModules.value
                if (currentModules.isEmpty()) {
                    // No cached data available, use mock data as fallback
                    println("WorkspaceModulesViewModel: Using mock data as fallback")
                    val mockModules = createMockAvailableModules()
                    _availableModules.value = mockModules
                    _errorMessage.value = "Using sample data - connection unavailable"
                } else {
                    // We have cached data, show subtle connectivity warning
                    println("WorkspaceModulesViewModel: Using cached data")
                    _errorMessage.value = "Using offline data - connection unavailable"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Mock data for testing - remove when backend is ready
     */
    private fun createMockAvailableModules(): List<AvailableModule> {
        return listOf(
            AvailableModule(
                moduleCode = "CUSTOMER_MGMT",
                name = "Customer Management",
                description = "Comprehensive customer relationship management with advanced analytics",
                category = "Business",
                version = "2.1.0",
                rating = 4.8,
                installCount = 15420,
                complexity = "Medium",
                icon = "customers",
                primaryColor = "#2196F3",
                featured = true,
                requiredTier = "FREE",
                sizeMb = 45
            ),
            AvailableModule(
                moduleCode = "INVENTORY_MGMT",
                name = "Inventory Management",
                description = "Real-time inventory tracking with automated alerts",
                category = "Operations",
                version = "1.8.2",
                rating = 4.6,
                installCount = 12300,
                complexity = "Simple",
                icon = "inventory",
                primaryColor = "#4CAF50",
                featured = true,
                requiredTier = "FREE",
                sizeMb = 32
            ),
            AvailableModule(
                moduleCode = "ANALYTICS_PRO",
                name = "Analytics Dashboard",
                description = "Advanced business intelligence and reporting tools",
                category = "Analytics",
                version = "3.0.1",
                rating = 4.9,
                installCount = 8750,
                complexity = "Advanced",
                icon = "analytics",
                primaryColor = "#FF9800",
                featured = true,
                requiredTier = "PRO",
                sizeMb = 78
            ),
            AvailableModule(
                moduleCode = "ORDER_MGMT",
                name = "Order Management",
                description = "Streamlined order processing and fulfillment",
                category = "Business",
                version = "2.3.0",
                rating = 4.5,
                installCount = 9850,
                complexity = "Simple",
                icon = "orders",
                primaryColor = "#9C27B0",
                featured = false,
                requiredTier = "FREE",
                sizeMb = 28
            ),
            AvailableModule(
                moduleCode = "FINANCIAL_MGMT",
                name = "Financial Management",
                description = "Complete accounting and financial tracking solution",
                category = "Finance",
                version = "2.0.5",
                rating = 4.7,
                installCount = 6420,
                complexity = "Advanced",
                icon = "finance",
                primaryColor = "#E91E63",
                featured = false,
                requiredTier = "PRO",
                sizeMb = 95
            ),
            AvailableModule(
                moduleCode = "HR_MGMT",
                name = "Human Resources",
                description = "Employee management and HR workflows",
                category = "HR",
                version = "1.5.8",
                rating = 4.3,
                installCount = 4200,
                complexity = "Medium",
                icon = "hr",
                primaryColor = "#607D8B",
                featured = false,
                requiredTier = "BUSINESS",
                sizeMb = 52
            )
        )
    }

    /**
     * Install module - matches web: async installModule(moduleCode: string)
     */
    fun installModule(moduleCode: String, onResult: (ModuleInstallationResponse?) -> Unit) {
        val wsId = workspaceId ?: run {
            onResult(null)
            return
        }

        viewModelScope.launch {
            try {
                _errorMessage.value = null

                // Call actual API through repository
                val result = moduleRepository.installModule(wsId, moduleCode)
                if (result.isSuccess) {
                    onResult(result.getOrThrow())
                    loadInstalledModules()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to install module"
                    _errorMessage.value = error
                    onResult(null)
                }
            } catch (e: Exception) {
                val error = e.message ?: "Failed to install module"
                _errorMessage.value = error
                onResult(null)
            }
        }
    }

    /**
     * Uninstall module - matches web: async uninstallModule(moduleId: string)
     */
    fun uninstallModule(moduleId: String, onResult: (ModuleUninstallationResponse?) -> Unit) {
        val wsId = workspaceId ?: run {
            onResult(null)
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val result = moduleRepository.uninstallModule(wsId, moduleId)
                if (result.isSuccess) {
                    onResult(result.getOrThrow())
                    // Refresh installed modules after successful uninstallation
                    loadInstalledModules()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to uninstall module"
                    _errorMessage.value = error
                    onResult(null)
                }
            } catch (e: Exception) {
                val error = e.message ?: "Failed to uninstall module"
                _errorMessage.value = error
                onResult(null)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Check if module is installed - matches web: isModuleInstalled(moduleCode: string)
     */
    suspend fun isModuleInstalled(moduleCode: String): Boolean {
        val wsId = workspaceId ?: return false
        return moduleRepository.isModuleInstalled(wsId, moduleCode)
    }

    /**
     * Get module by code - helper method
     */
    suspend fun getModuleByCode(moduleCode: String): InstalledModule? {
        val wsId = workspaceId ?: return null
        return moduleRepository.getModuleByCode(wsId, moduleCode)
    }

    /**
     * Get module details - matches backend: GET /workspace/v1/modules/{moduleId}
     */
    fun getModuleDetails(moduleId: String, onResult: (ModuleDetailResponse?) -> Unit) {
        val wsId = workspaceId ?: run {
            onResult(null)
            return
        }

        viewModelScope.launch {
            try {
                _errorMessage.value = null

                val result = moduleRepository.getModuleDetails(wsId, moduleId)
                if (result.isSuccess) {
                    onResult(result.getOrThrow())
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to load module details"
                    _errorMessage.value = error
                    onResult(null)
                }
            } catch (e: Exception) {
                val error = e.message ?: "Failed to load module details"
                _errorMessage.value = error
                onResult(null)
            }
        }
    }

    /**
     * Refresh all data
     */
    fun refresh() {
        loadInstalledModules()
        if (_availableModules.value.isNotEmpty()) {
            loadAvailableModules(refresh = true)
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}