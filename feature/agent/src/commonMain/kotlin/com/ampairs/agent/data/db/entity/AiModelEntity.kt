package com.ampairs.agent.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One persisted on-device model catalog entry (the backend manifest row). Stored in the **app-scoped**
 * [com.ampairs.agent.data.db.AgentCatalogDatabase] — global, not per-workspace — because the model
 * files live in a global app directory and the same catalog applies across every workspace. Persisting
 * the manifest lets the app recognize, select, and load already-downloaded models when the backend is
 * unreachable (the bundled fallback catalog uses different ids/file names). Mirrors `AiModelResponse`;
 * [platforms] is comma-joined.
 */
@Entity(tableName = "ai_model_catalog")
data class AiModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val family: String,
    val parameterLabel: String,
    /** Pipeline slot from the manifest ("INTENT"/"CHAT"/"FALLBACK"); persisted so role survives offline. */
    val role: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String?,
    val requiredRamMb: Int,
    val backendId: String,
    /** Cloud transport adapter id (when backendId == "cloud"); empty for on-device models. */
    val transport: String = "",
    /** Provider-side model id the cloud transport sends; empty for on-device models. */
    val remoteModelId: String = "",
    /** Comma-joined platform codes from the manifest (empty = none). */
    val platforms: String,
    val recommended: Boolean,
)
