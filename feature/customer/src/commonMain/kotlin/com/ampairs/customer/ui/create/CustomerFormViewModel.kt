package com.ampairs.customer.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.validation.ValidationResult
import com.ampairs.common.validation.gstin.GstinValidationError
import com.ampairs.common.validation.gstin.GstinValidator
import com.ampairs.common.validation.phone.PhoneNumberValidationError
import com.ampairs.common.validation.phone.PhoneNumberValidator
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerAddress
import com.ampairs.customer.domain.CustomerStore
import com.ampairs.customer.domain.State
import com.ampairs.customer.domain.StateStore
import com.ampairs.customer.domain.CustomerType
import com.ampairs.customer.data.repository.CustomerTypeRepository
import com.ampairs.customer.domain.CustomerGroup
import com.ampairs.customer.data.repository.CustomerGroupRepository
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.customer.util.CustomerConstants
import com.ampairs.customer.ui.components.contact.ContactData
import com.ampairs.customer.ui.components.contact.ContactPickerService
import com.ampairs.customer.ui.components.location.LocationService
import com.ampairs.form.data.repository.ConfigLookup
import com.ampairs.form.domain.EntityConfigSchema
import com.ampairs.form.domain.EntityType
import com.ampairs.customer.util.CustomerConstants.DEFAULT_COUNTRY_CODE
import com.ampairs.customer.util.CustomerConstants.STATUS_ACTIVE
import com.ampairs.customer.util.CustomerConstants.ERROR_VALIDATION_FIX
import com.ampairs.customer.util.CustomerConstants.ERROR_INVALID_EMAIL
import com.ampairs.customer.util.CustomerConstants.ERROR_INVALID_LANDLINE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey

data class CustomerFormState(
    val uid: String = "",
    val refId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val landline: String = "",
    val countryCode: Int = 91,
    val customerType: String = "",
    val customerTypeName: String = "",
    val customerGroup: String = "",
    val customerGroupName: String = "",
    val gstNumber: String = "",
    val panNumber: String = "",
    val creditLimit: Double = 0.0,
    val creditDays: Int = 0,
    val address: String = "",
    val street: String = "",
    val street2: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val country: String = "India",
    val status: String = STATUS_ACTIVE,
    val attributes: Map<String, String> = emptyMap(),
    // Billing Address
    val useBillingAsMainAddress: Boolean = true,
    val billingStreet: String = "",
    val billingCity: String = "",
    val billingState: String = "",
    val billingPincode: String = "",
    val billingCountry: String = "India",
    // Shipping Address
    val useShippingAsMainAddress: Boolean = true,
    val shippingStreet: String = "",
    val shippingCity: String = "",
    val shippingState: String = "",
    val shippingPincode: String = "",
    val shippingCountry: String = "India",
    // Location
    val latitude: Double? = null,
    val longitude: Double? = null,
    // Validation errors
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val landlineError: String? = null,
    val gstinError: String? = null
) {
    fun isValid(): Boolean {
        return name.isNotBlank() &&
                nameError == null &&
                emailError == null &&
                phoneError == null &&
                landlineError == null &&
                gstinError == null
    }

    fun toCustomer(): Customer {
        return Customer(
            uid = uid,
            refId = refId.trim().takeIf { it.isNotBlank() },
            name = name.trim(),
            email = email.trim().takeIf { it.isNotBlank() },
            phone = phone.trim().takeIf { it.isNotBlank() },
            landline = landline.trim().takeIf { it.isNotBlank() },
            countryCode = if (countryCode == 0) DEFAULT_COUNTRY_CODE else countryCode,
            customerType = customerType.takeIf { it.isNotBlank() },
            customerGroup = customerGroup.takeIf { it.isNotBlank() },
            gstNumber = gstNumber.trim().takeIf { it.isNotBlank() },
            panNumber = panNumber.trim().takeIf { it.isNotBlank() },
            creditLimit = if (creditLimit > 0) creditLimit else null,
            creditDays = if (creditDays > 0) creditDays else null,
            address = address.trim().takeIf { it.isNotBlank() },
            street = street.trim().takeIf { it.isNotBlank() },
            street2 = street2.trim().takeIf { it.isNotBlank() },
            city = city.trim().takeIf { it.isNotBlank() },
            state = state.trim().takeIf { it.isNotBlank() },
            pincode = pincode.trim().takeIf { it.isNotBlank() },
            country = country.trim(),
            status = status.takeIf { it.isNotBlank() },
            attributes = attributes.takeIf { it.isNotEmpty() },
            latitude = latitude,
            longitude = longitude,
            billingAddress = if (useBillingAsMainAddress) {
                CustomerAddress(
                    street = street.trim(),
                    city = city.trim(),
                    state = state.trim(),
                    pincode = pincode.trim(),
                    country = country.trim()
                ).takeIf { street.isNotBlank() || city.isNotBlank() }
            } else {
                CustomerAddress(
                    street = billingStreet.trim(),
                    city = billingCity.trim(),
                    state = billingState.trim(),
                    pincode = billingPincode.trim(),
                    country = billingCountry.trim()
                ).takeIf { billingStreet.isNotBlank() || billingCity.isNotBlank() }
            },
            shippingAddress = if (useShippingAsMainAddress) {
                CustomerAddress(
                    street = street.trim(),
                    city = city.trim(),
                    state = state.trim(),
                    pincode = pincode.trim(),
                    country = country.trim()
                ).takeIf { street.isNotBlank() || city.isNotBlank() }
            } else {
                CustomerAddress(
                    street = shippingStreet.trim(),
                    city = shippingCity.trim(),
                    state = shippingState.trim(),
                    pincode = shippingPincode.trim(),
                    country = shippingCountry.trim()
                ).takeIf { shippingStreet.isNotBlank() || shippingCity.isNotBlank() }
            },
        )
    }
}

data class CustomerFormUiState(
    val formState: CustomerFormState = CustomerFormState(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val canSave: Boolean = false,
    val states: List<State> = emptyList(),
    val isLoadingStates: Boolean = false,
    val cities: List<String> = emptyList(),
    val pincodes: List<String> = emptyList(),
    val isLoadingCitiesAndPincodes: Boolean = false,
    val customerTypes: List<CustomerType> = emptyList(),
    val isLoadingCustomerTypes: Boolean = false,
    val customerGroups: List<CustomerGroup> = emptyList(),
    val isLoadingCustomerGroups: Boolean = false,
    val entityConfig: EntityConfigSchema? = null,
    val showCustomerImages: Boolean = true,
    val customerImagesReadOnly: Boolean = false,
    val isImportingContact: Boolean = false,
    val contactImportError: String? = null
)

@AssistedInject
class CustomerFormViewModel(
    @Assisted private val customerId: String?,
    private val customerStore: CustomerStore,
    private val stateStore: StateStore,
    private val customerTypeRepository: CustomerTypeRepository,
    private val customerGroupRepository: CustomerGroupRepository,
    private val configRepository: ConfigLookup,
    val contactPickerService: ContactPickerService,
    val locationService: LocationService
) : ViewModel() {

    val isContactPickerAvailable: Boolean get() = contactPickerService.isAvailable()

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(customerId: String?): CustomerFormViewModel
    }

    private val _uiState = MutableStateFlow(CustomerFormUiState())
    val uiState: StateFlow<CustomerFormUiState> = _uiState.asStateFlow()

    private var originalCustomer: Customer? = null

    init {
        observeFormValidation()
        loadStates()
        loadCitiesAndPincodes()
        loadCustomerTypes()
        loadCustomerGroups()
        loadEntityConfig()
        loadCustomerImagesConfig()
    }

    private fun loadCustomerImagesConfig() {
        viewModelScope.launch {
            val config = configRepository.observeConfigSchema("customer").first()
            val fieldConfig = config?.fieldConfigs?.find { it.fieldName == "customerImages" }
            _uiState.update {
                it.copy(
                    showCustomerImages = fieldConfig?.visible != false,
                    customerImagesReadOnly = fieldConfig?.enabled == false
                )
            }
        }
    }

    fun pickContactAndImport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingContact = true, contactImportError = null) }
            contactPickerService.pickContact()
                .onSuccess { contactData -> importContact(contactData) }
                .onFailure { error ->
                    val msg = error.message
                    if (msg?.contains("cancelled", ignoreCase = true) != true) {
                        _uiState.update { it.copy(contactImportError = msg ?: "Failed to import contact") }
                    }
                }
            _uiState.update { it.copy(isImportingContact = false) }
        }
    }

    private fun importContact(contactData: ContactData) {
        val current = _uiState.value.formState
        var updated = current
        if (contactData.name.isNotBlank() && current.name.isBlank()) updated = updated.copy(name = contactData.name)
        if (contactData.email.isNotBlank() && current.email.isBlank()) updated = updated.copy(email = contactData.email)
        if (contactData.phone.isNotBlank() && current.phone.isBlank()) {
            val parsedCode = contactData.countryCode.removePrefix("+").toIntOrNull()
            val countryCodeInt = if (parsedCode == null || parsedCode == 0) 91 else parsedCode
            updated = updated.copy(phone = contactData.phone, countryCode = countryCodeInt)
        }
        if (contactData.street.isNotBlank() && current.street.isBlank()) updated = updated.copy(street = contactData.street)
        if (contactData.city.isNotBlank() && current.city.isBlank()) updated = updated.copy(city = contactData.city)
        if (contactData.state.isNotBlank() && current.state.isBlank()) updated = updated.copy(state = contactData.state)
        if (contactData.pincode.isNotBlank() && current.pincode.isBlank()) updated = updated.copy(pincode = contactData.pincode)
        if (contactData.country.isNotBlank() && current.country.isBlank()) updated = updated.copy(country = contactData.country)
        _uiState.update { it.copy(formState = validateForm(updated)) }
    }

    fun clearContactImportError() {
        _uiState.update { it.copy(contactImportError = null) }
    }

    fun loadCustomer() {
        if (customerId == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                customerStore.observeCustomer(customerId)
                    .collect { customer ->
                        if (customer != null) {
                            originalCustomer = customer
                            _uiState.update {
                                it.copy(
                                    formState = customer.toFormState(),
                                    isLoading = false,
                                    error = null
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Customer not found"
                                )
                            }
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

    fun updateForm(formState: CustomerFormState) {
        val validatedForm = validateForm(formState)
        _uiState.update {
            it.copy(
                formState = validatedForm,
                error = null
            )
        }
    }

    fun saveCustomer(onSuccess: () -> Unit) {
        val currentFormState = _uiState.value.formState

        if (!currentFormState.isValid()) {
            _uiState.update {
                it.copy(error = ERROR_VALIDATION_FIX)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            try {
                val result = if (currentFormState.uid.isBlank()) {
                    // Create new customer
                    val updatedFormState = currentFormState.copy(uid = UidGenerator.generateUid(CustomerConstants.UID_PREFIX))
                    val newCustomer = updatedFormState.toCustomer()
                    customerStore.createCustomer(newCustomer)
                } else {
                    // Update existing customer
                    val updatedCustomer = currentFormState.toCustomer()
                    customerStore.updateCustomer(updatedCustomer)
                }

                if (result.isSuccess) {
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to save customer"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Failed to save customer"
                    )
                }
            }
        }
    }

    private fun loadStates() {
        stateStore.observeStates()
            .onEach { states -> _uiState.update { it.copy(states = states, isLoadingStates = false) } }
            .launchIn(viewModelScope)
        _uiState.update { it.copy(isLoadingStates = true) }
        viewModelScope.launch {
            try { stateStore.syncStates() } catch (_: Exception) {}
        }
    }

    private fun loadCitiesAndPincodes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCitiesAndPincodes = true) }

            try {
                val cities = customerStore.getUniqueCities()
                val pincodes = customerStore.getUniquePincodes()

                _uiState.update {
                    it.copy(
                        cities = cities,
                        pincodes = pincodes,
                        isLoadingCitiesAndPincodes = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingCitiesAndPincodes = false,
                        error = e.message ?: "Failed to load cities and pincodes"
                    )
                }
            }
        }
    }

    private fun loadCustomerTypes() {
        customerTypeRepository.observeCustomerTypes()
            .onEach { types -> _uiState.update { it.copy(customerTypes = types, isLoadingCustomerTypes = false) } }
            .launchIn(viewModelScope)
        _uiState.update { it.copy(isLoadingCustomerTypes = true) }
        viewModelScope.launch {
            try { customerTypeRepository.syncCustomerTypes() } catch (_: Exception) {}
        }
    }

    private fun loadCustomerGroups() {
        customerGroupRepository.observeCustomerGroups()
            .onEach { groups -> _uiState.update { it.copy(customerGroups = groups, isLoadingCustomerGroups = false) } }
            .launchIn(viewModelScope)
        _uiState.update { it.copy(isLoadingCustomerGroups = true) }
        viewModelScope.launch {
            try { customerGroupRepository.syncCustomerGroups() } catch (_: Exception) {}
        }
    }

    private fun loadEntityConfig() {
        // Observe database for reactive updates (will emit immediately if cached data exists)
        configRepository.observeConfigSchema(EntityType.CUSTOMER)
            .onEach { schema ->
                _uiState.update { it.copy(entityConfig = schema) }
            }
            .launchIn(viewModelScope)

        // Sync all form configs from backend (will update database, triggering flow above)
        viewModelScope.launch {
            try {
                configRepository.syncFormConfigs()
            } catch (e: Exception) {
                // Silently fail - form will work without config using default behavior
                println("Failed to sync form configs: ${e.message}")
            }
        }
    }

    fun onCustomerTypeSelected(customerType: CustomerType) {
        val currentFormState = _uiState.value.formState
        _uiState.update {
            it.copy(
                formState = currentFormState.copy(
                    customerType = customerType.name,
                    customerTypeName = customerType.name
                ),
                error = null
            )
        }
    }

    fun onCustomerGroupSelected(customerGroup: CustomerGroup) {
        val currentFormState = _uiState.value.formState
        _uiState.update {
            it.copy(
                formState = currentFormState.copy(
                    customerGroup = customerGroup.name,
                    customerGroupName = customerGroup.name
                ),
                error = null
            )
        }
    }

    fun onStateSelected(state: State) {
        val currentFormState = _uiState.value.formState
        _uiState.update {
            it.copy(
                formState = currentFormState.copy(state = state.name),
                error = null
            )
        }
    }

    private fun observeFormValidation() {
        uiState
            .map { it.formState }
            .distinctUntilChanged()
            .onEach { formState ->
                _uiState.update {
                    it.copy(canSave = formState.isValid())
                }
            }
            .launchIn(viewModelScope)
    }

    private fun validateForm(formState: CustomerFormState): CustomerFormState {
        val nameError = when {
            formState.name.isBlank() -> "Name is required"
            formState.name.length < 2 -> "Name must be at least 2 characters"
            else -> null
        }

        val emailError = if (formState.email.isNotBlank() && !isValidEmail(formState.email)) {
            ERROR_INVALID_EMAIL
        } else null

        val phoneError = if (formState.phone.isNotBlank()) {
            when (val result = PhoneNumberValidator().validate(formState.phone)) {
                is ValidationResult.Invalid -> (result.errors.firstOrNull() as? PhoneNumberValidationError)?.message
                is ValidationResult.Valid -> null
            }
        } else null

        val landlineError = if (formState.landline.isNotBlank()) {
            when {
                !isValidLandline(formState.landline) -> ERROR_INVALID_LANDLINE
                else -> null
            }
        } else null

        val gstinError = if (formState.gstNumber.isNotBlank()) {
            when (val result = GstinValidator().validate(formState.gstNumber)) {
                is ValidationResult.Invalid -> (result.errors.firstOrNull() as? GstinValidationError)?.message
                is ValidationResult.Valid -> null
            }
        } else null

        return formState.copy(
            nameError = nameError,
            emailError = emailError,
            phoneError = phoneError,
            landlineError = landlineError,
            gstinError = gstinError
        )
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    private fun isValidLandline(landline: String): Boolean {
        // Indian landline format: STD Code (2-5 digits) + Local number (6-8 digits)
        // Examples: 080-12345678, 0120-1234567, 022-12345678
        val cleanedLandline = landline.replace(Regex("[\\s-()]"), "")

        // Check for basic landline pattern (6-13 digits total)
        if (!cleanedLandline.matches(Regex("^0?\\d{6,12}$"))) {
            return false
        }

        // Must start with 0 (STD code prefix) if more than 8 digits
        if (cleanedLandline.length > 8 && !cleanedLandline.startsWith("0")) {
            return false
        }

        return true
    }

}

private fun Customer.toFormState(): CustomerFormState {
    return CustomerFormState(
        uid = uid,
        refId = refId ?: "",
        name = name,
        email = email ?: "",
        phone = phone ?: "",
        landline = landline ?: "",
        countryCode = if (countryCode == 0) DEFAULT_COUNTRY_CODE else countryCode,
        customerType = customerType ?: "",
        customerTypeName = customerType ?: "", // Use the name directly since we store names now
        customerGroup = customerGroup ?: "",
        customerGroupName = customerGroup ?: "", // Use the name directly since we store names now
        gstNumber = gstNumber ?: "",
        panNumber = panNumber ?: "",
        creditLimit = creditLimit ?: 0.0,
        creditDays = creditDays ?: 0,
        address = address ?: "",
        street = street ?: "",
        street2 = street2 ?: "",
        city = city ?: "",
        state = state ?: "",
        pincode = pincode ?: "",
        country = country,
        status = status ?: STATUS_ACTIVE,
        attributes = attributes ?: emptyMap(),
        // Billing Address
        useBillingAsMainAddress = billingAddress == null ||
                (billingAddress?.street == street && billingAddress?.city == city &&
                        billingAddress?.state == state && billingAddress?.pincode == pincode),
        billingStreet = billingAddress?.street ?: "",
        billingCity = billingAddress?.city ?: "",
        billingState = billingAddress?.state ?: "",
        billingPincode = billingAddress?.pincode ?: "",
        billingCountry = billingAddress?.country ?: "India",
        // Shipping Address
        useShippingAsMainAddress = shippingAddress == null ||
                (shippingAddress?.street == street && shippingAddress?.city == city &&
                        shippingAddress?.state == state && shippingAddress?.pincode == pincode),
        shippingStreet = shippingAddress?.street ?: "",
        shippingCity = shippingAddress?.city ?: "",
        shippingState = shippingAddress?.state ?: "",
        shippingPincode = shippingAddress?.pincode ?: "",
        shippingCountry = shippingAddress?.country ?: "India",
        // Location
        latitude = latitude,
        longitude = longitude,
        // Validation errors
        nameError = null,
        emailError = null,
        phoneError = null,
        gstinError = null
    )
}