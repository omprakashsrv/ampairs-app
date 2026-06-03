package com.ampairs.common.workspace

interface WorkspaceActivator {
    fun activateWorkspace(workspaceId: String, workspaceSlug: String, userId: String)
}
