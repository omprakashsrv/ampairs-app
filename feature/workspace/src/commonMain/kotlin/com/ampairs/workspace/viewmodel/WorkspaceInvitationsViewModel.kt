package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.api.model.CreateInvitationRequest
import com.ampairs.workspace.api.model.ResendInvitationRequest
import com.ampairs.workspace.db.OfflineFirstWorkspaceInvitationRepository
import com.ampairs.workspace.db.OfflineFirstRolesPermissionsRepository
import com.ampairs.workspace.domain.InvitationAcceptanceResult
import com.ampairs.workspace.domain.WorkspaceInvitation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for workspace invitation management with Store5 offline-first approach
 *
 * Manages invitation listing, creation, acceptance, resending, and cancellation
 * with proper offline-first state management, automatic sync, and error handling.
 */
class WorkspaceInvitationsViewModel(
    private val workspaceId: String,
    private val invitationRepository: OfflineFirstWorkspaceInvitationRepository,
    private val rolesRepository: OfflineFirstRolesPermissionsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkspaceInvitationsState())
    val state: StateFlow<WorkspaceInvitationsState> = _state.asStateFlow()

    private var allInvitations = listOf<WorkspaceInvitation>()
    private var currentPage = 0
    private val pageSize = 20

    /**
     * Load workspace invitations using Store5 offline-first approach
     */
    fun loadInvitations(
        page: Int = 0,
        size: Int = pageSize,
        sortBy: String = "createdAt",
        sortDir: String = "desc",
        refresh: Boolean = false
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            currentPage = page

            println("📝 WorkspaceInvitationsViewModel: Loading invitations for workspace $workspaceId (page=$page, refresh=$refresh)")

            try {
                invitationRepository.getWorkspaceInvitations(
                    workspaceId = workspaceId,
                    page = page,
                    size = size,
                    sortBy = sortBy,
                    sortDir = sortDir,
                    refresh = refresh
                ).catch { error ->
                    println("❌ WorkspaceInvitationsViewModel: Error loading invitations: ${error.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load invitations"
                    )
                }.collectLatest { pageResult ->
                    println("✅ WorkspaceInvitationsViewModel: Received ${pageResult.content.size} invitations")
                    allInvitations = pageResult.content
                    _state.value = _state.value.copy(
                        invitations = allInvitations,
                        isLoading = false,
                        currentPage = pageResult.currentPage,
                        totalPages = pageResult.totalPages,
                        totalInvitations = pageResult.totalElements,
                        hasNextPage = !pageResult.isLast
                    )
                }
            } catch (e: Exception) {
                println("❌ WorkspaceInvitationsViewModel: Exception during load: ${e.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load invitations"
                )
            }
        }
    }

    /**
     * Load more invitations for pagination with Store5 support
     */
    fun loadMoreInvitations() {
        val currentState = _state.value
        if (currentState.isLoading || !currentState.hasNextPage) return

        loadInvitations(page = currentState.currentPage + 1, refresh = false)
    }

    /**
     * Filter invitations by status and role using Store5 filters
     */
    fun filterInvitations(status: String, role: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                activeFilters = mapOf("status" to status, "role" to role)
            )

            val filterStatus = if (status == "ALL") null else status
            val filterRole = if (role == "ALL") null else role

            invitationRepository.getFilteredInvitations(
                workspaceId = workspaceId,
                status = filterStatus,
                role = filterRole,
                page = 0,
                size = pageSize,
                refresh = false
            ).catch { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = error.message ?: "Failed to filter invitations"
                )
            }.collectLatest { pageResult ->
                allInvitations = pageResult.content
                _state.value = _state.value.copy(
                    invitations = allInvitations,
                    isLoading = false,
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    totalInvitations = pageResult.totalElements,
                    hasNextPage = !pageResult.isLast
                )
            }
        }
    }

    /**
     * Search invitations by recipient name or email using Store5 repository
     */
    fun searchInvitations(query: String) {
        if (query.isBlank()) {
            // Reset to show all invitations
            loadInvitations(page = 0, refresh = false)
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            invitationRepository.searchInvitations(workspaceId, query)
                .catch { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to search invitations"
                    )
                }.collectLatest { searchResults ->
                    _state.value = _state.value.copy(
                        invitations = searchResults,
                        isLoading = false,
                        totalInvitations = searchResults.size,
                        currentPage = 0,
                        totalPages = 1,
                        hasNextPage = false
                    )
                }
        }
    }

    /**
     * Create and send new invitation
     */
    fun createInvitation(
        countryCode: Int,
        phone: String,
        recipientName: String? = null,
        invitedRole: String,
        customMessage: String? = null,
        expiresInDays: Int = 7,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val request = CreateInvitationRequest(
                    countryCode = countryCode,
                    phone = phone,
                    name = recipientName,
                    invitedRole = invitedRole,
                    customMessage = customMessage,
                    expiresInDays = expiresInDays,
                    sendNotification = true,
                    welcomeTour = true
                )

                val newInvitation = invitationRepository.createInvitation(
                    workspaceId = workspaceId,
                    request = request
                )

                // Add new invitation to local state
                val updatedInvitations = listOf(newInvitation) + _state.value.invitations
                allInvitations = updatedInvitations

                _state.value = _state.value.copy(
                    invitations = updatedInvitations,
                    isLoading = false,
                    totalInvitations = _state.value.totalInvitations + 1,
                    successMessage = "Invitation sent successfully to +$countryCode $phone"
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to send invitation"
                )
            }
        }
    }

    /**
     * Accept invitation using token (for invitation acceptance flow)
     */
    fun acceptInvitation(token: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val acceptanceResult = invitationRepository.acceptInvitation(token)

                _state.value = _state.value.copy(
                    isLoading = false,
                    acceptanceResult = acceptanceResult,
                    successMessage = "Welcome to ${acceptanceResult.workspaceName}!"
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to accept invitation"
                )
            }
        }
    }

    /**
     * Resend invitation
     */
    fun resendInvitation(
        invitationId: String,
        updatedMessage: String? = null,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val request = ResendInvitationRequest(
                    updatedMessage = updatedMessage,
                    priorityDelivery = true,
                    includeReminder = true
                )

                val updatedInvitation = invitationRepository.resendInvitation(
                    workspaceId = workspaceId,
                    invitationId = invitationId,
                    request = request
                )

                // Update invitation in local state
                val updatedInvitations = _state.value.invitations.map { invitation ->
                    if (invitation.id == invitationId) updatedInvitation else invitation
                }

                allInvitations = updatedInvitations
                _state.value = _state.value.copy(
                    invitations = updatedInvitations,
                    isLoading = false,
                    successMessage = "Invitation resent successfully"
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to resend invitation"
                )
            }
        }
    }

    /**
     * Cancel invitation
     */
    fun cancelInvitation(invitationId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val result = invitationRepository.cancelInvitation(workspaceId, invitationId)

                // Update invitation status in local state
                val updatedInvitations = _state.value.invitations.map { invitation ->
                    if (invitation.id == invitationId) {
                        invitation.copy(status = "CANCELLED")
                    } else invitation
                }

                allInvitations = updatedInvitations
                _state.value = _state.value.copy(
                    invitations = updatedInvitations,
                    isLoading = false,
                    successMessage = "Invitation cancelled successfully"
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to cancel invitation"
                )
            }
        }
    }

    /**
     * Get invitation statistics
     */
    fun getInvitationStats(): InvitationStats {
        val invitations = _state.value.invitations
        return InvitationStats(
            totalSent = invitations.size,
            pending = invitations.count { it.status == "PENDING" },
            accepted = invitations.count { it.status == "ACCEPTED" },
            expired = invitations.count { it.status == "EXPIRED" },
            cancelled = invitations.count { it.status == "CANCELLED" },
            declined = invitations.count { it.status == "DECLINED" }
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _state.value = _state.value.copy(successMessage = null)
    }

    /**
     * Clear acceptance result
     */
    fun clearAcceptanceResult() {
        _state.value = _state.value.copy(acceptanceResult = null)
    }

    /**
     * Load available workspace roles for invitation creation
     */
    fun loadAvailableRoles() {
        viewModelScope.launch {
            try {
                rolesRepository.getWorkspaceRoles(workspaceId)
                    .catch { error ->
                        println("Failed to load roles: ${error.message}")
                    }.collectLatest { roles ->
                        _state.value = _state.value.copy(availableRoles = roles)
                    }
            } catch (e: Exception) {
                // Silently fail for roles loading - use fallback roles in UI
                println("Failed to load roles: ${e.message}")
            }
        }
    }

    /**
     * Refresh invitations data from server
     */
    fun refresh() {
        loadInvitations(page = 0, refresh = true)
    }
}

/**
 * UI state for workspace invitations screen
 */
data class WorkspaceInvitationsState(
    val invitations: List<WorkspaceInvitation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val totalInvitations: Int = 0,
    val hasNextPage: Boolean = false,
    val activeFilters: Map<String, String> = emptyMap(),
    val acceptanceResult: InvitationAcceptanceResult? = null,
    val availableRoles: List<com.ampairs.workspace.api.model.WorkspaceRole> = emptyList(),
)

/**
 * Invitation statistics data class
 */
data class InvitationStats(
    val totalSent: Int,
    val pending: Int,
    val accepted: Int,
    val expired: Int,
    val cancelled: Int,
    val declined: Int,
) {
    val acceptanceRate: Double get() = if (totalSent > 0) (accepted.toDouble() / totalSent) * 100 else 0.0
    val pendingRate: Double get() = if (totalSent > 0) (pending.toDouble() / totalSent) * 100 else 0.0
}