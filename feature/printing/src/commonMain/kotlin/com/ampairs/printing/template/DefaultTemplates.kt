package com.ampairs.printing.template

import com.ampairs.printing.core.model.Align
import com.ampairs.printing.core.model.BindingScope
import com.ampairs.printing.core.model.DocumentType
import com.ampairs.printing.core.model.EntityRef
import com.ampairs.printing.core.model.FieldBinding
import com.ampairs.printing.core.model.FieldSource
import com.ampairs.printing.core.model.PaperSpec
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.Template
import com.ampairs.printing.core.model.TemplateBlock
import com.ampairs.printing.core.model.TemplateColumn
import com.ampairs.printing.core.model.TemplateStyle

/**
 * Built-in templates so documents print correctly out of the box before any customization (FR-014).
 * These are the seed; the workspace can override them via the visual editor once that lands.
 */
object DefaultTemplates {

    /** A thermal tax-invoice template for the given paper (80mm by default). */
    fun thermalInvoice(paper: PaperSpec.Thermal = PaperSpec.THERMAL_80): Template = Template(
        id = "default_invoice_thermal_${paper.widthChars}",
        name = "Invoice (${paper.widthChars}c thermal)",
        documentType = DocumentType.INVOICE,
        printerClass = PrinterClass.THERMAL,
        paper = paper,
        blocks = listOf(
            TemplateBlock.BoundText(
                binding = std("seller_name"),
                style = TemplateStyle(bold = true, align = Align.CENTER, widthScale = 2, heightScale = 2),
            ),
            TemplateBlock.StaticText("TAX INVOICE", TemplateStyle(bold = true, align = Align.CENTER)),
            TemplateBlock.Divider(),
            TemplateBlock.KeyValue("No:", std("invoice_number")),
            TemplateBlock.KeyValue("Date:", std("invoice_date")),
            TemplateBlock.KeyValue("Customer:", std("customer_name", EntityRef.CUSTOMER)),
            TemplateBlock.Divider(),
            TemplateBlock.LineTable(
                columns = listOf(
                    TemplateColumn("Item", line("description"), weight = 5, align = Align.LEFT),
                    TemplateColumn("Qty", line("quantity"), weight = 2, align = Align.RIGHT),
                    TemplateColumn("Rate", line("price"), weight = 3, align = Align.RIGHT),
                    TemplateColumn("Amount", line("total_cost"), weight = 3, align = Align.RIGHT),
                ),
            ),
            TemplateBlock.Divider(),
            TemplateBlock.KeyValue("Sub Total", std("base_price"), TemplateStyle(align = Align.RIGHT)),
            TemplateBlock.KeyValue("Tax", std("total_tax"), TemplateStyle(align = Align.RIGHT)),
            TemplateBlock.KeyValue("TOTAL", std("total_cost"), TemplateStyle(bold = true, align = Align.RIGHT)),
            TemplateBlock.Divider(),
            TemplateBlock.QrField(FieldBinding("upi_qr", source = FieldSource.COMPUTED)),
            TemplateBlock.StaticText("Thank you for your business!", TemplateStyle(align = Align.CENTER)),
            TemplateBlock.Spacer(1),
            TemplateBlock.CutMark(),
        ),
    )

    /** A thermal order/estimate template (80mm by default). */
    fun thermalOrder(paper: PaperSpec.Thermal = PaperSpec.THERMAL_80): Template = Template(
        id = "default_order_thermal_${paper.widthChars}",
        name = "Order (${paper.widthChars}c thermal)",
        documentType = DocumentType.ORDER,
        printerClass = PrinterClass.THERMAL,
        paper = paper,
        blocks = listOf(
            TemplateBlock.BoundText(
                binding = std("seller_name"),
                style = TemplateStyle(bold = true, align = Align.CENTER, widthScale = 2, heightScale = 2),
            ),
            TemplateBlock.StaticText("ORDER", TemplateStyle(bold = true, align = Align.CENTER)),
            TemplateBlock.Divider(),
            TemplateBlock.KeyValue("No:", std("order_number")),
            TemplateBlock.KeyValue("Date:", std("order_date")),
            TemplateBlock.KeyValue("Customer:", std("customer_name", EntityRef.CUSTOMER)),
            TemplateBlock.Divider(),
            TemplateBlock.LineTable(
                columns = listOf(
                    TemplateColumn("Item", line("description"), weight = 5, align = Align.LEFT),
                    TemplateColumn("Qty", line("quantity"), weight = 2, align = Align.RIGHT),
                    TemplateColumn("Rate", line("price"), weight = 3, align = Align.RIGHT),
                    TemplateColumn("Amount", line("total_cost"), weight = 3, align = Align.RIGHT),
                ),
            ),
            TemplateBlock.Divider(),
            TemplateBlock.KeyValue("Sub Total", std("base_price"), TemplateStyle(align = Align.RIGHT)),
            TemplateBlock.KeyValue("Tax", std("total_tax"), TemplateStyle(align = Align.RIGHT)),
            TemplateBlock.KeyValue("TOTAL", std("total_cost"), TemplateStyle(bold = true, align = Align.RIGHT)),
            TemplateBlock.Divider(),
            TemplateBlock.StaticText("Thank you!", TemplateStyle(align = Align.CENTER)),
            TemplateBlock.Spacer(1),
            TemplateBlock.CutMark(),
        ),
    )

    /** A thermal sale-receipt template (compact). */
    fun thermalReceipt(paper: PaperSpec.Thermal = PaperSpec.THERMAL_58): Template = Template(
        id = "default_receipt_thermal_${paper.widthChars}",
        name = "Receipt (${paper.widthChars}c thermal)",
        documentType = DocumentType.RECEIPT,
        printerClass = PrinterClass.THERMAL,
        paper = paper,
        blocks = listOf(
            TemplateBlock.BoundText(std("seller_name"), TemplateStyle(bold = true, align = Align.CENTER)),
            TemplateBlock.StaticText("RECEIPT", TemplateStyle(align = Align.CENTER)),
            TemplateBlock.Divider(),
            TemplateBlock.LineTable(
                columns = listOf(
                    TemplateColumn("Item", line("description"), weight = 6, align = Align.LEFT),
                    TemplateColumn("Amt", line("total_cost"), weight = 3, align = Align.RIGHT),
                ),
            ),
            TemplateBlock.Divider(),
            TemplateBlock.KeyValue("TOTAL", std("total_cost"), TemplateStyle(bold = true, align = Align.RIGHT)),
            TemplateBlock.QrField(FieldBinding("upi_qr", source = FieldSource.COMPUTED)),
            TemplateBlock.Spacer(1),
            TemplateBlock.CutMark(),
        ),
    )

    /** An A4 tax-invoice template for page printers. */
    fun pageInvoice(): Template = Template(
        id = "default_invoice_a4",
        name = "Invoice (A4)",
        documentType = DocumentType.INVOICE,
        printerClass = PrinterClass.PAGE,
        paper = PaperSpec.Page(),
        blocks = listOf(
            TemplateBlock.Logo(),
            TemplateBlock.BoundText(std("seller_name"), TemplateStyle(bold = true, align = Align.CENTER)),
            TemplateBlock.StaticText("TAX INVOICE", TemplateStyle(bold = true, align = Align.CENTER)),
            TemplateBlock.KeyValue("Invoice No:", std("invoice_number")),
            TemplateBlock.KeyValue("Date:", std("invoice_date")),
            TemplateBlock.KeyValue("Bill To:", std("customer_name", EntityRef.CUSTOMER)),
            TemplateBlock.LineTable(
                columns = listOf(
                    TemplateColumn("Item", line("description"), weight = 6, align = Align.LEFT),
                    TemplateColumn("Qty", line("quantity"), weight = 1, align = Align.RIGHT),
                    TemplateColumn("Rate", line("price"), weight = 2, align = Align.RIGHT),
                    TemplateColumn("Amount", line("total_cost"), weight = 2, align = Align.RIGHT),
                ),
            ),
            TemplateBlock.KeyValue("Sub Total", std("base_price"), TemplateStyle(align = Align.RIGHT)),
            TemplateBlock.KeyValue("Tax", std("total_tax"), TemplateStyle(align = Align.RIGHT)),
            TemplateBlock.KeyValue("Grand Total", std("total_cost"), TemplateStyle(bold = true, align = Align.RIGHT)),
            TemplateBlock.StaticText("This is a computer-generated invoice."),
        ),
    )

    /** An A4 order template for page printers. */
    fun pageOrder(): Template = Template(
        id = "default_order_a4",
        name = "Order (A4)",
        documentType = DocumentType.ORDER,
        printerClass = PrinterClass.PAGE,
        paper = PaperSpec.Page(),
        blocks = listOf(
            TemplateBlock.BoundText(std("seller_name"), TemplateStyle(bold = true, align = Align.CENTER)),
            TemplateBlock.StaticText("ORDER", TemplateStyle(bold = true, align = Align.CENTER)),
            TemplateBlock.KeyValue("Order No:", std("order_number")),
            TemplateBlock.KeyValue("Date:", std("order_date")),
            TemplateBlock.KeyValue("Customer:", std("customer_name", EntityRef.CUSTOMER)),
            TemplateBlock.LineTable(
                columns = listOf(
                    TemplateColumn("Item", line("description"), weight = 6, align = Align.LEFT),
                    TemplateColumn("Qty", line("quantity"), weight = 1, align = Align.RIGHT),
                    TemplateColumn("Rate", line("price"), weight = 2, align = Align.RIGHT),
                    TemplateColumn("Amount", line("total_cost"), weight = 2, align = Align.RIGHT),
                ),
            ),
            TemplateBlock.KeyValue("Sub Total", std("base_price"), TemplateStyle(align = Align.RIGHT)),
            TemplateBlock.KeyValue("Tax", std("total_tax"), TemplateStyle(align = Align.RIGHT)),
            TemplateBlock.KeyValue("Grand Total", std("total_cost"), TemplateStyle(bold = true, align = Align.RIGHT)),
        ),
    )

    /** All seeded templates, for first-run seeding into the template store. */
    fun all(): List<Template> = listOf(
        thermalInvoice(PaperSpec.THERMAL_80),
        thermalInvoice(PaperSpec.THERMAL_58),
        thermalOrder(PaperSpec.THERMAL_80),
        thermalReceipt(PaperSpec.THERMAL_58),
        pageInvoice(),
        pageOrder(),
    )

    private fun std(key: String, entityRef: EntityRef = EntityRef.SELF) =
        FieldBinding(fieldKey = key, scope = BindingScope.DOCUMENT, entityRef = entityRef, source = FieldSource.STANDARD)

    private fun line(key: String) =
        FieldBinding(fieldKey = key, scope = BindingScope.LINE, entityRef = EntityRef.SELF, source = FieldSource.STANDARD)
}
