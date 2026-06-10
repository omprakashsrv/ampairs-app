package com.ampairs.customer.data

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.customer.data.repository.CustomerGroupRepository
import com.ampairs.customer.data.repository.CustomerTypeRepository
import com.ampairs.form.render.DynamicOptionProvider
import com.ampairs.form.render.FieldOption
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Dynamic option sources for customer choice fields (spec 011, US1). The schema-driven renderer
 * resolves a field's `dynamicSourceKey` ("customer_types" / "customer_groups") to these providers and
 * populates the dropdown from live, already-synced workspace data. Values are NAMES (not uids) —
 * that is what the customer columns store today (see CustomerFormViewModel.onCustomerTypeSelected).
 */
@Inject
@ContributesIntoSet(WorkspaceScope::class)
class CustomerTypeOptionProvider(
    private val repository: CustomerTypeRepository,
) : DynamicOptionProvider {
    override val sourceKey: String = "customer_types"
    override fun options(): Flow<List<FieldOption>> =
        repository.observeCustomerTypes().map { types -> types.map { FieldOption(it.name, it.name) } }
}

@Inject
@ContributesIntoSet(WorkspaceScope::class)
class StateOptionProvider(
    private val stateStore: com.ampairs.customer.domain.StateStore,
) : DynamicOptionProvider {
    override val sourceKey: String = "states"
    override fun options(): Flow<List<FieldOption>> =
        stateStore.observeStates().map { states -> states.map { FieldOption(it.name, it.name) } }
}

@Inject
@ContributesIntoSet(WorkspaceScope::class)
class CustomerGroupOptionProvider(
    private val repository: CustomerGroupRepository,
) : DynamicOptionProvider {
    override val sourceKey: String = "customer_groups"
    override fun options(): Flow<List<FieldOption>> =
        repository.observeCustomerGroups().map { groups -> groups.map { FieldOption(it.name, it.name) } }
}
