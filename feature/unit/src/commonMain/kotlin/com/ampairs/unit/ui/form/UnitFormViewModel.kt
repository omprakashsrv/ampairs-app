package com.ampairs.unit.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.aiops.AiOpsRunner
import com.ampairs.common.aiops.AiOpsUndo
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.unit.data.repository.UnitRepository
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import com.ampairs.unit.domain.model.Unit
import com.ampairs.unit.util.UnitConstants
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One-off UI events surfacing what the AI Ops engine did for the just-saved unit. */
sealed interface UnitFormEvent {
    /** A short name was auto-standardized; [decisionId] lets the UI offer Undo. */
    data class AiOpsShortNameFixed(val newShortName: String, val decisionId: String) : UnitFormEvent

    /** A standardization was suggested (not auto-applied) — informational. */
    data class AiOpsSuggestion(val message: String) : UnitFormEvent

    /** An applied fix was rolled back. */
    data object AiOpsUndone : UnitFormEvent
}

/**
 * Form state for Unit create/edit screen
 */
data class UnitFormState(
    val uid: String = "",
    val name: String = "",
    val shortName: String = "",
    val decimalPlaces: String = "2",
    val description: String = "",
    val category: String = "",
    val active: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for Unit Form Screen (Create/Edit)
 *
 * Features:
 * - Create new unit or edit existing unit
 * - Form validation
 * - UID generation for new units
 * - Offline-first save pattern
 */
@AssistedInject
class UnitFormViewModel(
    private val unitRepository: UnitRepository,
    private val syncService: CentralSyncService,
    private val aiOpsRunner: AiOpsRunner,
    private val aiOpsUndo: AiOpsUndo,
    @Assisted private val unitId: String?
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(unitId: String?): UnitFormViewModel
    }

    private val _formState = MutableStateFlow(UnitFormState())
    val formState: StateFlow<UnitFormState> = _formState.asStateFlow()

    private val _events = MutableSharedFlow<UnitFormEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<UnitFormEvent> = _events.asSharedFlow()

    init {
        if (unitId != null) {
            loadUnit(unitId)
        }
    }

    /**
     * Load existing unit for editing
     */
    private fun loadUnit(id: String) {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }

            try {
                val unit = unitRepository.getUnitById(id)
                if (unit != null) {
                    _formState.update {
                        it.copy(
                            uid = unit.uid,
                            name = unit.name,
                            shortName = unit.shortName,
                            decimalPlaces = unit.decimalPlaces.toString(),
                            description = unit.description ?: "",
                            category = unit.category ?: "",
                            active = unit.active,
                            isLoading = false
                        )
                    }
                } else {
                    _formState.update {
                        it.copy(
                            isLoading = false,
                            error = "Unit not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load unit"
                    )
                }
            }
        }
    }

    // ======================================
    // FORM FIELD UPDATES
    // ======================================

    fun updateName(name: String) {
        _formState.update { it.copy(name = name, error = null) }
    }

    fun updateShortName(shortName: String) {
        _formState.update { it.copy(shortName = shortName, error = null) }
    }

    fun updateDecimalPlaces(decimalPlaces: String) {
        _formState.update { it.copy(decimalPlaces = decimalPlaces, error = null) }
    }

    fun updateDescription(description: String) {
        _formState.update { it.copy(description = description) }
    }

    fun updateCategory(category: String) {
        _formState.update { it.copy(category = category) }
    }

    fun updateActive(active: Boolean) {
        _formState.update { it.copy(active = active) }
    }

    // ======================================
    // SAVE OPERATION
    // ======================================

    /**
     * Save unit (create or update)
     *
     * Validates form, generates UID for new units, and saves using offline-first pattern
     *
     * @param onSuccess Callback on successful save (for navigation)
     */
    fun saveUnit(onSuccess: () -> kotlin.Unit) {
        viewModelScope.launch {
            if (!validateForm()) {
                return@launch
            }

            _formState.update { it.copy(isLoading = true, error = null) }

            try {
                val state = _formState.value
                val decimalPlaces = state.decimalPlaces.toIntOrNull() ?: UnitConstants.DEFAULT_DECIMAL_PLACES

                val unit = Unit(
                    uid = if (unitId != null) {
                        state.uid
                    } else {
                        // Generate UID for new units
                        UidGenerator.generateUid(UnitConstants.UNIT_UID_PREFIX)
                    },
                    name = state.name.trim(),
                    shortName = state.shortName.trim(),
                    decimalPlaces = decimalPlaces,
                    description = state.description.trim().ifBlank { null },
                    category = state.category.trim().ifBlank { null },
                    active = state.active
                )

                val result = if (unitId != null) {
                    unitRepository.updateUnit(unit)
                } else {
                    unitRepository.createUnit(unit)
                }

                if (result.isSuccess) {
                    syncService.markPendingPush(SyncEntity.UNIT)
                    // Best-effort AI Ops pass on the saved unit (standardize short name, etc.).
                    // Never blocks or fails the save — the runner swallows its own errors.
                    val outcome = runCatching { aiOpsRunner.onEntitySaved("unit", unit.uid) }.getOrNull()
                    outcome?.autoFixed?.firstOrNull { !it.after.isNullOrBlank() }?.let { fix ->
                        _events.tryEmit(UnitFormEvent.AiOpsShortNameFixed(fix.after!!, fix.decisionId))
                    }
                    if (outcome?.autoFixed.isNullOrEmpty()) {
                        outcome?.suggestions?.firstOrNull()?.let { s ->
                            _events.tryEmit(UnitFormEvent.AiOpsSuggestion(s.summary))
                        }
                    }
                    _formState.update { it.copy(isLoading = false) }
                    onSuccess()
                } else {
                    _formState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to save unit"
                        )
                    }
                }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An error occurred"
                    )
                }
            }
        }
    }

    /** Roll back an auto-applied AI Ops fix (from the "Undo" snackbar action). Best-effort. */
    fun undoFix(decisionId: String) {
        viewModelScope.launch {
            val reverted = runCatching { aiOpsUndo.undo(decisionId) }.getOrDefault(false)
            if (reverted) {
                // Reflect the restored value in the open form, if we're editing that unit.
                unitId?.let { id ->
                    runCatching { unitRepository.getUnitById(id) }.getOrNull()?.let { unit ->
                        _formState.update { it.copy(shortName = unit.shortName) }
                    }
                }
                _events.tryEmit(UnitFormEvent.AiOpsUndone)
            }
        }
    }

    // ======================================
    // VALIDATION
    // ======================================

    /**
     * Validate form fields
     *
     * @return true if valid, false otherwise (sets error message)
     */
    private fun validateForm(): Boolean {
        val state = _formState.value

        // Name is required
        if (state.name.isBlank()) {
            _formState.update { it.copy(error = "Unit name is required") }
            return false
        }

        // Short name is required
        if (state.shortName.isBlank()) {
            _formState.update { it.copy(error = "Short name is required") }
            return false
        }

        // Decimal places must be a valid non-negative number
        val decimalPlaces = state.decimalPlaces.toIntOrNull()
        if (decimalPlaces == null || decimalPlaces < 0) {
            _formState.update { it.copy(error = "Decimal places must be a non-negative number") }
            return false
        }

        return true
    }
}
