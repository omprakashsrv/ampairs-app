package com.ampairs.tallysync

import co.touchlab.kermit.Logger
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.customer.data.db.CustomerDao
import com.ampairs.customer.data.db.CustomerEntity
import com.ampairs.customer.data.db.CustomerGroupDao
import com.ampairs.supplier.data.db.SupplierDao
import com.ampairs.supplier.data.db.SupplierEntity
import com.ampairs.tally.TallyRepository
import com.ampairs.tally.model.TallyMessage
import com.ampairs.tally.model.buildMastersImport
import com.ampairs.tally.renderTallyXml
import com.ampairs.tallysync.TallyCustomerMapper.toTallyLedger
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

private val kermitLog = Logger.withTag("TallyLedgerPush")

/** Tally's reserved primary groups that root the debtor (customer) vs creditor (supplier) trees. */
private const val SUNDRY_DEBTORS = "Sundry Debtors"
private const val SUNDRY_CREDITORS = "Sundry Creditors"

/**
 * Pushes locally-created customers and suppliers *into* Tally as `LEDGER` masters (see
 * [TallyMastersPushService]) — both map to the same Tally ledger concept, just rooted under different
 * reserved primary groups. [pushCustomers]/[pushSuppliers] are independently callable (the manual
 * per-type push controls on `TallySettingsScreen`) and are also both run in sequence by
 * [TallyMastersPushService.push]'s bulk sweep. Depends on [TallyAccountGroupPushService] having
 * already run: a customer/supplier with a custom account group is skipped (per-record, not blocking
 * siblings) until that group carries a Tally reference, since
 * [com.ampairs.tally.model.master.Ledger.parent] must resolve to an existing Tally group.
 *
 * **Idempotency** mirrors [TallyInvoicePushService]: the Tally ledger GUID lands on
 * [CustomerDao.setTallyRef]/[SupplierDao.setTallyRef] (skip any row that already carries one), and the
 * pushed id also rides in the [AppPreferencesDataStore] pushed-customer/supplier-ids sets.
 */
@Inject
class TallyLedgerPushService(
    private val customerDao: CustomerDao,
    private val customerGroupDao: CustomerGroupDao,
    private val supplierDao: SupplierDao,
    private val dataStore: AppPreferencesDataStore,
) {
    private suspend fun customerCandidates(workspaceSlug: String): List<CustomerEntity> {
        val pushedCustomerIds = dataStore.getTallyPushedCustomerIds(workspaceSlug).first()
        return customerDao.getAllCustomers().first().filter {
            it.ref_id.isNullOrBlank() && it.id !in pushedCustomerIds
        }
    }

    private suspend fun supplierCandidates(workspaceSlug: String): List<SupplierEntity> {
        val pushedSupplierIds = dataStore.getTallyPushedSupplierIds(workspaceSlug).first()
        return supplierDao.getAllSuppliers().first().filter {
            it.ref_id.isNullOrBlank() && it.id !in pushedSupplierIds
        }
    }

    suspend fun pendingCustomerCount(workspaceSlug: String): Int = customerCandidates(workspaceSlug).size
    suspend fun pendingSupplierCount(workspaceSlug: String): Int = supplierCandidates(workspaceSlug).size

    suspend fun pushCustomers(workspaceSlug: String, repo: TallyRepository, log: (String) -> Unit): TallyMasterPushResult {
        var pushed = 0
        var failed = 0
        var skipped = 0

        val customerGroupsById = customerGroupDao.getAllCustomerGroups().first().associateBy { it.id }
        val candidates = customerCandidates(workspaceSlug)
        val newlyPushedCustomers = mutableSetOf<String>()
        for (c in candidates) {
            val customGroup = c.customer_group?.let { customerGroupsById[it] }
            val parentName = when {
                customGroup == null -> SUNDRY_DEBTORS
                !customGroup.ref_id.isNullOrBlank() -> customGroup.name
                else -> null
            }
            if (parentName == null) {
                skipped++
                log("  – customer ${c.name} skipped — account group \"${customGroup?.name}\" not yet in Tally")
                continue
            }

            val message = TallyMessage(ledger = c.toTallyLedger(parentName))
            log("  → request customer ${c.name}: ${renderTallyXml(buildMastersImport(listOf(message)))}")

            val outcome = runCatching { repo.importMasters(listOf(message)) }
            val result = outcome.getOrNull()
            val ok = outcome.isSuccess && result?.isSuccess == true
            if (ok) {
                pushed++
                newlyPushedCustomers += c.id
                val tallyRef = result?.lastMId?.trim()?.takeIf { it.isNotBlank() } ?: c.name
                customerDao.setTallyRef(c.id, tallyRef)
                log("  ✓ customer ${c.name} → Tally (ref $tallyRef)")
            } else {
                failed++
                val reason = outcome.exceptionOrNull()?.message
                    ?: result?.lineError?.takeIf { it.isNotBlank() }
                    ?: "Tally created nothing for customer ${c.name} — see 'IMPORTDATA reply' in the log"
                log("  ✗ customer ${c.name} — $reason")
                kermitLog.w { "Tally ledger push failed for customer ${c.id}: $reason" }
            }
        }
        if (newlyPushedCustomers.isNotEmpty()) dataStore.addTallyPushedCustomerIds(workspaceSlug, newlyPushedCustomers)

        log("Tally masters push: customers complete — pushed=$pushed, failed=$failed, skipped=$skipped")
        return TallyMasterPushResult(pushed = pushed, failed = failed, skipped = skipped)
    }

    suspend fun pushSuppliers(workspaceSlug: String, repo: TallyRepository, log: (String) -> Unit): TallyMasterPushResult {
        var pushed = 0
        var failed = 0
        var skipped = 0

        val pushedGroupNames = dataStore.getTallyPushedAccountGroupIds(workspaceSlug).first()
        val candidates = supplierCandidates(workspaceSlug)
        val newlyPushedSuppliers = mutableSetOf<String>()
        for (s in candidates) {
            val groupName = s.supplier_group?.trim()?.takeIf { it.isNotBlank() }
            val parentName = when {
                groupName == null -> SUNDRY_CREDITORS
                groupName in pushedGroupNames -> groupName
                else -> null
            }
            if (parentName == null) {
                skipped++
                log("  – supplier ${s.name} skipped — account group \"$groupName\" not yet in Tally")
                continue
            }

            val message = TallyMessage(ledger = s.toTallyLedger(parentName))
            log("  → request supplier ${s.name}: ${renderTallyXml(buildMastersImport(listOf(message)))}")

            val outcome = runCatching { repo.importMasters(listOf(message)) }
            val result = outcome.getOrNull()
            val ok = outcome.isSuccess && result?.isSuccess == true
            if (ok) {
                pushed++
                newlyPushedSuppliers += s.id
                val tallyRef = result?.lastMId?.trim()?.takeIf { it.isNotBlank() } ?: s.name
                supplierDao.setTallyRef(s.id, tallyRef)
                log("  ✓ supplier ${s.name} → Tally (ref $tallyRef)")
            } else {
                failed++
                val reason = outcome.exceptionOrNull()?.message
                    ?: result?.lineError?.takeIf { it.isNotBlank() }
                    ?: "Tally created nothing for supplier ${s.name} — see 'IMPORTDATA reply' in the log"
                log("  ✗ supplier ${s.name} — $reason")
                kermitLog.w { "Tally ledger push failed for supplier ${s.id}: $reason" }
            }
        }
        if (newlyPushedSuppliers.isNotEmpty()) dataStore.addTallyPushedSupplierIds(workspaceSlug, newlyPushedSuppliers)

        log("Tally masters push: suppliers complete — pushed=$pushed, failed=$failed, skipped=$skipped")
        return TallyMasterPushResult(pushed = pushed, failed = failed, skipped = skipped)
    }
}
