package com.ampairs.workspace.context

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * Composable wrapper that ensures workspace context is available
 *
 * This component handles the workspace selection flow automatically:
 * 1. If no workspace is selected, shows workspace selection screen
 * 2. If workspace is selected, shows the wrapped content
 * 3. Handles loading states and error scenarios
 */
@Composable
fun WorkspaceAwareContent(
    onWorkspaceSelectionRequired: () -> Unit,
    content: @Composable (WorkspaceContext) -> Unit
) {
    val workspaceManager = WorkspaceContextManager.getInstance()
    val currentWorkspace by workspaceManager.currentWorkspace.collectAsState()
    val isWorkspaceSelected by workspaceManager.isWorkspaceSelected.collectAsState()

    when {
        currentWorkspace != null && isWorkspaceSelected -> {
            // Workspace is selected, show content
            content(currentWorkspace!!)
        }
        else -> {
            // No workspace selected, trigger workspace selection
            LaunchedEffect(Unit) {
                onWorkspaceSelectionRequired()
            }
        }
    }
}

/**
 * Hook for accessing workspace context in ViewModels
 */
fun useWorkspaceContext(): WorkspaceContext? {
    return WorkspaceContextManager.getInstance().currentWorkspace.value
}

/**
 * Hook for requiring workspace context (throws if not available)
 */
fun requireWorkspaceContext(): WorkspaceContext {
    return WorkspaceContextManager.getInstance().requireWorkspaceContext()
}