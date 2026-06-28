package com.ampairs.supplier.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.supplier.domain.Supplier
import com.ampairs.supplier.domain.SupplierStore
import com.ampairs.supplier.util.SupplierConstants
import com.ampairs.supplier.util.SupplierConstants.DEFAULT_COUNTRY
import com.ampairs.supplier.util.SupplierConstants.DEFAULT_COUNTRY_CODE
import com.ampairs.supplier.util.SupplierConstants.ERROR_VALIDATION_FIX
import com.ampairs.supplier.util.SupplierConstants.STATUS_ACTIVE
import com.ampairs.supplier.util.SupplierLogger
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SupplierFormState(
    val uid: String = "",
    val refId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val landline: String = "",
    val countryCode: Int = DEFAULT_COUNTRY_CODE,
    val gstNumber: String = "",
    val panNumber: String = "",
    val creditLimit: Double = 0.0,
    val creditDays: Int = 0,
    val outstandingAmount: Double = 0.0,
    val address: String = "",
    val street: String = "",
    val street2: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val country: String = DEFAULT_COUNTRY,
    val status: String = STATUS_ACTIVE,
    val nameError: String? = null,
    val emailError: String? = null,
) {
    fun isValid(): Boolean = name.isNotBlank() && nameError == null && emailError == null

    fun toSupplier(): Supplier = Supplier(
        uid = uid,
        refId = refId.trim().takeIf { it.isNotBlank() },
        name = name.trim(),
        email = email.trim().takeIf { it.isNotBlank() },
        phone = phone.trim().takeIf { it.isNotBlank() },
        landline = landline.trim().takeIf { it.isNotBlank() },
        countryCode = if (countryCode == 0) DEFAULT_COUNTRY_CODE else countryCode,
        gstNumber = gstNumber.trim().takeIf { it.isNotBlank() },
        panNumber = panNumber.trim().takeIf { it.isNotBlank() },
        creditLimit = if (creditLimit > 0) creditLimit else null,
        creditDays = if (creditDays > 0) creditDays else null,
        outstandingAmount = if (outstandingAmount != 0.0) outstandingAmount else null,
        address = address.trim().takeIf { it.isNotBlank() },
        street = street.trim().takeIf { it.isNotBlank() },
        street2 = street2.trim().takeIf { it.isNotBlank() },
        city = city.trim().takeIf { it.isNotBlank() },
        state = state.trim().takeIf { it.isNotBlank() },
        pincode = pincode.trim().takeIf { it.isNotBlank() },
        country = country.ifBlank { DEFAULT_COUNTRY },
        status = status,
        active = true,
    )
}

data class SupplierFormUiState(
    val form: SupplierFormState = SupplierFormState(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val error: String? = null,
)

@AssistedInject
class SupplierFormViewModel(
    @Assisted private val supplierId: String?,
    private val supplierStore: SupplierStore,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(supplierId: String?): SupplierFormViewModel
    }

    private val _uiState = MutableStateFlow(SupplierFormUiState(isEditMode = !supplierId.isNullOrBlank()))
    val uiState: StateFlow<SupplierFormUiState> = _uiState.asStateFlow()

    init {
        if (!supplierId.isNullOrBlank()) loadSupplier(supplierId)
    }

    private fun loadSupplier(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val supplier = supplierStore.getSupplier(id)
            if (supplier != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        form = supplier.toFormState(),
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Supplier not found") }
            }
        }
    }

    fun updateForm(transform: (SupplierFormState) -> SupplierFormState) {
        _uiState.update { it.copy(form = transform(it.form)) }
    }

    fun save(onSuccess: () -> Unit) {
        val form = _uiState.value.form
        if (!form.isValid()) {
            _uiState.update { it.copy(error = ERROR_VALIDATION_FIX, form = form.copy(nameError = if (form.name.isBlank()) "Name is required" else null)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                // UID generation belongs in the ViewModel, never the repository.
                val supplier = if (_uiState.value.isEditMode) {
                    form.toSupplier()
                } else {
                    form.copy(uid = UidGenerator.generateUid(SupplierConstants.UID_PREFIX)).toSupplier()
                }
                val result = if (_uiState.value.isEditMode) {
                    supplierStore.updateSupplier(supplier)
                } else {
                    supplierStore.createSupplier(supplier)
                }
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(isSaving = false, error = result.exceptionOrNull()?.message ?: "Failed to save supplier")
                    }
                }
            } catch (e: Exception) {
                SupplierLogger.e("SupplierFormViewModel", "Save failed", e)
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "Failed to save supplier") }
            }
        }
    }

    private fun Supplier.toFormState(): SupplierFormState = SupplierFormState(
        uid = uid,
        refId = refId.orEmpty(),
        name = name,
        email = email.orEmpty(),
        phone = phone.orEmpty(),
        landline = landline.orEmpty(),
        countryCode = if (countryCode == 0) DEFAULT_COUNTRY_CODE else countryCode,
        gstNumber = gstNumber.orEmpty(),
        panNumber = panNumber.orEmpty(),
        creditLimit = creditLimit ?: 0.0,
        creditDays = creditDays ?: 0,
        outstandingAmount = outstandingAmount ?: 0.0,
        address = address.orEmpty(),
        street = street.orEmpty(),
        street2 = street2.orEmpty(),
        city = city.orEmpty(),
        state = state.orEmpty(),
        pincode = pincode.orEmpty(),
        country = country.ifBlank { DEFAULT_COUNTRY },
        status = status ?: STATUS_ACTIVE,
    )
}
