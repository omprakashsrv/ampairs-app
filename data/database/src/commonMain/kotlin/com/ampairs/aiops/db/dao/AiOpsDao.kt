package com.ampairs.aiops.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.ampairs.aiops.db.entity.AiOpsDecisionEntity
import com.ampairs.aiops.db.entity.AiOpsFeedbackEntity
import com.ampairs.aiops.db.entity.AiOpsFindingEntity
import kotlinx.coroutines.flow.Flow

/**
 * Single DAO for the AI Ops audit tables in the consolidated workspace DB. Injected into the
 * `feature/aiops` runner via `WorkspaceDatabaseDaoModule` (features depend on `data/database` for DAOs).
 */
@Dao
interface AiOpsDao {

    // ── Findings ────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFinding(finding: AiOpsFindingEntity)

    @Query("SELECT * FROM aiops_finding WHERE status = :status ORDER BY created_at DESC")
    fun observeFindingsByStatus(status: String): Flow<List<AiOpsFindingEntity>>

    @Query("SELECT * FROM aiops_finding WHERE id = :id")
    suspend fun getFinding(id: String): AiOpsFindingEntity?

    @Query("UPDATE aiops_finding SET status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun setFindingStatus(id: String, status: String, updatedAt: Long)

    // ── Decisions (audit) ───────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(decision: AiOpsDecisionEntity)

    @Query("SELECT * FROM aiops_decision WHERE id = :id")
    suspend fun getDecision(id: String): AiOpsDecisionEntity?

    @Query("SELECT * FROM aiops_decision WHERE entity_type = :entityType AND entity_id = :entityId ORDER BY created_at DESC")
    suspend fun getDecisionsForEntity(entityType: String, entityId: String): List<AiOpsDecisionEntity>

    /** Reactive audit feed for the activity screen — most recent decisions first. */
    @Query("SELECT * FROM aiops_decision ORDER BY created_at DESC LIMIT :limit")
    fun observeRecentDecisions(limit: Int): Flow<List<AiOpsDecisionEntity>>

    @Query("UPDATE aiops_decision SET reverted_at = :revertedAt WHERE id = :id")
    suspend fun markDecisionReverted(id: String, revertedAt: Long)

    // ── Feedback (learning) ───────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: AiOpsFeedbackEntity)
}
