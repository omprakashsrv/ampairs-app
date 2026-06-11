package com.ampairs.invoice.editor

import ampairsapp.feature.invoice.generated.resources.Res
import ampairsapp.feature.invoice.generated.resources.doc_create_cancel
import ampairsapp.feature.invoice.generated.resources.doc_create_confirm
import ampairsapp.feature.invoice.generated.resources.doc_create_exempt_note
import ampairsapp.feature.invoice.generated.resources.doc_create_gst_chip
import ampairsapp.feature.invoice.generated.resources.doc_create_hsn
import ampairsapp.feature.invoice.generated.resources.doc_create_name
import ampairsapp.feature.invoice.generated.resources.doc_create_offline_note
import ampairsapp.feature.invoice.generated.resources.doc_create_price
import ampairsapp.feature.invoice.generated.resources.doc_create_title
import ampairsapp.feature.invoice.generated.resources.doc_create_unit
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

/** A workspace unit offered as the base unit of an inline-created product. */
data class DocBaseUnitChoice(val unitId: String, val name: String)

/**
 * Inline create-product (spec 010 v2, screen "inline create"): exactly four fields — name,
 * selling price ₹, base unit, HSN/tax code (live GST chip; blank ⇒ amber 0%/exempt note).
 * Dialog on expanded, bottom sheet on compact. Saves offline then lands as a line.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocCreateProduct(
    initialName: String,
    baseUnits: List<DocBaseUnitChoice>,
    hsnRatePercent: Double?,
    onHsnChanged: (String) -> Unit,
    onCreate: (name: String, price: Double, hsn: String, baseUnitId: String?) -> Unit,
    onDismiss: () -> Unit,
    compact: Boolean,
) {
    if (compact) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 18.dp)) {
                Text(stringResource(Res.string.doc_create_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
                CreateProductFields(initialName, baseUnits, hsnRatePercent, onHsnChanged, onCreate, onDismiss)
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(Res.string.doc_create_title)) },
            text = { CreateProductFields(initialName, baseUnits, hsnRatePercent, onHsnChanged, onCreate, onDismiss, dialog = true) },
            confirmButton = {},
        )
    }
}

@Composable
private fun CreateProductFields(
    initialName: String,
    baseUnits: List<DocBaseUnitChoice>,
    hsnRatePercent: Double?,
    onHsnChanged: (String) -> Unit,
    onCreate: (String, Double, String, String?) -> Unit,
    onDismiss: () -> Unit,
    dialog: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    var name by remember { mutableStateOf(initialName) }
    var price by remember { mutableStateOf("") }
    var hsn by remember { mutableStateOf("") }
    var baseUnit by remember { mutableStateOf(baseUnits.firstOrNull()) }
    var unitMenu by remember { mutableStateOf(false) }
    val nameFocus = remember { FocusRequester() }
    val valid = name.isNotBlank() && (price.toDoubleOrNull() ?: 0.0) > 0.0
    LaunchedEffect(Unit) { nameFocus.requestFocus() }
    LaunchedEffect(baseUnits) { if (baseUnit == null) baseUnit = baseUnits.firstOrNull() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text(stringResource(Res.string.doc_create_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = price,
                onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(stringResource(Res.string.doc_create_price)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.weight(1f)) {
                OutlinedTextField(
                    value = baseUnit?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.doc_create_unit)) },
                    trailingIcon = {
                        Icon(
                            Icons.Filled.ExpandMore, contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                // overlay click target — readOnly text fields swallow taps inconsistently
                Box(
                    Modifier
                        .matchParentSize()
                        .clickable { unitMenu = baseUnits.isNotEmpty() }
                )
                DropdownMenu(expanded = unitMenu, onDismissRequest = { unitMenu = false }) {
                    baseUnits.forEach { u ->
                        DropdownMenuItem(text = { Text(u.name) }, onClick = { baseUnit = u; unitMenu = false })
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = hsn,
                onValueChange = { raw ->
                    hsn = raw.filter { it.isDigit() }.take(8)
                    onHsnChanged(hsn)
                },
                label = { Text(stringResource(Res.string.doc_create_hsn)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.weight(1f),
            )
            Surface(color = cs.primaryContainer, shape = RoundedCornerShape(50)) {
                Text(
                    stringResource(Res.string.doc_create_gst_chip, ratePctLabel(hsnRatePercent ?: 0.0)),
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
        if (hsn.isBlank()) {
            NoteRow(Icons.Filled.Info, stringResource(Res.string.doc_create_exempt_note), amber = true)
        }
        NoteRow(Icons.Filled.CloudOff, stringResource(Res.string.doc_create_offline_note), amber = false)

        Row(Modifier.padding(top = 4.dp)) {
            Box(Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.doc_create_cancel)) }
            Button(
                enabled = valid,
                onClick = { onCreate(name.trim(), price.toDoubleOrNull() ?: 0.0, hsn, baseUnit?.unitId) },
            ) { Text(stringResource(Res.string.doc_create_confirm)) }
        }
    }
}

@Composable
private fun NoteRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, amber: Boolean) {
    val cs = MaterialTheme.colorScheme
    Surface(
        color = if (amber) cs.secondaryContainer.copy(alpha = 0.5f) else cs.surfaceContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (amber) cs.onSecondaryContainer else cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Text(
                "  $text",
                style = MaterialTheme.typography.labelSmall,
                color = if (amber) cs.onSecondaryContainer else cs.onSurfaceVariant,
            )
        }
    }
}
