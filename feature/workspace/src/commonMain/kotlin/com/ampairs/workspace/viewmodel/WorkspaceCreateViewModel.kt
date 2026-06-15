package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.workspace.WorkspaceActivator
import com.ampairs.workspace.api.WorkspaceApi
import com.ampairs.workspace.api.model.CreateWorkspaceRequest
import com.ampairs.workspace.api.model.UpdateWorkspaceRequest
import com.ampairs.workspace.db.WorkspaceRepository
import com.ampairs.workspace.navigation.GlobalNavigationManager
import com.ampairs.workspace.ui.WorkspaceCreateState
import com.ampairs.file.picker.FilePicker
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface WorkspaceCreateEvent {
    data object ArchiveSuccess : WorkspaceCreateEvent
    data object RestoreSuccess : WorkspaceCreateEvent
    // Emitted after a brand-new workspace is created AND activated — the UI should enter it.
    data class EnterCreatedWorkspace(val workspaceId: String, val workspaceSlug: String) : WorkspaceCreateEvent
}

@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class WorkspaceCreateViewModel(
    private val workspaceRepository: WorkspaceRepository,
    private val workspaceApi: WorkspaceApi,
    private val filePicker: FilePicker,
    private val tokenRepository: TokenRepository,
    private val userWorkspaceRepository: UserWorkspaceRepository,
    private val workspaceActivator: WorkspaceActivator,
    private val appPreferences: AppPreferencesDataStore,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkspaceCreateState())
    val state: StateFlow<WorkspaceCreateState> = _state.asStateFlow()

    private val _events = Channel<WorkspaceCreateEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

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

                // Activate and ENTER the new workspace immediately (mirror
                // WorkspaceListViewModel.selectWorkSpace): refresh the Ktor X-Workspace-ID header
                // and build the new workspace's Metro graph BEFORE navigating, so the first
                // workspace a user creates is entered directly instead of bouncing back to the
                // (possibly empty) workspace list.
                val currentUserId = tokenRepository.getCurrentUserId()
                if (currentUserId != null) {
                    userWorkspaceRepository.setWorkspaceIdForUser(currentUserId, workspace.id)
                    appPreferences.setLastWorkspaceId(workspace.id)
                    tokenRepository.getWorkspaceId()
                    workspaceActivator.activateWorkspace(workspace.id, workspace.slug, currentUserId)
                    GlobalNavigationManager.getInstance().onWorkspaceSelected()
                    _state.value = _state.value.copy(isLoading = false, error = null)
                    _events.send(WorkspaceCreateEvent.EnterCreatedWorkspace(workspace.id, workspace.slug))
                } else {
                    // No authenticated user (shouldn't happen post-login) — fall back to the old
                    // behavior of returning to the workspace list.
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = null,
                        createdWorkspaceId = workspace.id
                    )
                }
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

    fun archiveWorkspace() {
        val currentWorkspaceId = _state.value.workspaceId
        if (currentWorkspaceId == null) {
            _state.value = _state.value.copy(error = "No workspace ID provided")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isDeleting = true, deleteError = null)
            try {
                workspaceRepository.archiveWorkspace(currentWorkspaceId)
                _state.value = _state.value.copy(isDeleting = false, deleteError = null)
                _events.send(WorkspaceCreateEvent.ArchiveSuccess)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isDeleting = false,
                    deleteError = e.message ?: "Failed to archive workspace"
                )
            }
        }
    }

    fun restoreWorkspace() {
        val currentWorkspaceId = _state.value.workspaceId
        if (currentWorkspaceId == null) {
            _state.value = _state.value.copy(error = "No workspace ID provided")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                workspaceRepository.restoreWorkspace(currentWorkspaceId)
                _state.value = _state.value.copy(isLoading = false, error = null)
                _events.send(WorkspaceCreateEvent.RestoreSuccess)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to restore workspace"
                )
            }
        }
    }

    fun updateDeleteConfirmationSlug(slug: String) {
        _state.value = _state.value.copy(deleteConfirmationSlug = slug)
    }

    fun showDeleteDialog() {
        _state.value = _state.value.copy(showDeleteDialog = true)
    }

    fun hideDeleteDialog() {
        _state.value = _state.value.copy(
            showDeleteDialog = false,
            deleteConfirmationSlug = "",
            deleteError = null
        )
    }

    val isDeleteConfirmationValid: Boolean
        get() = _state.value.deleteConfirmationSlug == _state.value.slug

    // Avatar upload functionality

    fun pickAvatar() {
        viewModelScope.launch {
            val result = filePicker.pickSingleImage() ?: return@launch
            _state.value = _state.value.copy(
                selectedAvatarData = result.imageData,
                selectedAvatarFileName = result.fileName,
                selectedAvatarContentType = result.contentType,
                avatarMessage = null,
            )
        }
    }

    fun clearSelectedAvatar() {
        _state.value = _state.value.copy(
            selectedAvatarData = null,
            selectedAvatarFileName = null,
            selectedAvatarContentType = null
        )
    }

    fun uploadAvatar() {
        val workspaceId = _state.value.workspaceId
        if (workspaceId == null) {
            _state.value = _state.value.copy(avatarMessage = "Workspace must be created first before uploading avatar")
            return
        }

        val imageData = _state.value.selectedAvatarData
        val fileName = _state.value.selectedAvatarFileName
        val contentType = _state.value.selectedAvatarContentType

        if (imageData == null || fileName == null || contentType == null) {
            _state.value = _state.value.copy(avatarMessage = "No image selected")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isUploadingAvatar = true,
                avatarUploadError = null
            )

            val response = workspaceApi.uploadAvatar(workspaceId, imageData, fileName, contentType)

            if (response.data != null) {
                val updatedWorkspace = response.data!!
                // Update local avatar URL with cache-busting parameter
                val newAvatarUrl = if (!updatedWorkspace.avatarUrl.isNullOrBlank()) {
                    "${ApiUrlBuilder.workspaceAvatarUrl(workspaceId)}?t=${Random.nextLong()}"
                } else {
                    null
                }

                _state.value = _state.value.copy(
                    avatarUrl = newAvatarUrl,
                    selectedAvatarData = null,
                    selectedAvatarFileName = null,
                    selectedAvatarContentType = null,
                    isUploadingAvatar = false,
                    avatarMessage = "Avatar updated successfully"
                )
            } else {
                val errorMessage = response.error?.message.orEmpty().ifEmpty { "Failed to upload avatar" }
                _state.value = _state.value.copy(
                    isUploadingAvatar = false,
                    avatarUploadError = errorMessage,
                    avatarMessage = errorMessage
                )
            }
        }
    }

    fun deleteAvatar() {
        val workspaceId = _state.value.workspaceId
        if (workspaceId == null) {
            _state.value = _state.value.copy(avatarMessage = "No workspace ID")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isUploadingAvatar = true,
                avatarUploadError = null
            )

            val response = workspaceApi.deleteAvatar(workspaceId)

            if (response.data != null) {
                _state.value = _state.value.copy(
                    avatarUrl = null,
                    isUploadingAvatar = false,
                    avatarMessage = "Avatar removed successfully"
                )
            } else {
                val errorMessage = response.error?.message.orEmpty().ifEmpty { "Failed to delete avatar" }
                _state.value = _state.value.copy(
                    isUploadingAvatar = false,
                    avatarUploadError = errorMessage,
                    avatarMessage = errorMessage
                )
            }
        }
    }

    fun clearAvatarMessage() {
        _state.value = _state.value.copy(avatarMessage = null)
    }

    val hasSelectedAvatar: Boolean
        get() = _state.value.selectedAvatarData != null
}