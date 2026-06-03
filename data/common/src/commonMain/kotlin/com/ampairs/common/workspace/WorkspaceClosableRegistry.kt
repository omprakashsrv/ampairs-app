package com.ampairs.common.workspace

import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(WorkspaceScope::class)
class WorkspaceClosableRegistry {
    private val closables = mutableListOf<WorkspaceClosable>()

    fun register(closable: WorkspaceClosable) {
        closables.add(closable)
    }

    fun closeAll() {
        closables.forEach { runCatching { it.close() } }
        closables.clear()
    }
}
