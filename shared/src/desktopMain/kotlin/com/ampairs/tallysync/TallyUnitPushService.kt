package com.ampairs.tallysync

import co.touchlab.kermit.Logger
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.tally.TallyRepository
import com.ampairs.tally.model.TallyMessage
import com.ampairs.tally.model.buildMastersImport
import com.ampairs.tally.renderTallyXml
import com.ampairs.tallysync.TallyProductMapper.toTallyUnit
import com.ampairs.unit.data.db.dao.UnitDao
import com.ampairs.unit.data.db.entity.UnitEntity
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

private val kermitLog = Logger.withTag("TallyUnitPush")

/**
 * Pushes locally-created units *into* Tally as `UNIT` masters — the first level of the master push
 * (see [TallyMastersPushService]), since [com.ampairs.tally.model.master.StockItem.baseUnits] must
 * resolve to an existing Tally unit. No dependency on any other master type.
 *
 * **Idempotency** mirrors [TallyInvoicePushService]: the Tally master id lands on
 * [UnitDao.setTallyRef] (skip any unit that already carries one), and the pushed id also rides in the
 * [AppPreferencesDataStore] pushed-unit-ids set as a secondary guard.
 */
@Inject
class TallyUnitPushService(
    private val unitDao: UnitDao,
    private val dataStore: AppPreferencesDataStore,
) {
    private suspend fun candidates(workspaceSlug: String): List<UnitEntity> {
        val pushedIds = dataStore.getTallyPushedUnitIds(workspaceSlug).first()
        return unitDao.getAllUnits().first().filter {
            it.refId.isNullOrBlank() && it.id !in pushedIds
        }
    }

    suspend fun pendingCount(workspaceSlug: String): Int = candidates(workspaceSlug).size

    suspend fun push(workspaceSlug: String, repo: TallyRepository, log: (String) -> Unit): TallyMasterPushResult {
        val candidates = candidates(workspaceSlug)
        if (candidates.isEmpty()) {
            log("Tally masters push: no new units to push")
            return TallyMasterPushResult()
        }
        log("Tally masters push: ${candidates.size} unit(s) to push")

        var pushed = 0
        var failed = 0
        val newlyPushed = mutableSetOf<String>()
        for (u in candidates) {
            val tallyUnit = u.toTallyUnit()
            val message = TallyMessage(unit = tallyUnit)
            log("  → request unit ${u.name}: ${renderTallyXml(buildMastersImport(listOf(message)))}")

            val outcome = runCatching { repo.importMasters(listOf(message)) }
            val result = outcome.getOrNull()
            val ok = outcome.isSuccess && result?.isSuccess == true
            if (ok) {
                pushed++
                newlyPushed += u.id
                val tallyRef = result?.lastMId?.trim()?.takeIf { it.isNotBlank() } ?: u.name
                unitDao.setTallyRef(u.id, tallyRef)
                log("  ✓ unit ${u.name} → Tally (ref $tallyRef)")
            } else {
                failed++
                val reason = outcome.exceptionOrNull()?.message
                    ?: result?.lineError?.takeIf { it.isNotBlank() }
                    ?: "Tally created nothing for unit ${u.name} — see 'IMPORTDATA reply' in the log"
                log("  ✗ unit ${u.name} — $reason")
                kermitLog.w { "Tally unit push failed for ${u.id}: $reason" }
            }
        }

        if (newlyPushed.isNotEmpty()) dataStore.addTallyPushedUnitIds(workspaceSlug, newlyPushed)
        log("Tally masters push: units complete — pushed=$pushed, failed=$failed")
        return TallyMasterPushResult(pushed = pushed, failed = failed)
    }
}
