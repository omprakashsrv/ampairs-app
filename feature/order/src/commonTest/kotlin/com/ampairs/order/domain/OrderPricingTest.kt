package com.ampairs.order.domain

import com.ampairs.product.domain.ProductSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure domain / pricing tests for the order line and aggregate. No fakes, no coroutines — these
 * exercise the genuine arithmetic in [OrderItem] and [Order]. Every assertion mirrors what the
 * source ACTUALLY computes (verified against OrderItem.kt / Order.kt), not what one might expect.
 *
 * Notable behaviours pinned (so a regression is caught even if the formula is debatable):
 *  - `updateTaxes` derives basePrice from the tax-inclusive totalCost: basePrice = totalCost/(1+rate)
 *    where rate = sum(percentage)/100, and per-line `taxInfo.value = basePrice * percentage`
 *    (percentage as a whole number — NOT divided by 100 in that line).
 *  - Order-level taxInfos are merged by (name, percentage) summing their values.
 */
class OrderPricingTest {

    private fun product(
        id: String = "PRD1",
        sellingPrice: Double = 100.0,
        mrp: Double = 120.0,
        dp: Double = 80.0,
        taxCode: String = "1234",
        name: String = "Widget",
        code: String = "W-1",
    ) = ProductSummary(
        id = id,
        name = name,
        code = code,
        mrp = mrp,
        dp = dp,
        sellingPrice = sellingPrice,
        taxCode = taxCode,
    )

    // ── line total ──

    @Test
    fun `line total is quantity times price`() {
        val item = OrderItem(product(sellingPrice = 50.0))
        item.quantity = 3.0

        assertEquals(50.0, item.price)
        assertEquals(150.0, item.totalCost, "3 * 50 = 150")
    }

    @Test
    fun `setting quantity updates base quantity by the unit multiplier`() {
        val item = OrderItem(product())
        item.selectUnit(unitId = "BOX", name = "box", multiplier = 12.0)
        item.quantity = 2.0

        assertEquals(24.0, item.baseQuantity, "2 boxes * 12 = 24 base units")
        // selectUnit re-based the per-unit price to productPrice * multiplier (not overridden).
        assertEquals(1200.0, item.price, "100 * 12 = 1200 per box")
        assertEquals(2400.0, item.totalCost, "2 * 1200")
    }

    @Test
    fun `selectUnit clamps a non-positive multiplier to one`() {
        val item = OrderItem(product(sellingPrice = 100.0))
        item.quantity = 1.0
        item.selectUnit(unitId = "U", name = "u", multiplier = 0.0)

        assertEquals(1.0, item.unitMultiplier, "multiplier <= 0 is clamped to 1.0")
        assertEquals(100.0, item.price)
    }

    @Test
    fun `an overridden price survives a unit change`() {
        val item = OrderItem(product(sellingPrice = 100.0))
        item.priceOverridden = true
        item.price = 250.0
        item.selectUnit(unitId = "BOX", name = "box", multiplier = 6.0)

        assertEquals(250.0, item.price, "manual price is not rescaled when priceOverridden")
        assertTrue(item.priceOverridden)
    }

    // ── variant selection ──

    @Test
    fun `selectVariant rebases the price on the variant price`() {
        val item = OrderItem(product(sellingPrice = 100.0))
        item.quantity = 2.0
        item.selectVariant(sku = "SKU-RED", variantPrice = 130.0)

        assertEquals("SKU-RED", item.variantSku)
        assertEquals(130.0, item.productPrice)
        assertEquals(130.0, item.price, "unitMultiplier 1.0 → price = productPrice")
        assertEquals(260.0, item.totalCost)
    }

    @Test
    fun `selectVariant with a null price falls back to the product selling price`() {
        val item = OrderItem(product(sellingPrice = 100.0))
        item.selectVariant(sku = "SKU-PLAIN", variantPrice = null)

        assertEquals(100.0, item.productPrice, "null variant price → product sellingPrice")
        assertEquals(100.0, item.price)
    }

    // ── tax derivation (inclusive totalCost → basePrice) ──

    @Test
    fun `updateTaxes splits a tax-inclusive total into base and tax`() {
        val item = OrderItem(product())
        item.quantity = 1.0
        item.price = 118.0            // total is tax-inclusive
        item.updateTotal()
        item.taxInfos = listOf(
            TaxInfo(id = "GST", name = "GST", percentage = 18.0, taxSpec = TaxSpec.INTRA),
        )

        item.updateTaxes(TaxSpec.INTRA)

        // rate = sum(percentage)/100 = 0.18 → basePrice = 118 / 1.18 = 100.0
        assertEquals(100.0, item.basePrice, 1e-9)
        assertEquals(18.0, item.totalTax, 1e-9, "totalTax = totalCost - basePrice")
        assertEquals(TaxSpec.INTRA, item.taxSpec)
    }

    @Test
    fun `updateTaxes sets each tax line value to basePrice times the whole percentage`() {
        // This documents the actual code path: taxInfo.value = basePrice * percentage
        // (percentage is NOT divided by 100 in this assignment).
        val item = OrderItem(product())
        item.quantity = 1.0
        item.price = 112.0
        item.updateTotal()
        val tax = TaxInfo(id = "GST", name = "GST", percentage = 12.0, taxSpec = TaxSpec.INTRA)
        item.taxInfos = listOf(tax)

        item.updateTaxes(TaxSpec.INTRA)

        val expectedBase = 112.0 / 1.12 // = 100.0
        assertEquals(expectedBase, item.basePrice, 1e-9)
        assertEquals(expectedBase * 12.0, tax.value!!, 1e-9, "value = basePrice * percentage (whole number)")
    }

    @Test
    fun `updateTaxes with no tax lines leaves the whole total as base price`() {
        val item = OrderItem(product())
        item.quantity = 2.0
        item.price = 50.0
        item.updateTotal()
        item.taxInfos = emptyList()

        item.updateTaxes(TaxSpec.INTER)

        assertEquals(100.0, item.totalCost)
        assertEquals(100.0, item.basePrice, 1e-9, "rate 0 → basePrice == totalCost")
        assertEquals(0.0, item.totalTax, 1e-9)
    }

    // ── order aggregate totals ──

    @Test
    fun `assigning items updates the order totals`() {
        val a = OrderItem(product(id = "A", sellingPrice = 10.0)).apply { quantity = 2.0 } // 20
        val b = OrderItem(product(id = "B", sellingPrice = 30.0)).apply { quantity = 1.0 } // 30
        val order = Order()
        order.items = mutableListOf(a, b)

        assertEquals(50.0, order.totalCost, "20 + 30")
        assertEquals(3.0, order.totalQuantity, "2 + 1")
        assertEquals(2, order.totalItems)
    }

    @Test
    fun `updateTaxes on the order accumulates base price and tax across lines`() {
        val a = OrderItem(product(id = "A")).apply {
            quantity = 1.0; price = 118.0; updateTotal()
            taxInfos = listOf(TaxInfo(id = "GST", name = "GST", percentage = 18.0, taxSpec = TaxSpec.INTRA))
        }
        val b = OrderItem(product(id = "B")).apply {
            quantity = 1.0; price = 118.0; updateTotal()
            taxInfos = listOf(TaxInfo(id = "GST", name = "GST", percentage = 18.0, taxSpec = TaxSpec.INTRA))
        }
        val order = Order().apply { taxSpec = TaxSpec.INTRA; items = mutableListOf(a, b) }

        order.updateTaxes()

        assertEquals(200.0, order.basePrice, 1e-9, "100 + 100")
        assertEquals(36.0, order.totalTax, 1e-9, "18 + 18")
        // Same (name, percentage) → merged into a single order-level tax line.
        assertEquals(1, order.taxInfos?.size)
        assertEquals("GST", order.taxInfos?.first()?.name)
    }

    @Test
    fun `updateTaxes keeps distinct tax lines separate`() {
        val a = OrderItem(product(id = "A")).apply {
            quantity = 1.0; price = 105.0; updateTotal()
            taxInfos = listOf(TaxInfo(id = "T5", name = "GST", percentage = 5.0, taxSpec = TaxSpec.INTRA))
        }
        val b = OrderItem(product(id = "B")).apply {
            quantity = 1.0; price = 118.0; updateTotal()
            taxInfos = listOf(TaxInfo(id = "T18", name = "GST", percentage = 18.0, taxSpec = TaxSpec.INTRA))
        }
        val order = Order().apply { taxSpec = TaxSpec.INTRA; items = mutableListOf(a, b) }

        order.updateTaxes()

        assertEquals(2, order.taxInfos?.size, "different percentages are not merged")
    }

    // ── order discount aggregation ──

    @Test
    fun `updateDiscount sums line discounts into one order discount`() {
        val a = OrderItem(product(id = "A")).apply {
            discount.add(Discount(percent = 10.0, value = 15.0))
        }
        val b = OrderItem(product(id = "B")).apply {
            discount.add(Discount(percent = 5.0, value = 25.0))
        }
        val order = Order().apply { items = mutableListOf(a, b) }

        order.updateDiscount()

        assertEquals(1, order.discount?.size)
        assertEquals(15.0, order.discount?.first()?.percent, "10 + 5")
        assertEquals(40.0, order.discount?.first()?.value, "15 + 25")
    }

    @Test
    fun `updateDiscount leaves discount null when there is nothing to discount`() {
        val order = Order().apply { items = mutableListOf(OrderItem(product())) }

        order.updateDiscount()

        assertNull(order.discount, "no positive line discount → order discount stays null")
    }

    // ── identity / defaults ──

    @Test
    fun `a new order auto-generates a prefixed id`() {
        val order = Order()
        assertTrue(order.id.startsWith(ORDER_PREFIX), "id should start with ORD")
        assertEquals(OrderStatus.NEW, order.status)
        assertEquals(TaxSpec.INTER, order.taxSpec)
    }

    @Test
    fun `a new order item auto-generates a prefixed id`() {
        val item = OrderItem(product())
        assertTrue(item.id.startsWith(ORDER_ITEM_PREFIX), "id should start with OIT")
        assertFalse(item.priceOverridden)
    }
}
