package com.ampairs.product.domain

import com.ampairs.product.api.model.ProductGroupApiModel
import com.ampairs.product.db.entity.BrandEntity
import com.ampairs.product.db.entity.CategoryEntity
import com.ampairs.product.db.entity.GroupEntity
import com.ampairs.product.db.entity.ImageEntity
import com.ampairs.product.db.entity.SubCategoryEntity
import com.ampairs.product.db.model.BrandModel
import com.ampairs.product.db.model.CategoryModel
import com.ampairs.product.db.model.SubCategoryModel

data class Group(
    var id: String = "",
    var name: String = "",
    var active: Boolean = true,
    var image: Image? = null,
    var index: Int = 0,
)

fun GroupEntity.asGroupDomainModel(): Group {
    return Group(name = this.name, id = this.id, image = null, active = this.active == 1)
}

fun CategoryEntity.asCategoryGroupDomainModel(): Group {
    return Group(name = this.name, id = this.id, image = null, active = this.active == 1)
}

fun List<GroupEntity>.asGroupDomainModel(): List<Group> {
    return map { it.asGroupDomainModel() }
}

fun List<CategoryEntity>.asCategoryGroupDomainModel(): List<Group> {
    return map { it.asCategoryGroupDomainModel() }
}


fun List<BrandModel>.asBrandDomainModel(): List<Group> {
    return map {
        Group(
            name = it.brand.name,
            id = it.brand.id,
            image = it.image?.asDomainModel(),
            active = it.brand.active == 1
        )
    }
}

fun List<CategoryModel>.asCategoryDomainModel(): List<Group> {
    return map {
        Group(
            name = it.category.name,
            id = it.category.id,
            image = it.image?.asDomainModel(),
            active = it.category.active == 1
        )
    }
}

fun List<SubCategoryModel>.asSubCategoryDomainModel(): List<Group> {
    return map {
        Group(
            name = it.subCategory.name,
            id = it.subCategory.id,
            image = it.image?.asDomainModel(),
            active = it.subCategory.active == 1
        )
    }
}

fun List<Group>.asGroupDatabaseModel(): List<GroupEntity> {
    return map {
        GroupEntity(
            seq_id = 0, id = it.id, name = it.name,
            active = if (it.active) 1 else 0,
            image_id = it.image?.id,
            soft_deleted = 0,
            synced = 0
        )
    }
}

fun List<Group>.asCategoryDatabaseModel(): List<CategoryEntity> {
    return map {
        CategoryEntity(
            seq_id = 0, id = it.id, name = it.name,
            active = if (it.active) 1 else 0,
            image_id = it.image?.id,
            soft_deleted = 0,
            synced = 0
        )
    }
}

fun List<Group>.asSubCategoryDatabaseModel(): List<SubCategoryEntity> {
    return map {
        SubCategoryEntity(
            seq_id = 0, id = it.id, name = it.name,
            active = if (it.active) 1 else 0,
            image_id = it.image?.id,
            soft_deleted = 0,
            synced = 0
        )
    }
}

fun List<Group>.asBrandDatabaseModel(): List<BrandEntity> {
    return map {
        BrandEntity(
            seq_id = 0, id = it.id, name = it.name,
            active = if (it.active) 1 else 0,
            image_id = it.image?.id,
            soft_deleted = 0,
            synced = 0
        )
    }
}

fun List<ProductGroupApiModel>.asGroupDatabaseEntity(): List<GroupEntity> {
    return map {
        GroupEntity(
            seq_id = 0, id = it.id ?: "", name = it.name, active = if (it.active) 1 else 0,
            image_id = it.imageId ?: it.image?.id,
            soft_deleted = if (it.softDeleted) 1 else 0,
            synced = 1
        )
    }
}

fun List<GroupEntity>.asGroupApiModel(): List<ProductGroupApiModel> {
    return map {
        ProductGroupApiModel(
            id = it.id,
            name = it.name,
            active = it.active == 1,
            imageId = it.image_id,
            softDeleted = it.soft_deleted == 1,
        )
    }
}

fun List<CategoryEntity>.asCategoryApiModel(): List<ProductGroupApiModel> {
    return map {
        ProductGroupApiModel(
            id = it.id,
            name = it.name,
            active = it.active == 1,
            imageId = it.image_id,
            softDeleted = it.soft_deleted == 1,
        )
    }
}

fun List<SubCategoryEntity>.asSubCategoryApiModel(): List<ProductGroupApiModel> {
    return map {
        ProductGroupApiModel(
            id = it.id,
            name = it.name,
            active = it.active == 1,
            imageId = it.image_id,
            softDeleted = it.soft_deleted == 1,
        )
    }
}

fun List<BrandEntity>.asBrandApiModel(): List<ProductGroupApiModel> {
    return map {
        ProductGroupApiModel(
            id = it.id,
            name = it.name,
            active = it.active == 1,
            imageId = it.image_id,
            softDeleted = it.soft_deleted == 1,
        )
    }
}

fun List<ProductGroupApiModel>.asBrandDatabaseEntity(): List<BrandEntity> {
    return map {
        BrandEntity(
            seq_id = 0, id = it.id ?: "", name = it.name, active = if (it.active) 1 else 0,
            image_id = it.image?.id,
            soft_deleted = if (it.softDeleted) 1 else 0,
            synced = 1
        )
    }
}

fun List<ProductGroupApiModel>.asCategoryDatabaseEntity(): List<CategoryEntity> {
    return map {
        CategoryEntity(
            seq_id = 0, id = it.id ?: "", name = it.name, active = if (it.active) 1 else 0,
            image_id = it.image?.id,
            soft_deleted = if (it.softDeleted) 1 else 0,
            synced = 1
        )
    }
}


fun List<ProductGroupApiModel>.asSubCategoryDatabaseEntity(): List<SubCategoryEntity> {
    return map {
        SubCategoryEntity(
            seq_id = 0, id = it.id ?: "", name = it.name, active = if (it.active) 1 else 0,
            image_id = it.image?.id, soft_deleted = if (it.softDeleted) 1 else 0,
            synced = 1
        )
    }
}

fun List<ProductGroupApiModel>.asImagesDatabaseEntity(): List<ImageEntity> {
    val images = mutableListOf<ImageEntity>()
    this.forEach {
        it.image?.let { it1 ->
            val imageEntity = ImageEntity(
                seq_id = 0,
                id = it1.id,
                name = it.image.name,
                bucket = it.image.bucket,
                object_key = it.image.objectKey
            )
            images.add(imageEntity)
        }
    }
    return images
}