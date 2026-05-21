package com.ampairs.workspace.context

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
@BindingContainer
interface WorkspaceContextModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideWorkspaceContextManager(): WorkspaceContextManager = WorkspaceContextManager.getInstance()
    }
}
