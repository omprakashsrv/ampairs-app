package com.ampairs.ecom.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.ecom.data.db.entity.CustomerAddressEntity
import com.ampairs.ecom.data.repository.AddressRepository
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Plain form payload from the screen (no domain object references). */
data class AddressDraft(
    val uid: String? = null,
    val label: String = "",
    val line1: String = "",
    val line2: String = "",
    val city: String = "",
    val state: String = "",
    val pinCode: String = "",
    val phone: String = "",
    val isDefault: Boolean = false,
)

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
class AddressesViewModel(
    private val addressRepository: AddressRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val addresses: StateFlow<List<CustomerAddressEntity>> =
        addressRepository.observeAddresses()
            .map { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.ECOM_ADDRESS))
    }

    fun save(draft: AddressDraft) {
        // UID is generated in the ViewModel (never the repository).
        val uid = draft.uid?.takeIf { it.isNotBlank() } ?: UidGenerator.generateUid("ADR")
        val entity = CustomerAddressEntity(
            uid = uid,
            label = draft.label.ifBlank { null },
            address_line1 = draft.line1,
            address_line2 = draft.line2.ifBlank { null },
            city = draft.city,
            state = draft.state,
            pin_code = draft.pinCode,
            phone = draft.phone.ifBlank { null },
            is_default = if (draft.isDefault) 1 else 0,
            active = 1,
            synced = 0,
        )
        viewModelScope.launch {
            addressRepository.saveLocally(entity)
            syncService.markPendingPush(SyncEntity.ECOM_ADDRESS)
        }
    }

    fun delete(uid: String) {
        viewModelScope.launch {
            addressRepository.markDeletedLocally(uid)
            syncService.markPendingPush(SyncEntity.ECOM_ADDRESS)
        }
    }
}
