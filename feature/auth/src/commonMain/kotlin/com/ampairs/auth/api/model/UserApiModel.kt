package com.ampairs.auth.api.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserApiModel(
    @SerialName("id") val id: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("user_name") val userName: String,
    @SerialName("country_code") val countryCode: Int,
    @SerialName("phone") val phone: String,
    @SerialName("profile_picture_url") val profilePictureUrl: String? = null,
    @SerialName("profile_picture_thumbnail_url") val profilePictureThumbnailUrl: String? = null,
)
