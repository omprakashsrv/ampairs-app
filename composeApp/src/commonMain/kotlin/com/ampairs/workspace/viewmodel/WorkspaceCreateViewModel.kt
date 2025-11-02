package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.workspace.api.model.CreateWorkspaceRequest
import com.ampairs.workspace.api.model.UpdateWorkspaceRequest
import com.ampairs.workspace.db.WorkspaceRepository
import com.ampairs.workspace.ui.WorkspaceCreateState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkspaceCreateViewModel(
    private val workspaceRepository: WorkspaceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkspaceCreateState())
    val state: StateFlow<WorkspaceCreateState> = _state.asStateFlow()

    private var slugCheckJob: Job? = null

    fun updateName(name: String) {
        _state.value = _state.value.copy(name = name)

        // Auto-generate slug from name if slug hasn't been manually modified
        if (!_state.value.isSlugModified && name.isNotEmpty()) {
            val autoSlug = generateSlugFromName(name)
            _state.value = _state.value.copy(slug = autoSlug)
            checkSlugAvailability(autoSlug)
        }

        validateName(name)
    }

    fun updateSlug(slug: String) {
        _state.value = _state.value.copy(
            slug = slug,
            isSlugModified = true
        )
        checkSlugAvailability(slug)
        validateSlug(slug)
    }

    fun updateDescription(description: String) {
        _state.value = _state.value.copy(description = description)
        validateDescription(description)
    }

    fun updateWorkspaceType(workspaceType: String) {
        _state.value = _state.value.copy(workspaceType = workspaceType)
    }

    fun updateAvatarUrl(avatarUrl: String?) {
        _state.value = _state.value.copy(avatarUrl = avatarUrl)
    }

    fun updateTimezone(timezone: String) {
        _state.value = _state.value.copy(timezone = timezone)
    }

    fun updateLanguage(language: String) {
        _state.value = _state.value.copy(language = language)
    }

    private fun checkSlugAvailability(slug: String) {
        if (slug.length < 2) return

        slugCheckJob?.cancel()
        slugCheckJob = viewModelScope.launch {
            delay(500) // Debounce API calls

            _state.value = _state.value.copy(isSlugChecking = true)

            try {
                val result = workspaceRepository.checkSlugAvailability(slug)
                val isAvailable = result["available"] ?: false

                _state.value = _state.value.copy(
                    isSlugAvailable = isAvailable,
                    isSlugChecking = false
                )

                if (!isAvailable) {
                    updateValidationError("slug", "This slug is already taken")
                } else {
                    clearValidationError("slug")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSlugChecking = false,
                    isSlugAvailable = true // Assume available on error
                )
            }
        }
    }

    fun createWorkspace() {
        if (!validateForm()) return

        val request = CreateWorkspaceRequest(
            name = _state.value.name.trim(),
            slug = _state.value.slug.trim().ifEmpty { null },
            description = _state.value.description.trim().ifEmpty { null },
            workspaceType = _state.value.workspaceType,
            avatarUrl = _state.value.avatarUrl,
            timezone = _state.value.timezone,
            language = _state.value.language
        )

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val workspace = workspaceRepository.createWorkspace(request)

                _state.value = _state.value.copy(
                    isLoading = false,
                    error = null,
                    createdWorkspaceId = workspace.id
                )

                // Navigation will be handled in the UI
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to create workspace"
                )
            }
        }
    }

    private fun validateForm(): Boolean {
        val errors = mutableMapOf<String, String>()

        if (_state.value.name.trim().length < 2) {
            errors["name"] = "Workspace name must be at least 2 characters"
        }

        if (_state.value.slug.trim().length < 2) {
            errors["slug"] = "Slug must be at least 2 characters"
        }

        if (!isValidSlug(_state.value.slug)) {
            errors["slug"] = "Slug can only contain lowercase letters, numbers, and hyphens"
        }

        if (!_state.value.isSlugAvailable) {
            errors["slug"] = "This slug is already taken"
        }

        _state.value = _state.value.copy(validationErrors = errors)
        return errors.isEmpty()
    }

    private fun validateName(name: String) {
        if (name.trim().length < 2) {
            updateValidationError("name", "Workspace name must be at least 2 characters")
        } else {
            clearValidationError("name")
        }
    }

    private fun validateSlug(slug: String) {
        when {
            slug.trim().length < 2 -> {
                updateValidationError("slug", "Slug must be at least 2 characters")
            }

            !isValidSlug(slug) -> {
                updateValidationError(
                    "slug",
                    "Slug can only contain lowercase letters, numbers, and hyphens"
                )
            }

            else -> {
                clearValidationError("slug")
            }
        }
    }

    private fun validateDescription(description: String) {
        if (description.length > 500) {
            updateValidationError("description", "Description must not exceed 500 characters")
        } else {
            clearValidationError("description")
        }
    }

    private fun updateValidationError(field: String, error: String) {
        val errors = _state.value.validationErrors.toMutableMap()
        errors[field] = error
        _state.value = _state.value.copy(validationErrors = errors)
    }

    private fun clearValidationError(field: String) {
        val errors = _state.value.validationErrors.toMutableMap()
        errors.remove(field)
        _state.value = _state.value.copy(validationErrors = errors)
    }

    private fun generateSlugFromName(name: String): String {
        return name.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .take(50)
    }

    private fun isValidSlug(slug: String): Boolean {
        return slug.matches(Regex("^[a-z0-9-]+$"))
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun resetForm() {
        _state.value = WorkspaceCreateState()
    }
    
    /**
     * Load workspace data for editing
     */
    fun loadWorkspaceForEdit(workspaceId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoadingWorkspace = true,
                error = null
            )
            
            try {
                val workspace = workspaceRepository.getWorkspaceById(workspaceId)
                if (workspace != null) {
                    _state.value = _state.value.copy(
                        workspaceId = workspaceId,
                        name = workspace.name,
                        slug = workspace.slug,
                        description = workspace.description ?: "",
                        workspaceType = workspace.workspaceType,
                        avatarUrl = workspace.avatarUrl,
                        timezone = workspace.timezone,
                        language = workspace.language,
                        isLoadingWorkspace = false,
                        isSlugModified = true, // Don't auto-generate slug from name in edit mode
                        isSlugAvailable = true // Current slug is available (it's the workspace's own slug)
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoadingWorkspace = false,
                        error = "Workspace not found"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingWorkspace = false,
                    error = e.message ?: "Failed to load workspace"
                )
            }
        }
    }
    
    /**
     * Update existing workspace
     */
    fun updateWorkspace() {
        val currentWorkspaceId = _state.value.workspaceId
        if (currentWorkspaceId == null) {
            _state.value = _state.value.copy(error = "No workspace ID provided for update")
            return
        }
        
        if (!validateForm()) return

        val request = UpdateWorkspaceRequest(
            name = _state.value.name.trim(),
            description = _state.value.description.trim().ifEmpty { null },
            workspaceType = _state.value.workspaceType,
            avatarUrl = _state.value.avatarUrl,
            timezone = _state.value.timezone,
            language = _state.value.language
        )

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val updatedWorkspace = workspaceRepository.updateWorkspace(currentWorkspaceId, request)

                _state.value = _state.value.copy(
                    isLoading = false,
                    error = null,
                    createdWorkspaceId = updatedWorkspace.id // Reuse this field to signal completion
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update workspace"
                )
            }
        }
    }
}