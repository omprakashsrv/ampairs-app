package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import com.ampairs.common.di.AppScope
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.api.model.InstalledModule
import com.ampairs.workspace.api.model.ModuleDetailResponse
import com.ampairs.workspace.db.WorkspaceModuleRepository
import com.ampairs.workspace.domain.WorkspaceModule
import com.ampairs.subscription.util.SubscriptionOnboardingLookup
import com.ampairs.workspace.navigation.GlobalNavigationManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@AssistedInject
class WorkspaceModulesViewModel(
    private val moduleRepository: WorkspaceModuleRepository,
    val onboardingManager: SubscriptionOnboardingLookup,
    @Assisted private val workspaceId: String?,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(workspaceId: String?): WorkspaceModulesViewModel
    }

    private val globalNavigationManager = GlobalNavigationManager.getInstance()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val allModules: StateFlow<List<WorkspaceModule>> = workspaceId
        ?.let { wsId ->
            moduleRepository.observeAllModules(wsId)
                .onEach {
                    _isLoading.value = false
                    val installedModules = it.filter { m -> m.isInstalled }.map { m -> m.toApiModel() }
                    globalNavigationManager.updateInstalledModules(installedModules)
                }
                .catch { e ->
                    _isLoading.value = false
                    if (_errorMessage.value == null) {
                        _errorMessage.value = e.message ?: "Failed to load modules"
                    }
                }
        }
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    val activeModules: StateFlow<List<InstalledModule>> = allModules
        .map { modules -> modules.filter { it.isInstalled && it.enabled && it.status == "ACTIVE" }.map { it.toApiModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Legacy: kept for WorkspaceModulesScreen which uses InstalledModule
    val installedModules: StateFlow<List<InstalledModule>> = allModules
        .map { modules -> modules.filter { it.isInstalled }.map { it.toApiModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadInstalledModules() {
        val wsId = workspaceId ?: run {
            _isLoading.value = false
            return
        }
        globalNavigationManager.setModuleLoading(true)
        _errorMessage.value = null

        viewModelScope.launch {
            moduleRepository.syncInstalledModules(wsId)
            globalNavigationManager.setModuleLoading(false)
        }
    }

    fun loadAvailableModules(
        category: String? = null,
        featured: Boolean = false,
        refresh: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                moduleRepository.getAvailableModules(category, featured, refresh)
            } catch (_: Exception) {
                // Available modules are shown from DB via observeAllModules flow
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleModule(moduleCode: String) {
        val wsId = workspaceId ?: return
        val module = allModules.value.find { it.moduleCode == moduleCode } ?: return

        viewModelScope.launch {
            _errorMessage.value = null
            if (module.isInstalled) {
                moduleRepository.toggleModuleEnabled(wsId, module.installedId!!, !module.enabled)
            } else {
                val result = moduleRepository.installModuleOfflineFirst(wsId, moduleCode)
                result.onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to install module"
                }
            }
        }
    }

    fun installModule(moduleCode: String, onResult: (Boolean) -> Unit = {}) {
        val wsId = workspaceId ?: run { onResult(false); return }
        viewModelScope.launch {
            _errorMessage.value = null
            val result = moduleRepository.installModuleOfflineFirst(wsId, moduleCode)
            result.onFailure { e -> _errorMessage.value = e.message ?: "Failed to install module" }
            onResult(result.isSuccess)
        }
    }

    fun uninstallModule(moduleId: String, onResult: (Boolean) -> Unit = {}) {
        val wsId = workspaceId ?: run { onResult(false); return }
        viewModelScope.launch {
            _errorMessage.value = null
            val result = moduleRepository.uninstallModuleOfflineFirst(wsId, moduleId)
            onResult(result.isSuccess)
        }
    }

    suspend fun isModuleInstalled(moduleCode: String): Boolean {
        val wsId = workspaceId ?: return false
        return moduleRepository.isModuleInstalled(wsId, moduleCode)
    }

    suspend fun getModuleByCode(moduleCode: String): InstalledModule? {
        val wsId = workspaceId ?: return null
        return moduleRepository.getModuleByCode(wsId, moduleCode)
    }

    fun getModuleDetails(moduleId: String, onResult: (ModuleDetailResponse?) -> Unit) {
        val wsId = workspaceId ?: run { onResult(null); return }
        viewModelScope.launch {
            _errorMessage.value = null
            val result = moduleRepository.getModuleDetails(wsId, moduleId)
            result.onSuccess { onResult(it) }
            result.onFailure { e ->
                _errorMessage.value = e.message ?: "Failed to load module details"
                onResult(null)
            }
        }
    }

    fun refresh() {
        loadInstalledModules()
        loadAvailableModules(refresh = true)
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

private fun WorkspaceModule.toApiModel(): InstalledModule = InstalledModule(
    id = installedId ?: moduleCode,
    workspaceId = "",
    moduleCode = moduleCode,
    name = name,
    category = category,
    version = version,
    status = status,
    enabled = enabled,
    installedAt = "",
    icon = icon,
    primaryColor = primaryColor,
    healthScore = null,
    needsAttention = null,
    description = description,
    routeInfo = com.ampairs.workspace.api.model.ModuleRouteInfo(
        basePath = moduleCode.lowercase(),
        displayName = name,
        iconName = "",
        menuItems = emptyList()
    ),
    navigationIndex = navigationIndex
)
