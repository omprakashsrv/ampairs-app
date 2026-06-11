package com.ampairs.form.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.form.generated.resources.Res
import ampairsapp.feature.form.generated.resources.*
import com.ampairs.form.domain.FieldDataType
import com.ampairs.form.domain.FormField
import com.ampairs.form.domain.FormSchema

/** Read-only render of the resulting entry form — what staff will see. Mirrors the design's preview. */
@Composable
fun LivePreview(schema: FormSchema, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val sections = schema.sections.filter { it.visible }.sortedBy { it.displayOrder }
    val anyField = sections.any { sec -> schema.fields.any { it.sectionUid == sec.uid && it.visible } }

    Box(modifier.fillMaxWidth(), Alignment.TopCenter) {
        Surface(
            Modifier.widthIn(max = 360.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(16.dp),
            color = scheme.surface,
            border = BorderStroke(1.dp, scheme.outlineVariant),
            shadowElevation = 2.dp,
        ) {
            Column {
                // device top bar
                Row(Modifier.fillMaxWidth().background(scheme.primary).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = scheme.onPrimary)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(Res.string.form_preview_new_entity, schema.entityType.replaceFirstChar { it.uppercase() }), color = scheme.onPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Surface(color = scheme.onPrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(50)) {
                        Text(stringResource(Res.string.form_preview_save), Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = scheme.onPrimary, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    if (!anyField) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.VisibilityOff, null, Modifier.size(32.dp), tint = scheme.onSurfaceVariant)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(Res.string.form_preview_all_hidden), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                        }
                    }
                    sections.forEach { sec ->
                        val fields = schema.fields.filter { it.sectionUid == sec.uid && it.visible }.sortedBy { it.displayOrder }
                        if (fields.isNotEmpty()) {
                            Column {
                                Text(sec.name, style = MaterialTheme.typography.titleSmall, color = scheme.primary)
                                Spacer(Modifier.size(6.dp))
                                androidx.compose.material3.HorizontalDivider()
                                Spacer(Modifier.size(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    fields.forEach { PreviewField(it) }
                                }
                            }
                        }
                    }
                    if (anyField) {
                        Surface(color = scheme.primary, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(Res.string.form_preview_save_entity, schema.entityType.replaceFirstChar { it.uppercase() }), Modifier.padding(13.dp), color = scheme.onPrimary, style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewField(f: FormField) {
    val scheme = MaterialTheme.colorScheme
    if (f.dataType == FieldDataType.BOOLEAN) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(f.displayName + if (f.mandatory) " *" else "", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = f.defaultValue.equals("yes", true), onCheckedChange = {}, enabled = false)
        }
        return
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(f.displayName, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            if (f.mandatory) Text(" *", style = MaterialTheme.typography.labelMedium, color = scheme.error)
        }
        Spacer(Modifier.size(5.dp))
        val placeholder = f.placeholder?.ifBlank { null } ?: f.defaultValue?.ifBlank { null }
        when (f.dataType) {
            FieldDataType.CHOICE -> FauxInput(placeholder ?: stringResource(Res.string.form_preview_select), trailing = Icons.Filled.ArrowDropDown, ro = !f.enabled)
            FieldDataType.MULTI_CHOICE -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (f.enumValues ?: listOf(stringResource(Res.string.form_preview_option_a), stringResource(Res.string.form_preview_option_b))).take(3).forEachIndexed { i, o ->
                    Surface(shape = RoundedCornerShape(50), color = if (i == 0) scheme.secondaryContainer else Color.Transparent, border = if (i == 0) null else BorderStroke(1.dp, scheme.outline)) {
                        Text(o, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = if (i == 0) scheme.onSecondaryContainer else scheme.onSurfaceVariant)
                    }
                }
            }
            FieldDataType.CUSTOM -> Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = scheme.surfaceContainerLow, border = BorderStroke(1.dp, scheme.outline)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(f.widgetKey?.replaceFirstChar { it.uppercase() } ?: stringResource(Res.string.form_preview_widget), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
            }
            FieldDataType.TEXTAREA -> FauxInput(placeholder ?: stringResource(Res.string.form_preview_type_here), minHeight = 72, ro = !f.enabled)
            else -> FauxInput(placeholder ?: stringResource(Res.string.form_preview_type_here), ro = !f.enabled)
        }
    }
}

@Composable
private fun FauxInput(text: String, trailing: androidx.compose.ui.graphics.vector.ImageVector? = null, minHeight: Int = 44, ro: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        Modifier.fillMaxWidth().heightIn(min = minHeight.dp),
        shape = RoundedCornerShape(4.dp),
        color = if (ro) scheme.surfaceContainer else scheme.surface,
        border = BorderStroke(1.dp, scheme.outline),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (trailing != null) Icon(trailing, null, Modifier.size(18.dp), tint = scheme.onSurfaceVariant)
        }
    }
}
