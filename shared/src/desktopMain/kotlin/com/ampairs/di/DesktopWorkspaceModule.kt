package com.ampairs.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.tallysync.TallySyncScheduler
import dev.zacsweers.metro.ContributesTo

/**
 * Desktop-specific workspace-scope graph accessors.
 * Metro generates the WorkspaceGraph implementation to satisfy these properties
 * from WorkspaceScope bindings. Cast via `workspaceGraph as? DesktopWorkspaceModule`.
 */
@ContributesTo(WorkspaceScope::class)
interface DesktopWorkspaceModule {
    val tallySyncScheduler: TallySyncScheduler
}
