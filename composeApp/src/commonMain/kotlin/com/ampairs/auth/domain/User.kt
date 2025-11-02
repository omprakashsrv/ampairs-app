package com.ampairs.auth.domain

import com.ampairs.auth.api.model.UserApiModel
import com.ampairs.auth.db.entity.UserEntity

data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val userName: String,
    val countryCode: Int,
    val phone: String,
)

fun UserApiModel.asDatabaseModel(): UserEntity {
    return UserEntity(
        seq_id = 0, 
        id = this.id,
        first_name = this.firstName,
        last_name = this.lastName,
        user_name = this.userName,
        country_code = this.countryCode.toLong(),
        phone = this.phone
    )
}