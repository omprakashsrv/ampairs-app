package com.ampairs.invoice.editor

import ampairsapp.feature.invoice.generated.resources.Res
import ampairsapp.feature.invoice.generated.resources.doc_cmd_a11y
import ampairsapp.feature.invoice.generated.resources.doc_cmd_added
import ampairsapp.feature.invoice.generated.resources.doc_cmd_create
import ampairsapp.feature.invoice.generated.resources.doc_cmd_create_sub
import ampairsapp.feature.invoice.generated.resources.doc_cmd_hint_discount
import ampairsapp.feature.invoice.generated.resources.doc_cmd_hint_discount_example
import ampairsapp.feature.invoice.generated.resources.doc_cmd_hint_keys
import ampairsapp.feature.invoice.generated.resources.doc_cmd_hint_price
import ampairsapp.feature.invoice.generated.resources.doc_cmd_hint_price_example
import ampairsapp.feature.invoice.generated.resources.doc_cmd_hint_qty_unit
import ampairsapp.feature.invoice.generated.resources.doc_cmd_hint_qty_unit_example
import ampairsapp.feature.invoice.generated.resources.doc_cmd_hint_scan
import ampairsapp.feature.invoice.generated.resources.doc_cmd_hint_scan_example
import ampairsapp.feature.invoice.generated.resources.doc_cmd_hsn_gst
import ampairsapp.feature.invoice.generated.resources.doc_cmd_placeholder
import ampairsapp.feature.invoice.generated.resources.doc_cmd_placeholder_compact
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.platform.LocalDensity
import com.ampairs.common.format.toDecimal
import com.ampairs.common.format.toInr
import org.jetbrains.compose.resources.stringResource

/**
 * The command-bar composer (spec 010 v2, screen "composer") — a single always-focused text field
 * pinned above the line area. Typing shows parse chips + up to 5 fully-resolved result rows;
 * Enter commits the highlighted row, ↑/↓ move, Esc clears; no match + Enter offers inline create.
 */
@Composable
fun DocCommandBar(
    state: ComposerUiState,
    onQueryChange: (String) -> Unit,
    onMoveHighlight: (Int) -> Unit,
    onCommitHighlighted: () -> Unit,
    onCommitAt: (Int) -> Unit,
    onCreate: (String) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val focusRequester = remember { FocusRequester() }
    val hasQuery = state.query.isNotBlank()
    val a11y = stringResource(Res.string.doc_cmd_a11y)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = modifier.fillMaxWidth()) {
        Box {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cs.surfaceContainerHigh,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (hasQuery) 2.dp else 1.dp,
                    color = if (hasQuery) cs.primary else cs.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp).heightIn(min = 52.dp),
                ) {
                    Icon(
                        if (hasQuery) Icons.Filled.KeyboardReturn else Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = cs.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    BasicTextField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                        cursorBrush = SolidColor(cs.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onCommitHighlighted() }),
                        decorationBox = { inner ->
                            Box {
                                if (state.query.isEmpty()) {
                                    Text(
                                        stringResource(if (compact) Res.string.doc_cmd_placeholder_compact else Res.string.doc_cmd_placeholder),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = cs.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp)
                            .focusRequester(focusRequester)
                            .semantics { contentDescription = a11y }
                            .onPreviewKeyEvent { e ->
                                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when (e.key) {
                                    Key.DirectionDown -> { onMoveHighlight(1); true }
                                    Key.DirectionUp -> { onMoveHighlight(-1); true }
                                    Key.Enter, Key.NumPadEnter -> { onCommitHighlighted(); true }
                                    Key.Escape -> { onQueryChange(""); true }
                                    else -> false
                                }
                            },
                    )
                    if (hasQuery) {
                        state.preview?.let { p -> ParseChips(p, compact) }
                    } else {
                        state.flash?.let { flash ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.CheckCircle, contentDescription = null,
                                    tint = cs.primary, modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    flash,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = cs.primary,
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (hasQuery && (state.results.isNotEmpty() || state.noMatch)) {
                val density = LocalDensity.current
                val yOffset = with(density) { 56.dp.roundToPx() }
                Popup(offset = IntOffset(0, yOffset)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = cs.surfaceContainer,
                        tonalElevation = 3.dp,
                        shadowElevation = 6.dp,
                        modifier = Modifier.widthIn(min = 320.dp, max = 720.dp),
                    ) {
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                            items(state.results.size, key = { state.results[it].productId }) { i ->
                                ResultRow(
                                    result = state.results[i],
                                    highlighted = i == state.highlight,
                                    compact = compact,
                                    onClick = { onCommitAt(i) },
                                )
                            }
                            if (state.noMatch) {
                                item(key = "create") {
                                    CreateRow(typed = state.typedWords, onClick = { onCreate(state.typedWords) })
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!compact && !hasQuery) {
            ComposerHints()
        }
    }
}

@Composable
private fun ParseChips(p: ComposerResultUi, compact: Boolean) {
    val cs = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Chip("${p.quantity.toDecimal()} ${p.unitName}", cs.primary, cs.onPrimary)
        if (!compact) {
            Chip(p.unitPrice.toInr() + if (p.priceOverridden) " ✎" else "", cs.surfaceContainerHighest, cs.onSurface)
            if (p.discountPercent > 0.0) Chip("−${p.discountPercent.toDecimal()}%", cs.secondaryContainer, cs.onSecondaryContainer)
        }
        Chip("= ${p.amount.toInr()}", cs.surfaceContainerHighest, cs.onSurface, mono = true, bold = true)
    }
}

@Composable
private fun Chip(
    text: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    mono: Boolean = false,
    bold: Boolean = false,
) {
    Surface(color = container, shape = RoundedCornerShape(50)) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = if (mono) FontFamily.Monospace else null,
            fontWeight = if (bold) FontWeight.SemiBold else null,
            color = content,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun ResultRow(
    result: ComposerResultUi,
    highlighted: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val gstLabel = result.gstRatePercent?.let { ratePctLabel(it) } ?: "0%"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (highlighted) cs.secondaryContainer.copy(alpha = 0.45f) else cs.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .heightIn(min = 44.dp),
    ) {
        Icon(
            Icons.Filled.Inventory2, contentDescription = null,
            tint = cs.tertiary, modifier = Modifier.size(18.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                result.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(Res.string.doc_cmd_hsn_gst, result.hsn, gstLabel) +
                    (result.barcode?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (!compact) {
            Text(
                "${result.quantity.toDecimal()} ${result.unitName} × ${result.unitPrice.toInr()}" +
                    (if (result.discountPercent > 0.0) " −${result.discountPercent.toDecimal()}%" else ""),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = cs.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Text(
            result.amount.toInr(),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp),
            maxLines = 1,
        )
        if (highlighted) {
            Text(
                "↵",
                style = MaterialTheme.typography.labelMedium,
                color = cs.primary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun CreateRow(typed: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(cs.secondaryContainer.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .heightIn(min = 44.dp),
    ) {
        Icon(Icons.Filled.AddCircle, contentDescription = null, tint = cs.primary, modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                stringResource(Res.string.doc_cmd_create, typed),
                style = MaterialTheme.typography.bodyMedium,
                color = cs.primary,
            )
            Text(
                stringResource(Res.string.doc_cmd_create_sub),
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
            )
        }
        Text("↵", style = MaterialTheme.typography.labelMedium, color = cs.primary)
    }
}

@Composable
private fun ComposerHints() {
    val cs = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
    ) {
        HintItem(stringResource(Res.string.doc_cmd_hint_qty_unit_example), stringResource(Res.string.doc_cmd_hint_qty_unit))
        HintItem(stringResource(Res.string.doc_cmd_hint_discount_example), stringResource(Res.string.doc_cmd_hint_discount))
        HintItem(stringResource(Res.string.doc_cmd_hint_price_example), stringResource(Res.string.doc_cmd_hint_price))
        HintItem(stringResource(Res.string.doc_cmd_hint_scan_example), stringResource(Res.string.doc_cmd_hint_scan))
        Text(
            stringResource(Res.string.doc_cmd_hint_keys),
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun HintItem(example: String, label: String) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            example,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurfaceVariant,
        )
        Text(
            " $label",
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
        )
    }
}

/** "5%" / "2.5%" — whole percentages without the decimal point. */
internal fun ratePctLabel(p: Double): String =
    if (p % 1.0 == 0.0) "${p.toInt()}%" else "${p.toDecimal()}%"
