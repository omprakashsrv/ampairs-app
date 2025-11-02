package com.ampairs.workspace.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
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

@Database(
    entities = [
        WorkspaceEntity::class,
        WorkspaceMemberEntity::class,
        WorkspaceInvitationEntity::class,
        WorkspaceRoleEntity::class,
        WorkspacePermissionEntity::class,
        InstalledModuleEntity::class,
        AvailableModuleEntity::class,
        ModuleMenuItemEntity::class,
        UserInvitationEntity::class
    ],
    version = 8,  // Increment version due to adding ModuleMenuItemEntity and updating InstalledModuleEntity
    exportSchema = false
)
@ConstructedBy(WorkspaceRoomDatabaseConstructor::class)
abstract class WorkspaceRoomDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun workspaceMemberDao(): WorkspaceMemberDao
    abstract fun workspaceInvitationDao(): WorkspaceInvitationDao
    abstract fun workspaceRoleDao(): WorkspaceRoleDao
    abstract fun workspacePermissionDao(): WorkspacePermissionDao
    abstract fun workspaceModuleDao(): WorkspaceModuleDao
    abstract fun userInvitationDao(): UserInvitationDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object WorkspaceRoomDatabaseConstructor : RoomDatabaseConstructor<WorkspaceRoomDatabase>

