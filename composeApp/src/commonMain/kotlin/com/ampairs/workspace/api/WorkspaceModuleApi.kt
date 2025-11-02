package com.ampairs.workspace.api

import com.ampairs.workspace.api.model.InstalledModule
import com.ampairs.workspace.api.model.AvailableModule
import com.ampairs.workspace.api.model.ModuleInstallationResponse
import com.ampairs.workspace.api.model.ModuleUninstallationResponse
import com.ampairs.workspace.api.model.ModuleDetailResponse

/**
 * Workspace Module API that exactly matches the web service calls
 * Maps directly to workspace-module.service.ts in the web app
 */
interface WorkspaceModuleApi {

    /**
     * Get all installed modules for the current workspace
     * GET /workspace/v1/modules
     * 
     * Matches: async getInstalledModules(): Promise<InstalledModule[]>
     */
    suspend fun getInstalledModules(workspaceId: String): Result<List<InstalledModule>>

    /**
     * Get available modules from the master catalog
     * GET /workspace/v1/modules/available?category={category}&featured={featured}
     * 
     * Matches: async getAvailableModules(category?: string, featured = false): Promise<AvailableModule[]>
     */
    suspend fun getAvailableModules(
        category: String? = null,
        featured: Boolean = false
    ): Result<List<AvailableModule>>

    /**
     * Install a module in the current workspace
     * POST /workspace/v1/modules/install/{moduleCode}
     * 
     * Matches: async installModule(moduleCode: string): Promise<ModuleInstallationResponse>
     */
    suspend fun installModule(workspaceId: String, moduleCode: String): Result<ModuleInstallationResponse>

    /**
     * Uninstall a module from the current workspace
     * DELETE /workspace/v1/modules/{moduleId}
     *
     * Matches: async uninstallModule(moduleId: string): Promise<ModuleUninstallationResponse>
     */
    suspend fun uninstallModule(workspaceId: String, moduleId: String): Result<ModuleUninstallationResponse>

    /**
     * Get detailed information about a specific module
     * GET /workspace/v1/modules/{moduleId}
     *
     * Returns comprehensive module details including configuration, analytics, and permissions
     */
    suspend fun getModuleDetails(workspaceId: String, moduleId: String): Result<ModuleDetailResponse>
}