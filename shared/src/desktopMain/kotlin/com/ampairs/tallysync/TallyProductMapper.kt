package com.ampairs.tallysync

import com.ampairs.product.db.entity.CategoryEntity
import com.ampairs.product.db.entity.GroupEntity
import com.ampairs.product.db.entity.ProductEntity
import com.ampairs.tally.model.master.StockCategory
import com.ampairs.tally.model.master.StockGroup
import com.ampairs.tally.model.master.StockItem
import com.ampairs.unit.data.db.entity.UnitEntity
import com.ampairs.tally.model.master.Unit as TallyUnit

internal object TallyProductMapper {

    const val ENTITY_STOCK_GROUP = "stock_group"
    const val ENTITY_STOCK_CATEGORY = "stock_category"
    const val ENTITY_STOCK_ITEM = "stock_item"
    const val ENTITY_UNIT = "unit"

    fun StockGroup.toGroupEntity(id: String): GroupEntity? {
        return GroupEntity(
            id = id,
            name = name ?: return null,
            ref_id = guid?.takeIf { it.isNotBlank() },
            active = 1,
            soft_deleted = 0,
            synced = 0
        )
    }

    fun StockCategory.toCategoryEntity(id: String): CategoryEntity? {
        return CategoryEntity(
            id = id,
            name = name ?: return null,
            ref_id = guid?.takeIf { it.isNotBlank() },
            active = 1,
            soft_deleted = 0,
            synced = 0
        )
    }

    fun StockItem.toProductEntity(
        id: String,
        groupIdByName: Map<String, String>,
        categoryIdByName: Map<String, String>,
    ): ProductEntity? {
        val productName = name ?: return null
        // standardPrice = selling/retail price (MRP in India); standardCost = purchase/cost price (→ dp proxy)
        val mrp = standardPrice?.rate?.parsePrice() ?: 0.0
        val buyingPrice = standardCost?.rate?.parsePrice() ?: 0.0
        val hsnCode = extractHsnCode() ?: ""
        return ProductEntity(
            id = id,
            name = productName,
            code = "",
            group_id = parent?.let { groupIdByName[it] },
            category_id = category?.let { categoryIdByName[it] },
            base_unit = baseUnits,
            tax_code = hsnCode,
            mrp = mrp,
            dp = if (buyingPrice > 0.0) buyingPrice else mrp,
            selling_price = mrp,
            ref_id = guid?.takeIf { it.isNotBlank() },
            active = 1,
            soft_deleted = 0,
            synced = 0
        )
    }

    fun TallyUnit.toUnitEntity(id: String): UnitEntity? {
        val unitName = name ?: return null
        val decimals = decimalPlaces?.toIntOrNull() ?: 0
        return UnitEntity(
            id = id,
            name = unitName,
            shortName = unitName,
            decimalPlaces = decimals,
            active = true,
            synced = false,
            refId = guid?.takeIf { it.isNotBlank() }
        )
    }

    /**
     * Extracts a usable HSN/SAC code from a Tally stock item. Tally exposes HSN in several places
     * depending on version and how the item was configured: inside GST details (GSTDETAILS.LIST),
     * inside HSN details (HSNDETAILS.LIST), or only as the HSN master name. Tries them in order of
     * reliability, falling back to a numeric-looking master name (4–8 digits).
     */
    fun StockItem.extractHsnCode(): String? {
        val directCode = (gstDetailList.orEmpty().mapNotNull { it.hsnCode } +
            hsnDetailList.orEmpty().mapNotNull { it.hsnCode })
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
        if (directCode != null) return directCode

        return (gstDetailList.orEmpty().mapNotNull { it.hsnMasterName } +
            hsnDetailList.orEmpty().mapNotNull { it.hsnMasterName })
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() && it.all(Char::isDigit) && it.length in 4..8 }
    }

    // Tally price strings: "15750.00/No", " 100.00 /Nos" — take everything before the "/"
    private fun String.parsePrice(): Double {
        return trim().substringBefore("/").trim().toDoubleOrNull() ?: 0.0
    }

    // Tally balance strings: "90 No", "-6 BOX", "3.000 KGS" — first token is the quantity
    fun String.parseClosingQty(): Double? {
        return trim().split(Regex("\\s+")).firstOrNull()?.toDoubleOrNull()
    }
}
