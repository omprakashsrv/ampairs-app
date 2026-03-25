package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.api.model.UpdateMemberRequest
import com.ampairs.workspace.api.model.UserRoleResponse
import com.ampairs.workspace.store.WorkspaceMemberStore
import com.ampairs.workspace.store.WorkspaceMemberKey
import com.ampairs.workspace.store.WorkspaceMemberUpdateStoreFactory
import com.ampairs.workspace.store.WorkspaceMemberUpdateKey
import com.ampairs.workspace.store.WorkspaceMemberUpdateRequest
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.model.PageResult
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import com.ampairs.workspace.domain.WorkspaceMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for workspace member management
 *
 * Manages member listing, filtering, role updates, and removal operations
 * with proper state management and error handling.
 */
class WorkspaceMembersViewModel(
    private val workspaceId: String,
    private val memberStore: WorkspaceMemberStore,
    private val memberUpdateStoreFactory: WorkspaceMemberUpdateStoreFactory,
    private val tokenRepository: TokenRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkspaceMembersState())
    val state = _state.asStateFlow()

    private var allMembers = listOf<WorkspaceMember>()

    init {
        loadCurrentUserRole()
    }

    /**
     * Load workspace members from API
     */
    fun loadMembers(
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "joinedAt",
        sortDir: String = "desc",
        forceRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val currentUserId = tokenRepository.getCurrentUserId()
                if (currentUserId == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    )
                    return@launch
                }

                val key = WorkspaceMemberKey(
                    userId = currentUserId,
                    workspaceId = workspaceId,
                    page = page,
                    size = size,
                    sortBy = sortBy,
                    sortDir = sortDir
                )
                
                // Use cached request with refresh - this will show cached data first, then try network
                val request = if (forceRefresh) {
                    StoreReadRequest.fresh(key)
                } else {
                    StoreReadRequest.cached(key, refresh = true)
                }
                
                memberStore.stream(request).collect { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            val pageResult = response.value
                            allMembers = pageResult.content
                            _state.value = _state.value.copy(
                                members = allMembers,
                                isLoading = false,
                                currentPage = pageResult.currentPage,
                                totalPages = pageResult.totalPages,
                                totalMembers = pageResult.totalElements,
                                hasNextPage = !pageResult.isLast,
                                error = null, // Clear any previous errors
                                isOfflineMode = false // Successfully loaded from network
                            )
                        }
                        is StoreReadResponse.Loading -> {
                            // Only show loading spinner if we don't have any data yet
                            if (_state.value.members.isEmpty()) {
                                _state.value = _state.value.copy(isLoading = true)
                            }
                        }
                        is StoreReadResponse.Error.Exception -> {
                            // Check if we have cached data to show
                            if (_state.value.members.isEmpty()) {
                                // No cached data, show error
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error = response.error.message ?: "Failed to load members"
                                )
                            } else {
                                // We have cached data, show it in offline mode
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error = null, // Don't show network error if we have cached data
                                    isOfflineMode = true // Indicate we're showing cached data
                                )
                            }
                        }
                        is StoreReadResponse.Error.Message -> {
                            // Check if we have cached data to show
                            if (_state.value.members.isEmpty()) {
                                // No cached data, show error
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error = response.message
                                )
                            } else {
                                // We have cached data, show it in offline mode
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error = null, // Don't show network error if we have cached data
                                    isOfflineMode = true // Indicate we're showing cached data
                                )
                            }
                        }
                        else -> {
                            // Handle other states
                        }
                    }
                }

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load members"
                )
            }
        }
    }

    /**
     * Load more members for pagination
     */
    @Suppress("unused")
    fun loadMoreMembers() {
        val currentState = _state.value
        if (currentState.isLoading || !currentState.hasNextPage) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val currentUserId = tokenRepository.getCurrentUserId()
                if (currentUserId == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    )
                    return@launch
                }

                val nextPage = currentState.currentPage + 1
                val key = WorkspaceMemberKey(
                    userId = currentUserId,
                    workspaceId = workspaceId,
                    page = nextPage,
                    size = 20 // Use same size as initial load
                )
                val request = StoreReadRequest.cached(key, refresh = true)

                memberStore.stream(request).collect { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            val pageResult = response.value
                            // Append new members to existing list
                            val allMembersList = allMembers + pageResult.content
                            allMembers = allMembersList
                            _state.value = _state.value.copy(
                                members = allMembersList,
                                isLoading = false,
                                currentPage = pageResult.currentPage,
                                totalPages = pageResult.totalPages,
                                totalMembers = pageResult.totalElements,
                                hasNextPage = !pageResult.isLast
                            )
                        }
                        is StoreReadResponse.Loading -> {
                            // Keep current loading state
                        }
                        is StoreReadResponse.Error.Exception -> {
                            // For pagination errors, just stop loading without showing error
                            // User can retry manually if needed
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = null // Don't show error for pagination failures
                            )
                        }
                        is StoreReadResponse.Error.Message -> {
                            // For pagination errors, just stop loading without showing error
                            // User can retry manually if needed
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = null // Don't show error for pagination failures
                            )
                        }
                        else -> {
                            // Handle other states
                        }
                    }
                }

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load more members"
                )
            }
        }
    }

    /**
     * Filter members by role and status
     */
    fun filterMembers(role: String, status: String) {
        val filteredMembers = allMembers.filter { member ->
            val matchesRole = role == "ALL" || member.role == role
            val matchesStatus = status == "ALL" || member.status == status
            matchesRole && matchesStatus
        }

        _state.value = _state.value.copy(
            members = filteredMembers,
            activeFilters = mapOf("role" to role, "status" to status)
        )
    }

    /**
     * Search members by name or email
     */
    @Suppress("unused")
    fun searchMembers(query: String) {
        if (query.isBlank()) {
            _state.value = _state.value.copy(members = allMembers)
            return
        }

        val searchResults = allMembers.filter { member ->
            member.name.contains(query, ignoreCase = true) ||
                    (member.email?.contains(query, ignoreCase = true) == true)
        }

        _state.value = _state.value.copy(members = searchResults)
    }

    /**
     * Update member role
     */
    @Suppress("unused")
    fun updateMemberRole(memberId: String, newRole: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val updateRequest = UpdateMemberRequest(
                    role = newRole,
                    reason = "Role updated via app"
                )

                // Use the Store5 update mechanism
                val updateKey = WorkspaceMemberUpdateKey(workspaceId, memberId)
                
                val success = memberUpdateStoreFactory.updateMember(updateKey, updateRequest)
                
                if (success) {
                    // Reload members to get fresh data
                    loadMembers()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "Member role updated successfully"
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to update member role"
                    )
                }

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update member role"
                )
            }
        }
    }

    /**
     * Remove member from workspace
     */
    @Suppress("unused")
    fun removeMember(memberId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                // Use Store5 remove mechanism
                val removeKey = WorkspaceMemberUpdateKey(workspaceId, memberId)
                
                val success = memberUpdateStoreFactory.removeMember(removeKey)
                
                if (success) {
                    // Reload members to get fresh data
                    loadMembers()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "Member removed successfully"
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to remove member"
                    )
                }

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to remove member"
                )
            }
        }
    }

    /**
     * Load current user's role and permissions
     */
    private fun loadCurrentUserRole() {
        viewModelScope.launch {
            try {
                // TODO: Use Store5 to load user role
                // For now, set a default role
                _state.value = _state.value.copy(currentUserRole = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Could not load user permissions: ${e.message}"
                )
            }
        }
    }

    /**
     * Clear error message
     */
    @Suppress("unused")
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    /**
     * Clear success message
     */
    @Suppress("unused")
    fun clearSuccessMessage() {
        _state.value = _state.value.copy(successMessage = null)
    }

    /**
     * Refresh members data
     */
    fun refresh() {
        loadMembers()
        loadCurrentUserRole()
    }
}

/**
 * UI state for workspace members screen
 */
data class WorkspaceMembersState(
    val members: List<WorkspaceMember> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val totalMembers: Int = 0,
    val hasNextPage: Boolean = false,
    val activeFilters: Map<String, String> = emptyMap(),
    val currentUserRole: UserRoleResponse? = null,
    val isOfflineMode: Boolean = false,
)