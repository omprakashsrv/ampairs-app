package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.api.model.UpdateMemberRequest
import com.ampairs.workspace.api.model.UserRoleResponse
import com.ampairs.workspace.api.model.WorkspaceRole
import com.ampairs.workspace.api.model.WorkspacePermission
import com.ampairs.workspace.db.WorkspaceMemberRepository
import com.ampairs.workspace.domain.WorkspaceMember
import com.ampairs.workspace.store.WorkspaceMemberStore
import com.ampairs.workspace.store.WorkspaceRolesStore
import com.ampairs.workspace.store.WorkspacePermissionsStore
import com.ampairs.workspace.store.WorkspaceRolesKey
import com.ampairs.workspace.store.WorkspacePermissionsKey
import com.ampairs.workspace.store.WorkspaceMemberKey
import com.ampairs.workspace.store.WorkspaceMemberUpdateStoreFactory
import com.ampairs.workspace.store.WorkspaceMemberUpdateKey
import com.ampairs.auth.api.TokenRepository
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Clean, refactored ViewModel for member details and editing functionality.
 *
 * Assumptions:
 * - repository methods return plain domain models or flows and throw exceptions on error.
 * - OfflineFirstRolesPermissionsRepository exposes cached data and Flows for fresh data.
 */
class MemberDetailsViewModel(
    private val workspaceId: String,
    private val memberId: String,
    private val memberStore: WorkspaceMemberStore,
    private val memberUpdateStoreFactory: WorkspaceMemberUpdateStoreFactory,
    private val memberRepository: WorkspaceMemberRepository, // Keep for methods not yet using Store5
    private val rolesStore: WorkspaceRolesStore,
    private val permissionsStore: WorkspacePermissionsStore,
    private val tokenRepository: TokenRepository,
) : ViewModel() {

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

    /**
     * Load member details (synchronous-like call wrapped in coroutine).
     */
    fun loadMemberDetails(forceRefresh: Boolean = false) {
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

                // Use Store5 to get single member details
                val key = WorkspaceMemberKey.forMember(currentUserId, workspaceId, memberId)
                val request = if (forceRefresh) {
                    StoreReadRequest.fresh(key)
                } else {
                    StoreReadRequest.cached(key, refresh = true)
                }
                
                memberStore.stream(request).collect { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            val pageResult = response.value
                            val member = pageResult.content.firstOrNull()
                            
                            if (member != null) {
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    member = member,
                                    originalMember = member,
                                    displayMember = member,
                                    error = null,
                                    isOfflineMode = false // Successfully loaded from network
                                )
                                
                                // Recalculate permissions now that member is loaded
                                // If userRole is not available, load it first
                                if (_state.value.userRole == null) {
                                    loadUserRole()
                                } else {
                                    val userRole = _state.value.userRole!!
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
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error = "Member not found"
                                )
                            }
                        }
                        is StoreReadResponse.Loading -> {
                            // Only show loading if we don't have any data yet
                            if (_state.value.member == null) {
                                _state.value = _state.value.copy(isLoading = true)
                            }
                        }
                        is StoreReadResponse.Error.Exception -> {
                            // Check if we have cached data to show
                            if (_state.value.member == null) {
                                // No cached data, show error
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error = response.error.message ?: "Failed to load member details"
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
                            if (_state.value.member == null) {
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
                    error = e.message ?: "Failed to load member details"
                )
            }
        }
    }

    /**
     * Load current user's role (used to compute granular permissions)
     */
    private fun loadUserRole() {
        viewModelScope.launch {
            try {
                val userRole = memberRepository.getMyRole(workspaceId)
                _state.value = _state.value.copy(userRole = userRole)

                // Recalculate permissions if member already loaded
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
                // Non-fatal: store an error but continue
                _state.value = _state.value.copy(
                    error = "Could not load user role: ${e.message}"
                )
            }
        }
    }

    /**
     * Load roles & permissions using Store5 for offline-first data access.
     */
    private fun loadAvailableRolesAndPermissions() {
        viewModelScope.launch {
            try {
                val currentUserId = tokenRepository.getCurrentUserId() ?: "unknown_user"

                // Load workspace roles using Store5
                launch {
                    try {
                        val rolesKey = WorkspaceRolesKey(workspaceId, currentUserId)
                        val rolesRequest = StoreReadRequest.cached(rolesKey, refresh = true)
                        
                        rolesStore.stream(rolesRequest).collect { response ->
                            when (response) {
                                is StoreReadResponse.Data -> {
                                    _state.value = _state.value.copy(availableRoles = response.value)
                                }
                                is StoreReadResponse.Loading -> {
                                    // Keep existing data while loading
                                }
                                is StoreReadResponse.Error -> {
                                    // Keep existing cached data on error
                                }
                                else -> {
                                    // Handle other response types
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // ignore background errors
                    }
                }

                // Load workspace permissions using Store5
                launch {
                    try {
                        val permissionsKey = WorkspacePermissionsKey(workspaceId, currentUserId)
                        val permissionsRequest = StoreReadRequest.cached(permissionsKey, refresh = true)
                        
                        permissionsStore.stream(permissionsRequest).collect { response ->
                            when (response) {
                                is StoreReadResponse.Data -> {
                                    _state.value = _state.value.copy(availablePermissions = response.value)
                                }
                                is StoreReadResponse.Loading -> {
                                    // Keep existing data while loading
                                }
                                is StoreReadResponse.Error -> {
                                    // Keep existing cached data on error
                                }
                                else -> {
                                    // Handle other response types
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // ignore background errors
                    }
                }

            } catch (_: Exception) {
                // ignore load errors; nothing critical
            }
        }
    }

    /**
     * Update pending role for UI (does not persist until saveMemberChanges)
     */
    fun updateRole(newRole: String) {
        val current = _state.value.member ?: return
        pendingRole = if (current.role == newRole) null else newRole
        updateDisplayMember()
        updateHasChanges()
    }

    /**
     * Update pending status for UI
     */
    fun updateStatus(newStatus: String) {
        val current = _state.value.member ?: return
        pendingStatus = if (current.status == newStatus) null else newStatus
        updateDisplayMember()
        updateHasChanges()
    }

    /**
     * Update pending custom permissions for UI
     */
    fun updatePermissions(newPermissions: List<String>) {
        pendingPermissions = newPermissions
        updateDisplayMember()
        updateHasChanges()
    }

    /**
     * Apply pending changes to the displayMember for immediate UI feedback
     */
    private fun updateDisplayMember() {
        val current = _state.value.member ?: return
        val display = current.copy(
            role = pendingRole ?: current.role,
            status = pendingStatus ?: current.status,
            permissions = pendingPermissions ?: current.permissions
        )
        _state.value = _state.value.copy(displayMember = display)
    }

    /**
     * Persist pending changes to the server via repository
     */
    fun saveMemberChanges() {
        if (_state.value.member == null) return
         if (!_state.value.hasChanges) return

         viewModelScope.launch {
             _state.value = _state.value.copy(isLoading = true, error = null)

             try {
                 // Capture mutable properties to avoid smart cast issues
                 val currentPendingPermissions = pendingPermissions
                 
                 // Convert permissions to Set<WorkspacePermission>
                 val permissionSet = if (!currentPendingPermissions.isNullOrEmpty()) {
                     currentPendingPermissions.mapNotNull { permission ->
                         // Convert from colon format (e.g., "member:view") to underscore format (e.g., "MEMBER_VIEW")
                         val convertedPermission = if (permission.contains(":")) {
                             val parts = permission.split(":")
                             "${parts[0].uppercase()}_${parts[1].uppercase()}"
                         } else {
                             permission.uppercase()
                         }
                         WorkspacePermission.fromString(convertedPermission)
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

                 // Use Store5 update mechanism for better sync
                val updateKey = WorkspaceMemberUpdateKey(workspaceId, memberId)
                val success = memberUpdateStoreFactory.updateMember(updateKey, updateRequest)
                
                if (success) {
                    // Reload member details to get fresh data from Store5
                    loadMemberDetails(forceRefresh = true)
                } else {
                    throw Exception("Failed to update member")
                }
                
                // For backward compatibility, get the updated member from state
                val updatedMember = _state.value.member!!

                 _state.value = _state.value.copy(
                     isLoading = false,
                     member = updatedMember,
                     originalMember = updatedMember,
                     displayMember = updatedMember,
                     hasChanges = false,
                     successMessage = "Member updated successfully",
                     error = null
                 )

                 clearPendingChanges()
             } catch (e: Exception) {
                 _state.value = _state.value.copy(
                     isLoading = false,
                     error = e.message ?: "Failed to save changes"
                 )
             }
         }
     }

    /**
     * Remove member from workspace
     */
    fun removeMember() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                // Use Store5 remove mechanism 
                val removeKey = WorkspaceMemberUpdateKey(workspaceId, memberId)
                val success = memberUpdateStoreFactory.removeMember(removeKey)
                
                if (!success) {
                    throw Exception("Failed to remove member")
                }

                // remove locally from UI
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

    /**
     * Discard pending changes
     */
    fun discardChanges() {
        clearPendingChanges()
        updateHasChanges()
    }

    /**
     * Calculate granular member permissions based on current user role and loaded member
     */
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

        // evaluate change role permission in a way that avoids unreachable-condition warnings
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
