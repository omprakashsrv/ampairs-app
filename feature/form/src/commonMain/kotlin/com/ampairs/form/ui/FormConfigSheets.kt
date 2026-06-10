package com.ampairs.form.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.form.domain.FieldDataType
import com.ampairs.form.domain.FieldSource
import com.ampairs.form.domain.FormField
import com.ampairs.form.domain.FormSchema
import com.ampairs.form.domain.FormSection
import com.ampairs.form.domain.OptionSource
import com.ampairs.form.domain.validation.FormatKind
import com.ampairs.form.domain.validation.ValidationRule
import com.ampairs.form.domain.validation.ValidationRuleType

/* ── Shared sheet scaffold (right-side panel + scrim, per the design) ─────────────────────────── */

@Composable
internal fun SideSheet(
    title: String,
    subtitle: String?,
    onClose: () -> Unit,
    footer: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)).clickable(onClick = onClose))
        Surface(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().widthIn(max = 460.dp).fillMaxWidth(0.92f),
            color = scheme.surface,
            shadowElevation = 8.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClose) { Icon(Icons.Filled.Close, "Close") }
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = scheme.outlineVariant)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) { content() }
                HorizontalDivider(color = scheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) { footer() }
            }
        }
    }
}

@Composable
private fun FieldGroup(label: String, hint: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        content()
        if (hint != null) {
            Spacer(Modifier.height(6.dp))
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(name: String, desc: String, on: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = on, onCheckedChange = onChange, enabled = enabled)
    }
}

/* ── Type picker grid ─────────────────────────────────────────────────────────────────────────── */

private val MAIN_TYPES = listOf(
    FieldDataType.TEXT, FieldDataType.TEXTAREA, FieldDataType.NUMBER, FieldDataType.BOOLEAN,
    FieldDataType.DATE, FieldDataType.CHOICE, FieldDataType.MULTI_CHOICE, FieldDataType.CUSTOM,
)

private val WIDGET_KINDS = listOf(
    "address" to "Address", "image_gallery" to "Image Gallery", "location" to "Map Location", "business_hours" to "Business Hours",
)

@Composable
private fun TypeCard(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) scheme.primaryContainer else scheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(36.dp), shape = RoundedCornerShape(8.dp), color = if (selected) scheme.primary else scheme.surfaceContainerHigh) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Icon(icon, null, Modifier.size(20.dp), tint = if (selected) scheme.onPrimary else scheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TypeGrid(selected: FieldDataType, enabled: Boolean, onSelect: (FieldDataType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MAIN_TYPES.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { t ->
                    Box(Modifier.weight(1f)) {
                        TypeCard(t.typeLabel(), t.icon(), selected == t, enabled) { onSelect(t) }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/* ── Choice option source (Fixed list / From data) ────────────────────────────────────────────── */

@Composable
internal fun ChoiceSourceEditor(draft: FormField, sourceKeys: Collection<String>, onChange: (FormField) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val mode = draft.optionSource ?: OptionSource.STATIC
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = mode == OptionSource.STATIC, onClick = { onChange(draft.copy(optionSource = OptionSource.STATIC)) },
            label = { Text("Fixed list") }, leadingIcon = { Icon(Icons.Filled.List, null, Modifier.size(18.dp)) })
        FilterChip(selected = mode == OptionSource.DYNAMIC, onClick = { onChange(draft.copy(optionSource = OptionSource.DYNAMIC)) },
            label = { Text("From data") }, leadingIcon = { Icon(Icons.Filled.Dataset, null, Modifier.size(18.dp)) })
    }
    if (mode == OptionSource.STATIC) {
        val options = draft.enumValues ?: emptyList()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (options.isEmpty()) Text("No options yet. Add the choices staff can pick from.",
                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            options.forEachIndexed { i, o ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = o, onValueChange = { v ->
                        onChange(draft.copy(enumValues = options.toMutableList().also { it[i] = v }))
                    }, singleLine = true, modifier = Modifier.weight(1f), placeholder = { Text("Type an option…") })
                    IconButton({ onChange(draft.copy(enumValues = options.filterIndexed { j, _ -> j != i })) }) {
                        Icon(Icons.Filled.Close, "Remove option", Modifier.size(18.dp))
                    }
                }
            }
            TextButton({ onChange(draft.copy(enumValues = options + "")) }) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Add option")
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Bind to a live list — it stays in sync as that data changes.",
                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            if (sourceKeys.isEmpty()) Text("No data sources registered for this module yet.",
                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            sourceKeys.sorted().forEach { key ->
                val selected = draft.dynamicSourceKey == key
                Surface(
                    Modifier.fillMaxWidth().clickable { onChange(draft.copy(dynamicSourceKey = key)) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) scheme.primaryContainer else scheme.surface,
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(36.dp), shape = RoundedCornerShape(8.dp), color = scheme.tertiaryContainer) {
                            Box(Modifier.fillMaxSize(), Alignment.Center) { Icon(Icons.Filled.Dataset, null, Modifier.size(20.dp), tint = scheme.onTertiaryContainer) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(key.split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                            style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        if (selected) Icon(Icons.Filled.CheckCircle, null, tint = scheme.primary)
                    }
                }
            }
        }
    }
}

/* ── Add / Edit field sheet ───────────────────────────────────────────────────────────────────── */

internal fun String.slugKey(): String =
    trim().lowercase().map { if (it.isLetterOrDigit()) it else '_' }.joinToString("").trim('_').ifBlank { "field" }

@Composable
internal fun AddEditFieldSheet(
    schema: FormSchema,
    sectionUid: String,
    field: FormField?,                                 // null = create
    sourceKeys: Collection<String>,
    onClose: () -> Unit,
    onSave: (draft: FormField, targetSectionUid: String, isNew: Boolean) -> Unit,
) {
    val isNew = field == null
    val locked = field?.source == FieldSource.STANDARD
    var draft by remember {
        mutableStateOf(field ?: FormField(uid = "", source = FieldSource.CUSTOM, fieldKey = "",
            displayName = "", dataType = FieldDataType.TEXT, sectionUid = sectionUid))
    }
    var targetSection by remember { mutableStateOf(field?.sectionUid ?: sectionUid) }

    SideSheet(
        title = if (isNew) "Add custom field" else "Edit field",
        subtitle = if (isNew) "Workspace field, fully editable" else if (locked) "Built-in field — limited edits" else "Custom field",
        onClose = onClose,
        footer = {
            TextButton(onClose) { Text("Cancel") }
            Button(
                enabled = draft.displayName.isNotBlank(),
                onClick = {
                    val final = if (isNew) draft.copy(fieldKey = draft.displayName.slugKey()) else draft
                    onSave(final, targetSection, isNew)
                },
            ) {
                Icon(if (isNew) Icons.Filled.Add else Icons.Filled.Check, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp)); Text(if (isNew) "Add field" else "Save changes")
            }
        },
    ) {
        FieldGroup("Field label", hint = "This is what your staff will see above the input.") {
            OutlinedTextField(draft.displayName, { draft = draft.copy(displayName = it) },
                placeholder = { Text("e.g. Delivery Instructions") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        FieldGroup("Data type", hint = if (locked) "Built-in fields keep their type." else null) {
            TypeGrid(draft.dataType, enabled = !locked) { t ->
                draft = draft.copy(dataType = t, widgetKey = if (t == FieldDataType.CUSTOM) draft.widgetKey ?: "address" else null)
            }
        }
        if (draft.dataType == FieldDataType.CUSTOM) {
            FieldGroup("Widget control") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WIDGET_KINDS.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (k, label) ->
                                Box(Modifier.weight(1f)) {
                                    TypeCard(label, FieldDataType.CUSTOM.icon(), draft.widgetKey == k) { draft = draft.copy(widgetKey = k) }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        if (draft.dataType == FieldDataType.CHOICE || draft.dataType == FieldDataType.MULTI_CHOICE) {
            FieldGroup("Options") { ChoiceSourceEditor(draft, sourceKeys) { draft = it } }
        }
        FieldGroup("Section") {
            SectionSelect(schema.sections.sortedBy { it.displayOrder }, targetSection) { targetSection = it }
        }
        if (draft.dataType in listOf(FieldDataType.TEXT, FieldDataType.TEXTAREA, FieldDataType.NUMBER)) {
            FieldGroup("Placeholder (optional)") {
                OutlinedTextField(draft.placeholder.orEmpty(), { draft = draft.copy(placeholder = it.ifBlank { null }) },
                    placeholder = { Text("Hint shown inside the empty field") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
        FieldGroup("Help text (optional)") {
            OutlinedTextField(draft.helpText.orEmpty(), { draft = draft.copy(helpText = it.ifBlank { null }) },
                placeholder = { Text("A short note under the field") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        SwitchRow("Show on form", "Visible to staff entering data", draft.visible) {
            draft = draft.copy(visible = it, mandatory = if (it) draft.mandatory else false)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SwitchRow("Required", "Staff must fill this in", draft.mandatory, enabled = draft.visible && draft.enabled) {
            draft = draft.copy(mandatory = it)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SwitchRow("Read-only", "Shown but can't be edited", !draft.enabled) {
            draft = draft.copy(enabled = !it, mandatory = if (it) false else draft.mandatory)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionSelect(sections: List<FormSection>, selectedUid: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val current = sections.firstOrNull { it.uid == selectedUid }
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(
            value = current?.name ?: "Select…", onValueChange = {}, readOnly = true, singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Folder, null, Modifier.size(18.dp)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            sections.forEach { s ->
                DropdownMenuItem(text = { Text(s.name) }, onClick = { onSelect(s.uid); expanded = false })
            }
        }
    }
}

/* ── Advanced sheet (type + guided validation + choice source + default) ──────────────────────── */

private fun List<ValidationRule>?.rule(t: ValidationRuleType) = this?.firstOrNull { it.type == t }
private fun List<ValidationRule>?.without(t: ValidationRuleType) = (this ?: emptyList()).filterNot { it.type == t }
private fun List<ValidationRule>?.with(r: ValidationRule) = without(r.type) + r
private fun List<ValidationRule>.orNull() = takeIf { it.isNotEmpty() }

@Composable
internal fun AdvancedSheet(
    sectionName: String,
    field: FormField,
    sourceKeys: Collection<String>,
    onClose: () -> Unit,
    onApply: (FormField) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var draft by remember { mutableStateOf(field) }
    val isStandard = draft.source == FieldSource.STANDARD

    SideSheet(
        title = "Advanced",
        subtitle = "${draft.displayName} · $sectionName",
        onClose = onClose,
        footer = {
            TextButton(onClose) { Text("Cancel") }
            Button({ onApply(draft) }) { Icon(Icons.Filled.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Apply") }
        },
    ) {
        FieldGroup("Data type", hint = if (isStandard) "Built-in fields are backed by a data column, so the type is fixed." else null) {
            if (isStandard) {
                Surface(shape = RoundedCornerShape(12.dp), color = scheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(draft.dataType.icon(), null, Modifier.size(18.dp), tint = scheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text(draft.dataType.typeLabel(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.Lock, null, Modifier.size(14.dp), tint = scheme.onSurfaceVariant)
                    }
                }
            } else {
                TypeGrid(draft.dataType, enabled = true) { t ->
                    draft = draft.copy(dataType = t, validationRules = null,
                        widgetKey = if (t == FieldDataType.CUSTOM) draft.widgetKey ?: "address" else null)
                }
            }
        }

        FieldGroup("Validation rules", hint = "Pick the checks staff must pass. No formulas — just plain limits.") {
            SwitchRow("Required", "Can't be left empty", draft.mandatory, enabled = draft.visible && draft.enabled) {
                draft = draft.copy(mandatory = it)
            }
            when (draft.dataType) {
                FieldDataType.TEXT, FieldDataType.TEXTAREA -> {
                    LengthRangeEditor(draft) { draft = it }
                    if (draft.dataType == FieldDataType.TEXT) FormatEditor(draft) { draft = it }
                }
                FieldDataType.NUMBER -> NumberRangeEditor(draft) { draft = it }
                else -> Text("This type has no extra checks.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }

        if (draft.dataType == FieldDataType.CHOICE || draft.dataType == FieldDataType.MULTI_CHOICE) {
            FieldGroup("Where options come from") { ChoiceSourceEditor(draft, sourceKeys) { draft = it } }
        }

        if (draft.dataType in listOf(FieldDataType.TEXT, FieldDataType.TEXTAREA, FieldDataType.NUMBER)) {
            FieldGroup("Default value (optional)") {
                OutlinedTextField(draft.defaultValue.orEmpty(), { draft = draft.copy(defaultValue = it.ifBlank { null }) },
                    placeholder = { Text("Pre-filled when creating a new record") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun LengthRangeEditor(draft: FormField, onChange: (FormField) -> Unit) {
    val rule = draft.validationRules.rule(ValidationRuleType.LENGTH_RANGE)
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = rule != null, onClick = {
            onChange(draft.copy(validationRules =
                if (rule == null) draft.validationRules.with(ValidationRule(ValidationRuleType.LENGTH_RANGE, min = 0, max = 100)).orNull()
                else draft.validationRules.without(ValidationRuleType.LENGTH_RANGE).orNull()))
        }, label = { Text("Length limits") })
    }
    if (rule != null) {
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumBox(rule.min) { v -> onChange(draft.copy(validationRules = draft.validationRules.with(rule.copy(min = v)).orNull())) }
            Text("to", color = MaterialTheme.colorScheme.onSurfaceVariant)
            NumBox(rule.max) { v -> onChange(draft.copy(validationRules = draft.validationRules.with(rule.copy(max = v)).orNull())) }
            Text("characters", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NumberRangeEditor(draft: FormField, onChange: (FormField) -> Unit) {
    val rule = draft.validationRules.rule(ValidationRuleType.NUMBER_RANGE)
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = rule != null, onClick = {
            onChange(draft.copy(validationRules =
                if (rule == null) draft.validationRules.with(ValidationRule(ValidationRuleType.NUMBER_RANGE, minValue = 0.0, maxValue = 100.0)).orNull()
                else draft.validationRules.without(ValidationRuleType.NUMBER_RANGE).orNull()))
        }, label = { Text("Number range") })
    }
    if (rule != null) {
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumBox(rule.minValue?.toInt()) { v -> onChange(draft.copy(validationRules = draft.validationRules.with(rule.copy(minValue = v?.toDouble())).orNull())) }
            Text("to", color = MaterialTheme.colorScheme.onSurfaceVariant)
            NumBox(rule.maxValue?.toInt()) { v -> onChange(draft.copy(validationRules = draft.validationRules.with(rule.copy(maxValue = v?.toDouble())).orNull())) }
        }
    }
}

@Composable
private fun FormatEditor(draft: FormField, onChange: (FormField) -> Unit) {
    val rule = draft.validationRules.rule(ValidationRuleType.FORMAT)
    Column(Modifier.padding(top = 10.dp)) {
        Text("Format", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null, FormatKind.EMAIL, FormatKind.PHONE, FormatKind.GSTIN, FormatKind.PAN).forEach { kind ->
                FilterChip(
                    selected = rule?.kind == kind && (kind != null || rule == null),
                    onClick = {
                        onChange(draft.copy(validationRules =
                            if (kind == null) draft.validationRules.without(ValidationRuleType.FORMAT).orNull()
                            else draft.validationRules.with(ValidationRule(ValidationRuleType.FORMAT, kind = kind)).orNull()))
                    },
                    label = { Text(kind?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "None") },
                )
            }
        }
    }
}

@Composable
private fun NumBox(value: Int?, onChange: (Int?) -> Unit) {
    OutlinedTextField(
        value = value?.toString().orEmpty(),
        onValueChange = { onChange(it.toIntOrNull()) },
        singleLine = true,
        modifier = Modifier.width(90.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

/* ── Manage sections sheet ────────────────────────────────────────────────────────────────────── */

@Composable
internal fun ManageSectionsSheet(
    schema: FormSchema,
    onClose: () -> Unit,
    onRename: (String, String) -> Unit,
    onMove: (String, Int) -> Unit,
    onToggleVisible: (String) -> Unit,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val sections = schema.sections.sortedBy { it.displayOrder }
    SideSheet(
        title = "Manage sections",
        subtitle = "Group related fields. The form follows this order.",
        onClose = onClose,
        footer = { Button(onClose) { Icon(Icons.Filled.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Done") } },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sections.forEachIndexed { i, s ->
                val isGeneral = s.name.equals("General", true)
                val count = schema.fields.count { it.sectionUid == s.uid }
                Surface(shape = RoundedCornerShape(8.dp), color = scheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "$count field${if (count == 1) "" else "s"}" + (if (isGeneral) " · Default" else "") + (if (!s.visible) " · Hidden" else ""),
                                style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant,
                            )
                        }
                        IconButton({ onMove(s.uid, -1) }, enabled = i > 0) { Icon(Icons.Filled.ArrowUpward, "Move up", Modifier.size(18.dp)) }
                        IconButton({ onMove(s.uid, 1) }, enabled = i < sections.size - 1) { Icon(Icons.Filled.ArrowDownward, "Move down", Modifier.size(18.dp)) }
                        IconButton({ onToggleVisible(s.uid) }) {
                            Icon(if (s.visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, "Toggle visibility", Modifier.size(18.dp))
                        }
                        IconButton({ onDelete(s.uid) }, enabled = !isGeneral) {
                            Icon(if (isGeneral) Icons.Filled.Lock else Icons.Filled.Delete, "Delete", Modifier.size(18.dp),
                                tint = if (isGeneral) scheme.onSurfaceVariant else scheme.error)
                        }
                    }
                }
            }
            TextButton(onAdd) { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Add section") }
        }
    }
}

/* ── Delete-section dialog (reassignment flow) ────────────────────────────────────────────────── */

@Composable
internal fun DeleteSectionDialog(
    schema: FormSchema,
    section: FormSection,
    onClose: () -> Unit,
    onConfirm: (mode: String, target: String?) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val fieldCount = schema.fields.count { it.sectionUid == section.uid }
    val others = schema.sections.filter { it.uid != section.uid }.sortedBy { it.displayOrder }
    val general = others.firstOrNull { it.name.equals("General", true) } ?: others.firstOrNull()
    var mode by remember { mutableStateOf("move") }
    var target by remember { mutableStateOf(general?.uid) }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)).clickable(onClick = onClose))
        Surface(
            Modifier.align(Alignment.Center).widthIn(max = 440.dp).fillMaxWidth(0.92f),
            shape = RoundedCornerShape(28.dp),
            color = scheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
        ) {
            Column(Modifier.padding(24.dp)) {
                Surface(Modifier.size(48.dp), shape = CircleShape, color = scheme.errorContainer) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Icon(Icons.Filled.Delete, null, tint = scheme.error) }
                }
                Spacer(Modifier.height(16.dp))
                Text("Delete \"${section.name}\"?", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                Text(
                    if (fieldCount == 0) "This section is empty. It will be removed from the form."
                    else "This section has $fieldCount field${if (fieldCount == 1) "" else "s"}. Choose what happens to them first.",
                    style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
                )
                if (fieldCount > 0) {
                    Spacer(Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            Modifier.fillMaxWidth().clickable { mode = "move" },
                            shape = RoundedCornerShape(12.dp),
                            color = if (mode == "move") scheme.primaryContainer else Color.Transparent,
                            border = BorderStroke(if (mode == "move") 2.dp else 1.dp, if (mode == "move") scheme.primary else scheme.outlineVariant),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = mode == "move", onClick = { mode = "move" })
                                    Text("Move fields to another section", style = MaterialTheme.typography.bodyLarge)
                                }
                                if (mode == "move") {
                                    Spacer(Modifier.height(8.dp))
                                    SectionSelect(others, target ?: "", onSelect = { target = it })
                                }
                            }
                        }
                        Surface(
                            Modifier.fillMaxWidth().clickable { mode = "deleteAll" },
                            shape = RoundedCornerShape(12.dp),
                            color = if (mode == "deleteAll") scheme.primaryContainer else Color.Transparent,
                            border = BorderStroke(if (mode == "deleteAll") 2.dp else 1.dp, if (mode == "deleteAll") scheme.primary else scheme.outlineVariant),
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = mode == "deleteAll", onClick = { mode = "deleteAll" })
                                Column {
                                    Text("Delete the section and its custom fields", style = MaterialTheme.typography.bodyLarge)
                                    Text("Standard fields move to ${general?.name ?: "General"} automatically.",
                                        style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClose) { Text("Cancel") }
                    Button(
                        onClick = { onConfirm(if (fieldCount == 0) "move" else mode, target) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = scheme.error, contentColor = scheme.onError),
                    ) { Icon(Icons.Filled.Delete, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Delete section") }
                }
            }
        }
    }
}
