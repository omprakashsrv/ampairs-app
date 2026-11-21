package com.ampairs.tallysync

import com.ampairs.pricing.data.db.entity.PriceListItemEntity
import com.ampairs.product.db.entity.CategoryEntity
import com.ampairs.product.db.entity.GroupEntity
import com.ampairs.product.db.entity.ProductEntity
import com.ampairs.product.db.entity.ProductStandardCostEntity
import com.ampairs.tally.model.master.GSTDetail
import com.ampairs.tally.model.master.HsnDetail
import com.ampairs.tally.model.master.StandardCost
import com.ampairs.tally.model.master.StandardPrice
import com.ampairs.tally.model.master.StockCategory
import com.ampairs.tally.model.master.StockGroup
import com.ampairs.tally.model.master.StockItem
import com.ampairs.unit.data.db.entity.UnitConversionEntity
import com.ampairs.unit.data.db.entity.UnitEntity
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong
import kotlin.time.Clock
import com.ampairs.tally.model.master.Unit as TallyUnit

internal object TallyProductMapper {

    const val ENTITY_STOCK_GROUP = "stock_group"
    const val ENTITY_STOCK_CATEGORY = "stock_category"
    const val ENTITY_STOCK_ITEM = "stock_item"
    const val ENTITY_UNIT = "unit"

    /** Deterministic id of the single workspace-wide price list that holds Tally standard prices. */
    const val TALLY_PRICE_LIST_ID = "PRCTALLYSTD"
    const val TALLY_PRICE_LIST_NAME = "Tally Standard Price"

    private const val ID_PREFIX_PRICE_ITEM = "PLITLY"
    private const val ID_PREFIX_STANDARD_COST = "PSCTLY"
    private const val ID_PREFIX_UNIT_CONVERSION = "UCNTLY"

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
        unitIdByName: Map<String, String>,
    ): ProductEntity? {
        val productName = name ?: return null
        // standardPrice = selling/retail price (MRP in India); standardCost = purchase/cost price (→ dp
        // proxy). Both are dated histories — the flat product fields take the newest-dated rate; the
        // full history flows to the price-list / standard-cost feeds (see the helpers below).
        val mrp = standardPrice.latestRate()
        val buyingPrice = standardCost.latestRate()
        val hsnCode = extractHsnCode() ?: ""
        // base_unit stores the workspace unit ID (resolved by name); the detail screen looks the unit
        // up by id, so storing the raw Tally name ("PCS") would leave the base unit unresolved.
        val baseUnitId = baseUnits?.trim()?.takeIf { it.isNotBlank() }?.let { unitIdByName[it] }
        return ProductEntity(
            id = id,
            name = productName,
            code = "",
            group_id = parent?.let { groupIdByName[it] },
            category_id = category?.let { categoryIdByName[it] },
            base_unit = baseUnitId,
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
        // Tally returns the name either as the element's NAME attribute or as a <NAME> child.
        val resolvedName = (name ?: unitName)?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val decimals = decimalPlaces?.toIntOrNull() ?: 0
        return UnitEntity(
            id = id,
            name = resolvedName,
            shortName = resolvedName,
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

    // --- Effective-dated standard price → PriceListItem ------------------------------------------

    /**
     * Maps a stock item's dated standard-price history to effective-dated [PriceListItemEntity] rows
     * under the single Tally price list ([TALLY_PRICE_LIST_ID]). One row per dated rate; the id is
     * derived deterministically from the Tally GUID + date so re-syncing a changed rate upserts in
     * place (no duplicates). `unit_price_minor` is the rate in minor units (paise).
     */
    fun StockItem.toPriceListItemEntities(productId: String): List<PriceListItemEntity> {
        val guid = guid?.takeIf { it.isNotBlank() } ?: return emptyList()
        return standardPrice.orEmpty().mapIndexedNotNull { idx, sp ->
            val rate = sp.rate?.parsePrice() ?: return@mapIndexedNotNull null
            if (rate <= 0.0) return@mapIndexedNotNull null
            val dateDigits = sp.date.tallyDateDigits()
            PriceListItemEntity(
                id = ID_PREFIX_PRICE_ITEM + sanitize(guid) + "_" + dateDigits.ifBlank { "i$idx" },
                priceListId = TALLY_PRICE_LIST_ID,
                productId = productId,
                unitPriceMinor = (rate * 100).roundToLong(),
                currency = "INR",
                effectiveFrom = sp.date.tallyDateToIsoInstant(),
                active = true,
                synced = false,
            )
        }
    }

    // --- Effective-dated standard cost → ProductStandardCost -------------------------------------

    /**
     * Maps a stock item's dated standard-cost history to effective-dated [ProductStandardCostEntity]
     * rows. One row per dated rate, deterministically id'd from the GUID + date so re-sync upserts in
     * place. `cost_price` is a plain Double in major units (the product/purchase modules use Double).
     */
    fun StockItem.toStandardCostEntities(productId: String): List<ProductStandardCostEntity> {
        val guid = guid?.takeIf { it.isNotBlank() } ?: return emptyList()
        return standardCost.orEmpty().mapIndexedNotNull { idx, sc ->
            val rate = sc.rate?.parsePrice() ?: return@mapIndexedNotNull null
            if (rate <= 0.0) return@mapIndexedNotNull null
            val dateDigits = sc.date.tallyDateDigits()
            ProductStandardCostEntity(
                id = ID_PREFIX_STANDARD_COST + sanitize(guid) + "_" + dateDigits.ifBlank { "i$idx" },
                productId = productId,
                costPrice = rate,
                effectiveFrom = sc.date.tallyDateToIsoInstant(),
                active = true,
                synced = false,
            )
        }
    }

    // --- Compound unit → product-scoped UnitConversion ------------------------------------------

    /**
     * Maps a stock item's compound (alternate) unit to a product-scoped [UnitConversionEntity].
     * Tally models "1 [additionalUnit] = (conversion / denominator) [baseUnits]" (e.g. 1 Box = 12 Pcs).
     * The app's [UnitConversionEntity] is `derived_qty = base_qty * multiplier`, so base = the larger
     * additional unit, derived = the stock base unit. Returns null when there is no alternate unit or
     * either unit name can't be resolved to a workspace unit id.
     */
    fun StockItem.toUnitConversionEntity(
        productId: String,
        unitIdByName: Map<String, String>,
    ): UnitConversionEntity? {
        val guid = guid?.takeIf { it.isNotBlank() } ?: return null
        val additional = additionalUnits?.trim()?.takeIf { it.isNotBlank() && it != "Not Applicable" } ?: return null
        val base = baseUnits?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (additional == base) return null
        val additionalUnitId = unitIdByName[additional] ?: return null
        val baseUnitId = unitIdByName[base] ?: return null
        val conv = conversion?.trim()?.toDoubleOrNull() ?: return null
        val denom = denominator?.trim()?.toDoubleOrNull()?.takeIf { it != 0.0 } ?: 1.0
        val multiplier = conv / denom
        if (multiplier <= 0.0) return null
        return UnitConversionEntity(
            id = ID_PREFIX_UNIT_CONVERSION + sanitize(guid),
            productId = productId,
            baseUnitId = additionalUnitId,   // the larger unit (e.g. Box)
            derivedUnitId = baseUnitId,      // the stock base unit (e.g. Pcs)
            multiplier = multiplier,
            active = true,
            synced = false,
        )
    }

    // --- Push direction (Room → Tally), mirrors the field choices above -------------------------

    /** Maps a [UnitEntity] to a Tally [TallyUnit] (the reverse of [toUnitEntity]). */
    fun UnitEntity.toTallyUnit(): TallyUnit = TallyUnit(
        name = shortName.ifBlank { name },
        unitName = shortName.ifBlank { name },
        decimalPlaces = decimalPlaces.toString(),
        guid = refId,
    )

    /** Maps a [GroupEntity] (stock group) to a Tally [StockGroup] (the reverse of [toGroupEntity]). */
    fun GroupEntity.toTallyStockGroup(): StockGroup = StockGroup(
        name = name,
        guid = ref_id,
    )

    /** Maps a [CategoryEntity] to a Tally [StockCategory] (the reverse of [toCategoryEntity]). */
    fun CategoryEntity.toTallyStockCategory(): StockCategory = StockCategory(
        name = name,
        guid = ref_id,
    )

    /**
     * Maps a [ProductEntity] to a Tally [StockItem] for the master push (the reverse of
     * [toProductEntity]). [groupName]/[categoryName]/[unitName] are resolved by the caller from
     * [ProductEntity.group_id]/[category_id]/[base_unit] — those masters may need pushing first (see
     * [TallyStockGroupPushService]/[TallyStockCategoryPushService]/[TallyUnitPushService]). HSN
     * (`tax_code`) mirrors into both [StockItem.hsnDetailList] and [gstDetailList] since
     * [extractHsnCode] reads either; `mrp`/`dp` become single current-dated
     * [StandardPrice]/[StandardCost] entries (the full effective-dated history isn't reconstructed —
     * only the current app values round-trip).
     */
    fun ProductEntity.toTallyStockItem(
        groupName: String?,
        categoryName: String?,
        unitName: String?,
    ): StockItem {
        val hsn = tax_code.trim().takeIf { it.isNotBlank() }
        val today = todayTallyDate()
        val priceUnit = unitName ?: "Nos"
        return StockItem(
            name = name,
            guid = ref_id,
            parent = groupName,
            category = categoryName,
            baseUnits = unitName,
            hsnDetailList = hsn?.let { listOf(HsnDetail(hsnCode = it)) },
            gstDetailList = hsn?.let { listOf(GSTDetail(hsnCode = it)) },
            standardPrice = mrp.takeIf { it > 0.0 }?.let { listOf(StandardPrice(date = today, rate = "$it/$priceUnit")) },
            standardCost = dp.takeIf { it > 0.0 }?.let { listOf(StandardCost(date = today, rate = "$it/$priceUnit")) },
        )
    }

    /** Today's date as a Tally-format "YYYYMMDD" string, for dated standard price/cost entries. */
    private fun todayTallyDate(): String {
        val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val y = date.year.toString().padStart(4, '0')
        val m = date.monthNumber.toString().padStart(2, '0')
        val d = date.dayOfMonth.toString().padStart(2, '0')
        return "$y$m$d"
    }

    // --- Helpers --------------------------------------------------------------------------------

    private fun sanitize(guid: String): String = guid.filter { it.isLetterOrDigit() }

    /** Newest-dated rate from a dated Tally price/cost history, in major units (0 when empty). */
    @JvmName("latestRateStandardPrice")
    private fun List<StandardPrice>?.latestRate(): Double =
        this.orEmpty().maxByOrNull { it.date.tallyDateDigits() }?.rate?.parsePrice() ?: 0.0

    @JvmName("latestRateStandardCost")
    private fun List<StandardCost>?.latestRate(): Double =
        this.orEmpty().maxByOrNull { it.date.tallyDateDigits() }?.rate?.parsePrice() ?: 0.0

    /** Tally date "YYYYMMDD" → comparable digit string (blank when absent/odd-length). */
    private fun String?.tallyDateDigits(): String {
        val s = this?.trim()?.filter(Char::isDigit).orEmpty()
        return if (s.length == 8) s else ""
    }

    /** Tally date "YYYYMMDD" → ISO-8601 instant "yyyy-MM-ddT00:00:00Z" (null when unparseable). */
    private fun String?.tallyDateToIsoInstant(): String? {
        val s = tallyDateDigits()
        if (s.isEmpty()) return null
        return "${s.substring(0, 4)}-${s.substring(4, 6)}-${s.substring(6, 8)}T00:00:00Z"
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
