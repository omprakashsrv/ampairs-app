package com.ampairs.tallysync

import co.touchlab.kermit.Logger
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.entity.CategoryEntity
import com.ampairs.tally.TallyRepository
import com.ampairs.tally.model.TallyMessage
import com.ampairs.tally.model.buildMastersImport
import com.ampairs.tally.renderTallyXml
import com.ampairs.tallysync.TallyProductMapper.toTallyStockCategory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

private val kermitLog = Logger.withTag("TallyStockCategoryPush")

/**
 * Pushes locally-created stock categories *into* Tally as `STOCKCATEGORY` masters (see
 * [TallyMastersPushService]), since [com.ampairs.tally.model.master.StockItem.category] must resolve
 * to an existing Tally stock category. No dependency on any other master type.
 */
@Inject
class TallyStockCategoryPushService(
    private val categoryDao: CategoryDao,
    private val dataStore: AppPreferencesDataStore,
) {
    private suspend fun candidates(workspaceSlug: String): List<CategoryEntity> {
        val pushedIds = dataStore.getTallyPushedCategoryIds(workspaceSlug).first()
        return categoryDao.getCategories().filter {
            it.ref_id.isNullOrBlank() && it.active == 1 && it.soft_deleted == 0 && it.id !in pushedIds
        }
    }

    suspend fun pendingCount(workspaceSlug: String): Int = candidates(workspaceSlug).size

    suspend fun push(workspaceSlug: String, repo: TallyRepository, log: (String) -> Unit): TallyMasterPushResult {
        val candidates = candidates(workspaceSlug)
        if (candidates.isEmpty()) {
            log("Tally masters push: no new stock categories to push")
            return TallyMasterPushResult()
        }
        log("Tally masters push: ${candidates.size} stock category(ies) to push")

        var pushed = 0
        var failed = 0
        val newlyPushed = mutableSetOf<String>()
        for (c in candidates) {
            val message = TallyMessage(stockCategory = c.toTallyStockCategory())
            log("  → request stock category ${c.name}: ${renderTallyXml(buildMastersImport(listOf(message)))}")

            val outcome = runCatching { repo.importMasters(listOf(message)) }
            val result = outcome.getOrNull()
            val ok = outcome.isSuccess && result?.isSuccess == true
            if (ok) {
                pushed++
                newlyPushed += c.id
                val tallyRef = result?.lastMId?.trim()?.takeIf { it.isNotBlank() } ?: c.name
                categoryDao.setTallyRef(c.id, tallyRef)
                log("  ✓ stock category ${c.name} → Tally (ref $tallyRef)")
            } else {
                failed++
                val reason = outcome.exceptionOrNull()?.message
                    ?: result?.lineError?.takeIf { it.isNotBlank() }
                    ?: "Tally created nothing for stock category ${c.name} — see 'IMPORTDATA reply' in the log"
                log("  ✗ stock category ${c.name} — $reason")
                kermitLog.w { "Tally stock category push failed for ${c.id}: $reason" }
            }
        }

        if (newlyPushed.isNotEmpty()) dataStore.addTallyPushedCategoryIds(workspaceSlug, newlyPushed)
        log("Tally masters push: stock categories complete — pushed=$pushed, failed=$failed")
        return TallyMasterPushResult(pushed = pushed, failed = failed)
    }
}
