package com.ampairs.sfa.ui.beat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sfa.data.repository.SfaRepository
import com.ampairs.sfa.domain.model.Beat
import com.ampairs.sfa.util.SfaConstants
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

data class BeatFormState(
    val uid: String = "",
    val name: String = "",
    val description: String = "",
    val repMemberUid: String = "",
    val scheduledDays: String = "",
    val active: Boolean = true,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
)

@AssistedInject
class BeatFormViewModel(
    private val sfaRepository: SfaRepository,
    @Assisted private val beatId: String?,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(beatId: String?): BeatFormViewModel
    }

    private val _formState = MutableStateFlow(BeatFormState())
    val formState: StateFlow<BeatFormState> = _formState.asStateFlow()

    init {
        if (beatId != null) loadBeat(beatId)
    }

    private fun loadBeat(id: String) {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            val beat = sfaRepository.getBeatById(id)
            if (beat != null) {
                _formState.update {
                    it.copy(
                        uid = beat.uid,
                        name = beat.name,
                        description = beat.description ?: "",
                        repMemberUid = beat.repMemberUid ?: "",
                        scheduledDays = beat.scheduledDays ?: "",
                        active = beat.active,
                        isLoading = false,
                    )
                }
            } else {
                _formState.update { it.copy(isLoading = false, error = "Beat not found") }
            }
        }
    }

    fun updateName(value: String) = _formState.update { it.copy(name = value) }
    fun updateDescription(value: String) = _formState.update { it.copy(description = value) }
    fun updateRepMemberUid(value: String) = _formState.update { it.copy(repMemberUid = value) }
    fun updateScheduledDays(value: String) = _formState.update { it.copy(scheduledDays = value) }

    fun save(onSuccess: () -> Unit) {
        val state = _formState.value
        if (state.name.isBlank()) {
            _formState.update { it.copy(error = "Name is required") }
            return
        }
        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true, error = null) }
            val uid = state.uid.ifBlank { UidGenerator.generateUid(SfaConstants.BEAT_UID_PREFIX) }
            val beat = Beat(
                uid = uid,
                name = state.name.trim(),
                description = state.description.trim().ifBlank { null },
                repMemberUid = state.repMemberUid.trim().ifBlank { null },
                scheduledDays = state.scheduledDays.trim().ifBlank { null },
                active = state.active,
            )
            val result = sfaRepository.saveBeat(beat)
            if (result.isSuccess) {
                onSuccess()
            } else {
                _formState.update { it.copy(isSaving = false, error = result.exceptionOrNull()?.message ?: "Failed to save beat") }
            }
        }
    }
}
