package com.ampairs.product.domain

import com.ampairs.product.api.model.ImageApiModel
import com.ampairs.product.db.entity.ImageEntity

data class Image(
    var id: String = "",
    var name: String = "",
    var objectKey: String = "",
    var bucket: String = "",
    var url: String? = "",
)

fun List<ImageEntity>.asDomainModel(): List<Image> {
    return map {
        it.asDomainModel()
    }
}

fun ImageEntity.asDomainModel(): Image {
    return Image(id = this.id, name = this.name, bucket = this.bucket, objectKey = this.object_key)
}

fun List<Image>.asDatabaseModel(): List<ImageEntity> {
    return map {
        it.toImageDatabaseModel()
    }
}

fun Image.toImageDatabaseModel(): ImageEntity {
    return ImageEntity(
        seq_id = 0,
        id = this.id,
        name = this.name,
        bucket = this.bucket,
        object_key = this.objectKey
    )
}

fun List<ImageApiModel>.asImageDomainModel(): List<Image> {
    return map {
        Image(id = it.id, name = it.name, bucket = it.bucket, objectKey = it.objectKey)
    }
}

fun ImageApiModel.asImageDomainModel(): Image {
    return Image(id = this.id, name = this.name, bucket = this.bucket, objectKey = this.objectKey)

}