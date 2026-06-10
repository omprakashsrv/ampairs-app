package com.ampairs.form.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.form.data.repository.ConfigRepository
import com.ampairs.form.domain.FieldSource
import com.ampairs.form.domain.FormField
import com.ampairs.form.domain.FormSchema
import com.ampairs.form.domain.FormSection
import com.ampairs.form.render.CustomFieldWidgetRegistry
import com.ampairs.form.render.DynamicOptionRegistry
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
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

/** UI state for the Form Configuration editor (spec 011, US2). */
data class FormConfigUiState(
    val entityType: String = "",
    val working: FormSchema? = null,          // editable copy
    val collapsed: Set<String> = emptySet(),  // collapsed section uids (view-only)
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val dirty: Boolean = false,
    val toast: String? = null,
)

@AssistedInject
class FormConfigViewModel(
    @Assisted private val entityType: String,
    private val configRepository: ConfigRepository,
    private val syncService: CentralSyncService,
    val optionRegistry: DynamicOptionRegistry,
    val widgetRegistry: CustomFieldWidgetRegistry,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(entityType: String): FormConfigViewModel
    }

    private val _uiState = MutableStateFlow(FormConfigUiState(entityType = entityType))
    val uiState: StateFlow<FormConfigUiState> = _uiState.asStateFlow()

    private var baseline: String = ""          // content signature of the last saved/synced schema
    private var lastSynced: FormSchema? = null // for discard

    init {
        configRepository.observeSchema(entityType)
            .onEach { schema -> onServerSchema(schema) }
            .launchIn(viewModelScope)
        // First run is a full pull → backend seeds standard fields.
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.FORM))
    }

    private fun onServerSchema(schema: FormSchema?) {
        if (schema == null) {
            _uiState.update { it.copy(isLoading = it.working == null) }
            return
        }
        lastSynced = schema
        _uiState.update { st ->
            if (st.working == null || !st.dirty) {
                baseline = contentKey(schema)
                st.copy(working = schema, isLoading = false, dirty = false)
            } else {
                st.copy(isLoading = false) // keep local edits
            }
        }
    }

    // ---- dirty tracking -------------------------------------------------------------------------

    private fun contentKey(s: FormSchema): String = buildString {
        s.sections.sortedBy { it.displayOrder }.forEach { sec ->
            append(sec.uid).append('|').append(sec.name).append('|').append(sec.displayOrder).append('|').append(sec.visible).append('\n')
        }
        append("##\n")
        s.fields.sortedWith(compareBy({ it.sectionUid }, { it.displayOrder })).forEach { f ->
            append(f.uid).append('|').append(f.sectionUid).append('|').append(f.displayOrder)
                .append('|').append(f.displayName).append('|').append(f.dataType).append('|').append(f.source)
                .append('|').append(f.visible).append('|').append(f.mandatory).append('|').append(f.enabled)
                .append('|').append(f.defaultValue).append('|').append(f.placeholder).append('|').append(f.helpText)
                .append('|').append(f.optionSource).append('|').append(f.enumValues).append('|').append(f.dynamicSourceKey)
                .append('|').append(f.widgetKey).append('|').append(f.validationRules).append('\n')
        }
    }

    private fun mutate(transform: (FormSchema) -> FormSchema) {
        _uiState.update { st ->
            val w = st.working ?: return@update st
            val next = transform(w)
            st.copy(working = next, dirty = contentKey(next) != baseline)
        }
    }

    // ---- field ops ------------------------------------------------------------------------------

    fun patchField(fieldUid: String, transform: (FormField) -> FormField) = mutate { s ->
        s.copy(fields = s.fields.map { if (it.uid == fieldUid) transform(it) else it })
    }

    fun setFieldVisible(fieldUid: String, visible: Boolean) = patchField(fieldUid) {
        it.copy(visible = visible, mandatory = if (visible) it.mandatory else false)
    }

    fun setFieldRequired(fieldUid: String, required: Boolean) = patchField(fieldUid) { it.copy(mandatory = required) }

    fun setFieldReadOnly(fieldUid: String, readOnly: Boolean) = patchField(fieldUid) {
        it.copy(enabled = !readOnly, mandatory = if (readOnly) false else it.mandatory)
    }

    fun renameField(fieldUid: String, label: String) = patchField(fieldUid) { it.copy(displayName = label) }

    fun moveField(sectionUid: String, fieldUid: String, dir: Int) = mutate { s ->
        val ordered = s.fields.filter { it.sectionUid == sectionUid }.sortedBy { it.displayOrder }
        val i = ordered.indexOfFirst { it.uid == fieldUid }; val j = i + dir
        if (i < 0 || j < 0 || j >= ordered.size) return@mutate s
        val a = ordered[i]; val b = ordered[j]
        s.copy(fields = s.fields.map {
            when (it.uid) { a.uid -> it.copy(displayOrder = b.displayOrder); b.uid -> it.copy(displayOrder = a.displayOrder); else -> it }
        })
    }

    /** Move [fieldUid] into [toSectionUid] at [index] (drag-and-drop). */
    fun moveFieldTo(fieldUid: String, toSectionUid: String, index: Int) = mutate { s ->
        val moved = s.fields.firstOrNull { it.uid == fieldUid } ?: return@mutate s
        val target = s.fields.filter { it.sectionUid == toSectionUid && it.uid != fieldUid }.sortedBy { it.displayOrder }.toMutableList()
        target.add(index.coerceIn(0, target.size), moved.copy(sectionUid = toSectionUid))
        val order = target.mapIndexed { idx, f -> f.uid to idx }.toMap()
        s.copy(fields = s.fields.map { f ->
            when {
                f.uid == fieldUid -> f.copy(sectionUid = toSectionUid, displayOrder = order[fieldUid] ?: f.displayOrder)
                order.containsKey(f.uid) -> f.copy(displayOrder = order[f.uid]!!)
                else -> f
            }
        })
    }

    fun deleteField(fieldUid: String) = mutate { s -> s.copy(fields = s.fields.filterNot { it.uid == fieldUid }) }

    fun duplicateField(fieldUid: String) = mutate { s ->
        val src = s.fields.firstOrNull { it.uid == fieldUid } ?: return@mutate s
        s.copy(fields = s.fields + src.copy(uid = UidGenerator.generateUid("FF"), source = FieldSource.CUSTOM,
            displayName = src.displayName + " copy", displayOrder = src.displayOrder + 1))
    }

    /** Insert or update a field from the Add/Edit sheet. New custom fields get a stable, unique key. */
    fun upsertField(draft: FormField, targetSectionUid: String, isNew: Boolean) = mutate { s ->
        if (isNew) {
            val base = draft.fieldKey.ifBlank { draft.displayName.slugKey() }.ifBlank { "field" }
            val key = uniqueFieldKey(base, s.fields.mapTo(mutableSetOf()) { it.fieldKey })
            val order = s.fields.count { it.sectionUid == targetSectionUid }
            s.copy(fields = s.fields + draft.copy(uid = UidGenerator.generateUid("FF"), source = FieldSource.CUSTOM,
                fieldKey = key, sectionUid = targetSectionUid, displayOrder = order))
        } else {
            s.copy(fields = s.fields.map { if (it.uid == draft.uid) draft.copy(sectionUid = targetSectionUid) else it })
        }
    }

    /** Returns [base] if free, otherwise the first `${base}_N` (N≥2) not already used — no duplicate keys. */
    private fun uniqueFieldKey(base: String, existing: Set<String>): String {
        if (base !in existing) return base
        var i = 2
        while ("${base}_$i" in existing) i++
        return "${base}_$i"
    }

    // ---- section ops ----------------------------------------------------------------------------

    fun toggleCollapsed(sectionUid: String) = _uiState.update {
        it.copy(collapsed = if (sectionUid in it.collapsed) it.collapsed - sectionUid else it.collapsed + sectionUid)
    }

    fun setAllCollapsed(collapsed: Boolean) = _uiState.update { st ->
        st.copy(collapsed = if (collapsed) st.working?.sections?.map { it.uid }?.toSet() ?: emptySet() else emptySet())
    }

    fun renameSection(sectionUid: String, name: String) = mutate { s ->
        s.copy(sections = s.sections.map { if (it.uid == sectionUid) it.copy(name = name) else it })
    }

    fun setSectionVisible(sectionUid: String, visible: Boolean) = mutate { s ->
        s.copy(sections = s.sections.map { if (it.uid == sectionUid) it.copy(visible = visible) else it })
    }

    fun moveSection(sectionUid: String, dir: Int) = mutate { s ->
        val ordered = s.sections.sortedBy { it.displayOrder }
        val i = ordered.indexOfFirst { it.uid == sectionUid }; val j = i + dir
        if (i < 0 || j < 0 || j >= ordered.size) return@mutate s
        val a = ordered[i]; val b = ordered[j]
        s.copy(sections = s.sections.map {
            when (it.uid) { a.uid -> it.copy(displayOrder = b.displayOrder); b.uid -> it.copy(displayOrder = a.displayOrder); else -> it }
        })
    }

    /** Move [sectionUid] to absolute [index] (drag-and-drop). */
    fun moveSectionTo(sectionUid: String, index: Int) = mutate { s ->
        val ordered = s.sections.sortedBy { it.displayOrder }.toMutableList()
        val from = ordered.indexOfFirst { it.uid == sectionUid }
        if (from < 0) return@mutate s
        val item = ordered.removeAt(from)
        ordered.add(index.coerceIn(0, ordered.size), item)
        val order = ordered.mapIndexed { idx, sec -> sec.uid to idx }.toMap()
        s.copy(sections = s.sections.map { it.copy(displayOrder = order[it.uid] ?: it.displayOrder) })
    }

    fun addSection() = mutate { s ->
        val order = (s.sections.maxOfOrNull { it.displayOrder } ?: -1) + 1
        s.copy(sections = s.sections + FormSection(uid = UidGenerator.generateUid("FS"), name = "New Section", displayOrder = order))
    }

    /** Delete a section. mode="move" reassigns fields to [target]; "deleteAll" keeps standard in General, drops custom. */
    fun deleteSection(sectionUid: String, mode: String, target: String?) = mutate { s ->
        val general = s.sections.firstOrNull { it.name.equals("General", ignoreCase = true) } ?: s.sections.firstOrNull { it.uid != sectionUid }
        val dest = if (mode == "move") (s.sections.firstOrNull { it.uid == target } ?: general) else general
        val sectionFields = s.fields.filter { it.sectionUid == sectionUid }
        val reassigned = if (mode == "move") sectionFields else sectionFields.filter { it.source == FieldSource.STANDARD }
        val keep = s.fields.filterNot { it.sectionUid == sectionUid } +
            reassigned.map { it.copy(sectionUid = dest?.uid ?: it.sectionUid) }
        s.copy(sections = s.sections.filterNot { it.uid == sectionUid }, fields = keep)
    }

    // ---- save / discard -------------------------------------------------------------------------

    fun save() {
        val working = _uiState.value.working ?: return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = configRepository.saveSchema(working)
            _uiState.update {
                if (result.isSuccess) {
                    baseline = contentKey(working)
                    it.copy(isSaving = false, dirty = false, toast = "All changes saved")
                } else {
                    it.copy(isSaving = false, toast = result.exceptionOrNull()?.message ?: "Couldn't save")
                }
            }
        }
    }

    fun discard() {
        val base = lastSynced ?: return
        baseline = contentKey(base)
        _uiState.update { it.copy(working = base, dirty = false, toast = "Changes discarded") }
    }

    fun clearToast() = _uiState.update { it.copy(toast = null) }
}
