package com.ampairs.common

import com.ampairs.common.config.ConfigurationManager

/**
 * Utility class for building API URLs with the configured base URL
 */
object ApiUrlBuilder {

    /**
     * Build complete API URL for authentication endpoints
     */
    fun authUrl(path: String): String {
        return "${ConfigurationManager.apiBaseUrl}/$path"
    }

    /**
     * Build complete API URL for versioned endpoints (v1)
     */
    fun apiUrl(path: String): String {
        return ConfigurationManager.getApiUrl(path)
    }

    /**
     * Build complete API URL for workspace endpoints
     */
    fun workspaceUrl(path: String): String {
        return "${ConfigurationManager.apiBaseUrl}/workspace/$path"
    }

    /**
     * Build URL for workspace avatar
     */
    fun workspaceAvatarUrl(workspaceId: String): String {
        return "${ConfigurationManager.apiBaseUrl}/workspace/v1/$workspaceId/avatar"
    }

    /**
     * Build URL for workspace avatar thumbnail
     */
    fun workspaceAvatarThumbnailUrl(workspaceId: String): String {
        return "${ConfigurationManager.apiBaseUrl}/workspace/v1/$workspaceId/avatar/thumbnail"
    }

    /**
     * Build complete API URL for user endpoints
     */
    fun userUrl(path: String): String {
        return "${ConfigurationManager.apiBaseUrl}/user/$path"
    }

    /**
     * Build URL for current user's profile picture
     */
    fun currentUserPictureUrl(): String {
        return "${ConfigurationManager.apiBaseUrl}/user/v1/picture"
    }

    /**
     * Build URL for current user's profile picture thumbnail
     */
    fun currentUserPictureThumbnailUrl(): String {
        return "${ConfigurationManager.apiBaseUrl}/user/v1/picture/thumbnail"
    }

    /**
     * Build URL for a specific user's profile picture by user ID
     */
    fun userPictureUrl(userId: String): String {
        return "${ConfigurationManager.apiBaseUrl}/user/v1/$userId/picture"
    }

    /**
     * Build URL for a specific user's profile picture thumbnail by user ID
     */
    fun userPictureThumbnailUrl(userId: String): String {
        return "${ConfigurationManager.apiBaseUrl}/user/v1/$userId/picture/thumbnail"
    }

    /**
     * Build complete API URL for customer endpoints
     */
    fun customerUrl(path: String): String {
        return "${ConfigurationManager.apiBaseUrl}/customer/$path"
    }

    /**
     * Build complete API URL for product endpoints
     */
    fun productUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/product/$cleanPath"
    }

    /**
     * Build complete API URL for business endpoints
     */
    fun businessUrl(path: String = ""): String {
        val base = ConfigurationManager.getApiUrl("business")
        val cleanPath = path.removePrefix("/")
        return if (cleanPath.isBlank()) base else "$base/$cleanPath"
    }

    /**
     * Build URL for business logo (full size)
     * GET /api/v1/business/logo
     */
    fun businessLogoUrl(): String {
        return "${ConfigurationManager.getApiUrl("business")}/logo"
    }

    /**
     * Build URL for business logo thumbnail (256x256)
     * GET /api/v1/business/logo/thumbnail
     */
    fun businessLogoThumbnailUrl(): String {
        return "${ConfigurationManager.getApiUrl("business")}/logo/thumbnail"
    }

    /**
     * Build URL for business gallery image file (full size)
     * GET /api/v1/business/images/{imageUid}/file
     */
    fun businessImageUrl(imageUid: String): String {
        return "${ConfigurationManager.getApiUrl("business")}/images/$imageUid/file"
    }

    /**
     * Build URL for business gallery image thumbnail (400x400)
     * GET /api/v1/business/images/{imageUid}/thumbnail
     */
    fun businessImageThumbnailUrl(imageUid: String): String {
        return "${ConfigurationManager.getApiUrl("business")}/images/$imageUid/thumbnail"
    }

    /**
     * Build complete API URL for order endpoints
     */
    fun orderUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/order/$cleanPath"
    }

    /**
     * Build complete API URL for invoice endpoints
     */
    fun invoiceUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/invoice/$cleanPath"
    }

    /**
     * Build complete API URL for inventory endpoints
     */
    fun inventoryUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/inventory/$cleanPath"
    }

    /**
     * Build complete API URL for form configuration endpoints
     */
    fun formUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/form/$cleanPath"
    }

    /**
     * Build complete API URL for app update endpoints
     */
    fun appUpdateUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/v1/app-updates/$cleanPath"
    }

    /**
     * Build complete API URL for subscription endpoints
     */
    fun subscriptionUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/$cleanPath"
    }

    /**
     * Build WebSocket URL
     */
    fun wsUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.current.wsBaseUrl}/$cleanPath"
    }

    /**
     * Build complete URL from relative or absolute path.
     * If path already starts with http/https, returns as-is.
     * Otherwise, prepends the API base URL directly (without double /api/v1/).
     */
    fun buildCompleteUrl(path: String): String {
        return if (path.startsWith("http")) {
            path
        } else {
            "${ConfigurationManager.apiBaseUrl}${if (!path.startsWith("/")) "/" else ""}${path}"
        }
    }
}
