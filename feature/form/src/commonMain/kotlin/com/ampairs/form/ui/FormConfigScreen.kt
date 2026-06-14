package com.ampairs.form.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.form.generated.resources.Res
import ampairsapp.feature.form.generated.resources.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.ampairs.common.navigation.ScreenBackButton
import com.ampairs.form.domain.FieldSource
import com.ampairs.form.domain.FormField
import com.ampairs.form.domain.FormSchema
import com.ampairs.form.domain.FormSection
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import kotlinx.coroutines.delay

/** Which sheet/dialog is open. */
private sealed interface EditorSheet {
    data class AddField(val sectionUid: String) : EditorSheet
    data class EditField(val fieldUid: String) : EditorSheet
    data class Advanced(val fieldUid: String) : EditorSheet
    data object Sections : EditorSheet
    data class DeleteSection(val sectionUid: String) : EditorSheet
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormConfigScreen(
    entityType: String,
    onNavigateBack: () -> Unit,
    viewModel: FormConfigViewModel = assistedMetroViewModel<FormConfigViewModel, FormConfigViewModel.Factory>(key = entityType) { create(entityType) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    var sheet by remember { mutableStateOf<EditorSheet?>(null) }
    var showPreviewSheet by remember { mutableStateOf(false) }
    var showExitGuard by remember { mutableStateOf(false) }
    val dnd = remember { FormDndState() }

    // Unsaved-changes navigation guard: leave immediately when clean, otherwise confirm (T046).
    val guardedBack: () -> Unit = { if (state.dirty) showExitGuard = true else onNavigateBack() }

    LaunchedEffect(state.toast) {
        if (state.toast != null) { delay(2200); viewModel.clearToast() }
    }

    if (showExitGuard) {
        AlertDialog(
            onDismissRequest = { showExitGuard = false },
            title = { Text(stringResource(Res.string.form_unsaved_title)) },
            text = { Text(stringResource(Res.string.form_unsaved_message)) },
            confirmButton = {
                TextButton(onClick = { showExitGuard = false; viewModel.save(); onNavigateBack() }) {
                    Text(stringResource(Res.string.form_unsaved_save_leave))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showExitGuard = false; viewModel.discard(); onNavigateBack() }) {
                        Text(stringResource(Res.string.form_unsaved_discard_leave))
                    }
                    TextButton(onClick = { showExitGuard = false }) {
                        Text(stringResource(Res.string.form_unsaved_keep_editing))
                    }
                }
            },
        )
    }

    Box(Modifier.fillMaxSize().background(scheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                entityLabel = entityType.replaceFirstChar { it.uppercase() },
                dirty = state.dirty,
                saving = state.isSaving,
                onBack = guardedBack,
                onSave = viewModel::save,
                onDiscard = viewModel::discard,
                onManageSections = { sheet = EditorSheet.Sections },
                onExpandAll = { viewModel.setAllCollapsed(false) },
                onCollapseAll = { viewModel.setAllCollapsed(true) },
            )

            if (state.dirty && !state.isLoading) UnsavedBanner(saving = state.isSaving, onDiscard = viewModel::discard, onSave = viewModel::save)

            val schema = state.working
            if (state.isLoading || schema == null) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text(stringResource(Res.string.form_edit_loading), color = scheme.onSurfaceVariant) }
            } else {
                val orderedSections = schema.sections.sortedBy { it.displayOrder }
                val sectionUids = orderedSections.map { it.uid }
                val fieldsOf: (String) -> List<FormField> = { sid ->
                    schema.fields.filter { it.sectionUid == sid }.sortedBy { it.displayOrder }
                }
                val onFieldDrop: (String) -> Unit = drop@{ uid ->
                    val target = dnd.hoveredSection(sectionUids) ?: return@drop
                    val idx = dnd.fieldInsertionIn(target, fieldsOf(target).map { it.uid }) ?: return@drop
                    viewModel.moveFieldTo(uid, target, idx)
                }
                val onSectionDrop: (String) -> Unit = drop@{ uid ->
                    val idx = dnd.sectionInsertion(sectionUids) ?: return@drop
                    viewModel.moveSectionTo(uid, idx)
                }

                Row(Modifier.fillMaxSize()) {
                    LazyColumn(
                        Modifier.weight(1f).fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            Toolbar(schema,
                                onManageSections = { sheet = EditorSheet.Sections },
                                onAddField = { sheet = EditorSheet.AddField(sectionUids.first()) })
                        }
                        itemsIndexed(orderedSections, key = { _, s -> s.uid }) { index, sec ->
                            SectionCard(
                                section = sec,
                                index = index,
                                total = orderedSections.size,
                                fields = fieldsOf(sec.uid),
                                collapsed = sec.uid in state.collapsed,
                                vm = viewModel,
                                dnd = dnd,
                                onFieldDrop = onFieldDrop,
                                onSectionDrop = onSectionDrop,
                                onAddField = { sheet = EditorSheet.AddField(sec.uid) },
                                onEditField = { sheet = EditorSheet.EditField(it) },
                                onAdvanced = { sheet = EditorSheet.Advanced(it) },
                                onDeleteSection = { sheet = EditorSheet.DeleteSection(sec.uid) },
                            )
                        }
                        item { AddDashedButton(stringResource(Res.string.form_edit_add_a_section), Icons.Filled.CreateNewFolder, onClick = viewModel::addSection) }
                    }

                    if (isExpanded) {
                        Surface(Modifier.width(420.dp).fillMaxSize(), color = scheme.surfaceContainer) {
                            Column(Modifier.fillMaxSize()) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Visibility, null, tint = scheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(Res.string.form_edit_live_preview), style = MaterialTheme.typography.titleSmall)
                                }
                                HorizontalDivider()
                                LivePreview(schema, Modifier.weight(1f).padding(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Compact: floating preview button
        if (!isExpanded && !state.isLoading && state.working != null) {
            ExtendedFloatingActionButton(
                onClick = { showPreviewSheet = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                icon = { Icon(Icons.Filled.Visibility, null) },
                text = { Text(stringResource(Res.string.form_edit_preview_form)) },
            )
        }

        // Toast
        state.toast?.let { toast ->
            val msg = when (toast) {
                FormToast.Saved -> stringResource(Res.string.form_edit_toast_saved)
                FormToast.Discarded -> stringResource(Res.string.form_edit_toast_discarded)
                is FormToast.SaveFailed -> toast.message ?: stringResource(Res.string.form_edit_toast_save_failed)
            }
            Surface(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                shape = RoundedCornerShape(8.dp),
                color = scheme.inverseSurface,
                shadowElevation = 4.dp,
            ) {
                Text(msg, Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = scheme.inverseOnSurface, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Sheets / dialogs
        val schema = state.working
        if (schema != null) {
            when (val s = sheet) {
                is EditorSheet.AddField -> AddEditFieldSheet(
                    schema = schema, sectionUid = s.sectionUid, field = null,
                    sourceKeys = viewModel.optionRegistry.keys,
                    onClose = { sheet = null },
                    onSave = { draft, target, isNew -> viewModel.upsertField(draft, target, isNew); sheet = null },
                )
                is EditorSheet.EditField -> schema.fields.firstOrNull { it.uid == s.fieldUid }?.let { f ->
                    AddEditFieldSheet(
                        schema = schema, sectionUid = f.sectionUid, field = f,
                        sourceKeys = viewModel.optionRegistry.keys,
                        onClose = { sheet = null },
                        onSave = { draft, target, isNew -> viewModel.upsertField(draft, target, isNew); sheet = null },
                    )
                }
                is EditorSheet.Advanced -> schema.fields.firstOrNull { it.uid == s.fieldUid }?.let { f ->
                    AdvancedSheet(
                        sectionName = schema.sections.firstOrNull { it.uid == f.sectionUid }?.name ?: "",
                        field = f,
                        sourceKeys = viewModel.optionRegistry.keys,
                        onClose = { sheet = null },
                        onApply = { updated -> viewModel.patchField(updated.uid) { updated }; sheet = null },
                    )
                }
                EditorSheet.Sections -> ManageSectionsSheet(
                    schema = schema,
                    onClose = { sheet = null },
                    onRename = viewModel::renameSection,
                    onMove = viewModel::moveSection,
                    onToggleVisible = { uid -> viewModel.setSectionVisible(uid, !(schema.sections.firstOrNull { it.uid == uid }?.visible ?: true)) },
                    onAdd = viewModel::addSection,
                    onDelete = { uid -> sheet = EditorSheet.DeleteSection(uid) },
                )
                is EditorSheet.DeleteSection -> schema.sections.firstOrNull { it.uid == s.sectionUid }?.let { sec ->
                    DeleteSectionDialog(
                        schema = schema, section = sec,
                        onClose = { sheet = null },
                        onConfirm = { mode, target -> viewModel.deleteSection(sec.uid, mode, target); sheet = null },
                    )
                }
                null -> {}
            }
        }
    }

    if (showPreviewSheet && state.working != null) {
        ModalBottomSheet(onDismissRequest = { showPreviewSheet = false }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Visibility, null, tint = scheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.form_edit_live_preview), style = MaterialTheme.typography.titleMedium)
                }
                LivePreview(state.working!!, Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun TopBar(
    entityLabel: String, dirty: Boolean, saving: Boolean,
    onBack: () -> Unit, onSave: () -> Unit, onDiscard: () -> Unit,
    onManageSections: () -> Unit, onExpandAll: () -> Unit, onCollapseAll: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    Surface(color = scheme.surface) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScreenBackButton(onClick = onBack, contentDescription = stringResource(Res.string.form_edit_back_cd))
            Spacer(Modifier.width(4.dp))
            Column {
                Text(stringResource(Res.string.form_edit_overline), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                Text(stringResource(Res.string.form_edit_title), style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(12.dp))
            Surface(color = scheme.surfaceContainerHigh, shape = RoundedCornerShape(8.dp)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.People, null, Modifier.size(18.dp), tint = scheme.onPrimaryContainer)
                    Spacer(Modifier.width(6.dp))
                    Text(entityLabel, style = MaterialTheme.typography.titleSmall)
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDiscard, enabled = dirty && !saving) { Text(stringResource(Res.string.form_edit_discard)) }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onSave, enabled = dirty || saving) {
                Icon(if (dirty) Icons.Filled.CloudUpload else Icons.Filled.CloudDone, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (saving) stringResource(Res.string.form_edit_saving) else if (dirty) stringResource(Res.string.form_edit_save) else stringResource(Res.string.form_edit_saved))
            }
            Box {
                IconButton({ menuOpen = true }) { Icon(Icons.Filled.MoreVert, stringResource(Res.string.form_edit_more_cd)) }
                DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(stringResource(Res.string.form_edit_manage_sections)) }, leadingIcon = { Icon(Icons.Filled.ViewAgenda, null) }, onClick = { onManageSections(); menuOpen = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.form_edit_expand_all)) }, onClick = { onExpandAll(); menuOpen = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.form_edit_collapse_all)) }, onClick = { onCollapseAll(); menuOpen = false })
                }
            }
        }
    }
}

@Composable
private fun UnsavedBanner(saving: Boolean, onDiscard: () -> Unit, onSave: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(color = scheme.secondaryContainer) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(scheme.secondary, CircleShape))
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(Res.string.form_edit_unsaved_banner),
                style = MaterialTheme.typography.bodyMedium, color = scheme.onSecondaryContainer, modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDiscard) { Text(stringResource(Res.string.form_edit_discard)) }
            FilledTonalButton(onClick = onSave) { Text(if (saving) stringResource(Res.string.form_edit_saving) else stringResource(Res.string.form_edit_save_changes)) }
        }
    }
}

@Composable
private fun Toolbar(schema: FormSchema, onManageSections: () -> Unit, onAddField: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val total = schema.fields.size
    val shown = schema.fields.count { f -> f.visible && schema.sections.firstOrNull { it.uid == f.sectionUid }?.visible == true }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(Res.string.form_edit_summary, schema.sections.size, total, shown),
            style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        OutlinedButton(onClick = onManageSections) {
            Icon(Icons.Filled.ViewAgenda, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(Res.string.form_edit_sections))
        }
        Spacer(Modifier.width(8.dp))
        FilledTonalButton(onClick = onAddField) {
            Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(Res.string.form_edit_add_field))
        }
    }
}

@Composable
private fun SectionCard(
    section: FormSection, index: Int, total: Int, fields: List<FormField>, collapsed: Boolean,
    vm: FormConfigViewModel, dnd: FormDndState,
    onFieldDrop: (String) -> Unit, onSectionDrop: (String) -> Unit,
    onAddField: () -> Unit, onEditField: (String) -> Unit, onAdvanced: (String) -> Unit, onDeleteSection: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    val isDraggingThis = dnd.dragSectionUid == section.uid
    val insertIdx = dnd.fieldInsertionIn(section.uid, fields.map { it.uid })

    Surface(
        Modifier.fillMaxWidth().dndBounds(dnd, "s:${section.uid}").alpha(if (isDraggingThis) 0.5f else 1f),
        shape = RoundedCornerShape(16.dp),
        color = if (section.visible) scheme.surface else scheme.surfaceContainerLow,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        shadowElevation = 1.dp,
    ) {
        Column {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ vm.toggleCollapsed(section.uid) }) { Icon(Icons.Filled.ExpandMore, if (collapsed) stringResource(Res.string.form_edit_expand_cd) else stringResource(Res.string.form_edit_collapse_cd)) }
                Box(Modifier.sectionDragHandle(dnd, section.uid, onSectionDrop)) {
                    Icon(Icons.Filled.DragIndicator, stringResource(Res.string.form_edit_drag_section_cd), Modifier.size(20.dp), tint = scheme.outline)
                }
                Spacer(Modifier.width(6.dp))
                InlineEditableText(section.name, MaterialTheme.typography.titleMedium, onCommit = { vm.renameSection(section.uid, it) })
                if (section.name.equals("General", true)) { Spacer(Modifier.width(8.dp)); Chip(stringResource(Res.string.form_edit_chip_default), scheme.surfaceContainerHigh, scheme.onSurfaceVariant) }
                if (!section.visible) { Spacer(Modifier.width(8.dp)); Chip(stringResource(Res.string.form_edit_chip_hidden), scheme.surfaceContainerHighest, scheme.onSurfaceVariant) }
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.form_edit_shown_count, fields.count { it.visible }, fields.size), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                IconButton({ vm.setSectionVisible(section.uid, !section.visible) }) {
                    Icon(if (section.visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, stringResource(Res.string.form_edit_toggle_section_cd))
                }
                Box {
                    IconButton({ menuOpen = true }) { Icon(Icons.Filled.MoreVert, stringResource(Res.string.form_edit_section_options_cd)) }
                    DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(Res.string.form_edit_add_field)) }, leadingIcon = { Icon(Icons.Filled.Add, null) }, onClick = { onAddField(); menuOpen = false })
                        DropdownMenuItem(text = { Text(if (section.visible) stringResource(Res.string.form_edit_hide_section) else stringResource(Res.string.form_edit_show_section)) }, leadingIcon = { Icon(if (section.visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null) }, onClick = { vm.setSectionVisible(section.uid, !section.visible); menuOpen = false })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text(stringResource(Res.string.form_edit_move_up)) }, enabled = index > 0, leadingIcon = { Icon(Icons.Filled.ArrowUpward, null) }, onClick = { vm.moveSection(section.uid, -1); menuOpen = false })
                        DropdownMenuItem(text = { Text(stringResource(Res.string.form_edit_move_down)) }, enabled = index < total - 1, leadingIcon = { Icon(Icons.Filled.ArrowDownward, null) }, onClick = { vm.moveSection(section.uid, 1); menuOpen = false })
                        HorizontalDivider()
                        if (section.name.equals("General", true)) {
                            DropdownMenuItem(text = { Text(stringResource(Res.string.form_edit_default_no_delete)) }, enabled = false, leadingIcon = { Icon(Icons.Filled.Lock, null) }, onClick = {})
                        } else {
                            DropdownMenuItem(text = { Text(stringResource(Res.string.form_edit_delete_section)) }, leadingIcon = { Icon(Icons.Filled.Delete, null, tint = scheme.error) }, onClick = { onDeleteSection(); menuOpen = false })
                        }
                    }
                }
            }
            if (!collapsed) {
                HorizontalDivider(color = scheme.outlineVariant)
                Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    if (fields.isEmpty()) {
                        if (insertIdx != null) DropIndicator()
                        Text(stringResource(Res.string.form_edit_no_fields), Modifier.padding(12.dp), color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                    fields.forEachIndexed { i, f ->
                        if (insertIdx == i) DropIndicator()
                        FieldRow(f, section.uid, i, fields.size, vm, dnd, onFieldDrop, onEditField, onAdvanced)
                    }
                    if (fields.isNotEmpty() && insertIdx == fields.size) DropIndicator()
                    AddDashedButton(stringResource(Res.string.form_edit_add_field_to, section.name), Icons.Filled.Add, onClick = onAddField)
                }
            }
        }
    }
}

@Composable
private fun DropIndicator() {
    Box(
        Modifier.fillMaxWidth().padding(start = 44.dp, end = 8.dp).height(2.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp)),
    )
}

@Composable
private fun FieldRow(
    f: FormField, sectionUid: String, index: Int, count: Int,
    vm: FormConfigViewModel, dnd: FormDndState, onFieldDrop: (String) -> Unit,
    onEditField: (String) -> Unit, onAdvanced: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    val essential = f.isEssential()
    val isDraggingThis = dnd.dragFieldUid == f.uid

    Row(
        Modifier.fillMaxWidth().dndBounds(dnd, "f:${f.uid}").alpha(if (isDraggingThis) 0.35f else if (!f.visible) 0.55f else 1f).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.fieldDragHandle(dnd, f.uid, onFieldDrop)) {
            Icon(Icons.Filled.DragIndicator, stringResource(Res.string.form_edit_drag_field_cd), Modifier.size(20.dp), tint = scheme.outline)
        }
        Spacer(Modifier.width(6.dp))
        Surface(Modifier.size(34.dp), shape = RoundedCornerShape(8.dp), color = if (f.mandatory) scheme.primaryContainer else scheme.surfaceContainerHigh) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Icon(f.icon(), null, Modifier.size(19.dp), tint = if (f.mandatory) scheme.onPrimaryContainer else scheme.onSurfaceVariant) }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InlineEditableText(f.displayName, MaterialTheme.typography.bodyLarge, onCommit = { vm.renameField(f.uid, it) })
                if (f.mandatory) { Spacer(Modifier.width(6.dp)); Text("*", color = scheme.error, style = MaterialTheme.typography.bodyLarge) }
                Spacer(Modifier.width(8.dp))
                if (f.source == FieldSource.CUSTOM) Chip(stringResource(Res.string.form_edit_chip_custom), scheme.tertiaryContainer, scheme.onTertiaryContainer)
                else Chip(stringResource(Res.string.form_edit_chip_standard), scheme.surface, scheme.onSurfaceVariant, bordered = true)
                if (!f.enabled) { Spacer(Modifier.width(6.dp)); Chip(stringResource(Res.string.form_edit_chip_readonly), scheme.surfaceContainerHighest, scheme.onSurfaceVariant) }
                if (!f.visible) { Spacer(Modifier.width(6.dp)); Chip(stringResource(Res.string.form_edit_chip_hidden), scheme.surfaceContainerHighest, scheme.onSurfaceVariant) }
            }
            Text(f.summaryLine(), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        ControlSwitch(if (essential) stringResource(Res.string.form_edit_always) else stringResource(Res.string.form_edit_shown), f.visible, enabled = !essential) { vm.setFieldVisible(f.uid, it) }
        Spacer(Modifier.width(4.dp))
        ControlSwitch(stringResource(Res.string.form_edit_required), f.mandatory, enabled = f.enabled && f.visible && !essential) { vm.setFieldRequired(f.uid, it) }
        Box {
            IconButton({ menuOpen = true }) { Icon(Icons.Filled.MoreVert, stringResource(Res.string.form_edit_field_options_cd)) }
            DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(stringResource(Res.string.form_edit_advanced)) }, leadingIcon = { Icon(Icons.Filled.Tune, null) }, onClick = { onAdvanced(f.uid); menuOpen = false })
                DropdownMenuItem(text = { Text(stringResource(Res.string.form_edit_edit_field)) }, leadingIcon = { Icon(Icons.Filled.Edit, null) }, onClick = { onEditField(f.uid); menuOpen = false })
                if (f.enabled) DropdownMenuItem(text = { Text(stringResource(Res.string.form_edit_make_readonly)) }, leadingIcon = { Icon(Icons.Filled.Lock, null) }, onClick = { vm.setFieldReadOnly(f.uid, true); menuOpen = false })
                else DropdownMenuItem(text = { Text(stringResource(Res.string.form_edit_allow_editing)) }, leadingIcon = { Icon(Icons.Filled.LockOpen, null) }, onClick = { vm.setFieldReadOnly(f.uid, false); menuOpen = false })
                HorizontalDivider()
                DropdownMenuItem(text = { Text(stringResource(Res.string.form_edit_move_up)) }, enabled = index > 0, leadingIcon = { Icon(Icons.Filled.ArrowUpward, null) }, onClick = { vm.moveField(sectionUid, f.uid, -1); menuOpen = false })
                DropdownMenuItem(text = { Text(stringResource(Res.string.form_edit_move_down)) }, enabled = index < count - 1, leadingIcon = { Icon(Icons.Filled.ArrowDownward, null) }, onClick = { vm.moveField(sectionUid, f.uid, 1); menuOpen = false })
                HorizontalDivider()
                if (f.source == FieldSource.CUSTOM) {
                    DropdownMenuItem(text = { Text(stringResource(Res.string.form_edit_duplicate)) }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null) }, onClick = { vm.duplicateField(f.uid); menuOpen = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.form_edit_delete_field)) }, leadingIcon = { Icon(Icons.Filled.Delete, null, tint = scheme.error) }, onClick = { vm.deleteField(f.uid); menuOpen = false })
                } else {
                    DropdownMenuItem(text = { Text(stringResource(Res.string.form_edit_cant_delete_standard)) }, enabled = false, leadingIcon = { Icon(Icons.Filled.Delete, null) }, onClick = {})
                }
            }
        }
    }
}

@Composable
private fun ControlSwitch(label: String, on: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Switch(checked = on, onCheckedChange = onChange, enabled = enabled)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun Chip(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color, bordered: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (bordered) androidx.compose.ui.graphics.Color.Transparent else bg,
        border = if (bordered) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = fg)
    }
}

@Composable
private fun AddDashedButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = androidx.compose.ui.graphics.Color.Transparent,
        border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(20.dp), tint = scheme.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
        }
    }
}

/** Click-to-edit text (label / section name). */
@Composable
internal fun InlineEditableText(value: String, style: androidx.compose.ui.text.TextStyle, onCommit: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(value) { mutableStateOf(value) }
    if (editing) {
        androidx.compose.material3.OutlinedTextField(
            value = draft, onValueChange = { draft = it }, singleLine = true, textStyle = style,
            modifier = Modifier.width(220.dp),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onCommit(draft.trim().ifBlank { value }); editing = false }),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
        )
    } else {
        Text(value.ifBlank { stringResource(Res.string.form_edit_untitled) }, style = style, modifier = Modifier.clickable { draft = value; editing = true })
    }
}
