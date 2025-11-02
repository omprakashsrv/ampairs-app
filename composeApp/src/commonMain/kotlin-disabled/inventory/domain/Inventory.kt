package com.ampairs.inventory.domain

import com.ampairs.domain.Unit
import com.ampairs.inventory.db.entity.InventoryEntity
import com.ampairs.product.api.model.InventoryApiModel
import com.ampairs.product.domain.Product
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class Inventory(
    val seqId: Long = 0,
    var id: String = "",
    var description: String = "",
    var active: Boolean = true,
    var softDeleted: Boolean = false,
    var mrp: Double = 0.0,
    var stock: Double = 0.0,
    var dp: Double = 0.0,
    var sellingPrice: Double = 0.0,
    var buyingPrice: Double = 0.0,
    var unitId: String? = null,
    var unit: Unit? = null,
    var productId: String? = null,
    var product: Product? = null,
    var customerFields: List<CustomField>? = null
)

fun InventoryEntity.asDomainModel(): Inventory {
    return Inventory(
        id = this.id,
        description = this.description,
        unitId = this.unit_id,
        mrp = this.mrp,
        sellingPrice = this.selling_price,
        dp = this.dp,
        stock = this.stock,
        active = this.active == 1,
        softDeleted = this.soft_deleted == 1,
        customerFields = this.custom_fields?.let { Json.decodeFromString(it) }
    )
}

fun List<InventoryEntity>.asDomainModel(): List<Inventory> {
    return map {
        it.asDomainModel()
    }
}


@OptIn(ExperimentalTime::class)
fun Inventory.asDatabaseModel(): InventoryEntity {
    return InventoryEntity(
        seq_id = this.seqId,
        product_id = this.productId,
        id = this.id,
        description = this.description,
        unit_id = this.unitId,
        mrp = this.mrp,
        selling_price = this.sellingPrice,
        buying_price = this.buyingPrice,
        dp = this.dp,
        stock = this.stock,
        active = if (this.active) 1 else 0,
        soft_deleted = if (this.softDeleted) 1 else 0,
        custom_fields = this.customerFields?.let { Json.encodeToString(it) },
        created_at = "",
        updated_at = "",
        last_updated = Clock.System.now().toEpochMilliseconds(),
        synced = 0,
    )
}


fun List<InventoryEntity>.asInventoryApiModel(): List<InventoryApiModel> {
    return map {
        it.asInventoryApiModel()
    }
}

fun InventoryEntity.asInventoryApiModel(): InventoryApiModel {
    return InventoryApiModel(
        id = this.id,
        productId = this.product_id,
        refId = "",
        description = this.description,
        active = this.active == 1,
        stock = this.stock,
        buyingPrice = this.buying_price,
        mrp = this.mrp,
        dp = this.dp,
        baseUnitId = this.unit_id,
        sellingPrice = this.selling_price,
        softDeleted = this.soft_deleted == 1,
        lastUpdated = 0,
    )
}


fun List<InventoryApiModel>.asDatabaseModel(): List<InventoryEntity> {
    return map {
        InventoryEntity(
            seq_id = 0,
            id = it.id ?: "",
            product_id = it.productId,
            stock = it.stock ?: 0.0,
            custom_fields = "",
            description = it.description ?: "",
            active = if (it.active == true) 1 else 0,
            last_updated = it.lastUpdated,
            created_at = it.createdAt,
            updated_at = it.updatedAt,
            mrp = it.mrp ?: 0.0,
            dp = it.dp ?: 0.0,
            unit_id = it.baseUnitId,
            selling_price = it.sellingPrice ?: 0.0,
            soft_deleted = if (it.softDeleted == true) 1 else 0,
            buying_price = it.buyingPrice ?: 0.0,
            synced = 1
        )
    }
}