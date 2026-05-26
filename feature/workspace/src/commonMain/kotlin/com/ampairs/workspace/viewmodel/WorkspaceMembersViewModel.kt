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
import com.ampairs.workspace.api.model.UpdateMemberRequest
import com.ampairs.workspace.api.model.UserRoleResponse
import com.ampairs.workspace.db.OfflineFirstWorkspaceMemberRepository
import com.ampairs.workspace.domain.WorkspaceMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@AssistedInject
class WorkspaceMembersViewModel(
    @Assisted private val workspaceId: String,
    private val memberRepository: OfflineFirstWorkspaceMemberRepository,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(workspaceId: String): WorkspaceMembersViewModel
    }

    private val _state = MutableStateFlow(WorkspaceMembersState())
    val state = _state.asStateFlow()

    private var allMembers = listOf<WorkspaceMember>()

    init {
        loadMembers()
    }

    fun loadMembers(
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "joinedAt",
        sortDir: String = "desc",
        forceRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = _state.value.members.isEmpty(), error = null)
            try {
                val pageResult = memberRepository.getWorkspaceMembers(
                    workspaceId = workspaceId,
                    page = page,
                    size = size,
                    sortBy = sortBy,
                    sortDir = sortDir,
                    forceRefresh = forceRefresh
                )
                allMembers = pageResult.content
                _state.value = _state.value.copy(
                    members = allMembers,
                    isLoading = false,
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    totalMembers = pageResult.totalElements,
                    hasNextPage = !pageResult.isLast,
                    error = null,
                    isOfflineMode = false
                )
            } catch (e: Exception) {
                if (_state.value.members.isEmpty()) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load members"
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false, isOfflineMode = true)
                }
            }
        }
    }

    @Suppress("unused")
    fun loadMoreMembers() {
        val currentState = _state.value
        if (currentState.isLoading || !currentState.hasNextPage) return

        viewModelScope.launch {
            try {
                val nextPage = currentState.currentPage + 1
                val pageResult = memberRepository.getWorkspaceMembers(
                    workspaceId = workspaceId,
                    page = nextPage,
                    size = 20
                )
                val combined = allMembers + pageResult.content
                allMembers = combined
                _state.value = _state.value.copy(
                    members = combined,
                    isLoading = false,
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    totalMembers = pageResult.totalElements,
                    hasNextPage = !pageResult.isLast
                )
            } catch (_: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

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

    @Suppress("unused")
    fun updateMemberRole(memberId: String, newRole: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val updateRequest = UpdateMemberRequest(
                    role = newRole,
                    reason = "Role updated via app"
                )
                memberRepository.updateWorkspaceMember(workspaceId, memberId, updateRequest)
                loadMembers()
                _state.value = _state.value.copy(
                    isLoading = false,
                    successMessage = "Member role updated successfully"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update member role"
                )
            }
        }
    }

    @Suppress("unused")
    fun removeMember(memberId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                memberRepository.removeMember(workspaceId, memberId)
                loadMembers()
                _state.value = _state.value.copy(
                    isLoading = false,
                    successMessage = "Member removed successfully"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to remove member"
                )
            }
        }
    }

    @Suppress("unused")
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    @Suppress("unused")
    fun clearSuccessMessage() {
        _state.value = _state.value.copy(successMessage = null)
    }

    fun refresh() {
        loadMembers(forceRefresh = true)
    }
}

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
