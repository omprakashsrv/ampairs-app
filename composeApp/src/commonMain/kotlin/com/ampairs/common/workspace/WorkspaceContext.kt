package com.ampairs.common.workspace

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the current workspace context for database path resolution.
 * This ensures that each workspace has its own isolated databases.
 */
object WorkspaceContext {
    private val _currentWorkspaceSlug = MutableStateFlow<String?>(null)
    val currentWorkspaceSlug: StateFlow<String?> = _currentWorkspaceSlug.asStateFlow()

    /**
     * Set the current workspace slug. This should be called when switching workspaces.
     * @param slug The workspace slug to set as current
     */
    fun setCurrentWorkspace(slug: String?) {
        _currentWorkspaceSlug.value = slug
    }

    /**
     * Get the current workspace slug or throw if not set
     * @throws IllegalStateException if no workspace is currently set
     */
    fun getCurrentWorkspaceSlugOrThrow(): String {
        return _currentWorkspaceSlug.value
            ?: throw IllegalStateException("No workspace is currently set. Call setCurrentWorkspace() first.")
    }

    /**
     * Get the current workspace slug or return a default
     * @param default The default slug to use if no workspace is set
     */
    fun getCurrentWorkspaceSlugOrDefault(default: String = "default"): String {
        return _currentWorkspaceSlug.value ?: default
    }

    /**
     * Check if a workspace is currently set
     */
    fun hasCurrentWorkspace(): Boolean {
        return _currentWorkspaceSlug.value != null
    }
}