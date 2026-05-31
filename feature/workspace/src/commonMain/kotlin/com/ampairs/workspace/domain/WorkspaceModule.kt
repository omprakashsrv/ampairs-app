package com.ampairs.workspace.domain

data class WorkspaceModule(
    val moduleCode: String,
    val name: String,
    val description: String?,
    val category: String,
    val version: String,
    val icon: String,
    val primaryColor: String,
    val featured: Boolean,
    val requiredTier: String,
    val rating: Double,
    val installedId: String?,
    val isInstalled: Boolean,
    val enabled: Boolean,
    val status: String,
    val navigationIndex: Int,
    val hasPendingSync: Boolean,
)
