package com.ampairs.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.ampairs.agent.data.db.dao.AiModelDao
import com.ampairs.agent.data.db.entity.AiModelEntity
import com.ampairs.auth.db.dao.UserDao
import com.ampairs.auth.db.dao.UserSessionDao
import com.ampairs.auth.db.dao.UserTokenDao
import com.ampairs.auth.db.entity.UserEntity
import com.ampairs.auth.db.entity.UserSessionEntity
import com.ampairs.auth.db.entity.UserTokenEntity
import com.ampairs.workspace.db.dao.UserInvitationDao
import com.ampairs.workspace.db.dao.WorkspaceDao
import com.ampairs.workspace.db.dao.WorkspaceInvitationDao
import com.ampairs.workspace.db.dao.WorkspaceMemberDao
import com.ampairs.workspace.db.dao.WorkspaceModuleDao
import com.ampairs.workspace.db.dao.WorkspacePermissionDao
import com.ampairs.workspace.db.dao.WorkspaceRoleDao
import com.ampairs.workspace.db.entity.AvailableModuleEntity
import com.ampairs.workspace.db.entity.InstalledModuleEntity
import com.ampairs.workspace.db.entity.ModuleMenuItemEntity
import com.ampairs.workspace.db.entity.UserInvitationEntity
import com.ampairs.workspace.db.entity.WorkspaceEntity
import com.ampairs.workspace.db.entity.WorkspaceInvitationEntity
import com.ampairs.workspace.db.entity.WorkspaceMemberEntity
import com.ampairs.workspace.db.entity.WorkspacePermissionEntity
import com.ampairs.workspace.db.entity.WorkspaceRoleEntity

/**
 * Consolidated AppScope database — one file (`ampairs_app.db`) for all app-lifetime data that
 * exists independently of the selected workspace: auth (user/token/session), the workspace
 * registry, and the agent model catalog.
 *
 * Replaces the former `AuthRoomDatabase` (auth.db), `WorkspaceRoomDatabase` (workspace.db) and
 * `AgentCatalogDatabase` (agent_catalog.db). The legacy classes are deleted (a DAO may only have
 * ONE Room-generated impl per app classpath). On upgrade the consolidated file is created fresh and
 * server-authoritative data re-syncs; the old per-module files are left in place.
 *
 * Migration policy:
 * - NEVER add `fallbackToDestructiveMigration` to this database — it now carries durable auth data.
 * - The agent catalog table (`ai_models` / [AiModelEntity]) is a re-pullable cache: schema changes
 *   to it ship as a trivial DROP TABLE + CREATE TABLE migration; the manifest re-pulls on launch.
 */
@Database(
    entities = [
        // auth (was auth.db v3)
        UserEntity::class,
        UserTokenEntity::class,
        UserSessionEntity::class,
        // workspace registry (was workspace.db v8)
        WorkspaceEntity::class,
        WorkspaceMemberEntity::class,
        WorkspaceInvitationEntity::class,
        WorkspaceRoleEntity::class,
        WorkspacePermissionEntity::class,
        InstalledModuleEntity::class,
        AvailableModuleEntity::class,
        ModuleMenuItemEntity::class,
        UserInvitationEntity::class,
        // agent model catalog (was agent_catalog.db v3) — disposable cache, see class KDoc
        AiModelEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(AmpairsAppDatabaseConstructor::class)
abstract class AmpairsAppDatabase : RoomDatabase() {
    // auth
    abstract fun userDao(): UserDao
    abstract fun userTokenDao(): UserTokenDao
    abstract fun userSessionDao(): UserSessionDao

    // workspace registry
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun workspaceMemberDao(): WorkspaceMemberDao
    abstract fun workspaceInvitationDao(): WorkspaceInvitationDao
    abstract fun workspaceRoleDao(): WorkspaceRoleDao
    abstract fun workspacePermissionDao(): WorkspacePermissionDao
    abstract fun workspaceModuleDao(): WorkspaceModuleDao
    abstract fun userInvitationDao(): UserInvitationDao

    // agent model catalog
    abstract fun aiModelDao(): AiModelDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AmpairsAppDatabaseConstructor : RoomDatabaseConstructor<AmpairsAppDatabase> {
    override fun initialize(): AmpairsAppDatabase
}
