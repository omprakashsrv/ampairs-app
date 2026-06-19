package com.ampairs.printing.core.engine

import com.ampairs.printing.core.model.BindingScope
import com.ampairs.printing.core.model.EntityRef
import com.ampairs.printing.core.model.FieldBinding
import com.ampairs.printing.core.model.FieldSource
import com.ampairs.printing.core.model.FieldValue
import com.ampairs.printing.core.model.PlainValueFormatter
import com.ampairs.printing.core.model.PrintDocument
import com.ampairs.printing.core.model.PrintElement
import com.ampairs.printing.core.model.PrintMeta
import com.ampairs.printing.core.model.TableHeader
import com.ampairs.printing.core.model.ColumnStyle
import com.ampairs.printing.core.model.Template
import com.ampairs.printing.core.model.TemplateBlock
import com.ampairs.printing.core.provider.ComputedFieldCatalog
import com.ampairs.printing.core.provider.LineValues
import com.ampairs.printing.core.provider.PrintValueProvider

/**
 * Generic, domain-agnostic engine: resolves a [Template] against a [PrintValueProvider] (+ optional
 * [ComputedFieldCatalog]) into a printer-agnostic [PrintDocument]. Knows nothing about invoices/orders —
 * document knowledge lives in the per-feature provider (§4 inversion).
 */
class PrintEngine(
    private val computed: ComputedFieldCatalog? = null,
) {

    suspend fun build(
        template: Template,
        documentId: String,
        provider: PrintValueProvider,
    ): PrintDocument {
        val standard = provider.standardValues(documentId)
        val custom = provider.customValues(documentId)

        // Resolve nested header entities referenced by any DOCUMENT-scope binding (e.g. customer).
        val nestedRefs = template.blocks
            .flatMap(::bindingsOf)
            .filter { it.scope == BindingScope.DOCUMENT && it.entityRef != EntityRef.SELF }
            .map { it.entityRef }
            .toSet()
        val nested = HashMap<EntityRef, Map<String, FieldValue>>()
        for (ref in nestedRefs) nested[ref] = provider.nestedValues(documentId, ref)

        val needsLines = template.blocks.any { it is TemplateBlock.LineTable }
        val lines: List<LineValues> = if (needsLines) provider.lines(documentId) else emptyList()

        val elements = ArrayList<PrintElement>(template.blocks.size)
        for (block in template.blocks) {
            elements += renderBlock(block, template, documentId, standard, custom, nested, lines)
        }
        return PrintDocument(PrintMeta(documentType = template.documentType), elements)
    }

    private suspend fun renderBlock(
        block: TemplateBlock,
        template: Template,
        documentId: String,
        standard: Map<String, FieldValue>,
        custom: Map<String, String>,
        nested: Map<EntityRef, Map<String, FieldValue>>,
        lines: List<LineValues>,
    ): List<PrintElement> = when (block) {
        is TemplateBlock.BoundText -> {
            val v = resolveDoc(block.binding, template, documentId, standard, custom, nested)
            listOf(PrintElement.TextLine(v, block.style.toPrintStyle(), block.region))
        }

        is TemplateBlock.StaticText ->
            listOf(PrintElement.TextLine(FieldValue.Text(block.text), block.style.toPrintStyle(), block.region))

        is TemplateBlock.KeyValue -> {
            val v = resolveDoc(block.binding, template, documentId, standard, custom, nested)
            listOf(PrintElement.KeyValueRow(FieldValue.Text(block.label), v, block.style.toPrintStyle(), block.region))
        }

        is TemplateBlock.LineTable -> {
            val headers = block.columns.map { TableHeader(it.header, ColumnStyle(it.weight, it.align)) }
            val rows = lines.map { line ->
                block.columns.map { col ->
                    BindingResolver.resolve(col.binding, line.standard, line.custom, line.nested, null)
                }
            }
            listOf(PrintElement.Table(headers, rows, block.region))
        }

        is TemplateBlock.Divider -> listOf(PrintElement.Divider(block.char, block.region))
        is TemplateBlock.Spacer -> listOf(PrintElement.Spacer(block.lines, block.region))
        is TemplateBlock.Logo -> listOf(PrintElement.Image(block.ref, block.region))

        is TemplateBlock.BarcodeField -> {
            val v = resolveDoc(block.binding, template, documentId, standard, custom, nested)
            listOf(PrintElement.Barcode(PlainValueFormatter.format(v), block.symbology, region = block.region))
        }

        is TemplateBlock.QrField -> {
            val v = resolveDoc(block.binding, template, documentId, standard, custom, nested)
            listOf(PrintElement.Qr(PlainValueFormatter.format(v), region = block.region))
        }

        is TemplateBlock.CutMark -> listOf(PrintElement.Cut(block.partial, block.region))
        is TemplateBlock.CashDrawer -> listOf(PrintElement.CashDrawerKick(block.region))
    }

    private suspend fun resolveDoc(
        binding: FieldBinding,
        template: Template,
        documentId: String,
        standard: Map<String, FieldValue>,
        custom: Map<String, String>,
        nested: Map<EntityRef, Map<String, FieldValue>>,
    ): FieldValue {
        val computedValue = if (binding.source == FieldSource.COMPUTED) {
            computed?.compute(template.documentType, documentId, binding.fieldKey)
        } else {
            null
        }
        return BindingResolver.resolve(binding, standard, custom, nested, computedValue)
    }

    private fun bindingsOf(block: TemplateBlock): List<FieldBinding> = when (block) {
        is TemplateBlock.BoundText -> listOf(block.binding)
        is TemplateBlock.KeyValue -> listOf(block.binding)
        is TemplateBlock.LineTable -> block.columns.map { it.binding }
        is TemplateBlock.BarcodeField -> listOf(block.binding)
        is TemplateBlock.QrField -> listOf(block.binding)
        else -> emptyList()
    }
}
