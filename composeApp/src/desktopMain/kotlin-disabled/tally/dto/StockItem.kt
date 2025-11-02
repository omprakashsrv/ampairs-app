package com.ampairs.tally.dto

import com.ampairs.product.api.model.ProductApiModel
import com.ampairs.product.api.model.ProductGroupApiModel
import com.ampairs.product.api.model.UnitApiModel
import com.ampairs.tally.model.master.StockCategory
import com.ampairs.tally.model.master.StockGroup
import com.ampairs.tally.model.master.StockItem
import com.ampairs.tally.model.master.Unit

fun Unit.asUnitApiModel(): UnitApiModel {
    return UnitApiModel(
        refId = this.guid,
        id = "",
        name = this.unitName ?: "",
        shortName = this.reservedName ?: "",
        decimalPlaces = this.decimalPlaces?.trim()?.toInt() ?: 0,
        active = true,
        softDeleted = false
    )
}

fun List<Unit>.asUnitApiModel(): List<UnitApiModel> {
    return map {
        it.asUnitApiModel()
    }
}

fun StockGroup.asStockGroupModel(): ProductGroupApiModel {
    return ProductGroupApiModel(
        refId = this.guid,
        id = "",
        active = true,
        softDeleted = false,
        name = this.name ?: ""
    )
}

fun List<StockGroup>.asStockGroupModel(): List<ProductGroupApiModel> {
    return map {
        it.asStockGroupModel()
    }
}

fun List<StockCategory>.asStockCategoryModel(): List<ProductGroupApiModel> {
    return map {
        it.asStockCategoryModel()
    }
}

fun StockCategory.asStockCategoryModel(): ProductGroupApiModel {
    return ProductGroupApiModel(
        refId = this.guid,
        id = "",
        active = true,
        softDeleted = false,
        name = this.name ?: ""
    )
}

fun StockItem.asProductApiModel(): ProductApiModel {
    return ProductApiModel(
        refId = this.guid,
        id = "",
        active = true,
        softDeleted = false,
        name = this.name ?: "",
        code = "",
        taxCode = this.gstDetailList?.get(0)?.hsnCode ?: "",
        mrp = 0.0,
        sellingPrice = this.standardPrice?.rate?.split("/")?.get(0)?.toDouble() ?: 0.0,
        dp = this.standardCost?.rate?.split("/")?.get(0)?.toDouble() ?: 0.0,
        baseUnit = null,
        baseUnitId = null,
        brandId = "",
        categoryId = "",
        subCategoryId = "",
        groupId = "",
        taxCodes = arrayListOf(),
        createdAt = "",
        updatedAt = "",
        lastUpdated = 0,
        unitConversions = arrayListOf(),
        images = arrayListOf(),
    )
}