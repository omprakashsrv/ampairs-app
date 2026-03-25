package com.ampairs.product.ui.variant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.product.data.repository.ProductRepository
import com.ampairs.product.domain.ProductVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for creating and editing product variants
 */
class VariantFormViewModel(
    private val productId: String,
    private val variantId: String?,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VariantFormUiState())
    val uiState: StateFlow<VariantFormUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(VariantFormState())
    val formState: StateFlow<VariantFormState> = _formState.asStateFlow()

    private val _attributeOptions = MutableStateFlow(AttributeOptions())
    val attributeOptions: StateFlow<AttributeOptions> = _attributeOptions.asStateFlow()

    init {
        loadAttributeOptions()
        variantId?.let { loadVariant(it) }
    }

    private fun loadVariant(variantId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val variant = productRepository.getVariantById(variantId)
                if (variant != null) {
                    _formState.update {
                        VariantFormState(
                            sku = variant.sku,
                            variantName = variant.variantName,
                            attribute1Name = variant.attribute1Name,
                            attribute1Value = variant.attribute1Value,
                            attribute2Name = variant.attribute2Name,
                            attribute2Value = variant.attribute2Value,
                            attribute3Name = variant.attribute3Name,
                            attribute3Value = variant.attribute3Value,
                            mrp = variant.mrp,
                            dealerPrice = variant.dealerPrice,
                            sellingPrice = variant.sellingPrice,
                            stockQuantity = variant.stockQuantity,
                            lowStockAlert = variant.lowStockAlert,
                            active = variant.active
                        )
                    }
                    _uiState.update { it.copy(isLoading = false, isEditMode = true) }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Variant not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load variant"
                    )
                }
            }
        }
    }

    private fun loadAttributeOptions() {
        viewModelScope.launch {
            try {
                val attributeNames = productRepository.getAttributeNames(productId)
                _attributeOptions.update { it.copy(availableAttributeNames = attributeNames) }
            } catch (e: Exception) {
                // Graceful fallback - user can type custom attribute names
            }
        }
    }

    fun loadAttributeValues(attributeName: String) {
        viewModelScope.launch {
            try {
                val values = productRepository.getAttributeValues(productId, attributeName)
                _attributeOptions.update { current ->
                    current.copy(
                        attributeValues = current.attributeValues + (attributeName to values)
                    )
                }
            } catch (e: Exception) {
                // Graceful fallback - user can type custom values
            }
        }
    }

    fun updateForm(newState: VariantFormState) {
        _formState.update { newState.copy().validate() }
    }

    fun saveVariant(onSuccess: () -> Unit) {
        val state = _formState.value
        if (!state.isValid) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            try {
                // Generate UID for new variant (in ViewModel, not repository)
                val uid = variantId ?: UidGenerator.generateUid("VAR")

                val variant = state.toProductVariant(productId, uid)

                val result = if (variantId == null) {
                    productRepository.createVariant(variant)
                } else {
                    productRepository.updateVariant(variant)
                }

                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                successMessage = if (variantId == null) {
                                    "Created ${state.variantName}"
                                } else {
                                    "Updated ${state.variantName}"
                                }
                            )
                        }
                        onSuccess()
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = error.message ?: "Failed to save variant"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to save variant"
                    )
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

/**
 * UI State for Variant Form Screen
 */
data class VariantFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * Form State for Variant Data
 */
data class VariantFormState(
    val sku: String = "",
    val variantName: String = "",
    val attribute1Name: String? = null,
    val attribute1Value: String? = null,
    val attribute2Name: String? = null,
    val attribute2Value: String? = null,
    val attribute3Name: String? = null,
    val attribute3Value: String? = null,
    val mrp: Double? = null,
    val dealerPrice: Double? = null,
    val sellingPrice: Double? = null,
    val stockQuantity: Double = 0.0,
    val lowStockAlert: Double? = null,
    val active: Boolean = true,
    val skuError: String? = null,
    val nameError: String? = null,
    val priceError: String? = null,
    val attributeError: String? = null
) {
    val isValid: Boolean
        get() = sku.isNotBlank() &&
                variantName.isNotBlank() &&
                skuError == null &&
                nameError == null &&
                priceError == null &&
                attributeError == null

    fun validate(): VariantFormState {
        return copy(
            skuError = when {
                sku.isBlank() -> "SKU is required"
                sku.length < 3 -> "SKU must be at least 3 characters"
                else -> null
            },
            nameError = when {
                variantName.isBlank() -> "Variant name is required"
                else -> null
            },
            priceError = when {
                mrp != null && mrp < 0 -> "MRP cannot be negative"
                dealerPrice != null && dealerPrice < 0 -> "Dealer price cannot be negative"
                sellingPrice != null && sellingPrice < 0 -> "Selling price cannot be negative"
                else -> null
            },
            attributeError = when {
                // Attribute pairs must have both name AND value, or both null
                (attribute1Name != null && attribute1Value == null) -> "Attribute 1 value is required"
                (attribute1Name == null && attribute1Value != null) -> "Attribute 1 name is required"
                (attribute2Name != null && attribute2Value == null) -> "Attribute 2 value is required"
                (attribute2Name == null && attribute2Value != null) -> "Attribute 2 name is required"
                (attribute3Name != null && attribute3Value == null) -> "Attribute 3 value is required"
                (attribute3Name == null && attribute3Value != null) -> "Attribute 3 name is required"
                else -> null
            }
        )
    }

    fun toProductVariant(productId: String, variantId: String): ProductVariant {
        return ProductVariant(
            id = variantId,
            productId = productId,
            sku = sku.trim(),
            variantName = variantName.trim(),
            attribute1Name = attribute1Name?.trim()?.takeIf { it.isNotBlank() },
            attribute1Value = attribute1Value?.trim()?.takeIf { it.isNotBlank() },
            attribute2Name = attribute2Name?.trim()?.takeIf { it.isNotBlank() },
            attribute2Value = attribute2Value?.trim()?.takeIf { it.isNotBlank() },
            attribute3Name = attribute3Name?.trim()?.takeIf { it.isNotBlank() },
            attribute3Value = attribute3Value?.trim()?.takeIf { it.isNotBlank() },
            mrp = mrp,
            dealerPrice = dealerPrice,
            sellingPrice = sellingPrice,
            stockQuantity = stockQuantity,
            lowStockAlert = lowStockAlert,
            active = active
        )
    }
}

/**
 * Attribute Options for Dropdowns
 */
data class AttributeOptions(
    val availableAttributeNames: List<String> = emptyList(),
    val attributeValues: Map<String, List<String>> = emptyMap()
) {
    fun getValuesForAttribute(attributeName: String): List<String> {
        return attributeValues[attributeName] ?: emptyList()
    }
}
