package com.ampairs.product.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ampairs.domain.Unit
import com.ampairs.product.api.model.ProductApiModel
import com.ampairs.product.db.entity.ProductEntity
import com.ampairs.product.db.model.ProductImageModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class Product(
    var id: String = "",
    var name: String = "",
    var code: String = "",
    var groupId: String = "",
    var brandId: String = "",
    var categoryId: String = "",
    var subCategoryId: String = "",
    var active: Boolean = true,
    var softDeleted: Boolean = false,
    var taxCode: String = "",
    var index: Int = 0,
    var mrp: Double = 0.0,
    var dp: Double = 0.0,
    var sellingPrice: Double = 0.0,
    var baseUnitId: String? = null,
    var baseUnit: Unit? = null,
    var taxInfos: List<TaxInfo>? = null,
    var images: List<ProductImage>? = null,
    var description: String = "",
    var stockQuantity: Double? = null,
    var lowStockAlert: Double? = null,
    var categoryName: String? = null,
    var brandName: String? = null,
    // var inventory: Inventory? = null
) {
    var quantity: Double by mutableStateOf(0.0)

    val primaryImageUrl: String?
        get() = images?.firstOrNull()?.image?.url

    val isLowStock: Boolean
        get() = stockQuantity != null && lowStockAlert != null && stockQuantity!! <= lowStockAlert!!
}

fun ProductEntity.asDomainModel(): Product {
    return Product(
        id = this.id,
        name = this.name,
        code = this.code,
        groupId = this.group_id ?: "",
        categoryId = this.category_id ?: "",
        brandId = this.brand_id ?: "",
        subCategoryId = this.sub_category_id ?: "",
        active = this.active == 1,
        taxCode = this.tax_code,
        mrp = this.mrp,
        dp = this.dp,
        baseUnit = null,
        sellingPrice = this.selling_price,
    )
}

@OptIn(ExperimentalTime::class)
fun Product.asDatabaseModel(): ProductEntity {
    return ProductEntity(
        seq_id = 0,
        id = this.id,
        name = this.name,
        code = this.code,
        group_id = this.groupId,
        category_id = this.categoryId,
        brand_id = this.brandId,
        sub_category_id = this.subCategoryId,
        active = if (this.active) 1 else 0,
        tax_code = this.taxCode,
        mrp = this.mrp,
        dp = this.dp,
        selling_price = this.sellingPrice,
        base_unit = this.baseUnitId ?: "",
        created_at = "",
        updated_at = "",
        last_updated = Clock.System.now().toEpochMilliseconds(),
        soft_deleted = if (this.softDeleted) 1 else 0,
        synced = 0
    )
}

fun List<ProductEntity>.asProductApiModel(): List<ProductApiModel> {
    return map {
        it.asProductApiModel()
    }
}

fun ProductEntity.asProductApiModel(): ProductApiModel {
    return ProductApiModel(
        id = this.id,
        name = this.name,
        code = this.code,
        groupId = this.group_id ?: "",
        categoryId = this.category_id ?: "",
        brandId = this.brand_id ?: "",
        subCategoryId = this.sub_category_id ?: "",
        active = this.active == 1,
        taxCode = this.tax_code,
        mrp = this.mrp,
        dp = this.dp,
        baseUnit = null,
        baseUnitId = null,
        sellingPrice = this.selling_price,
        createdAt = this.created_at ?: "",
        updatedAt = this.updated_at ?: "",
        images = arrayListOf(),
        lastUpdated = 0,
        softDeleted = this.soft_deleted == 1,
        taxCodes = arrayListOf(),
        unitConversions = arrayListOf(),
    )
}

fun List<ProductEntity>.asProductDomainModel(): List<Product> {
    return map {
        it.asDomainModel()
    }
}

fun List<ProductApiModel>.asDatabaseModel(): List<ProductEntity> {
    return map {
        ProductEntity(
            seq_id = 0,
            id = it.id,
            name = it.name,
            code = it.code,
            group_id = it.groupId,
            brand_id = it.brandId,
            category_id = it.categoryId,
            sub_category_id = it.subCategoryId,
            active = if (it.active) 1 else 0,
            tax_code = it.taxCode,
            last_updated = it.lastUpdated,
            created_at = it.createdAt,
            updated_at = it.updatedAt,
            mrp = it.mrp,
            dp = it.dp,
            base_unit = it.baseUnit?.name,
            selling_price = it.sellingPrice,
            soft_deleted = if (it.softDeleted) 1 else 0,
            synced = 1
        )
    }
}

// fun List<ProductApiModel>.asInventoryDatabaseModel(): List<InventoryEntity> {
//     return map {
//         InventoryEntity(
//             seq_id = 0,
//             id = it.inventory?.id ?: "",
//             product_id = it.inventory?.productId ?: "",
//             description = it.inventory?.description ?: "",
//             last_updated = it.inventory?.lastUpdated,
//             created_at = it.inventory?.createdAt,
//             updated_at = it.inventory?.updatedAt,
//             mrp = it.inventory?.mrp ?: 0.0,
//             dp = it.inventory?.dp ?: 0.0,
//             unit_id = it.inventory?.baseUnitId,
//             selling_price = it.inventory?.sellingPrice ?: 0.0,
//             buying_price = it.inventory?.buyingPrice ?: 0.0,
//             stock = it.inventory?.stock ?: 0.0,
//             active = if (it.inventory?.active == true) 1 else 0,
//             soft_deleted = if (it.inventory?.softDeleted == true) 1 else 0,
//             custom_fields = null,
//             synced = 1,
//         )
//     }
// }

fun List<ProductImageModel>.asProductImageDomainModel(): List<ProductImage> {
    return map {
        ProductImage(
            productId = it.productImage.product_id,
            image = it.image.asDomainModel()
        )
    }
}