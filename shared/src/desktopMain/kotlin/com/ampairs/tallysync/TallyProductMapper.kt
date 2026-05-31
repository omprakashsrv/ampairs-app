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

    // Entity type keys used as DataStore keys for alterId watermarks
    const val ENTITY_STOCK_GROUP = "stock_group"
    const val ENTITY_STOCK_CATEGORY = "stock_category"
    const val ENTITY_STOCK_ITEM = "stock_item"
    const val ENTITY_UNIT = "unit"

    fun StockGroup.toGroupEntity(): GroupEntity? {
        val id = tallyId(guid, name) ?: return null
        return GroupEntity(
            id = id,
            name = name ?: return null,
            active = 1,
            soft_deleted = 0,
            synced = 0
        )
    }

    fun StockCategory.toCategoryEntity(): CategoryEntity? {
        val id = tallyId(guid, name) ?: return null
        return CategoryEntity(
            id = id,
            name = name ?: return null,
            active = 1,
            soft_deleted = 0,
            synced = 0
        )
    }

    fun StockItem.toProductEntity(
        groupIdByName: Map<String, String>,
        categoryIdByName: Map<String, String>,
    ): ProductEntity? {
        val id = tallyId(guid, name) ?: return null
        val productName = name ?: return null
        // standardPrice = selling/retail price (MRP in India); standardCost = purchase/cost price (→ dp proxy)
        val mrp = standardPrice?.rate?.parsePrice() ?: 0.0
        val buyingPrice = standardCost?.rate?.parsePrice() ?: 0.0
        // HSN code is the closest match for tax_code in Ampairs
        val hsnCode = gstDetailList?.firstOrNull()?.hsnCode?.trim()?.takeIf { it.isNotBlank() } ?: ""
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
            active = 1,
            soft_deleted = 0,
            synced = 0
        )
    }

    fun TallyUnit.toUnitEntity(): UnitEntity? {
        val id = tallyId(guid, name) ?: return null
        val unitName = name ?: return null
        val decimals = decimalPlaces?.toIntOrNull() ?: 0
        return UnitEntity(
            id = id,
            name = unitName,
            shortName = unitName,
            decimalPlaces = decimals,
            active = true,
            synced = false
        )
    }

    // Deterministic Ampairs ID from Tally GUID or name
    private fun tallyId(guid: String?, name: String?): String? {
        return when {
            !guid.isNullOrBlank() -> guid
            !name.isNullOrBlank() -> "TALLY_${name.hashCode()}"
            else -> null
        }
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
