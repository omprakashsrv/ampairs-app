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
import com.ampairs.workspace.api.model.WorkspaceRole
import com.ampairs.workspace.api.model.WorkspacePermission
import com.ampairs.workspace.db.WorkspaceMemberRepository
import com.ampairs.workspace.db.OfflineFirstWorkspaceMemberRepository
import com.ampairs.workspace.db.OfflineFirstRolesPermissionsRepository
import com.ampairs.workspace.domain.WorkspaceMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AssistedInject
class MemberDetailsViewModel(
    @Assisted private val workspaceId: String,
    @Assisted private val memberId: String,
    private val memberRepository: OfflineFirstWorkspaceMemberRepository,
    private val memberRoleRepository: WorkspaceMemberRepository,
    private val rolesPermissionsRepository: OfflineFirstRolesPermissionsRepository,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(workspaceId: String, memberId: String): MemberDetailsViewModel
    }

    data class MemberDetailsState(
        val isLoading: Boolean = false,
        val member: WorkspaceMember? = null,
        val originalMember: WorkspaceMember? = null,
        val displayMember: WorkspaceMember? = null,
        val userRole: UserRoleResponse? = null,
        val availableRoles: List<WorkspaceRole> = emptyList(),
        val availablePermissions: Map<String, Map<String, Boolean>> = emptyMap(),
        val canEdit: Boolean = false,
        val canChangeRole: Boolean = false,
        val canChangeStatus: Boolean = false,
        val canChangePermissions: Boolean = false,
        val canRemoveMember: Boolean = false,
        val hasChanges: Boolean = false,
        val error: String? = null,
        val successMessage: String? = null,
        val isOfflineMode: Boolean = false,
    )

    private val _state = MutableStateFlow(MemberDetailsState())
    val state: StateFlow<MemberDetailsState> = _state.asStateFlow()

    private var pendingRole: String? = null
    private var pendingStatus: String? = null
    private var pendingPermissions: List<String>? = null

    private data class MemberPermissions(
        val canEdit: Boolean = false,
        val canChangeRole: Boolean = false,
        val canChangeStatus: Boolean = false,
        val canChangePermissions: Boolean = false,
        val canRemoveMember: Boolean = false,
    )

    init {
        loadMemberDetails()
        loadUserRole()
        loadAvailableRolesAndPermissions()
    }

    fun loadMemberDetails(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = _state.value.member == null, error = null)
            try {
                val member = if (forceRefresh) {
                    val result = memberRepository.getWorkspaceMembers(
                        workspaceId = workspaceId,
                        forceRefresh = true
                    )
                    result.content.firstOrNull { it.id == memberId }
                } else {
                    memberRepository.getWorkspaceMemberById(workspaceId, memberId)
                        ?: memberRepository.getWorkspaceMembers(workspaceId, forceRefresh = false)
                            .content.firstOrNull { it.id == memberId }
                }

                if (member != null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        member = member,
                        originalMember = member,
                        displayMember = member,
                        error = null,
                        isOfflineMode = false
                    )
                    val userRole = _state.value.userRole
                    if (userRole != null) {
                        val perms = calculateMemberPermissions(userRole)
                        _state.value = _state.value.copy(
                            canEdit = perms.canEdit,
                            canChangeRole = perms.canChangeRole,
                            canChangeStatus = perms.canChangeStatus,
                            canChangePermissions = perms.canChangePermissions,
                            canRemoveMember = perms.canRemoveMember
                        )
                    }
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Member not found")
                }
            } catch (e: Exception) {
                if (_state.value.member == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load member details"
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false, isOfflineMode = true)
                }
            }
        }
    }

    private fun loadUserRole() {
        viewModelScope.launch {
            try {
                val userRole = memberRoleRepository.getMyRole(workspaceId)
                _state.value = _state.value.copy(userRole = userRole)
                val currentMember = _state.value.member
                if (currentMember != null) {
                    val perms = calculateMemberPermissions(userRole)
                    _state.value = _state.value.copy(
                        canEdit = perms.canEdit,
                        canChangeRole = perms.canChangeRole,
                        canChangeStatus = perms.canChangeStatus,
                        canChangePermissions = perms.canChangePermissions,
                        canRemoveMember = perms.canRemoveMember
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Could not load user role: ${e.message}")
            }
        }
    }

    private fun loadAvailableRolesAndPermissions() {
        viewModelScope.launch {
            launch {
                try {
                    rolesPermissionsRepository.getWorkspaceRoles(workspaceId)
                        .catch { _ -> }
                        .collectLatest { roles ->
                            _state.value = _state.value.copy(availableRoles = roles)
                        }
                } catch (_: Exception) {}
            }
            launch {
                try {
                    rolesPermissionsRepository.getWorkspacePermissions(workspaceId)
                        .catch { _ -> }
                        .collectLatest { permissions ->
                            _state.value = _state.value.copy(availablePermissions = permissions)
                        }
                } catch (_: Exception) {}
            }
        }
    }

    fun updateRole(newRole: String) {
        val current = _state.value.member ?: return
        pendingRole = if (current.role == newRole) null else newRole
        updateDisplayMember()
        updateHasChanges()
    }

    fun updateStatus(newStatus: String) {
        val current = _state.value.member ?: return
        pendingStatus = if (current.status == newStatus) null else newStatus
        updateDisplayMember()
        updateHasChanges()
    }

    fun updatePermissions(newPermissions: List<String>) {
        pendingPermissions = newPermissions
        updateDisplayMember()
        updateHasChanges()
    }

    private fun updateDisplayMember() {
        val current = _state.value.member ?: return
        val display = current.copy(
            role = pendingRole ?: current.role,
            status = pendingStatus ?: current.status,
            permissions = pendingPermissions ?: current.permissions
        )
        _state.value = _state.value.copy(displayMember = display)
    }

    fun saveMemberChanges() {
        if (_state.value.member == null || !_state.value.hasChanges) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val currentPendingPermissions = pendingPermissions
                val permissionSet = if (!currentPendingPermissions.isNullOrEmpty()) {
                    currentPendingPermissions.mapNotNull { permission ->
                        val converted = if (permission.contains(":")) {
                            val parts = permission.split(":")
                            "${parts[0].uppercase()}_${parts[1].uppercase()}"
                        } else {
                            permission.uppercase()
                        }
                        WorkspacePermission.fromString(converted)
                    }.toSet()
                } else {
                    emptySet()
                }

                val updateRequest = UpdateMemberRequest(
                    role = pendingRole,
                    permissions = permissionSet,
                    reason = "Updated via member details screen",
                    notifyMember = true
                )
                memberRepository.updateWorkspaceMember(workspaceId, memberId, updateRequest)
                clearPendingChanges()
                _state.value = _state.value.copy(
                    isLoading = false,
                    hasChanges = false,
                    successMessage = "Member updated successfully",
                    error = null
                )
                loadMemberDetails(forceRefresh = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to save changes"
                )
            }
        }
    }

    fun removeMember() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                memberRepository.removeMember(workspaceId, memberId)
                _state.value = _state.value.copy(
                    isLoading = false,
                    successMessage = "Member removed",
                    member = null,
                    originalMember = null,
                    displayMember = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to remove member"
                )
            }
        }
    }

    fun discardChanges() {
        clearPendingChanges()
        updateHasChanges()
    }

    private fun calculateMemberPermissions(userRole: UserRoleResponse?): MemberPermissions {
        val currentMember = _state.value.member ?: return MemberPermissions()
        if (userRole == null) return MemberPermissions()

        val isOwner = userRole.currentRole == "OWNER" || (userRole.roleHierarchy["OWNER"] == true)
        val isAdmin = userRole.currentRole == "ADMIN" || (userRole.roleHierarchy["ADMIN"] == true)
        val canManageMembers = userRole.permissions["members"]?.get("manage") == true
        val canEditMembers = userRole.permissions["members"]?.get("edit") == true
        val canDeleteMembers = userRole.permissions["members"]?.get("delete") == true

        val targetIsOwner = currentMember.role == "OWNER"
        if (targetIsOwner && !isOwner) return MemberPermissions()

        val canEditThisRole = when {
            isOwner -> true
            isAdmin -> currentMember.role !in listOf("OWNER", "ADMIN")
            else -> currentMember.role in listOf("MEMBER", "VIEWER")
        }

        val canChangeRole = canManageMembers && (isOwner || (isAdmin && !targetIsOwner))
        val canEdit = (canManageMembers || canEditMembers) && canEditThisRole
        val canChangeStatus = canEdit
        val canChangePermissions = (isOwner || isAdmin) && canManageMembers && canEditThisRole
        val canRemoveMember = (canDeleteMembers || canManageMembers) && !targetIsOwner && canEditThisRole

        return MemberPermissions(
            canEdit = canEdit,
            canChangeRole = canChangeRole,
            canChangeStatus = canChangeStatus,
            canChangePermissions = canChangePermissions,
            canRemoveMember = canRemoveMember
        )
    }

    private fun updateHasChanges() {
        val hasChanges = pendingRole != null || pendingStatus != null || pendingPermissions != null
        _state.value = _state.value.copy(hasChanges = hasChanges)
    }

    private fun clearPendingChanges() {
        pendingRole = null
        pendingStatus = null
        pendingPermissions = null
        _state.value = _state.value.copy(displayMember = _state.value.originalMember)
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
        loadMemberDetails(forceRefresh = true)
        loadUserRole()
        loadAvailableRolesAndPermissions()
    }
}
