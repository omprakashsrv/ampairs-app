package com.ampairs.form.render

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.ampairs.form.domain.FieldDataType
import com.ampairs.form.domain.FormField
import com.ampairs.form.domain.OptionSource
import kotlinx.coroutines.flow.flowOf

/**
 * Renders a single [FormField] bound to [FormValueState], switching on data type. CHOICE/MULTI_CHOICE
 * resolve options either statically (enumValues) or dynamically (via [DynamicOptionRegistry]); CUSTOM
 * delegates to a [CustomFieldWidget].
 *
 * NOTE (spec 011): drafted, not yet compile-gated.
 */
@Composable
fun FormFieldRenderer(
    field: FormField,
    state: FormValueState,
    optionRegistry: DynamicOptionRegistry,
    widgetRegistry: CustomFieldWidgetRegistry,
    modifier: Modifier = Modifier,
) {
    val error = state.error(field.fieldKey)
    when (field.dataType) {
        FieldDataType.TEXT, FieldDataType.TEXTAREA, FieldDataType.NUMBER, FieldDataType.DATE ->
            TextLikeField(field, state, error, modifier)

        FieldDataType.BOOLEAN -> BooleanField(field, state, modifier)

        FieldDataType.CHOICE -> ChoiceField(field, state, error, optionRegistry, modifier)

        FieldDataType.MULTI_CHOICE -> MultiChoiceField(field, state, error, optionRegistry, modifier)

        FieldDataType.CUSTOM -> {
            val widget = widgetRegistry.forKey(field.widgetKey)
            if (widget != null) {
                widget.Render(
                    field = field,
                    value = state.text(field.fieldKey),
                    onValueChange = { state.setText(field, it.orEmpty()) },
                    enabled = field.enabled,
                    error = error,
                )
            } else {
                Text("Unsupported field: ${field.displayName}", modifier = modifier)
            }
        }
    }
}

@Composable
private fun TextLikeField(field: FormField, state: FormValueState, error: String?, modifier: Modifier) {
    OutlinedTextField(
        value = state.text(field.fieldKey),
        onValueChange = { state.setText(field, it) },
        modifier = modifier.fillMaxWidth(),
        enabled = field.enabled,
        label = { Text(field.displayName + if (field.mandatory) " *" else "") },
        placeholder = field.placeholder?.let { { Text(it) } },
        supportingText = (error ?: field.helpText)?.let { { Text(it) } },
        isError = error != null,
        singleLine = field.dataType != FieldDataType.TEXTAREA,
        keyboardOptions = if (field.dataType == FieldDataType.NUMBER)
            KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
    )
}

@Composable
private fun BooleanField(field: FormField, state: FormValueState, modifier: Modifier) {
    val checked = state.text(field.fieldKey).equals("true", ignoreCase = true)
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(field.displayName, modifier = Modifier.weight(1f))
        Switch(checked = checked, enabled = field.enabled, onCheckedChange = { state.setText(field, it.toString()) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceField(
    field: FormField,
    state: FormValueState,
    error: String?,
    optionRegistry: DynamicOptionRegistry,
    modifier: Modifier,
) {
    val options = rememberOptions(field, optionRegistry)
    var expanded by remember { mutableStateOf(false) }
    val current = state.text(field.fieldKey)
    val currentLabel = options.firstOrNull { it.value == current }?.label ?: current

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            enabled = field.enabled,
            label = { Text(field.displayName + if (field.mandatory) " *" else "") },
            isError = error != null,
            supportingText = (error ?: field.helpText)?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        androidx.compose.material3.ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        state.setSelected(field, option.value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun MultiChoiceField(
    field: FormField,
    state: FormValueState,
    error: String?,
    optionRegistry: DynamicOptionRegistry,
    modifier: Modifier,
) {
    val options = rememberOptions(field, optionRegistry)
    val selected = state.selected(field.fieldKey)
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(field.displayName + if (field.mandatory) " *" else "")
        FlowRow {
            options.forEach { option ->
                FilterChip(
                    selected = option.value in selected,
                    onClick = { state.toggleMulti(field, option.value) },
                    label = { Text(option.label) },
                    enabled = field.enabled,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
        (error ?: field.helpText)?.let { Text(it) }
    }
}

@Composable
private fun rememberOptions(field: FormField, optionRegistry: DynamicOptionRegistry): List<FieldOption> {
    return when (field.optionSource) {
        OptionSource.STATIC -> field.enumValues.orEmpty().map { FieldOption(it, it) }
        OptionSource.DYNAMIC -> {
            val flow = remember(field.dynamicSourceKey) {
                optionRegistry.forKey(field.dynamicSourceKey)?.options() ?: flowOf(emptyList())
            }
            val items by flow.collectAsState(initial = emptyList())
            items
        }
        null -> emptyList()
    }
}
