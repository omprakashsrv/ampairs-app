package com.ampairs.workspace.db.dao

import androidx.room.*
import com.ampairs.workspace.db.entity.InstalledModuleEntity
import com.ampairs.workspace.db.entity.AvailableModuleEntity
import com.ampairs.workspace.db.entity.ModuleMenuItemEntity
import com.ampairs.workspace.db.entity.InstalledModuleWithMenuItems
import kotlinx.coroutines.flow.Flow

/**
 * Simple DAO for module database operations
 * Following the same patterns as other workspace DAOs
 */
@Dao
interface WorkspaceModuleDao {

    // Installed Modules Operations (workspace-scoped)

    @Transaction
    @Query("SELECT * FROM installed_module WHERE workspaceId = :workspaceId ORDER BY navigationIndex ASC, name ASC")
    fun getInstalledModulesWithMenuItemsFlow(workspaceId: String): Flow<List<InstalledModuleWithMenuItems>>

    @Transaction
    @Query("SELECT * FROM installed_module WHERE workspaceId = :workspaceId ORDER BY navigationIndex ASC, name ASC")
    suspend fun getInstalledModulesWithMenuItems(workspaceId: String): List<InstalledModuleWithMenuItems>

    @Query("SELECT * FROM installed_module WHERE workspaceId = :workspaceId ORDER BY name ASC")
    fun getInstalledModulesFlow(workspaceId: String): Flow<List<InstalledModuleEntity>>

    @Query("SELECT * FROM installed_module WHERE workspaceId = :workspaceId ORDER BY name ASC")
    suspend fun getInstalledModules(workspaceId: String): List<InstalledModuleEntity>

    @Query("SELECT * FROM installed_module WHERE id = :moduleId AND workspaceId = :workspaceId LIMIT 1")
    suspend fun getInstalledModule(moduleId: String, workspaceId: String): InstalledModuleEntity?

    @Query("SELECT * FROM installed_module WHERE workspaceId = :workspaceId AND status = 'ACTIVE' AND enabled = 1 ORDER BY name ASC")
    suspend fun getActiveModules(workspaceId: String): List<InstalledModuleEntity>

    @Query("SELECT * FROM installed_module WHERE workspaceId = :workspaceId AND (status != 'ACTIVE' OR enabled = 0) ORDER BY name ASC")
    suspend fun getInactiveModules(workspaceId: String): List<InstalledModuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstalledModule(module: InstalledModuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstalledModules(modules: List<InstalledModuleEntity>)

    @Update
    suspend fun updateInstalledModule(module: InstalledModuleEntity)

    @Query("UPDATE installed_module SET sync_state = :syncState, last_synced_at = :syncTime WHERE id = :moduleId AND workspaceId = :workspaceId")
    suspend fun updateInstalledModuleSyncState(moduleId: String, workspaceId: String, syncState: String, syncTime: Long?)

    @Delete
    suspend fun deleteInstalledModule(module: InstalledModuleEntity)

    @Query("DELETE FROM installed_module WHERE id = :moduleId AND workspaceId = :workspaceId")
    suspend fun deleteInstalledModuleById(moduleId: String, workspaceId: String)

    @Query("DELETE FROM installed_module WHERE workspaceId = :workspaceId")
    suspend fun deleteAllInstalledModules(workspaceId: String)

    // Module Menu Item Operations

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItems(menuItems: List<ModuleMenuItemEntity>)

    @Query("DELETE FROM module_menu_item WHERE moduleId = :moduleId")
    suspend fun deleteMenuItemsByModuleId(moduleId: String)

    @Query("DELETE FROM module_menu_item WHERE moduleId IN (SELECT id FROM installed_module WHERE workspaceId = :workspaceId)")
    suspend fun deleteAllMenuItemsByWorkspace(workspaceId: String)

    // Available Modules Operations

    @Query("SELECT * FROM available_module ORDER BY CASE WHEN featured = 1 THEN 0 ELSE 1 END, name ASC")
    suspend fun getAvailableModules(): List<AvailableModuleEntity>

    @Query("SELECT * FROM available_module WHERE category = :category ORDER BY CASE WHEN featured = 1 THEN 0 ELSE 1 END, name ASC")
    suspend fun getAvailableModulesByCategory(category: String): List<AvailableModuleEntity>

    @Query("SELECT * FROM available_module WHERE featured = 1 ORDER BY name ASC")
    suspend fun getFeaturedModules(): List<AvailableModuleEntity>

    @Query("SELECT * FROM available_module WHERE moduleCode = :moduleCode LIMIT 1")
    suspend fun getAvailableModule(moduleCode: String): AvailableModuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAvailableModule(module: AvailableModuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAvailableModules(modules: List<AvailableModuleEntity>)

    @Update
    suspend fun updateAvailableModule(module: AvailableModuleEntity)

    @Query("UPDATE available_module SET sync_state = :syncState, last_synced_at = :syncTime WHERE moduleCode = :moduleCode")
    suspend fun updateAvailableModuleSyncState(moduleCode: String, syncState: String, syncTime: Long?)

    @Query("DELETE FROM available_module")
    suspend fun deleteAllAvailableModules()

    // Utility Methods

    @Query("SELECT COUNT(*) FROM installed_module")
    suspend fun getInstalledModuleCount(): Int

    @Query("SELECT COUNT(*) FROM installed_module WHERE status = 'ACTIVE' AND enabled = 1")
    suspend fun getActiveModuleCount(): Int

    @Query("SELECT COUNT(*) FROM available_module")
    suspend fun getAvailableModuleCount(): Int

    @Query("SELECT DISTINCT category FROM available_module ORDER BY category")
    suspend fun getAvailableCategories(): List<String>

    // Check if module is installed
    @Query("SELECT EXISTS(SELECT 1 FROM installed_module WHERE workspaceId = :workspaceId AND moduleCode = :moduleCode AND status = 'ACTIVE' AND enabled = 1)")
    suspend fun isModuleInstalled(workspaceId: String, moduleCode: String): Boolean

    // Sync-related queries following existing patterns
    @Query("SELECT * FROM installed_module WHERE sync_state IN ('PENDING', 'ERROR') ORDER BY updated_at ASC")
    suspend fun getPendingSyncInstalledModules(): List<InstalledModuleEntity>

    @Query("SELECT * FROM available_module WHERE sync_state IN ('PENDING', 'ERROR') ORDER BY updated_at ASC")
    suspend fun getPendingSyncAvailableModules(): List<AvailableModuleEntity>
}