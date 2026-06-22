package com.ampairs.invoice.domain

import com.ampairs.customer.domain.Customer
import com.ampairs.product.domain.ProductSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure domain-logic tests for the invoice aggregate ([Invoice] / [InvoiceItem]) and its entity
 * mappers. These cover the numeric core of a tax invoice — line totals, unit/variant rescaling,
 * GST back-calculation, header roll-ups and the entity round-trip — with no Room, DI or network.
 */
class InvoiceDomainTest {

    private fun product(
        id: String = "PRD1",
        name: String = "Widget",
        price: Double = 100.0,
        hsn: String = "1234",
        baseUnitId: String = "U_PCS",
        mrp: Double = 120.0,
    ) = ProductSummary(id = id, name = name, code = "C$id", sellingPrice = price, taxCode = hsn, baseUnitId = baseUnitId, mrp = mrp)

    private fun taxInfo(name: String, percent: Double, spec: TaxSpec = TaxSpec.INTRA) =
        TaxInfo(id = "T_$name", name = name, percentage = percent, formattedName = name, taxSpec = spec, value = 0.0)

    // ── InvoiceItem: totals + unit/variant ──

    @Test
    fun `item total is quantity times price`() {
        val item = InvoiceItem(product(price = 50.0))
        item.quantity = 3.0
        assertEquals(150.0, item.totalCost)
        assertEquals(3.0, item.baseQuantity, "base qty = qty × multiplier (1.0 by default)")
    }

    @Test
    fun `setting quantity propagates to the backing product summary`() {
        val p = product(price = 10.0)
        val item = InvoiceItem(p)
        item.quantity = 4.0
        assertEquals(4.0, p.quantity)
        assertEquals(40.0, item.totalCost)
    }

    @Test
    fun `selectUnit rescales the per-unit price and base quantity`() {
        val item = InvoiceItem(product(price = 100.0))
        item.quantity = 2.0
        item.selectUnit(unitId = "U_BOX", name = "BOX", multiplier = 12.0)

        assertEquals("U_BOX", item.unitId)
        assertEquals("BOX", item.unitName)
        assertEquals(12.0, item.unitMultiplier)
        assertEquals(1200.0, item.price, "100 product price × 12 multiplier")
        assertEquals(24.0, item.baseQuantity, "2 boxes × 12 = 24 base units")
        assertEquals(2400.0, item.totalCost)
    }

    @Test
    fun `selectUnit does not overwrite a manually overridden price`() {
        val item = InvoiceItem(product(price = 100.0))
        item.quantity = 1.0
        item.price = 90.0
        item.priceOverridden = true
        item.selectUnit(unitId = "U_BOX", name = "BOX", multiplier = 12.0)

        assertEquals(90.0, item.price, "an overridden price survives a unit switch")
        assertEquals(12.0, item.baseQuantity)
    }

    @Test
    fun `selectUnit treats a non-positive multiplier as 1`() {
        val item = InvoiceItem(product(price = 100.0))
        item.quantity = 2.0
        item.selectUnit(unitId = "U", name = "X", multiplier = 0.0)
        assertEquals(1.0, item.unitMultiplier)
        assertEquals(2.0, item.baseQuantity)
    }

    @Test
    fun `selectVariant re-bases the price on the variant price`() {
        val item = InvoiceItem(product(price = 100.0))
        item.quantity = 1.0
        item.selectVariant(sku = "SKU-L", variantPrice = 150.0)

        assertEquals("SKU-L", item.variantSku)
        assertEquals(150.0, item.productPrice)
        assertEquals(150.0, item.price)
    }

    @Test
    fun `selectVariant with null price falls back to the product selling price`() {
        val item = InvoiceItem(product(price = 100.0))
        item.quantity = 1.0
        item.selectVariant(sku = "SKU-X", variantPrice = null)
        assertEquals(100.0, item.productPrice)
        assertEquals(100.0, item.price)
    }

    @Test
    fun `selectVariant keeps an overridden price`() {
        val item = InvoiceItem(product(price = 100.0))
        item.quantity = 1.0
        item.price = 80.0
        item.priceOverridden = true
        item.selectVariant(sku = "SKU-L", variantPrice = 150.0)
        assertEquals(80.0, item.price)
        assertEquals(150.0, item.productPrice, "productPrice still updates even when display price is pinned")
    }

    // ── InvoiceItem: GST back-calculation (tax-inclusive total) ──

    @Test
    fun `updateTaxes back-calculates base and tax from a tax-inclusive total`() {
        // Percentages are whole numbers per the source's `getTaxPercent() / 100` scaling:
        // 9 + 9 = 18 → 0.18; base = 118 / 1.18 = 100; tax = 18.
        val item = InvoiceItem(product(price = 118.0))
        item.quantity = 1.0                       // totalCost = 118
        item.taxInfos = listOf(taxInfo("CGST", 9.0), taxInfo("SGST", 9.0))
        item.updateTaxes(TaxSpec.INTRA)

        assertEquals(100.0, item.basePrice, absoluteTolerance = 1e-9, message = "118 / (1 + 0.18)")
        assertEquals(18.0, item.totalTax, absoluteTolerance = 1e-9)
        assertEquals(TaxSpec.INTRA, item.taxSpec)
    }

    @Test
    fun `updateTaxes with no tax rows yields zero tax and base equal to total`() {
        val item = InvoiceItem(product(price = 100.0))
        item.quantity = 2.0                       // totalCost = 200
        item.updateTaxes(TaxSpec.INTER)
        assertEquals(200.0, item.basePrice, absoluteTolerance = 1e-9)
        assertEquals(0.0, item.totalTax, absoluteTolerance = 1e-9)
    }

    // ── Invoice: header roll-ups ──

    @Test
    fun `setting items recomputes totalCost totalQuantity and totalItems`() {
        val a = InvoiceItem(product(id = "A", price = 50.0)).apply { quantity = 2.0 }   // 100
        val b = InvoiceItem(product(id = "B", price = 30.0)).apply { quantity = 3.0 }   // 90
        val invoice = Invoice().apply { items = mutableListOf(a, b) }

        assertEquals(190.0, invoice.totalCost)
        assertEquals(5.0, invoice.totalQuantity)
        assertEquals(2, invoice.totalItems)
    }

    @Test
    fun `updateTaxes aggregates per-line GST into invoice-level tax infos`() {
        val a = InvoiceItem(product(id = "A", price = 118.0)).apply {
            quantity = 1.0
            taxInfos = listOf(taxInfo("CGST", 9.0), taxInfo("SGST", 9.0))
        }
        val b = InvoiceItem(product(id = "B", price = 236.0)).apply {
            quantity = 1.0
            taxInfos = listOf(taxInfo("CGST", 9.0), taxInfo("SGST", 9.0))
        }
        val invoice = Invoice().apply {
            taxSpec = TaxSpec.INTRA
            items = mutableListOf(a, b)
        }
        invoice.updateTaxes()

        // base: 100 + 200 = 300; tax: 18 + 36 = 54
        assertEquals(300.0, invoice.basePrice, absoluteTolerance = 1e-9)
        assertEquals(54.0, invoice.totalTax, absoluteTolerance = 1e-9)
        // CGST and SGST collapse into one row each (same name + percentage)
        val infos = invoice.taxInfos!!
        assertEquals(2, infos.size)
        // Per-row `value` aggregates each line's CGST value (base × percentage in the source):
        // line A: 100 × 9 = 900, line B: 200 × 9 = 1800 → 2700.
        val cgst = infos.first { it.name == "CGST" }
        assertEquals(2700.0, cgst.value!!, absoluteTolerance = 1e-6)
    }

    @Test
    fun `updateDiscount rolls up per-line discounts when any value is positive`() {
        val a = InvoiceItem(product(id = "A")).apply {
            quantity = 1.0
            discount.add(Discount(percent = 10.0, value = 15.0))
        }
        val invoice = Invoice().apply { items = mutableListOf(a) }
        invoice.updateDiscount()

        assertNotNull(invoice.discount)
        assertEquals(10.0, invoice.discount!!.first().percent)
        assertEquals(15.0, invoice.discount!!.first().value)
    }

    @Test
    fun `updateDiscount leaves discount null when no line has a positive value`() {
        val a = InvoiceItem(product(id = "A")).apply {
            quantity = 1.0
            discount.add(Discount(percent = 5.0, value = 0.0))
        }
        val invoice = Invoice().apply { items = mutableListOf(a) }
        invoice.updateDiscount()
        assertNull(invoice.discount, "discount is set only when the rolled-up value exceeds 0")
    }

    @Test
    fun `a new invoice gets an INV-prefixed id`() {
        val invoice = Invoice()
        assertTrue(invoice.id.startsWith(INVOICE_PREFIX), "id was: ${invoice.id}")
        assertTrue(invoice.id.length > INVOICE_PREFIX.length)
    }

    // ── entity mappers ──

    @Test
    fun `asDatabaseModel snapshots customer fields and serializes synced as zero`() {
        val invoice = Invoice().apply {
            id = "INV9"
            invoiceNumber = "INV/9"
            status = InvoiceStatus.NEW
            customer = Customer(uid = "CUS1", name = "Acme", gstNumber = "27ABCDE", phone = "999")
            totalCost = 500.0
            totalTax = 90.0
            basePrice = 410.0
            totalItems = 2
            totalQuantity = 4.0
        }

        val entity = invoice.asDatabaseModel()

        assertEquals("INV9", entity.id)
        assertEquals("INV/9", entity.invoice_number)
        assertEquals("CUS1", entity.customer_id)
        assertEquals("Acme", entity.customer_name)
        assertEquals("27ABCDE", entity.customer_gst)
        assertEquals("999", entity.customer_phone)
        assertEquals(500.0, entity.total_cost)
        assertEquals(90.0, entity.total_tax)
        assertEquals(2L, entity.total_items)
        assertEquals(0L, entity.synced, "writes are always unsynced")
        assertEquals(InvoiceStatus.NEW.name, entity.status)
    }

    @Test
    fun `asDatabaseModel maps a null customer to empty strings`() {
        val entity = Invoice().apply { customer = null }.asDatabaseModel()
        assertEquals("", entity.customer_id)
        assertEquals("", entity.customer_name)
        assertEquals("", entity.customer_gst)
        assertNull(entity.customer_phone)
    }

    @Test
    fun `asDomainModelSimple round-trips the core invoice fields`() {
        val invoice = Invoice().apply {
            id = "INV7"
            invoiceNumber = "INV/7"
            status = InvoiceStatus.NEW
            customer = Customer(uid = "CUS2", name = "Beta", gstNumber = "29XYZ")
            totalCost = 250.0
            totalTax = 30.0
            basePrice = 220.0
            totalItems = 1
            totalQuantity = 1.0
            sellerName = "My Biz"
        }
        val back = invoice.asDatabaseModel().asDomainModelSimple()

        assertEquals("INV7", back.id)
        assertEquals("INV/7", back.invoiceNumber)
        assertEquals(InvoiceStatus.NEW, back.status)
        assertEquals("CUS2", back.customer?.uid)
        assertEquals("Beta", back.customer?.name)
        assertEquals("29XYZ", back.customer?.gstNumber)
        assertEquals(250.0, back.totalCost)
        assertEquals(30.0, back.totalTax)
        assertEquals("My Biz", back.sellerName)
        assertTrue(back.items.isEmpty(), "the simple mapper does not hydrate items")
    }

    @Test
    fun `list asDatabaseModel maps each item with its invoice id and serialized tax`() {
        val item = InvoiceItem(product(id = "P", price = 100.0)).apply {
            quantity = 2.0
            taxInfos = listOf(taxInfo("IGST", 18.0, TaxSpec.INTER))
        }
        val entities = listOf(item).asDatabaseModel("INV-PARENT")

        assertEquals(1, entities.size)
        val e = entities.single()
        assertEquals("INV-PARENT", e.invoice_id)
        assertEquals("P", e.product_id)
        assertEquals(2.0, e.quantity)
        assertEquals(200.0, e.total_cost)
        assertEquals("1234", e.tax_code, "tax code comes from the product")
        assertTrue(e.tax_info!!.contains("IGST"), "tax infos are serialized into tax_info")
        assertEquals(1L, e.active)
    }

    @Test
    fun `list asDatabaseModel omits discount json when there are no discounts`() {
        val item = InvoiceItem(product(id = "P")).apply { quantity = 1.0 }
        val e = listOf(item).asDatabaseModel("INV-X").single()
        assertNull(e.discount, "no discounts → null discount column, not an empty json array")
    }
}
