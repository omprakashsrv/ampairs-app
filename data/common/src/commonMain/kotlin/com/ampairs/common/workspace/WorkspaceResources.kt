package com.ampairs.common.workspace

import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(WorkspaceScope::class)
class WorkspaceResources(val registry: WorkspaceClosableRegistry) {
    fun close() = registry.closeAll()
}
