package com.ampairs.workspace.context

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.ampairs.common.concurrency.Volatile
import com.ampairs.common.concurrency.synchronized
import com.ampairs.common.database.DatabaseScopeManager

/**
 * Global workspace context manager for state-based workspace management
 *
 * This approach decouples workspace context from URL routing, enabling:
 * 1. Deep linking to any feature while preserving workspace context
 * 2. Cross-module navigation without losing workspace state
 * 3. Better user experience with persistent workspace sessions
 * 4. Support for workspace-specific settings and configurations
 */
class WorkspaceContextManager private constructor() {

    // Current workspace state
    private val _currentWorkspace = MutableStateFlow<WorkspaceContext?>(null)
    val currentWorkspace: StateFlow<WorkspaceContext?> = _currentWorkspace.asStateFlow()

    // Workspace selection state
    private val _isWorkspaceSelected = MutableStateFlow(false)
    val isWorkspaceSelected: StateFlow<Boolean> = _isWorkspaceSelected.asStateFlow()

    // Workspace loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: WorkspaceContextManager? = null

        fun getInstance(): WorkspaceContextManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WorkspaceContextManager().also { INSTANCE = it }
            }
        }
    }

    /**
     * Set the current workspace context
     * Called when user selects a workspace from workspace list
     */
    fun setWorkspaceContext(workspace: WorkspaceContext) {
        _currentWorkspace.value = workspace
        _isWorkspaceSelected.value = true
    }

    /**
     * Clear workspace context
     * Called when user logs out or switches to workspace selection
     */
    fun clearWorkspaceContext() {
        // Close all databases before clearing context
        DatabaseScopeManager.getInstance().clearAllDatabases()

        _currentWorkspace.value = null
        _isWorkspaceSelected.value = false
    }

    /**
     * Get current workspace ID for API calls
     * Returns null if no workspace is selected
     */
    fun getCurrentWorkspaceId(): String? = _currentWorkspace.value?.id

    /**
     * Get current workspace slug for URL generation (optional)
     * Returns null if no workspace is selected
     */
    fun getCurrentWorkspaceSlug(): String? = _currentWorkspace.value?.slug

    /**
     * Check if user has access to specific module
     */
    fun hasModuleAccess(moduleCode: String): Boolean {
        return _currentWorkspace.value?.enabledModules?.contains(moduleCode) ?: false
    }

    /**
     * Set loading state during workspace operations
     */
    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    /**
     * Validate workspace context before operations
     * Throws exception if no workspace is selected
     */
    fun requireWorkspaceContext(): WorkspaceContext {
        return _currentWorkspace.value
            ?: throw IllegalStateException("No workspace context available. User must select a workspace first.")
    }
}

/**
 * Workspace context data class
 */
data class WorkspaceContext(
    val id: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    val logoUrl: String? = null,
    val primaryColor: String? = null,
    val enabledModules: Set<String> = emptySet(),
    val userRole: String? = null,
    val permissions: Set<String> = emptySet(),
    val settings: Map<String, Any> = emptyMap()
)

/**
 * Workspace context state for UI components
 */
enum class WorkspaceContextState {
    NO_WORKSPACE,        // No workspace selected, show workspace selection
    WORKSPACE_SELECTED,  // Workspace selected, show main app content
    LOADING             // Loading workspace data
}