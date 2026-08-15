package com.ampairs.tallysync

import co.touchlab.kermit.Logger
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.entity.GroupEntity
import com.ampairs.tally.TallyRepository
import com.ampairs.tally.model.TallyMessage
import com.ampairs.tally.model.buildMastersImport
import com.ampairs.tally.renderTallyXml
import com.ampairs.tallysync.TallyProductMapper.toTallyStockGroup
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

private val kermitLog = Logger.withTag("TallyStockGroupPush")

/**
 * Pushes locally-created stock groups *into* Tally as `STOCKGROUP` masters (see
 * [TallyMastersPushService]), since [com.ampairs.tally.model.master.StockItem.parent] must resolve
 * to an existing Tally stock group. No dependency on any other master type.
 */
@Inject
class TallyStockGroupPushService(
    private val groupDao: GroupDao,
    private val dataStore: AppPreferencesDataStore,
) {
    private suspend fun candidates(workspaceSlug: String): List<GroupEntity> {
        val pushedIds = dataStore.getTallyPushedGroupIds(workspaceSlug).first()
        return groupDao.getGroups().filter {
            it.ref_id.isNullOrBlank() && it.active == 1 && it.soft_deleted == 0 && it.id !in pushedIds
        }
    }

    suspend fun pendingCount(workspaceSlug: String): Int = candidates(workspaceSlug).size

    suspend fun push(workspaceSlug: String, repo: TallyRepository, log: (String) -> Unit): TallyMasterPushResult {
        val candidates = candidates(workspaceSlug)
        if (candidates.isEmpty()) {
            log("Tally masters push: no new stock groups to push")
            return TallyMasterPushResult()
        }
        log("Tally masters push: ${candidates.size} stock group(s) to push")

        var pushed = 0
        var failed = 0
        val newlyPushed = mutableSetOf<String>()
        for (g in candidates) {
            val message = TallyMessage(stockGroup = g.toTallyStockGroup())
            log("  → request stock group ${g.name}: ${renderTallyXml(buildMastersImport(listOf(message)))}")

            val outcome = runCatching { repo.importMasters(listOf(message)) }
            val result = outcome.getOrNull()
            val ok = outcome.isSuccess && result?.isSuccess == true
            if (ok) {
                pushed++
                newlyPushed += g.id
                val tallyRef = result?.lastMId?.trim()?.takeIf { it.isNotBlank() } ?: g.name
                groupDao.setTallyRef(g.id, tallyRef)
                log("  ✓ stock group ${g.name} → Tally (ref $tallyRef)")
            } else {
                failed++
                val reason = outcome.exceptionOrNull()?.message
                    ?: result?.lineError?.takeIf { it.isNotBlank() }
                    ?: "Tally created nothing for stock group ${g.name} — see 'IMPORTDATA reply' in the log"
                log("  ✗ stock group ${g.name} — $reason")
                kermitLog.w { "Tally stock group push failed for ${g.id}: $reason" }
            }
        }

        if (newlyPushed.isNotEmpty()) dataStore.addTallyPushedGroupIds(workspaceSlug, newlyPushed)
        log("Tally masters push: stock groups complete — pushed=$pushed, failed=$failed")
        return TallyMasterPushResult(pushed = pushed, failed = failed)
    }
}
