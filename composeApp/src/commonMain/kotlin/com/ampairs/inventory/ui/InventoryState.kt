package com.ampairs.inventory.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ampairs.inventory.domain.Inventory

class InventoryState(val inventory: Inventory) {
    var description by mutableStateOf(this.inventory.description)
    var mrp by mutableStateOf(this.inventory.mrp)
    var dp by mutableStateOf(this.inventory.dp)
    var sellingPrice by mutableStateOf(this.inventory.sellingPrice)
    var buyingPrice by mutableStateOf(this.inventory.buyingPrice)
    var stock by mutableStateOf(this.inventory.stock)

    var mrpFraction by mutableStateOf(inventory.mrp.toInt().toDouble() != inventory.mrp)
    var dpFraction by mutableStateOf(inventory.dp.toInt().toDouble() != inventory.dp)
    var sellingPriceFraction by mutableStateOf(
        inventory.sellingPrice.toInt().toDouble() != inventory.sellingPrice
    )
    var buyingPriceFraction by mutableStateOf(
        inventory.buyingPrice.toInt().toDouble() != inventory.buyingPrice
    )
    var stockFraction by mutableStateOf(inventory.stock.toInt().toDouble() != inventory.stock)


//    var customerFields by mutableStateListOf<CustomField>(this.inventory.customerFields)
}

fun InventoryState.toDomainModel(): Inventory {
    this.inventory.description = this.description
    this.inventory.stock = this.stock
    this.inventory.mrp = this.mrp
    this.inventory.dp = this.dp
    this.inventory.sellingPrice = this.sellingPrice
    this.inventory.buyingPrice = this.buyingPrice
    return this.inventory
}