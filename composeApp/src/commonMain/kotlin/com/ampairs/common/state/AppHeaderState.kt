package com.ampairs.common.state

import com.ampairs.auth.domain.UserInfo
import com.ampairs.workspace.domain.WorkspaceList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HeaderState(
    val currentUser: UserInfo? = null,
    val currentWorkspace: WorkspaceList? = null,
    val isUserLoading: Boolean = false,
    val isWorkspaceLoading: Boolean = false
)

class AppHeaderStateManager {
    private val _headerState = MutableStateFlow(HeaderState())
    val headerState: StateFlow<HeaderState> = _headerState.asStateFlow()

    fun updateUser(user: UserInfo?, isLoading: Boolean = false) {
        _headerState.value = _headerState.value.copy(
            currentUser = user,
            isUserLoading = isLoading
        )
    }

    fun updateWorkspace(workspace: WorkspaceList?, isLoading: Boolean = false) {
        _headerState.value = _headerState.value.copy(
            currentWorkspace = workspace,
            isWorkspaceLoading = isLoading
        )
    }

    fun setUserLoading(isLoading: Boolean) {
        _headerState.value = _headerState.value.copy(isUserLoading = isLoading)
    }

    fun setWorkspaceLoading(isLoading: Boolean) {
        _headerState.value = _headerState.value.copy(isWorkspaceLoading = isLoading)
    }

    fun reset() {
        _headerState.value = HeaderState()
    }

    companion object {
        val instance = AppHeaderStateManager()
    }
}