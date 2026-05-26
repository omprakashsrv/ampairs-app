package com.ampairs.workspace

import com.ampairs.common.di.AppScope
import com.ampairs.workspace.db.WorkspaceRoomDatabase
import com.ampairs.workspace.db.dao.UserInvitationDao
import com.ampairs.workspace.db.dao.WorkspaceDao
import com.ampairs.workspace.db.dao.WorkspaceInvitationDao
import com.ampairs.workspace.db.dao.WorkspaceMemberDao
import com.ampairs.workspace.db.dao.WorkspaceModuleDao
import com.ampairs.workspace.db.dao.WorkspacePermissionDao
import com.ampairs.workspace.db.dao.WorkspaceRoleDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Metro bindings for workspace DAOs (sourced from the WorkspaceRoomDatabase).
 */
@ContributesTo(AppScope::class)
interface WorkspaceDaoModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideWorkspaceDao(db: WorkspaceRoomDatabase): WorkspaceDao = db.workspaceDao()

        @Provides
        @SingleIn(AppScope::class)
        fun provideWorkspaceMemberDao(db: WorkspaceRoomDatabase): WorkspaceMemberDao = db.workspaceMemberDao()

        @Provides
        @SingleIn(AppScope::class)
        fun provideWorkspaceInvitationDao(db: WorkspaceRoomDatabase): WorkspaceInvitationDao = db.workspaceInvitationDao()

        @Provides
        @SingleIn(AppScope::class)
        fun provideWorkspaceRoleDao(db: WorkspaceRoomDatabase): WorkspaceRoleDao = db.workspaceRoleDao()

        @Provides
        @SingleIn(AppScope::class)
        fun provideWorkspacePermissionDao(db: WorkspaceRoomDatabase): WorkspacePermissionDao = db.workspacePermissionDao()

        @Provides
        @SingleIn(AppScope::class)
        fun provideWorkspaceModuleDao(db: WorkspaceRoomDatabase): WorkspaceModuleDao = db.workspaceModuleDao()

        @Provides
        @SingleIn(AppScope::class)
        fun provideUserInvitationDao(db: WorkspaceRoomDatabase): UserInvitationDao = db.userInvitationDao()
    }
}

