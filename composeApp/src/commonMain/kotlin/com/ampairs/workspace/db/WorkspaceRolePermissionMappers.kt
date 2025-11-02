package com.ampairs.workspace.db

import com.ampairs.common.time.currentTimeMillis
import com.ampairs.workspace.api.model.WorkspacePermission
import com.ampairs.workspace.api.model.WorkspacePermissionResponse
import com.ampairs.workspace.api.model.WorkspaceRole
import com.ampairs.workspace.db.entity.WorkspacePermissionEntity
import com.ampairs.workspace.db.entity.WorkspaceRoleEntity
import kotlinx.serialization.json.Json

/**
 * Mappers for converting between API models and database entities
 * for offline-first storage of roles and permissions
 */

// WorkspaceRole API model to Entity mapping

fun WorkspaceRole.toEntity(userId: String, workspaceId: String): WorkspaceRoleEntity {
    return WorkspaceRoleEntity(
        id = this.name, // Use name as id since backend doesn't provide separate id
        user_id = userId,
        workspace_id = workspaceId,
        name = this.name,
        description = this.description,
        level = this.level,
        is_system_role = true, // All backend roles are system roles
        permissions = Json.encodeToString(this.manageableRoles), // Store manageable roles as permissions
        can_manage_members = this.manageableRoles.isNotEmpty(),
        can_edit_workspace = this.level >= 80, // ADMIN level and above
        can_delete_workspace = this.level >= 100, // OWNER level only
        created_at = "",
        updated_at = "",
        sync_state = "SYNCED",
        last_synced_at = currentTimeMillis(),
        local_updated_at = currentTimeMillis(),
        server_updated_at = currentTimeMillis()
    )
}

fun WorkspaceRoleEntity.toApiModel(): WorkspaceRole {
    val manageableRolesList = try {
        Json.decodeFromString<List<String>>(this.permissions)
    } catch (e: Exception) {
        emptyList()
    }

    return WorkspaceRole(
        name = this.name,
        displayName = this.name, // Use name as display name
        level = this.level,
        description = this.description,
        manageableRoles = manageableRolesList
    )
}

// Permission mapping functions

fun List<WorkspacePermissionResponse>.toPermissionEntities(
    userId: String,
    workspaceId: String
): List<WorkspacePermissionEntity> {
    // Store permissions directly as received from backend
    // Backend should handle grouping and structuring of permissions
    return this.mapIndexed { index, permission ->
        WorkspacePermissionEntity(
            user_id = userId,
            workspace_id = workspaceId,
            module = permission.name, // Use permission name directly from backend
            actions = Json.encodeToString(mapOf(permission.permissionName to true)), // Store individual permission
            description = permission.description,
            sync_state = "SYNCED",
            last_synced_at = currentTimeMillis(),
            local_updated_at = currentTimeMillis(),
            server_updated_at = currentTimeMillis()
        )
    }
}

fun List<WorkspacePermissionEntity>.toPermissionMap(): Map<String, Map<String, Boolean>> {
    return this.associate { entity ->
        val actions = try {
            Json.decodeFromString<Map<String, Boolean>>(entity.actions)
        } catch (e: Exception) {
            emptyMap()
        }
        entity.module to actions
    }
}

// Convert API response list to UI expected map format
fun convertPermissionsListToMap(permissionsList: List<WorkspacePermissionResponse>): Map<String, Map<String, Boolean>> {
    // The permissionsList contains ALL available permissions from the backend
    // Group them by module and convert to the UI expected format
    val groupedPermissions = permissionsList.groupBy { permission ->
        // Extract module from permission name (e.g., "MEMBER_VIEW" -> "member")
        val parts = permission.permissionName.split("_")
        if (parts.size > 1) parts[0].lowercase() else "general"
    }

    // Convert to UI format with all permissions set to true (available)
    return groupedPermissions.mapValues { (_, permissions) ->
        permissions.associate { permission ->
            // Extract action from permission name (e.g., "MEMBER_VIEW" -> "view")
            val action = permission.permissionName.split("_").drop(1).joinToString("_").lowercase()
            action.ifEmpty { "general" } to true
        }
    }
}

// Convert UI permission map back to backend enum set
fun convertPermissionMapToEnumSet(permissionMap: Map<String, Map<String, Boolean>>): Set<WorkspacePermission> {
    val permissions = mutableSetOf<WorkspacePermission>()

    permissionMap.forEach { (module, actions) ->
        actions.forEach { (action, enabled) ->
            if (enabled) {
                // Reconstruct the permission name (e.g., "member" + "view" -> "MEMBER_VIEW")
                val permissionName = "${module.uppercase()}_${action.uppercase()}"
                WorkspacePermission.fromString(permissionName)?.let { permissions.add(it) }
            }
        }
    }

    return permissions
}

// Batch conversion utilities

fun List<WorkspaceRole>.toEntities(userId: String, workspaceId: String): List<WorkspaceRoleEntity> {
    return this.map { it.toEntity(userId, workspaceId) }
}

fun List<WorkspaceRoleEntity>.toApiModels(): List<WorkspaceRole> {
    return this.map { it.toApiModel() }
}

// Sync state helpers

fun WorkspaceRoleEntity.markAsPendingUpload(pendingChanges: Map<String, Any> = emptyMap()): WorkspaceRoleEntity {
    return this.copy(
        sync_state = "PENDING_UPLOAD",
        local_updated_at = currentTimeMillis(),
        pending_changes = if (pendingChanges.isNotEmpty()) Json.encodeToString(pendingChanges) else this.pending_changes,
        retry_count = 0
    )
}

fun WorkspacePermissionEntity.markAsPendingUpload(pendingChanges: Map<String, Any> = emptyMap()): WorkspacePermissionEntity {
    return this.copy(
        sync_state = "PENDING_UPLOAD",
        local_updated_at = currentTimeMillis(),
        pending_changes = if (pendingChanges.isNotEmpty()) Json.encodeToString(pendingChanges) else this.pending_changes,
        retry_count = 0
    )
}

fun WorkspaceRoleEntity.markAsSynced(): WorkspaceRoleEntity {
    return this.copy(
        sync_state = "SYNCED",
        last_synced_at = currentTimeMillis(),
        pending_changes = "",
        conflict_data = "",
        retry_count = 0
    )
}

fun WorkspacePermissionEntity.markAsSynced(): WorkspacePermissionEntity {
    return this.copy(
        sync_state = "SYNCED",
        last_synced_at = currentTimeMillis(),
        pending_changes = "",
        conflict_data = "",
        retry_count = 0
    )
}