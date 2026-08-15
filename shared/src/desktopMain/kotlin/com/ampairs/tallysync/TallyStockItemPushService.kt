package com.ampairs.tallysync

import co.touchlab.kermit.Logger
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.product.db.entity.ProductEntity
import com.ampairs.tally.TallyRepository
import com.ampairs.tally.model.TallyMessage
import com.ampairs.tally.model.buildMastersImport
import com.ampairs.tally.renderTallyXml
import com.ampairs.tallysync.TallyProductMapper.toTallyStockItem
import com.ampairs.unit.data.db.dao.UnitDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

private val kermitLog = Logger.withTag("TallyStockItemPush")

/**
 * Pushes locally-created products *into* Tally as `STOCKITEM` masters — the last master level (see
 * [TallyMastersPushService]), pushed after invoices assume it exists
 * ([TallyInvoiceVoucherMapper]'s documented precondition). Depends on
 * [TallyStockGroupPushService]/[TallyStockCategoryPushService]/[TallyUnitPushService] having already
 * run: a product whose group/category/unit isn't yet in Tally is skipped per-record (siblings whose
 * references resolved still push), since `PARENT`/`CATEGORY`/`BASEUNITS` must all resolve.
 *
 * **Idempotency** mirrors [TallyInvoicePushService]: the Tally stock-item GUID lands on
 * [ProductDao.setTallyRef] (skip any product that already carries one), and the pushed id also rides
 * in the [AppPreferencesDataStore] pushed-product-ids set.
 */
@Inject
class TallyStockItemPushService(
    private val productDao: ProductDao,
    private val groupDao: GroupDao,
    private val categoryDao: CategoryDao,
    private val unitDao: UnitDao,
    private val dataStore: AppPreferencesDataStore,
) {
    private suspend fun candidates(workspaceSlug: String): List<ProductEntity> {
        val pushedIds = dataStore.getTallyPushedProductIds(workspaceSlug).first()
        return productDao.getProducts().filter {
            it.ref_id.isNullOrBlank() && it.active == 1 && it.soft_deleted == 0 && it.id !in pushedIds
        }
    }

    suspend fun pendingCount(workspaceSlug: String): Int = candidates(workspaceSlug).size

    suspend fun push(workspaceSlug: String, repo: TallyRepository, log: (String) -> Unit): TallyMasterPushResult {
        val groupsById = groupDao.getGroups().associateBy { it.id }
        val categoriesById = categoryDao.getCategories().associateBy { it.id }
        val unitsById = unitDao.getAllUnits().first().associateBy { it.id }

        val candidates = candidates(workspaceSlug)
        if (candidates.isEmpty()) {
            log("Tally masters push: no new products to push")
            return TallyMasterPushResult()
        }
        log("Tally masters push: ${candidates.size} product(s) to push")

        var pushed = 0
        var failed = 0
        var skipped = 0
        val newlyPushed = mutableSetOf<String>()
        for (p in candidates) {
            val group = p.group_id?.let { groupsById[it] }
            val category = p.category_id?.let { categoriesById[it] }
            val unit = p.base_unit?.let { unitsById[it] }

            val unresolved = buildList {
                if (p.group_id != null && group?.ref_id.isNullOrBlank()) add("stock group")
                if (p.category_id != null && category?.ref_id.isNullOrBlank()) add("stock category")
                if (p.base_unit != null && unit?.refId.isNullOrBlank()) add("unit")
            }
            if (unresolved.isNotEmpty()) {
                skipped++
                log("  – product ${p.name} skipped — ${unresolved.joinToString(", ")} not yet in Tally")
                continue
            }

            val stockItem = p.toTallyStockItem(
                groupName = group?.name,
                categoryName = category?.name,
                unitName = unit?.shortName?.ifBlank { unit.name },
            )
            val message = TallyMessage(stockItem = stockItem)
            log("  → request product ${p.name}: ${renderTallyXml(buildMastersImport(listOf(message)))}")

            val outcome = runCatching { repo.importMasters(listOf(message)) }
            val result = outcome.getOrNull()
            val ok = outcome.isSuccess && result?.isSuccess == true
            if (ok) {
                pushed++
                newlyPushed += p.id
                val tallyRef = result?.lastMId?.trim()?.takeIf { it.isNotBlank() } ?: p.name
                productDao.setTallyRef(p.id, tallyRef)
                log("  ✓ product ${p.name} → Tally (ref $tallyRef)")
            } else {
                failed++
                val reason = outcome.exceptionOrNull()?.message
                    ?: result?.lineError?.takeIf { it.isNotBlank() }
                    ?: "Tally created nothing for product ${p.name} — see 'IMPORTDATA reply' in the log"
                log("  ✗ product ${p.name} — $reason")
                kermitLog.w { "Tally stock item push failed for ${p.id}: $reason" }
            }
        }

        if (newlyPushed.isNotEmpty()) dataStore.addTallyPushedProductIds(workspaceSlug, newlyPushed)
        log("Tally masters push: products complete — pushed=$pushed, failed=$failed, skipped=$skipped")
        return TallyMasterPushResult(pushed = pushed, failed = failed, skipped = skipped)
    }
}
