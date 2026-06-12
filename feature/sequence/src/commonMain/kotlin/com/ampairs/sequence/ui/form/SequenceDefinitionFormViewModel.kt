package com.ampairs.sequence.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.sequence.data.repository.SequenceRepository
import com.ampairs.sequence.domain.SequenceFormatter
import com.ampairs.sequence.domain.model.SequenceDefinition
import com.ampairs.sequence.domain.model.SequenceScope
import com.ampairs.sequence.util.SequenceConstants
import com.ampairs.sequence.util.SequenceLogger
import com.ampairs.workspace.db.OfflineFirstWorkspaceMemberRepository
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

/** Workspace member option for the USER-scope picker. */
data class MemberOption(
    val userId: String,
    val name: String,
)

data class SequenceDefinitionFormState(
    val uid: String = "",
    val entityType: String = "",
    val scope: String = SequenceScope.WORKSPACE,
    val selectedUserId: String? = null,
    val members: List<MemberOption> = emptyList(),
    val prefix: String = "",
    val suffix: String = "",
    val paddingLength: String = "0",
    val startValue: String = "1",
    val incrementStep: String = "1",
    val active: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val selectedUserName: String
        get() = members.firstOrNull { it.userId == selectedUserId }?.name ?: ""

    /** Live sample of the first number this scheme would issue. */
    val preview: String
        get() = SequenceFormatter.format(
            prefix = prefix.trim().ifBlank { null },
            suffix = suffix.trim().ifBlank { null },
            paddingLength = paddingLength.toIntOrNull() ?: 0,
            value = startValue.toLongOrNull() ?: 1L,
        )

    val isValid: Boolean
        get() = entityType.isNotBlank() &&
            (scope != SequenceScope.USER || !selectedUserId.isNullOrBlank()) &&
            (paddingLength.toIntOrNull() ?: -1) in 0..SequenceConstants.MAX_PADDING_LENGTH &&
            (startValue.toLongOrNull() ?: 0L) >= 1L &&
            (incrementStep.toIntOrNull() ?: 0) >= 1
}

@AssistedInject
class SequenceDefinitionFormViewModel(
    private val repository: SequenceRepository,
    private val memberRepository: OfflineFirstWorkspaceMemberRepository,
    private val workspaceConfig: WorkspaceConfig,
    @Assisted private val definitionUid: String?,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(definitionUid: String?): SequenceDefinitionFormViewModel
    }

    private val _formState = MutableStateFlow(SequenceDefinitionFormState())
    val formState: StateFlow<SequenceDefinitionFormState> = _formState.asStateFlow()

    init {
        if (definitionUid != null) {
            loadDefinition(definitionUid)
        }
        loadMembers()
    }

    private fun loadDefinition(uid: String) {
        viewModelScope.launch {
            val definition = repository.getDefinitionByUid(uid) ?: return@launch
            _formState.update {
                it.copy(
                    uid = definition.uid,
                    entityType = definition.entityType,
                    scope = definition.scope,
                    selectedUserId = definition.userId,
                    prefix = definition.prefix.orEmpty(),
                    suffix = definition.suffix.orEmpty(),
                    paddingLength = definition.paddingLength.toString(),
                    startValue = definition.startValue.toString(),
                    incrementStep = definition.incrementStep.toString(),
                    active = definition.active,
                )
            }
        }
    }

    private fun loadMembers() {
        viewModelScope.launch {
            try {
                memberRepository.getLocalWorkspaceMembers(workspaceConfig.workspaceId).collect { members ->
                    _formState.update { state ->
                        state.copy(members = members.map { MemberOption(userId = it.userId, name = it.name) })
                    }
                }
            } catch (e: Exception) {
                SequenceLogger.w("SequenceDefinitionFormViewModel", "Failed to load workspace members", e)
            }
        }
    }

    fun updateEntityType(value: String) = _formState.update { it.copy(entityType = value, error = null) }
    fun updateScope(value: String) = _formState.update { it.copy(scope = value, error = null) }
    fun updateSelectedUser(userId: String) = _formState.update { it.copy(selectedUserId = userId, error = null) }
    fun updatePrefix(value: String) = _formState.update { it.copy(prefix = value, error = null) }
    fun updateSuffix(value: String) = _formState.update { it.copy(suffix = value, error = null) }
    fun updatePaddingLength(value: String) = _formState.update { it.copy(paddingLength = value, error = null) }
    fun updateStartValue(value: String) = _formState.update { it.copy(startValue = value, error = null) }
    fun updateIncrementStep(value: String) = _formState.update { it.copy(incrementStep = value, error = null) }
    fun updateActive(value: Boolean) = _formState.update { it.copy(active = value, error = null) }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _formState.value
            if (!state.isValid) return@launch

            _formState.update { it.copy(isLoading = true, error = null) }

            val definition = SequenceDefinition(
                uid = state.uid.ifBlank {
                    UidGenerator.generateUid(SequenceConstants.SEQUENCE_DEFINITION_UID_PREFIX)
                },
                entityType = state.entityType.trim().lowercase(),
                scope = state.scope,
                userId = if (state.scope == SequenceScope.USER) state.selectedUserId else null,
                prefix = state.prefix.trim().ifBlank { null },
                suffix = state.suffix.trim().ifBlank { null },
                paddingLength = state.paddingLength.toIntOrNull() ?: 0,
                startValue = state.startValue.toLongOrNull() ?: 1L,
                incrementStep = state.incrementStep.toIntOrNull() ?: 1,
                active = state.active,
            )

            val result = repository.saveDefinition(definition)
            if (result.isSuccess) {
                _formState.update { it.copy(isLoading = false) }
                onSuccess()
            } else {
                _formState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to save numbering scheme"
                    )
                }
            }
        }
    }
}
