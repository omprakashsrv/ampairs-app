package com.ampairs.auth.domain

import com.ampairs.auth.api.model.UserApiModel
import com.ampairs.auth.db.entity.UserEntity
import com.ampairs.common.ApiUrlBuilder

data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val userName: String,
    val countryCode: Int,
    val phone: String,
    val profilePictureUrl: String? = null,
    val profilePictureThumbnailUrl: String? = null,
)

/**
 * Converts API model to database entity.
 * IMPORTANT: The backend returns raw storage paths for profile pictures (e.g., "profile-pictures/UID.../profile.jpg").
 * We convert these to API endpoint URLs since the client should NOT access storage directly.
 * The API endpoints serve the images with proper authentication.
 */
fun UserApiModel.asDatabaseModel(): UserEntity {
    // Convert raw storage paths to API endpoint URLs
    // If the profile picture URL exists (even if it's a storage path), use the API endpoint
    val apiProfilePictureUrl = if (!this.profilePictureUrl.isNullOrBlank()) {
        ApiUrlBuilder.currentUserPictureUrl()
    } else {
        null
    }

    val apiProfilePictureThumbnailUrl = if (!this.profilePictureThumbnailUrl.isNullOrBlank()) {
        ApiUrlBuilder.currentUserPictureThumbnailUrl()
    } else {
        null
    }

    return UserEntity(
        seq_id = 0,
        id = this.id,
        first_name = this.firstName,
        last_name = this.lastName,
        user_name = this.userName,
        country_code = this.countryCode.toLong(),
        phone = this.phone,
        profile_picture_url = apiProfilePictureUrl,
        profile_picture_thumbnail_url = apiProfilePictureThumbnailUrl
    )
}