package com.ampairs.printing.render

import com.ampairs.printing.core.engine.PrintEngine
import com.ampairs.printing.core.model.Align
import com.ampairs.printing.core.model.BindingScope
import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.model.DocumentType
import com.ampairs.printing.core.model.EntityRef
import com.ampairs.printing.core.model.FieldBinding
import com.ampairs.printing.core.model.FieldSource
import com.ampairs.printing.core.model.FieldValue
import com.ampairs.printing.core.model.PaperSpec
import com.ampairs.printing.core.model.PlainValueFormatter
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.model.Template
import com.ampairs.printing.core.model.TemplateBlock
import com.ampairs.printing.core.model.TemplateColumn
import com.ampairs.printing.core.model.TemplateStyle
import com.ampairs.printing.core.provider.LineValues
import com.ampairs.printing.core.provider.PrintValueProvider
import com.ampairs.printing.core.render.RenderedOutput
import com.ampairs.printing.render.escpos.EscPosRenderer
import com.ampairs.printing.render.html.HtmlRenderer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

private class FakeInvoiceProvider : PrintValueProvider {
    override val documentType = DocumentType.INVOICE
    override suspend fun standardValues(documentId: String) = mapOf(
        "invoice_number" to FieldValue.Text("INV-001"),
        "grand_total" to FieldValue.Money(240.00),
    )
    override suspend fun customValues(documentId: String) = emptyMap<String, String>()
    override suspend fun lines(documentId: String) = listOf(
        LineValues(standard = mapOf("name" to FieldValue.Text("Widget"), "qty" to FieldValue.Number(2.0, 0), "amount" to FieldValue.Money(240.0))),
    )
}

private val template = Template(
    id = "tpl-inv-80",
    name = "Invoice 80mm",
    documentType = DocumentType.INVOICE,
    printerClass = PrinterClass.THERMAL,
    paper = PaperSpec.THERMAL_80,
    blocks = listOf(
        TemplateBlock.StaticText("TAX INVOICE", TemplateStyle(bold = true, align = Align.CENTER)),
        TemplateBlock.KeyValue("No:", FieldBinding("invoice_number", source = FieldSource.STANDARD)),
        TemplateBlock.LineTable(
            columns = listOf(
                TemplateColumn("Item", FieldBinding("name", BindingScope.LINE, EntityRef.SELF, FieldSource.STANDARD), weight = 3),
                TemplateColumn("Qty", FieldBinding("qty", BindingScope.LINE, EntityRef.SELF, FieldSource.STANDARD), weight = 1, align = Align.RIGHT),
                TemplateColumn("Amt", FieldBinding("amount", BindingScope.LINE, EntityRef.SELF, FieldSource.STANDARD), weight = 2, align = Align.RIGHT),
            ),
        ),
        TemplateBlock.KeyValue("TOTAL", FieldBinding("grand_total", source = FieldSource.STANDARD)),
        TemplateBlock.CutMark(),
    ),
)

private val profile = PrinterProfile(
    id = "p1", name = "Counter", printerClass = PrinterClass.THERMAL,
    connectionType = ConnectionType.NETWORK, paper = PaperSpec.THERMAL_80,
)

class RenderPipelineTest {

    @Test
    fun engineThenEscPosProducesBytesWithStoredTotal() = runTest {
        val doc = PrintEngine().build(template, "INV-001", FakeInvoiceProvider())
        val output = EscPosRenderer().render(doc, profile, PlainValueFormatter)
        assertTrue(output is RenderedOutput.Bytes)
        val text = (output as RenderedOutput.Bytes).data.decodeToString()
        assertTrue(text.contains("TAX INVOICE"))
        assertTrue(text.contains("INV-001"))
        assertTrue(text.contains("Widget"))
        // Total uses the document's stored amount (240.00) — never recomputed.
        assertTrue(text.contains("240.00"), "printed total must equal the stored amount")
    }

    @Test
    fun engineThenHtmlProducesMarkup() = runTest {
        val doc = PrintEngine().build(template.copy(printerClass = PrinterClass.PAGE, paper = PaperSpec.Page()), "INV-001", FakeInvoiceProvider())
        val output = HtmlRenderer().render(doc, profile.copy(printerClass = PrinterClass.PAGE, paper = PaperSpec.Page()), PlainValueFormatter)
        assertTrue(output is RenderedOutput.Markup)
        val html = (output as RenderedOutput.Markup).html
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("INV-001"))
        assertTrue(html.contains("240.00"))
    }
}
