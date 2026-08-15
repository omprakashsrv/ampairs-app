package com.ampairs.tallysync

import co.touchlab.kermit.Logger
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.customer.data.db.CustomerGroupDao
import com.ampairs.customer.data.db.CustomerGroupEntity
import com.ampairs.supplier.data.db.SupplierDao
import com.ampairs.tally.TallyRepository
import com.ampairs.tally.model.TallyMessage
import com.ampairs.tally.model.buildMastersImport
import com.ampairs.tally.renderTallyXml
import com.ampairs.tallysync.TallyCustomerMapper.toTallyGroup
import com.ampairs.tallysync.TallyCustomerMapper.toTallySupplierGroup
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

private val kermitLog = Logger.withTag("TallyAccountGroupPush")

/**
 * Pushes custom customer/supplier account sub-groups *into* Tally as `GROUP` masters (see
 * [TallyMastersPushService]). Most ledgers need no group push at all — Tally ships "Sundry Debtors"/
 * "Sundry Creditors" as built-in reserved primaries, and [TallyLedgerPushService] points a ledger with
 * no custom group directly at one of those. This service only handles the minority case: a customer
 * with a custom [com.ampairs.customer.data.db.CustomerGroupEntity], or a supplier with a non-blank
 * `supplier_group` free-text name.
 *
 * **Idempotency** differs by source: customer groups anchor on [CustomerGroupDao.setTallyRef] (an
 * entity row exists). Supplier groups are a free-text field with no entity row to hold a `ref_id`, so
 * their idempotency anchors on the [AppPreferencesDataStore] pushed-account-group-ids set, keyed by
 * the group *name* itself.
 */
@Inject
class TallyAccountGroupPushService(
    private val customerGroupDao: CustomerGroupDao,
    private val supplierDao: SupplierDao,
    private val dataStore: AppPreferencesDataStore,
) {
    private suspend fun customerGroupCandidates(): List<CustomerGroupEntity> =
        customerGroupDao.getAllCustomerGroups().first().filter { it.ref_id.isNullOrBlank() }

    private suspend fun supplierGroupNameCandidates(workspaceSlug: String): List<String> {
        val pushedNames = dataStore.getTallyPushedAccountGroupIds(workspaceSlug).first()
        return supplierDao.getAllSuppliers().first()
            .mapNotNull { it.supplier_group?.trim()?.takeIf { n -> n.isNotBlank() } }
            .distinct()
            .filter { it !in pushedNames }
    }

    suspend fun pendingCount(workspaceSlug: String): Int =
        customerGroupCandidates().size + supplierGroupNameCandidates(workspaceSlug).size

    suspend fun push(workspaceSlug: String, repo: TallyRepository, log: (String) -> Unit): TallyMasterPushResult {
        var pushed = 0
        var failed = 0

        // --- Customer sub-groups (id-anchored) -----------------------------------------------
        val groupCandidates = customerGroupCandidates()
        for (g in groupCandidates) {
            val message = TallyMessage(group = g.toTallyGroup())
            log("  → request account group ${g.name}: ${renderTallyXml(buildMastersImport(listOf(message)))}")

            val outcome = runCatching { repo.importMasters(listOf(message)) }
            val result = outcome.getOrNull()
            val ok = outcome.isSuccess && result?.isSuccess == true
            if (ok) {
                pushed++
                val tallyRef = result?.lastMId?.trim()?.takeIf { it.isNotBlank() } ?: g.name
                customerGroupDao.setTallyRef(g.id, tallyRef)
                log("  ✓ account group ${g.name} → Tally (ref $tallyRef)")
            } else {
                failed++
                val reason = outcome.exceptionOrNull()?.message
                    ?: result?.lineError?.takeIf { it.isNotBlank() }
                    ?: "Tally created nothing for account group ${g.name} — see 'IMPORTDATA reply' in the log"
                log("  ✗ account group ${g.name} — $reason")
                kermitLog.w { "Tally account group push failed for ${g.id}: $reason" }
            }
        }

        // --- Supplier free-text groups (name-anchored) ----------------------------------------
        val supplierGroupNames = supplierGroupNameCandidates(workspaceSlug)

        val newlyPushedNames = mutableSetOf<String>()
        for (groupName in supplierGroupNames) {
            val message = TallyMessage(group = toTallySupplierGroup(groupName))
            log("  → request account group $groupName: ${renderTallyXml(buildMastersImport(listOf(message)))}")

            val outcome = runCatching { repo.importMasters(listOf(message)) }
            val result = outcome.getOrNull()
            val ok = outcome.isSuccess && result?.isSuccess == true
            if (ok) {
                pushed++
                newlyPushedNames += groupName
                log("  ✓ account group $groupName → Tally")
            } else {
                failed++
                val reason = outcome.exceptionOrNull()?.message
                    ?: result?.lineError?.takeIf { it.isNotBlank() }
                    ?: "Tally created nothing for account group $groupName — see 'IMPORTDATA reply' in the log"
                log("  ✗ account group $groupName — $reason")
                kermitLog.w { "Tally account group push failed for $groupName: $reason" }
            }
        }
        if (newlyPushedNames.isNotEmpty()) dataStore.addTallyPushedAccountGroupIds(workspaceSlug, newlyPushedNames)

        log("Tally masters push: account groups complete — pushed=$pushed, failed=$failed")
        return TallyMasterPushResult(pushed = pushed, failed = failed)
    }
}
