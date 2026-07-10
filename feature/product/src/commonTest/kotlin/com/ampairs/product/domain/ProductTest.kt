package com.ampairs.product.domain

import com.ampairs.product.db.entity.ProductEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure domain logic for [Product] computed properties and the entity ↔ domain / API mappers in
 * Product.kt. No DB/coroutines. Assertions reflect the source exactly (e.g. `isLowStock` requires
 * BOTH stock and alert to be non-null; `totalStock` sums variants only when hasVariants is true).
 */
class ProductTest {

    private fun entity(
        id: String = "P1",
        name: String = "Widget",
        active: Int = 1,
        softDeleted: Int = 0,
        hasVariants: Int = 0,
        productType: String? = null,
        serviceType: String? = null,
        stock: Double? = null,
        attributesJson: String? = null,
        baseUnit: String? = null,
    ) = ProductEntity(
        id = id,
        name = name,
        code = "C1",
        tax_code = "HSN1",
        mrp = 100.0,
        dp = 80.0,
        selling_price = 90.0,
        stock_quantity = stock,
        product_type = productType,
        service_type = serviceType,
        has_variants = hasVariants,
        active = active,
        soft_deleted = softDeleted,
        attributes_json = attributesJson,
        base_unit = baseUnit,
    )

    // ---- computed properties ----

    @Test
    fun `isLowStock requires both stock and alert non-null`() {
        assertFalse(Product(stockQuantity = null, lowStockAlert = 5.0).isLowStock)
        assertFalse(Product(stockQuantity = 1.0, lowStockAlert = null).isLowStock)
        assertTrue(Product(stockQuantity = 5.0, lowStockAlert = 5.0).isLowStock, "boundary inclusive")
        assertTrue(Product(stockQuantity = 2.0, lowStockAlert = 5.0).isLowStock)
        assertFalse(Product(stockQuantity = 6.0, lowStockAlert = 5.0).isLowStock)
    }

    @Test
    fun `totalStock sums variant stock when product hasVariants`() {
        val p = Product(
            hasVariants = true,
            stockQuantity = 999.0, // ignored when variants present
            variants = listOf(
                ProductVariant(id = "v1", productId = "P", sku = "s1", variantName = "n1", stockQuantity = 3.0),
                ProductVariant(id = "v2", productId = "P", sku = "s2", variantName = "n2", stockQuantity = 4.0),
            ),
        )
        assertEquals(7.0, p.totalStock)
    }

    @Test
    fun `totalStock falls back to own stock when no variants`() {
        assertEquals(12.0, Product(hasVariants = false, stockQuantity = 12.0).totalStock)
        assertEquals(0.0, Product(hasVariants = false, stockQuantity = null).totalStock, "null stock -> 0.0")
    }

    @Test
    fun `totalStock uses own stock when hasVariants true but variants null`() {
        // hasVariants is true but the variant list wasn't loaded -> falls back to stockQuantity.
        assertEquals(8.0, Product(hasVariants = true, variants = null, stockQuantity = 8.0).totalStock)
    }

    @Test
    fun `hasLowStockVariants true only when a variant is low and hasVariants set`() {
        val lowVariant = ProductVariant(
            id = "v1", productId = "P", sku = "s", variantName = "n",
            stockQuantity = 1.0, lowStockAlert = 2.0,
        )
        assertTrue(Product(hasVariants = true, variants = listOf(lowVariant)).hasLowStockVariants)
        assertFalse(Product(hasVariants = false, variants = listOf(lowVariant)).hasLowStockVariants)
        assertFalse(Product(hasVariants = true, variants = null).hasLowStockVariants)
    }

    // ---- mappers ----

    @Test
    fun `asDomainModel maps Int flags and nullable ids to defaults`() {
        val domain = entity(active = 0, hasVariants = 1).asDomainModel()
        assertEquals("P1", domain.id)
        assertFalse(domain.active, "active = 0 -> false")
        assertTrue(domain.hasVariants)
        assertEquals("", domain.groupId, "null group_id maps to empty string")
        assertEquals(90.0, domain.sellingPrice)
        assertEquals(emptyMap(), domain.attributes, "null attributes_json -> empty map")
    }

    @Test
    fun `asDomainModel carries base_unit into baseUnitId (composer unit resolution)`() {
        // Regression: the composer resolves a line's units from ProductSummary.baseUnitId, so
        // asDomainModel dropping base_unit left every picked product with a blank "(base)" unit.
        assertEquals("UNT_PCS", entity(baseUnit = "UNT_PCS").asDomainModel().baseUnitId)
        assertEquals("UNT_PCS", entity(baseUnit = "UNT_PCS").asDomainModel().toSummary().baseUnitId)
    }

    @Test
    fun `asDomainModel parses valid product and service type, null on garbage`() {
        val ok = entity(productType = "RETAIL", serviceType = "DIGITAL").asDomainModel()
        assertEquals(ProductType.RETAIL, ok.productType)
        assertEquals(ServiceType.DIGITAL, ok.serviceType)

        val bad = entity(productType = "NOT_A_TYPE", serviceType = "ALSO_BAD").asDomainModel()
        assertNull(bad.productType, "invalid enum name -> null (try/catch in mapper)")
        assertNull(bad.serviceType)
    }

    @Test
    fun `asDomainModel decodes attributes_json map`() {
        val domain = entity(attributesJson = """{"Size":"L","Color":"Blue"}""").asDomainModel()
        assertEquals(mapOf("Size" to "L", "Color" to "Blue"), domain.attributes)
    }

    @Test
    fun `asDomainModel returns empty map on malformed attributes_json`() {
        val domain = entity(attributesJson = "not-json").asDomainModel()
        assertEquals(emptyMap(), domain.attributes)
    }

    @Test
    fun `asDatabaseModel marks unsynced and maps booleans to Int`() {
        val product = Product(
            id = "P9", name = "X", active = false, softDeleted = true,
            mrp = 10.0, dp = 8.0, sellingPrice = 9.0, taxCode = "T",
            hasVariants = true,
            attributes = mapOf("Size" to "M"),
        )
        val e = product.asDatabaseModel()
        assertEquals(0, e.active, "active false -> 0")
        assertEquals(1, e.soft_deleted, "softDeleted true -> 1")
        assertEquals(0, e.synced, "always written unsynced")
        assertEquals(1, e.has_variants)
        assertEquals("""{"Size":"M"}""", e.attributes_json)
    }

    @Test
    fun `asDatabaseModel encodes null attributes_json for empty attributes`() {
        val e = Product(id = "P", attributes = emptyMap()).asDatabaseModel()
        assertNull(e.attributes_json, "empty attribute map -> null json")
    }

    @Test
    fun `asProductApiModel pushes soft-deleted row as DELETED status`() {
        val deleted = entity(softDeleted = 1).asProductApiModel()
        assertEquals("DELETED", deleted.status)
        assertTrue(deleted.softDeleted)

        val live = entity(softDeleted = 0).asProductApiModel()
        assertEquals("ACTIVE", live.status)
        assertFalse(live.softDeleted)
    }

    @Test
    fun `asProductApiModel mirrors pricing into nested inventory`() {
        val api = entity(stock = 15.0).asProductApiModel()
        assertEquals("HSN1", api.taxCode)
        assertEquals(90.0, api.sellingPrice)
        val inventory = api.inventory!!
        assertEquals(15.0, inventory.stock)
        assertEquals(90.0, inventory.sellingPrice)
        assertEquals("P1", inventory.productId)
    }

    @Test
    fun `asProductApiModel defaults null stock to zero in inventory`() {
        val api = entity(stock = null).asProductApiModel()
        assertEquals(0.0, api.inventory!!.stock)
    }

    @Test
    fun `list mappers map every element`() {
        val entities = listOf(entity(id = "A"), entity(id = "B"))
        assertEquals(listOf("A", "B"), entities.asProductDomainModel().map { it.id })
        assertEquals(listOf("A", "B"), entities.asProductApiModel().map { it.id })
    }

    // ---- base unit resolution on pull (base_unit_id / unit_id fallback) ----

    private fun apiModel(
        id: String = "P1",
        baseUnitId: String? = null,
        unitId: String? = null,
    ) = com.ampairs.product.api.model.ProductApiModel(
        id = id,
        name = "Widget",
        code = "C1",
        groupId = null,
        brandId = null,
        categoryId = null,
        subCategoryId = null,
        mrp = 100.0,
        dp = 80.0,
        sellingPrice = 90.0,
        taxCode = "HSN1",
        unitId = unitId,
        baseUnitId = baseUnitId,
        baseUnit = null,
        unitConversions = emptyList(),
        images = emptyList(),
    )

    @Test
    fun `asDatabaseModel prefers base_unit_id when present`() {
        val e = listOf(apiModel(baseUnitId = "U_BASE", unitId = "U_UNIT")).asDatabaseModel().single()
        assertEquals("U_BASE", e.base_unit, "base_unit_id wins over unit_id")
    }

    @Test
    fun `asDatabaseModel falls back to unit_id when base_unit_id is blank`() {
        assertEquals("U_UNIT", listOf(apiModel(baseUnitId = null, unitId = "U_UNIT")).asDatabaseModel().single().base_unit)
        assertEquals("U_UNIT", listOf(apiModel(baseUnitId = "", unitId = "U_UNIT")).asDatabaseModel().single().base_unit)
    }
}
