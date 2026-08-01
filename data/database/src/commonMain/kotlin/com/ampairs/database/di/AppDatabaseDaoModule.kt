package com.ampairs.database.di

import com.ampairs.agent.data.db.dao.AiModelDao
import com.ampairs.common.di.AppScope
import com.ampairs.database.AmpairsAppDatabase
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
 * Sources every AppScope DAO from the consolidated [AmpairsAppDatabase]. DAO types and scoping are
 * unchanged from the old per-feature modules, so no injection site needs to know about the
 * consolidation.
 *
 * The auth DAOs (UserDao/UserTokenDao/UserSessionDao) are deliberately NOT provided here even
 * though this module also owns those tables — they live in `:shared`'s `EcomAuthAppDaoModule`
 * instead. `:shared-ecom` depends on this module (for the storefront `@Database` class
 * definitions) but does NOT depend on `:shared`, and provides those same DAOs from its own
 * `StorefrontAppDatabase`. Since Metro merges every `@ContributesTo(AppScope::class)` visible on
 * the classpath, keeping those providers here would duplicate-bind them in the storefront apps'
 * graph.
 */
@ContributesTo(AppScope::class)
interface AppDatabaseDaoModule {
    companion object {
        // workspace registry
        @Provides @SingleIn(AppScope::class)
        fun provideWorkspaceDao(db: AmpairsAppDatabase): WorkspaceDao = db.workspaceDao()

        @Provides @SingleIn(AppScope::class)
        fun provideWorkspaceMemberDao(db: AmpairsAppDatabase): WorkspaceMemberDao = db.workspaceMemberDao()

        @Provides @SingleIn(AppScope::class)
        fun provideWorkspaceInvitationDao(db: AmpairsAppDatabase): WorkspaceInvitationDao = db.workspaceInvitationDao()

        @Provides @SingleIn(AppScope::class)
        fun provideWorkspaceRoleDao(db: AmpairsAppDatabase): WorkspaceRoleDao = db.workspaceRoleDao()

        @Provides @SingleIn(AppScope::class)
        fun provideWorkspacePermissionDao(db: AmpairsAppDatabase): WorkspacePermissionDao = db.workspacePermissionDao()

        @Provides @SingleIn(AppScope::class)
        fun provideWorkspaceModuleDao(db: AmpairsAppDatabase): WorkspaceModuleDao = db.workspaceModuleDao()

        @Provides @SingleIn(AppScope::class)
        fun provideUserInvitationDao(db: AmpairsAppDatabase): UserInvitationDao = db.userInvitationDao()

        // agent model catalog
        @Provides
        fun provideAiModelDao(db: AmpairsAppDatabase): AiModelDao = db.aiModelDao()
    }
}
