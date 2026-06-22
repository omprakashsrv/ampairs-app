package com.ampairs.printing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ampairsapp.feature.printing.generated.resources.Res
import ampairsapp.feature.printing.generated.resources.printing_add_cell
import ampairsapp.feature.printing.generated.resources.printing_add_column
import ampairsapp.feature.printing.generated.resources.printing_add_divider
import ampairsapp.feature.printing.generated.resources.printing_add_grid
import ampairsapp.feature.printing.generated.resources.printing_add_space
import ampairsapp.feature.printing.generated.resources.printing_add_table
import ampairsapp.feature.printing.generated.resources.printing_add_text
import ampairsapp.feature.printing.generated.resources.printing_align_center
import ampairsapp.feature.printing.generated.resources.printing_align_left
import ampairsapp.feature.printing.generated.resources.printing_align_right
import ampairsapp.feature.printing.generated.resources.printing_block_barcode
import ampairsapp.feature.printing.generated.resources.printing_block_bound
import ampairsapp.feature.printing.generated.resources.printing_block_cash_drawer
import ampairsapp.feature.printing.generated.resources.printing_block_cut
import ampairsapp.feature.printing.generated.resources.printing_block_grid
import ampairsapp.feature.printing.generated.resources.printing_block_label
import ampairsapp.feature.printing.generated.resources.printing_block_line_table
import ampairsapp.feature.printing.generated.resources.printing_block_logo
import ampairsapp.feature.printing.generated.resources.printing_block_qr
import ampairsapp.feature.printing.generated.resources.printing_block_text
import ampairsapp.feature.printing.generated.resources.printing_blocks
import ampairsapp.feature.printing.generated.resources.printing_bold
import ampairsapp.feature.printing.generated.resources.printing_cell_label
import ampairsapp.feature.printing.generated.resources.printing_column_header
import ampairsapp.feature.printing.generated.resources.printing_divider_char
import ampairsapp.feature.printing.generated.resources.printing_edit_template
import ampairsapp.feature.printing.generated.resources.printing_field_key
import ampairsapp.feature.printing.generated.resources.printing_grid_columns
import ampairsapp.feature.printing.generated.resources.printing_large
import ampairsapp.feature.printing.generated.resources.printing_live_preview
import ampairsapp.feature.printing.generated.resources.printing_move_down
import ampairsapp.feature.printing.generated.resources.printing_move_up
import ampairsapp.feature.printing.generated.resources.printing_not_editable
import ampairsapp.feature.printing.generated.resources.printing_remove
import ampairsapp.feature.printing.generated.resources.printing_replace_html
import ampairsapp.feature.printing.generated.resources.printing_save
import ampairsapp.feature.printing.generated.resources.printing_spacer_lines
import ampairsapp.feature.printing.generated.resources.printing_static_template
import ampairsapp.feature.printing.generated.resources.printing_static_template_note
import ampairsapp.feature.printing.generated.resources.printing_template_name
import ampairsapp.feature.printing.generated.resources.printing_weight
import com.ampairs.printing.core.model.Align
import com.ampairs.printing.core.model.PaperSpec
import com.ampairs.printing.core.model.PrintElement
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.Template
import com.ampairs.printing.core.model.TemplateBlock
import com.ampairs.printing.core.model.TemplateKind
import com.ampairs.printing.core.model.TemplateStyle
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditScreen(
    templateId: String,
    onBack: () -> Unit,
    viewModel: TemplateEditViewModel = assistedMetroViewModel<TemplateEditViewModel, TemplateEditViewModel.Factory>(
        key = templateId,
    ) { create(templateId) },
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect { onBack() }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            val template = state.template
            TopAppBar(
                title = {
                    Column {
                        Text(
                            template?.name ?: stringResource(Res.string.printing_edit_template),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                        )
                        if (template != null) {
                            Text(
                                "${template.documentType.displayName} · ${template.printerClass.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.save() },
                        enabled = state.template != null && !state.saving,
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(Res.string.printing_save))
                    }
                },
            )
        },
    ) { padding ->
        val template = state.template
        if (state.loading || template == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (state.loading) CircularProgressIndicator()
            }
            return@Scaffold
        }

        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            if (maxWidth >= 700.dp) {
                // Wide (tablet / desktop): a fixed editor sidebar + a large preview pane. A 50/50 split
                // squeezes the preview below an A4 page's width, so the page gets clipped ("only half
                // shows"); a fixed sidebar gives the preview the room it needs to show the full page.
                val sidebarWidth = (maxWidth * 0.4f).coerceIn(320.dp, 460.dp)
                Row(Modifier.fillMaxSize()) {
                    EditorList(
                        template = template,
                        viewModel = viewModel,
                        modifier = Modifier.width(sidebarWidth).fillMaxHeight(),
                    )
                    VerticalDivider()
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        PreviewHeader(template.printerClass)
                        TemplatePreviewPane(
                            template = template,
                            previewHtml = state.previewHtml,
                            previewElements = state.previewElements,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                    }
                }
            } else {
                // Narrow (phone): Blocks / Live Preview tabs.
                var tab by rememberSaveable { mutableIntStateOf(0) }
                Column(Modifier.fillMaxSize()) {
                    SingleChoiceSegmentedButtonRow(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        SegmentedButton(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                        ) { Text(stringResource(Res.string.printing_blocks)) }
                        SegmentedButton(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                        ) { Text(stringResource(Res.string.printing_live_preview)) }
                    }
                    if (tab == 0) {
                        EditorList(template, viewModel, Modifier.fillMaxWidth().weight(1f))
                    } else {
                        Column(Modifier.fillMaxWidth().weight(1f)) {
                            PreviewHeader(template.printerClass)
                            TemplatePreviewPane(
                                template = template,
                                previewHtml = state.previewHtml,
                                previewElements = state.previewElements,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorList(template: Template, viewModel: TemplateEditViewModel, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (template.kind == TemplateKind.STATIC) {
            staticTemplateItems(template, viewModel)
        } else {
            templateEditorItems(template, viewModel)
        }
    }
}

@Composable
private fun PreviewHeader(printerClass: PrinterClass) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Filled.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Text(
            "${stringResource(Res.string.printing_live_preview).uppercase()} · ${printerClass.label.uppercase()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The scrollable editor controls (name + block list + add buttons), reused in both layouts. */
private fun LazyListScope.templateEditorItems(
    template: Template,
    viewModel: TemplateEditViewModel,
) {
    item {
        OutlinedTextField(
            value = template.name,
            onValueChange = viewModel::updateName,
            label = { Text(stringResource(Res.string.printing_template_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    itemsIndexed(template.blocks, key = { index, _ -> index }) { index, block ->
        BlockCard(
            index = index,
            block = block,
            isFirst = index == 0,
            isLast = index == template.blocks.lastIndex,
            viewModel = viewModel,
            onTextChange = { viewModel.updateBlockText(index, it) },
            onToggleBold = { viewModel.toggleBold(index) },
            onSetAlign = { viewModel.setAlign(index, it) },
            onToggleLarge = { viewModel.toggleLarge(index) },
            onMoveUp = { viewModel.moveUp(index) },
            onMoveDown = { viewModel.moveDown(index) },
            onRemove = { viewModel.removeBlock(index) },
        )
    }

    item { AddBlockBar(viewModel) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddBlockBar(viewModel: TemplateEditViewModel) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AddBlockButton(Icons.Filled.TextFields, stringResource(Res.string.printing_add_text)) { viewModel.addText() }
        AddBlockButton(Icons.Filled.HorizontalRule, stringResource(Res.string.printing_add_divider)) { viewModel.addDivider() }
        AddBlockButton(Icons.Filled.SpaceBar, stringResource(Res.string.printing_add_space)) { viewModel.addSpacer() }
        AddBlockButton(Icons.Filled.TableChart, stringResource(Res.string.printing_add_table)) { viewModel.addTable() }
        AddBlockButton(Icons.Filled.GridView, stringResource(Res.string.printing_add_grid)) { viewModel.addGrid() }
    }
}

@Composable
private fun AddBlockButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun TemplatePreviewPane(
    template: Template,
    previewHtml: String?,
    previewElements: List<PrintElement>,
    modifier: Modifier = Modifier,
) {
    if (template.printerClass == PrinterClass.PAGE) {
        // The page preview is a native WebView (a heavyweight SwingPanel on desktop). It must fill its
        // slot directly — wrapping it in a width-capped / centered Box mis-sizes the native panel and
        // it overflows the pane off-screen. Let it fill and scroll natively.
        Box(modifier) {
            previewHtml?.let { HtmlPreview(it, Modifier.fillMaxSize()) }
        }
    } else {
        // Thermal / label is pure Compose, so we can render a paper-width sheet centered on a tinted
        // tray — on a wide pane the receipt stays a narrow strip (amounts don't fly to the far edge).
        Box(
            modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.TopCenter,
        ) {
            val paperWidth = (template.paper as? PaperSpec.Thermal)
                ?.let { if (it.widthChars <= 33) 260.dp else 340.dp } ?: 320.dp
            Box(
                Modifier
                    .padding(vertical = 18.dp)
                    .width(paperWidth)
                    .fillMaxHeight()
                    .shadow(2.dp, RoundedCornerShape(4.dp))
                    .background(Color.White),
            ) {
                ReceiptPreview(previewElements, Modifier.fillMaxSize())
            }
        }
    }
}

/** Editor for a STATIC (HTML-file) template: rename + re-import the .html file; preview is live. */
private fun LazyListScope.staticTemplateItems(
    template: Template,
    viewModel: TemplateEditViewModel,
) {
    item {
        OutlinedTextField(
            value = template.name,
            onValueChange = viewModel::updateName,
            label = { Text(stringResource(Res.string.printing_template_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    item {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.printing_static_template), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(Res.string.printing_static_template_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = { viewModel.replaceHtmlFile() }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(Res.string.printing_replace_html))
                }
            }
        }
    }
}

@Composable
private fun BlockCard(
    index: Int,
    block: TemplateBlock,
    isFirst: Boolean,
    isLast: Boolean,
    viewModel: TemplateEditViewModel,
    onTextChange: (String) -> Unit,
    onToggleBold: () -> Unit,
    onSetAlign: (Align) -> Unit,
    onToggleLarge: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    Icons.Filled.DragIndicator,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp),
                )
                IconTile(
                    icon = block.icon,
                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                    content = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 38,
                    cornerRadius = 10,
                    iconSize = 20,
                )
                Column(Modifier.weight(1f)) {
                    Text(blockTitle(block), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    val sub = blockSubtitle(block)
                    if (sub.isNotEmpty()) {
                        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                Column {
                    IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = stringResource(Res.string.printing_move_up), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = stringResource(Res.string.printing_move_down), modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(Res.string.printing_remove), tint = MaterialTheme.colorScheme.error)
                }
            }

            val editableLabel = editableTextLabel(block)
            if (editableLabel != null) {
                OutlinedTextField(
                    value = editableTextValue(block),
                    onValueChange = onTextChange,
                    label = { Text(editableLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (!hasStyle(block) && block !is TemplateBlock.LineTable && block !is TemplateBlock.InfoGrid) {
                Text(
                    stringResource(Res.string.printing_not_editable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when (block) {
                is TemplateBlock.LineTable -> LineTableEditor(index, block, viewModel)
                is TemplateBlock.InfoGrid -> InfoGridEditor(index, block, viewModel)
                else -> Unit
            }

            val style = styleOf(block)
            if (style != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = style.bold,
                        onClick = onToggleBold,
                        label = { Text(stringResource(Res.string.printing_bold)) },
                    )
                    FilterChip(
                        selected = style.widthScale > 1 || style.heightScale > 1,
                        onClick = onToggleLarge,
                        label = { Text(stringResource(Res.string.printing_large)) },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = style.align == Align.LEFT,
                        onClick = { onSetAlign(Align.LEFT) },
                        label = { Text(stringResource(Res.string.printing_align_left)) },
                    )
                    FilterChip(
                        selected = style.align == Align.CENTER,
                        onClick = { onSetAlign(Align.CENTER) },
                        label = { Text(stringResource(Res.string.printing_align_center)) },
                    )
                    FilterChip(
                        selected = style.align == Align.RIGHT,
                        onClick = { onSetAlign(Align.RIGHT) },
                        label = { Text(stringResource(Res.string.printing_align_right)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LineTableEditor(index: Int, block: TemplateBlock.LineTable, viewModel: TemplateEditViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        block.columns.forEachIndexed { col, column ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = column.header,
                        onValueChange = { viewModel.updateColumnHeader(index, col, it) },
                        label = { Text(stringResource(Res.string.printing_column_header)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { viewModel.removeColumn(index, col) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(Res.string.printing_remove))
                    }
                }
                OutlinedTextField(
                    value = column.binding.fieldKey,
                    onValueChange = { viewModel.updateColumnField(index, col, it) },
                    label = { Text(stringResource(Res.string.printing_field_key)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = column.weight.toString(),
                        onValueChange = { viewModel.updateColumnWeight(index, col, it.toIntOrNull() ?: column.weight) },
                        label = { Text(stringResource(Res.string.printing_weight)) },
                        singleLine = true,
                        modifier = Modifier.width(96.dp),
                    )
                    AlignChips(column.align) { viewModel.setColumnAlign(index, col, it) }
                }
                HorizontalDivider()
            }
        }
        TextButton(onClick = { viewModel.addColumn(index) }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(Res.string.printing_add_column))
        }
    }
}

@Composable
private fun InfoGridEditor(index: Int, block: TemplateBlock.InfoGrid, viewModel: TemplateEditViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = block.columns.toString(),
            onValueChange = { viewModel.setGridColumns(index, it.toIntOrNull() ?: block.columns) },
            label = { Text(stringResource(Res.string.printing_grid_columns)) },
            singleLine = true,
            modifier = Modifier.width(160.dp),
        )
        block.cells.forEachIndexed { cell, gridCell ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = gridCell.label,
                        onValueChange = { viewModel.updateCellLabel(index, cell, it) },
                        label = { Text(stringResource(Res.string.printing_cell_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { viewModel.removeCell(index, cell) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(Res.string.printing_remove))
                    }
                }
                OutlinedTextField(
                    value = gridCell.binding?.fieldKey ?: "",
                    onValueChange = { viewModel.updateCellField(index, cell, it) },
                    label = { Text(stringResource(Res.string.printing_field_key)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider()
            }
        }
        TextButton(onClick = { viewModel.addCell(index) }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(Res.string.printing_add_cell))
        }
    }
}

@Composable
private fun AlignChips(selected: Align, onSelect: (Align) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected == Align.LEFT, { onSelect(Align.LEFT) }, label = { Text(stringResource(Res.string.printing_align_left)) })
        FilterChip(selected == Align.CENTER, { onSelect(Align.CENTER) }, label = { Text(stringResource(Res.string.printing_align_center)) })
        FilterChip(selected == Align.RIGHT, { onSelect(Align.RIGHT) }, label = { Text(stringResource(Res.string.printing_align_right)) })
    }
}

private fun styleOf(block: TemplateBlock): TemplateStyle? = when (block) {
    is TemplateBlock.StaticText -> block.style
    is TemplateBlock.BoundText -> block.style
    is TemplateBlock.KeyValue -> block.style
    else -> null
}

private fun hasStyle(block: TemplateBlock): Boolean = styleOf(block) != null

@Composable
private fun blockTitle(block: TemplateBlock): String = when (block) {
    is TemplateBlock.StaticText -> stringResource(Res.string.printing_block_text)
    is TemplateBlock.BoundText -> stringResource(Res.string.printing_block_bound, block.binding.fieldKey)
    is TemplateBlock.KeyValue -> stringResource(Res.string.printing_block_bound, block.binding.fieldKey)
    is TemplateBlock.LineTable -> stringResource(Res.string.printing_block_line_table)
    is TemplateBlock.InfoGrid -> stringResource(Res.string.printing_block_grid)
    is TemplateBlock.Divider -> stringResource(Res.string.printing_divider_char)
    is TemplateBlock.Spacer -> stringResource(Res.string.printing_spacer_lines)
    is TemplateBlock.Logo -> stringResource(Res.string.printing_block_logo)
    is TemplateBlock.BarcodeField -> stringResource(Res.string.printing_block_barcode)
    is TemplateBlock.QrField -> stringResource(Res.string.printing_block_qr)
    is TemplateBlock.CutMark -> stringResource(Res.string.printing_block_cut)
    is TemplateBlock.CashDrawer -> stringResource(Res.string.printing_block_cash_drawer)
}

/** A short, secondary description of a block — what it binds or how many parts it has. */
private fun blockSubtitle(block: TemplateBlock): String = when (block) {
    is TemplateBlock.BoundText -> block.binding.fieldKey
    is TemplateBlock.LineTable -> "${block.columns.size} columns"
    is TemplateBlock.InfoGrid -> "${block.cells.size} cells · ${block.columns} per row"
    is TemplateBlock.Spacer -> "${block.lines} lines"
    else -> ""
}

@Composable
private fun editableTextLabel(block: TemplateBlock): String? = when (block) {
    is TemplateBlock.StaticText -> stringResource(Res.string.printing_block_text)
    is TemplateBlock.KeyValue -> stringResource(Res.string.printing_block_label)
    is TemplateBlock.Divider -> stringResource(Res.string.printing_divider_char)
    is TemplateBlock.Spacer -> stringResource(Res.string.printing_spacer_lines)
    else -> null
}

private fun editableTextValue(block: TemplateBlock): String = when (block) {
    is TemplateBlock.StaticText -> block.text
    is TemplateBlock.KeyValue -> block.label
    is TemplateBlock.Divider -> block.char.toString()
    is TemplateBlock.Spacer -> block.lines.toString()
    else -> ""
}
