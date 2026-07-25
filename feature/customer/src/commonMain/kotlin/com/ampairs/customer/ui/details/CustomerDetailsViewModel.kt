package com.ampairs.customer.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.customer.data.repository.CustomerContactRepository
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerContactResponse
import com.ampairs.customer.domain.CustomerStore
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.form.data.repository.ConfigLookup
import com.ampairs.form.domain.FieldSource
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class CustomerImagesConfig(
    val visible: Boolean = true,
    val readOnly: Boolean = false
)

data class CustomerDetailsUiState(
    val customer: Customer? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    /**
     * Custom attribute rows ready for display (spec 011, T035): the customer's attributes joined
     * with the unified schema — hidden fields filtered out, labels from `displayName`. Attributes
     * with no schema field (stale keys) keep their raw key as the label.
     */
    val attributeRows: List<Pair<String, String>> = emptyList(),
)

/** "Linked accounts" section state — kept separate from [CustomerDetailsUiState] since it's a
 * live, UI-invoked concern (not part of the offline-synced customer record). */
data class LinkedAccountsUiState(
    val contacts: List<CustomerContactResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isLinking: Boolean = false,
    val error: String? = null,
)

@AssistedInject
class CustomerDetailsViewModel(
    @Assisted private val customerId: String,
    private val customerStore: CustomerStore,
    private val configRepository: ConfigLookup,
    private val contactRepository: CustomerContactRepository,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(customerId: String): CustomerDetailsViewModel
    }

    private val _uiState = MutableStateFlow(CustomerDetailsUiState())
    val uiState: StateFlow<CustomerDetailsUiState> = _uiState.asStateFlow()

    private val _imagesConfig = MutableStateFlow(CustomerImagesConfig())
    val imagesConfig: StateFlow<CustomerImagesConfig> = _imagesConfig.asStateFlow()

    private val _linkedAccounts = MutableStateFlow(LinkedAccountsUiState())
    val linkedAccounts: StateFlow<LinkedAccountsUiState> = _linkedAccounts.asStateFlow()

    init {
        loadCustomerImagesConfig()
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            _linkedAccounts.update { it.copy(isLoading = true, error = null) }
            contactRepository.getContacts(customerId).fold(
                onSuccess = { contacts -> _linkedAccounts.update { it.copy(contacts = contacts, isLoading = false) } },
                onFailure = { e -> _linkedAccounts.update { it.copy(isLoading = false, error = e.message ?: "Failed to load linked accounts") } },
            )
        }
    }

    /** Owner-initiated: find the app account registered with [phone] and link it to this customer. */
    fun linkAccount(phone: String, name: String?) {
        if (_linkedAccounts.value.isLinking) return
        viewModelScope.launch {
            _linkedAccounts.update { it.copy(isLinking = true, error = null) }
            contactRepository.linkContact(customerId, phone, name).fold(
                onSuccess = {
                    _linkedAccounts.update { it.copy(isLinking = false) }
                    loadContacts()
                },
                onFailure = { e -> _linkedAccounts.update { it.copy(isLinking = false, error = e.message ?: "Couldn't link that phone number") } },
            )
        }
    }

    /** Restrict (false) or re-enable (true) a linked account's ordering access. */
    fun setAccountActive(contactUid: String, active: Boolean) {
        viewModelScope.launch {
            contactRepository.setContactActive(customerId, contactUid, active).fold(
                onSuccess = { loadContacts() },
                onFailure = { e -> _linkedAccounts.update { it.copy(error = e.message ?: "Couldn't update that account") } },
            )
        }
    }

    private fun loadCustomerImagesConfig() {
        viewModelScope.launch {
            val config = configRepository.observeConfigSchema("customer").first()
            val fieldConfig = config?.fieldConfigs?.find { it.fieldName == "customerImages" }
            _imagesConfig.value = CustomerImagesConfig(
                visible = fieldConfig?.visible != false,
                readOnly = fieldConfig?.enabled == false
            )
        }
    }

    fun loadCustomer() {

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                customerStore.observeCustomer(customerId)
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = throwable.message ?: "Unknown error"
                            )
                        }
                    }
                    .collect { customer ->
                        _uiState.update {
                            it.copy(
                                customer = customer,
                                isLoading = false,
                                error = if (customer == null) "Customer not found" else null,
                                attributeRows = customer?.let { c -> attributeRowsFor(c) } ?: emptyList(),
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load customer"
                    )
                }
            }
        }
    }

    /** Read-view honors the schema (T035): hide attributes whose field is hidden; label by displayName. */
    private suspend fun attributeRowsFor(customer: Customer): List<Pair<String, String>> {
        val attrs = customer.attributes?.takeIf { it.isNotEmpty() } ?: return emptyList()
        val schema = try {
            configRepository.observeSchema("customer").first()
        } catch (e: Exception) {
            null
        }
        val customFields = schema?.fields?.filter { it.source == FieldSource.CUSTOM }?.associateBy { it.fieldKey }
        return attrs.mapNotNull { (key, value) ->
            val field = customFields?.get(key)
            when {
                field == null -> key to value                       // no schema info — show raw
                !field.visible -> null                              // hidden in config — omit
                else -> field.displayName.ifBlank { key } to value  // configured label
            }
        }
    }

    fun deleteCustomer(onSuccess: () -> Unit) {

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val result = customerStore.deleteCustomer(customerId)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to delete customer"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to delete customer"
                    )
                }
            }
        }
    }
}