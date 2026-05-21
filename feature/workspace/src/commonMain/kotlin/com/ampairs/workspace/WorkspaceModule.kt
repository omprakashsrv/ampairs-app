package com.ampairs.workspace

import com.ampairs.auth.api.AuthApi
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.config.ConfigurationManager
import com.ampairs.common.di.AppScope
import com.ampairs.event.EventManagerFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import com.ampairs.workspace.db.WorkspaceRoomDatabase
import com.ampairs.workspace.db.dao.UserInvitationDao
import com.ampairs.workspace.db.dao.WorkspaceDao
import com.ampairs.workspace.db.dao.WorkspaceInvitationDao
import com.ampairs.workspace.db.dao.WorkspaceMemberDao
import com.ampairs.workspace.db.dao.WorkspaceModuleDao
import com.ampairs.workspace.db.dao.WorkspacePermissionDao
import com.ampairs.workspace.db.dao.WorkspaceRoleDao
import com.ampairs.workspace.store.UserInvitationStore
import com.ampairs.workspace.store.UserInvitationStoreFactory
import com.ampairs.workspace.store.WorkspaceInvitationStore
import com.ampairs.workspace.store.WorkspaceInvitationStoreFactory
import com.ampairs.workspace.store.WorkspaceMemberStore
import com.ampairs.workspace.store.WorkspaceMemberStoreFactory
import com.ampairs.workspace.store.WorkspaceMemberUpdateStore
import com.ampairs.workspace.store.WorkspaceMemberUpdateStoreFactory
import com.ampairs.workspace.store.WorkspacePermissionsStore
import com.ampairs.workspace.store.WorkspacePermissionsStoreFactory
import com.ampairs.workspace.store.WorkspaceRolesStore
import com.ampairs.workspace.store.WorkspaceRolesStoreFactory
import com.ampairs.workspace.store.WorkspaceStore
import com.ampairs.workspace.store.WorkspaceStoreFactory
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Provides EventManagerProvider that bridges DI with the static EventManagerFactory.
 */
@ContributesTo(AppScope::class)
interface WorkspaceEventModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideEventManagerProvider(
            engine: HttpClientEngine,
            tokenRepository: TokenRepository,
            authApi: AuthApi
        ): EventManagerProvider {
            val httpClient = HttpClient(engine)
            return EventManagerProvider { workspaceId, userId, deviceId ->
                EventManagerFactory.getOrCreate(
                    workspaceId = workspaceId,
                    userId = userId,
                    deviceId = deviceId,
                    httpClient = httpClient,
                    tokenProvider = { tokenRepository.getAccessToken() ?: "" },
                    tokenRefresher = { authApi.refreshToken().data != null },
                    baseUrl = ConfigurationManager.apiBaseUrl
                )
            }
        }
    }
}

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

/**
 * Metro bindings for Store5 store instances (created from factory singletons).
 */
@ContributesTo(AppScope::class)
interface WorkspaceStoreModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideWorkspaceStore(factory: WorkspaceStoreFactory): WorkspaceStore = factory.create()

        @Provides
        @SingleIn(AppScope::class)
        fun provideWorkspaceMemberStore(factory: WorkspaceMemberStoreFactory): WorkspaceMemberStore = factory.create()

        @Provides
        @SingleIn(AppScope::class)
        fun provideWorkspaceInvitationStore(factory: WorkspaceInvitationStoreFactory): WorkspaceInvitationStore = factory.create()

        @Provides
        @SingleIn(AppScope::class)
        fun provideWorkspaceRolesStore(factory: WorkspaceRolesStoreFactory): WorkspaceRolesStore = factory.create()

        @Provides
        @SingleIn(AppScope::class)
        fun provideWorkspacePermissionsStore(factory: WorkspacePermissionsStoreFactory): WorkspacePermissionsStore = factory.create()

        @Provides
        @SingleIn(AppScope::class)
        fun provideWorkspaceMemberUpdateStore(factory: WorkspaceMemberUpdateStoreFactory): WorkspaceMemberUpdateStore = factory.create()

        @Provides
        @SingleIn(AppScope::class)
        fun provideUserInvitationStore(factory: UserInvitationStoreFactory): UserInvitationStore = factory.create()
    }
}
