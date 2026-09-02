package com.ampairs.cbemployee.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.cbemployee.data.repository.EmployeeRepository
import com.ampairs.cbemployee.domain.model.Employee
import com.ampairs.cbemployee.domain.model.MaintenanceRoles
import com.ampairs.cbstore.data.repository.StoreLookup
import com.ampairs.cbstore.domain.model.ZonalOffice
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val EMPLOYEE_UID_PREFIX = "EMP"

data class CbEmployeeFormUiState(
    val employeeNo: String = "",
    val name: String = "",
    val role: String = MaintenanceRoles.EXECUTIVE,
    val mobile: String = "",
    val zonalOfficeId: String = "",
    val reportsToEmployeeId: String = "",
    val managerOptions: List<Employee> = emptyList(),
    val zonalOfficeOptions: List<ZonalOffice> = emptyList(),
    val isEdit: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
) {
    val isValid: Boolean get() = employeeNo.isNotBlank() && name.isNotBlank()
}

@AssistedInject
class CbEmployeeFormViewModel(
    @Assisted private val employeeId: String?,
    private val repository: EmployeeRepository,
    private val storeLookup: StoreLookup,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CbEmployeeFormUiState(isEdit = employeeId != null))
    val uiState: StateFlow<CbEmployeeFormUiState> = _uiState.asStateFlow()

    init {
        // Reporting-manager options — every active member except the one being edited (no self-report).
        repository.observeEmployees()
            .onEach { list ->
                _uiState.update { it.copy(managerOptions = list.filter { e -> e.uid != employeeId }) }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            _uiState.update { it.copy(zonalOfficeOptions = storeLookup.activeZonalOffices()) }
        }
        if (employeeId != null) {
            viewModelScope.launch {
                repository.getEmployee(employeeId)?.let { e ->
                    _uiState.update {
                        it.copy(
                            employeeNo = e.employeeNo,
                            name = e.name,
                            role = e.role,
                            mobile = e.mobile ?: "",
                            zonalOfficeId = e.zonalOfficeId ?: "",
                            reportsToEmployeeId = e.reportsToEmployeeId ?: "",
                        )
                    }
                }
            }
        }
    }

    fun onEmployeeNo(v: String) = _uiState.update { it.copy(employeeNo = v) }
    fun onName(v: String) = _uiState.update { it.copy(name = v) }
    fun onRole(v: String) = _uiState.update { it.copy(role = v) }
    fun onMobile(v: String) = _uiState.update { it.copy(mobile = v) }
    fun onZone(v: String) = _uiState.update { it.copy(zonalOfficeId = v) }
    fun onManager(v: String) = _uiState.update { it.copy(reportsToEmployeeId = v) }

    fun save() {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(error = "Employee number and name are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val uid = employeeId ?: UidGenerator.generateUid(EMPLOYEE_UID_PREFIX)
            val result = repository.saveEmployee(
                Employee(
                    uid = uid,
                    employeeNo = state.employeeNo.trim(),
                    name = state.name.trim(),
                    role = state.role,
                    mobile = state.mobile.trim().ifBlank { null },
                    zonalOfficeId = state.zonalOfficeId.trim().ifBlank { null },
                    reportsToEmployeeId = state.reportsToEmployeeId.ifBlank { null },
                    active = true,
                ),
            )
            _uiState.update {
                if (result.isSuccess) it.copy(isSaving = false, saved = true)
                else it.copy(isSaving = false, error = result.exceptionOrNull()?.message ?: "Failed to save employee")
            }
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(employeeId: String?): CbEmployeeFormViewModel
    }
}
