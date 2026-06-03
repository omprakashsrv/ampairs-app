package com.ampairs.product.domain

import com.ampairs.product.api.model.ProductGroupApiModel
import com.ampairs.product.db.entity.BrandEntity
import com.ampairs.product.db.entity.CategoryEntity
import com.ampairs.product.db.entity.GroupEntity
import com.ampairs.product.db.entity.SubCategoryEntity

data class Group(
    var id: String = "",
    var name: String = "",
    var active: Boolean = true,
    var index: Int = 0,
)

fun GroupEntity.asGroupDomainModel(): Group {
    return Group(name = this.name, id = this.id, active = this.active == 1)
}

fun CategoryEntity.asCategoryGroupDomainModel(): Group {
    return Group(name = this.name, id = this.id, active = this.active == 1)
}

fun List<GroupEntity>.asGroupDomainModel(): List<Group> {
    return map { it.asGroupDomainModel() }
}

fun List<CategoryEntity>.asCategoryGroupDomainModel(): List<Group> {
    return map { it.asCategoryGroupDomainModel() }
}

fun List<Group>.asGroupDatabaseModel(): List<GroupEntity> {
    return map {
        GroupEntity(
            seq_id = 0, id = it.id, name = it.name,
            active = if (it.active) 1 else 0,
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
            soft_deleted = 0,
            synced = 0
        )
    }
}

fun List<ProductGroupApiModel>.asGroupDatabaseEntity(): List<GroupEntity> {
    return map {
        GroupEntity(
            seq_id = 0, id = it.id ?: "", name = it.name, active = if (it.active) 1 else 0,
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
            softDeleted = it.soft_deleted == 1,
        )
    }
}

fun List<ProductGroupApiModel>.asBrandDatabaseEntity(): List<BrandEntity> {
    return map {
        BrandEntity(
            seq_id = 0, id = it.id ?: "", name = it.name, active = if (it.active) 1 else 0,
            soft_deleted = if (it.softDeleted) 1 else 0,
            synced = 1
        )
    }
}

fun List<ProductGroupApiModel>.asCategoryDatabaseEntity(): List<CategoryEntity> {
    return map {
        CategoryEntity(
            seq_id = 0, id = it.id ?: "", name = it.name, active = if (it.active) 1 else 0,
            soft_deleted = if (it.softDeleted) 1 else 0,
            synced = 1
        )
    }
}

fun List<ProductGroupApiModel>.asSubCategoryDatabaseEntity(): List<SubCategoryEntity> {
    return map {
        SubCategoryEntity(
            seq_id = 0, id = it.id ?: "", name = it.name, active = if (it.active) 1 else 0,
            soft_deleted = if (it.softDeleted) 1 else 0,
            synced = 1
        )
    }
}
