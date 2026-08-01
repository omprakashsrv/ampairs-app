package com.ampairs.printing.service

import com.ampairs.printing.core.model.Align
import com.ampairs.printing.core.model.ColumnStyle
import com.ampairs.printing.core.model.DocumentType
import com.ampairs.printing.core.model.FieldValue
import com.ampairs.printing.core.model.PrintDocument
import com.ampairs.printing.core.model.PrintElement
import com.ampairs.printing.core.model.PrintMeta
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.Symbology
import com.ampairs.printing.core.model.TableHeader
import com.ampairs.printing.core.model.TextStyle

/**
 * Self-contained test pages — no business data, no provider/engine. Each printer class gets a layout
 * exercising the features that matter for that class (thermal: styled text + barcode + QR + cut;
 * page: headings + a table; label: code + QR), so "Test print" proves the whole render→transport
 * path on any platform/OS without needing a real document.
 */
object TestPrintDocuments {

    fun forPrinter(printerClass: PrinterClass, printerName: String): PrintDocument =
        PrintDocument(
            meta = PrintMeta(documentType = DocumentType.RECEIPT),
            blocks = when (printerClass) {
                PrinterClass.THERMAL -> thermal(printerName)
                PrinterClass.PAGE -> page(printerName)
                PrinterClass.LABEL -> label(printerName)
            },
        )

    private fun text(
        s: String,
        bold: Boolean = false,
        align: Align = Align.LEFT,
        widthScale: Int = 1,
        heightScale: Int = 1,
    ) = PrintElement.TextLine(FieldValue.Text(s), TextStyle(bold, align, widthScale, heightScale))

    private fun kv(label: String, value: String) =
        PrintElement.KeyValueRow(FieldValue.Text(label), FieldValue.Text(value))

    private fun thermal(name: String) = listOf(
        text("AMPAIRS", bold = true, align = Align.CENTER, widthScale = 2, heightScale = 2),
        text("Test Print", align = Align.CENTER),
        PrintElement.Divider('='),
        kv("Printer", name),
        kv("Type", "Thermal (ESC/POS)"),
        kv("Status", "OK"),
        PrintElement.Divider('-'),
        text("Barcode", align = Align.CENTER),
        PrintElement.Barcode("123456789012", Symbology.CODE128),
        text("QR code", align = Align.CENTER),
        PrintElement.Qr("https://ampairs.com"),
        PrintElement.Divider('-'),
        text("If you can read this,", align = Align.CENTER),
        text("printing works!", bold = true, align = Align.CENTER),
        PrintElement.Feed(2),
        PrintElement.Cut(),
    )

    private fun page(name: String) = listOf(
        text("Ampairs — Test Page", bold = true, align = Align.CENTER, widthScale = 2, heightScale = 2),
        PrintElement.Spacer(1),
        kv("Printer", name),
        kv("Type", "Inkjet / Laser (page)"),
        PrintElement.Spacer(1),
        PrintElement.Table(
            columns = listOf(
                TableHeader("Item"),
                TableHeader("Qty", ColumnStyle(1, Align.RIGHT)),
                TableHeader("Amount", ColumnStyle(1, Align.RIGHT)),
            ),
            rows = listOf(
                listOf(FieldValue.Text("Sample item A"), FieldValue.Number(2.0, 0), FieldValue.Money(100.0)),
                listOf(FieldValue.Text("Sample item B"), FieldValue.Number(1.0, 0), FieldValue.Money(50.0)),
            ),
        ),
        PrintElement.Spacer(1),
        text("If this page prints correctly, your printer is ready.", align = Align.CENTER),
    )

    private fun label(name: String) = listOf(
        text(name, bold = true, align = Align.CENTER),
        PrintElement.Barcode("123456789012", Symbology.CODE128),
        PrintElement.Qr("https://ampairs.com", sizeModule = 4),
        text("TEST", align = Align.CENTER),
    )
}
