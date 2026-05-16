package com.ampairs.workspace

import com.ampairs.workspace.api.WorkspaceApi
import com.ampairs.workspace.api.WorkspaceApiImpl
import com.ampairs.workspace.api.WorkspaceMemberApi
import com.ampairs.workspace.api.WorkspaceMemberApiImpl
import com.ampairs.workspace.api.WorkspaceInvitationApi
import com.ampairs.workspace.api.WorkspaceInvitationApiImpl
import com.ampairs.workspace.api.WorkspaceModuleApi
import com.ampairs.workspace.api.WorkspaceModuleApiImpl
import com.ampairs.workspace.api.UserInvitationApi
import com.ampairs.workspace.api.UserInvitationApiImpl
import com.ampairs.workspace.db.WorkspaceRepository
import com.ampairs.workspace.db.OfflineFirstWorkspaceRepository
import com.ampairs.workspace.db.WorkspaceMemberRepository
import com.ampairs.workspace.db.OfflineFirstWorkspaceMemberRepository
import com.ampairs.workspace.db.WorkspaceInvitationRepository
import com.ampairs.workspace.db.OfflineFirstWorkspaceInvitationRepository
import com.ampairs.workspace.db.OfflineFirstRolesPermissionsRepository
import com.ampairs.workspace.db.WorkspaceModuleRepository
import com.ampairs.workspace.db.UserInvitationRepository
import com.ampairs.workspace.store.WorkspaceStoreFactory
import com.ampairs.workspace.store.WorkspaceMemberStoreFactory
import com.ampairs.workspace.store.WorkspaceInvitationStoreFactory
import com.ampairs.workspace.store.WorkspaceRolesStoreFactory
import com.ampairs.workspace.store.WorkspacePermissionsStoreFactory
import com.ampairs.workspace.store.WorkspaceMemberUpdateStoreFactory
import com.ampairs.workspace.store.WorkspaceModuleStoreFactory
import com.ampairs.workspace.store.UserInvitationStoreFactory
import com.ampairs.workspace.store.WorkspaceStore
import com.ampairs.workspace.store.WorkspaceMemberStore
import com.ampairs.workspace.store.WorkspaceInvitationStore
import com.ampairs.workspace.store.WorkspaceRolesStore
import com.ampairs.workspace.store.WorkspacePermissionsStore
import com.ampairs.workspace.store.WorkspaceMemberUpdateStore
import com.ampairs.workspace.store.UserInvitationStore
import com.ampairs.workspace.viewmodel.WorkspaceCreateViewModel
import com.ampairs.workspace.viewmodel.WorkspaceListViewModel
import com.ampairs.workspace.viewmodel.WorkspaceMembersViewModel
import com.ampairs.workspace.viewmodel.MemberDetailsViewModel
import com.ampairs.workspace.viewmodel.WorkspaceInvitationsViewModel
import com.ampairs.workspace.viewmodel.WorkspaceModulesViewModel
import com.ampairs.workspace.context.WorkspaceContextManager
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

fun workspaceModule() = module {

    // Database (provided by platform-specific modules)
    single { get<com.ampairs.workspace.db.WorkspaceRoomDatabase>().workspaceDao() }
    single { get<com.ampairs.workspace.db.WorkspaceRoomDatabase>().workspaceMemberDao() }
    single { get<com.ampairs.workspace.db.WorkspaceRoomDatabase>().workspaceInvitationDao() }
    single { get<com.ampairs.workspace.db.WorkspaceRoomDatabase>().workspaceRoleDao() }
    single { get<com.ampairs.workspace.db.WorkspaceRoomDatabase>().workspacePermissionDao() }
    single { get<com.ampairs.workspace.db.WorkspaceRoomDatabase>().workspaceModuleDao() }
    single { get<com.ampairs.workspace.db.WorkspaceRoomDatabase>().userInvitationDao() }

    // APIs
    singleOf(::WorkspaceApiImpl) bind WorkspaceApi::class
    singleOf(::WorkspaceMemberApiImpl) bind WorkspaceMemberApi::class
    singleOf(::WorkspaceInvitationApiImpl) bind WorkspaceInvitationApi::class
    singleOf(::WorkspaceModuleApiImpl) bind WorkspaceModuleApi::class
    singleOf(::UserInvitationApiImpl) bind UserInvitationApi::class
    
    // Repositories - now powered by Store5 for offline-first architecture
    single<WorkspaceRepository> { WorkspaceRepository(get(), get(), get()) }
    single { OfflineFirstWorkspaceRepository(get(), get(), get(), get(named("workspaceStore"))) } // Store5 workspace repository
    single<WorkspaceMemberRepository> { WorkspaceMemberRepository(get(), get(), get()) }
    single { WorkspaceInvitationRepository(get(), get()) } // Legacy invitation management repository (kept for compatibility)
    single { OfflineFirstWorkspaceInvitationRepository(get(), get(), get(named("workspaceInvitationStore")), get()) } // Store5 invitation repository
    single { OfflineFirstRolesPermissionsRepository(get(), get(), get(), get()) } // Roles & permissions offline-first repo
    single { WorkspaceModuleRepository(get(), get(), get()) } // Module repository
    single { UserInvitationRepository(get(named("userInvitationStore")), get()) } // User invitation repository

    // Store5 Factories for proper offline-first architecture
    single { WorkspaceStoreFactory(get(), get()) }
    single { WorkspaceMemberStoreFactory(get(), get()) }
    single { WorkspaceInvitationStoreFactory(get(), get()) }
    single { WorkspaceRolesStoreFactory(get(), get()) }
    single { WorkspacePermissionsStoreFactory(get(), get()) }
    single { WorkspaceMemberUpdateStoreFactory(get(), get()) }
    single { WorkspaceModuleStoreFactory(get(), get()) }
    single { UserInvitationStoreFactory(get(), get()) }
    
    // Store instances with qualifiers to prevent conflicts
    single<WorkspaceStore>(named("workspaceStore")) { get<WorkspaceStoreFactory>().create() }
    single<WorkspaceMemberStore>(named("workspaceMemberStore")) { get<WorkspaceMemberStoreFactory>().create() }
    single<WorkspaceInvitationStore>(named("workspaceInvitationStore")) { get<WorkspaceInvitationStoreFactory>().create() }
    single<WorkspaceRolesStore>(named("workspaceRolesStore")) { get<WorkspaceRolesStoreFactory>().create() }
    single<WorkspacePermissionsStore>(named("workspacePermissionsStore")) { get<WorkspacePermissionsStoreFactory>().create() }
    single<WorkspaceMemberUpdateStore>(named("workspaceMemberUpdateStore")) { get<WorkspaceMemberUpdateStoreFactory>().create() }
    single<UserInvitationStore>(named("userInvitationStore")) { get<UserInvitationStoreFactory>().create() }

    // Workspace Database Manager for switching contexts
    single { WorkspaceDatabaseManager() }

    // Workspace Context Manager (singleton)
    single { WorkspaceContextManager.getInstance() }

    // ViewModels with parameter support
    viewModel { WorkspaceListViewModel(get<OfflineFirstWorkspaceRepository>(), get(), get(), get(), get(), get()) }
    viewModelOf(::WorkspaceCreateViewModel)

    // Member and invitation ViewModels with workspaceId parameter
    viewModel { (workspaceId: String) -> WorkspaceMembersViewModel(workspaceId, get(named("workspaceMemberStore")), get(), get()) }
    viewModel { (workspaceId: String, memberId: String) -> MemberDetailsViewModel(workspaceId, memberId, get(named("workspaceMemberStore")), get(), get(), get(named("workspaceRolesStore")), get(named("workspacePermissionsStore")), get()) }
    viewModel { (workspaceId: String) -> WorkspaceInvitationsViewModel(workspaceId, get<OfflineFirstWorkspaceInvitationRepository>(), get<OfflineFirstRolesPermissionsRepository>()) }

    // Module management ViewModels
    viewModel { (workspaceId: String?) -> WorkspaceModulesViewModel(get(), workspaceId) } // ViewModel matching web
}