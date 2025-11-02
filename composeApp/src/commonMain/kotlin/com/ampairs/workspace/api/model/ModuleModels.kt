package com.ampairs.workspace.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Simple API models that exactly match the web implementation
 * These directly correspond to the TypeScript interfaces in workspace-module.service.ts
 */

@Serializable
data class ModuleMenuItem(
    @SerialName("id") val id: String,
    @SerialName("label") val label: String,
    @SerialName("route_path") val routePath: String,
    @SerialName("icon") val icon: String,
    @SerialName("order") val order: Int,
    @SerialName("is_default") val isDefault: Boolean = false // Optional with default value
)

@Serializable
data class ModuleRouteInfo(
    @SerialName("base_path") val basePath: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("icon_name") val iconName: String,
    @SerialName("menu_items") val menuItems: List<ModuleMenuItem>
)

@Serializable
data class InstalledModule(
    @SerialName("id") val id: String,
    val workspaceId: String = "", // Set from current workspace context, not from API
    @SerialName("module_code") val moduleCode: String,
    @SerialName("name") val name: String,
    @SerialName("category") val category: String,
    @SerialName("version") val version: String,
    @SerialName("status") val status: String, // ACTIVE | INSTALLED | INACTIVE
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("installed_at") val installedAt: String,
    @SerialName("icon") val icon: String,
    @SerialName("primary_color") val primaryColor: String,
    @SerialName("health_score") val healthScore: Double? = null,
    @SerialName("needs_attention") val needsAttention: Boolean? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("route_info") val routeInfo: ModuleRouteInfo,
    @SerialName("navigation_index") val navigationIndex: Int
)

@Serializable
data class AvailableModule(
    @SerialName("module_code") val moduleCode: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("category") val category: String,
    @SerialName("version") val version: String,
    @SerialName("rating") val rating: Double,
    @SerialName("install_count") val installCount: Int,
    @SerialName("complexity") val complexity: String,
    @SerialName("icon") val icon: String,
    @SerialName("primary_color") val primaryColor: String,
    @SerialName("featured") val featured: Boolean,
    @SerialName("required_tier") val requiredTier: String,
    @SerialName("size_mb") val sizeMb: Int,
)

@Serializable
data class ModuleInstallationResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("module_id") val moduleId: String,
    @SerialName("module_code") val moduleCode: String,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("message") val message: String,
    @SerialName("installed_at") val installedAt: String,
)

@Serializable
data class ModuleUninstallationResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("module_id") val moduleId: String,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("message") val message: String,
    @SerialName("uninstalled_at") val uninstalledAt: String,
)

@Serializable
data class ModuleDetailResponse(
    @SerialName("module_id") val moduleId: String,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("module_info") val moduleInfo: ModuleInfoResponse,
    @SerialName("configuration") val configuration: ModuleConfigurationResponse,
    @SerialName("analytics") val analytics: ModuleAnalyticsResponse,
    @SerialName("permissions") val permissions: ModulePermissionsResponse,
    @SerialName("health_score") val healthScore: Double,
    @SerialName("needs_attention") val needsAttention: Boolean,
)

@Serializable
data class ModuleInfoResponse(
    @SerialName("name") val name: String,
    @SerialName("category") val category: String,
    @SerialName("description") val description: String,
    @SerialName("version") val version: String,
    @SerialName("status") val status: String,
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("installed_at") val installedAt: String,
    @SerialName("last_updated") val lastUpdated: String? = null,
)

@Serializable
data class ModuleConfigurationResponse(
    @SerialName("auto_sync") val autoSync: Boolean,
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean,
    @SerialName("custom_fields") val customFields: List<String> = emptyList(),
)

@Serializable
data class ModuleAnalyticsResponse(
    @SerialName("daily_active_users") val dailyActiveUsers: Int,
    @SerialName("monthly_access") val monthlyAccess: Int,
    @SerialName("average_session_duration") val averageSessionDuration: String,
)

@Serializable
data class ModulePermissionsResponse(
    @SerialName("can_configure") val canConfigure: Boolean,
    @SerialName("can_uninstall") val canUninstall: Boolean,
    @SerialName("can_view_analytics") val canViewAnalytics: Boolean,
)