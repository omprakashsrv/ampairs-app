package com.ampairs.unit.aiops

import com.ampairs.common.aiops.AiOpsExecutor
import com.ampairs.common.aiops.AiOpsExecutorKey
import com.ampairs.common.aiops.Candidate
import com.ampairs.common.aiops.ExecResult
import com.ampairs.common.aiops.Finding
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.unit.data.repository.UnitRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Applies an approved unit-standardization [Candidate] by rewriting the unit's `short_name` through
 * [UnitRepository.updateUnit] — the normal offline-first write (persists `synced = false` and flags
 * `UNIT` pending-push), so the fix rides the existing `UnitSyncDelegate` to the server.
 *
 * The same path serves rollback: undo passes a `Candidate` whose `after` is the original value, so the
 * engine's [com.ampairs.aiops.engine.AiOpsUndoService] restores it through this one executor.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@AiOpsExecutorKey("unit.shortname")
class UnitShortNameExecutor(
    private val unitRepository: UnitRepository,
) : AiOpsExecutor {

    override val capabilityKey: String = UnitShortNameCapability.KEY

    override suspend fun apply(candidate: Candidate, finding: Finding): ExecResult {
        val target = candidate.after
        if (target.isNullOrBlank()) return ExecResult(false, "no target short name")
        val unit = unitRepository.getUnitById(finding.entityId)
            ?: return ExecResult(false, "unit ${finding.entityId} not found")
        if (unit.shortName == target) return ExecResult(true, "already \"$target\"")

        return unitRepository.updateUnit(unit.copy(shortName = target)).fold(
            onSuccess = { ExecResult(true, "unit short name → \"$target\"") },
            onFailure = { ExecResult(false, it.message ?: "unit update failed") },
        )
    }
}
