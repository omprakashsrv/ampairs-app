package com.ampairs.printing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ampairsapp.feature.printing.generated.resources.Res
import ampairsapp.feature.printing.generated.resources.printing_add_divider
import ampairsapp.feature.printing.generated.resources.printing_add_space
import ampairsapp.feature.printing.generated.resources.printing_add_text
import ampairsapp.feature.printing.generated.resources.printing_align_center
import ampairsapp.feature.printing.generated.resources.printing_align_left
import ampairsapp.feature.printing.generated.resources.printing_align_right
import ampairsapp.feature.printing.generated.resources.printing_block_barcode
import ampairsapp.feature.printing.generated.resources.printing_block_bound
import ampairsapp.feature.printing.generated.resources.printing_block_cash_drawer
import ampairsapp.feature.printing.generated.resources.printing_block_cut
import ampairsapp.feature.printing.generated.resources.printing_block_label
import ampairsapp.feature.printing.generated.resources.printing_block_line_table
import ampairsapp.feature.printing.generated.resources.printing_block_logo
import ampairsapp.feature.printing.generated.resources.printing_block_qr
import ampairsapp.feature.printing.generated.resources.printing_block_text
import ampairsapp.feature.printing.generated.resources.printing_bold
import ampairsapp.feature.printing.generated.resources.printing_divider_char
import ampairsapp.feature.printing.generated.resources.printing_edit_template
import ampairsapp.feature.printing.generated.resources.printing_large
import ampairsapp.feature.printing.generated.resources.printing_move_down
import ampairsapp.feature.printing.generated.resources.printing_move_up
import ampairsapp.feature.printing.generated.resources.printing_not_editable
import ampairsapp.feature.printing.generated.resources.printing_remove
import ampairsapp.feature.printing.generated.resources.printing_save
import ampairsapp.feature.printing.generated.resources.printing_spacer_lines
import ampairsapp.feature.printing.generated.resources.printing_template_name
import com.ampairs.printing.core.model.Align
import com.ampairs.printing.core.model.PrintElement
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.Template
import com.ampairs.printing.core.model.TemplateBlock
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

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.savedEvent.collect { onBack() }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(state.template?.name ?: stringResource(Res.string.printing_edit_template)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save() }, enabled = state.template != null && !state.saving) {
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
                // Wide (tablet / desktop): settings and live preview side by side.
                Row(Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        templateEditorItems(template, viewModel)
                    }
                    VerticalDivider()
                    TemplatePreviewPane(
                        template = template,
                        previewHtml = state.previewHtml,
                        previewElements = state.previewElements,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            } else {
                // Narrow (phone): preview on top, settings below.
                Column(Modifier.fillMaxSize()) {
                    TemplatePreviewPane(
                        template = template,
                        previewHtml = state.previewHtml,
                        previewElements = state.previewElements,
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                    )
                    HorizontalDivider()
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        templateEditorItems(template, viewModel)
                    }
                }
            }
        }
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
            onTextChange = { viewModel.updateBlockText(index, it) },
            onToggleBold = { viewModel.toggleBold(index) },
            onSetAlign = { viewModel.setAlign(index, it) },
            onToggleLarge = { viewModel.toggleLarge(index) },
            onMoveUp = { viewModel.moveUp(index) },
            onMoveDown = { viewModel.moveDown(index) },
            onRemove = { viewModel.removeBlock(index) },
        )
    }

    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { viewModel.addText() }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(stringResource(Res.string.printing_add_text))
            }
            TextButton(onClick = { viewModel.addDivider() }) {
                Icon(Icons.Default.HorizontalRule, contentDescription = null)
                Text(stringResource(Res.string.printing_add_divider))
            }
            TextButton(onClick = { viewModel.addSpacer() }) {
                Icon(Icons.Default.SpaceBar, contentDescription = null)
                Text(stringResource(Res.string.printing_add_space))
            }
        }
    }
}

@Composable
private fun TemplatePreviewPane(
    template: Template,
    previewHtml: String?,
    previewElements: List<PrintElement>,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        if (template.printerClass == PrinterClass.PAGE) {
            previewHtml?.let { HtmlPreview(it, Modifier.fillMaxSize()) }
        } else {
            ReceiptPreview(previewElements, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun BlockCard(
    index: Int,
    block: TemplateBlock,
    isFirst: Boolean,
    isLast: Boolean,
    onTextChange: (String) -> Unit,
    onToggleBold: () -> Unit,
    onSetAlign: (Align) -> Unit,
    onToggleLarge: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    blockTitle(block),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(Res.string.printing_move_up))
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = stringResource(Res.string.printing_move_down))
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.printing_remove))
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
            } else if (!hasStyle(block)) {
                Text(
                    stringResource(Res.string.printing_not_editable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
    is TemplateBlock.Divider -> stringResource(Res.string.printing_divider_char)
    is TemplateBlock.Spacer -> stringResource(Res.string.printing_spacer_lines)
    is TemplateBlock.Logo -> stringResource(Res.string.printing_block_logo)
    is TemplateBlock.BarcodeField -> stringResource(Res.string.printing_block_barcode)
    is TemplateBlock.QrField -> stringResource(Res.string.printing_block_qr)
    is TemplateBlock.CutMark -> stringResource(Res.string.printing_block_cut)
    is TemplateBlock.CashDrawer -> stringResource(Res.string.printing_block_cash_drawer)
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
