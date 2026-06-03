package com.ampairs.common

import com.ampairs.common.config.ConfigurationManager

/**
 * Utility class for building API URLs with the configured base URL.
 *
 * All endpoints follow the pattern: /api/{module}/v{n}/{resource}
 */
object ApiUrlBuilder {

    fun authUrl(path: String): String {
        return "${ConfigurationManager.apiBaseUrl}/api/$path"
    }

    fun accountUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/account/$cleanPath"
    }

    fun workspaceUrl(path: String): String {
        return "${ConfigurationManager.apiBaseUrl}/api/workspace/$path"
    }

    fun workspaceAvatarUrl(workspaceId: String): String {
        return "${ConfigurationManager.apiBaseUrl}/api/workspace/v1/workspaces/$workspaceId/avatar"
    }

    fun workspaceAvatarThumbnailUrl(workspaceId: String): String {
        return "${ConfigurationManager.apiBaseUrl}/api/workspace/v1/workspaces/$workspaceId/avatar/thumbnail"
    }

    fun userUrl(path: String): String {
        return "${ConfigurationManager.apiBaseUrl}/api/user/$path"
    }

    fun currentUserPictureUrl(): String {
        return "${ConfigurationManager.apiBaseUrl}/api/user/v1/picture"
    }

    fun currentUserPictureThumbnailUrl(): String {
        return "${ConfigurationManager.apiBaseUrl}/api/user/v1/picture/thumbnail"
    }

    fun userPictureUrl(userId: String): String {
        return "${ConfigurationManager.apiBaseUrl}/api/user/v1/$userId/picture"
    }

    fun userPictureThumbnailUrl(userId: String): String {
        return "${ConfigurationManager.apiBaseUrl}/api/user/v1/$userId/picture/thumbnail"
    }

    fun customerUrl(path: String): String {
        return "${ConfigurationManager.apiBaseUrl}/api/customer/$path"
    }

    fun productUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/product/$cleanPath"
    }

    fun unitUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/unit/$cleanPath"
    }

    fun businessUrl(path: String = ""): String {
        val cleanPath = path.removePrefix("/")
        val base = "${ConfigurationManager.apiBaseUrl}/api/business/v1/businesses"
        return if (cleanPath.isBlank()) base else "$base/$cleanPath"
    }

    fun businessLogoUrl(): String {
        return "${ConfigurationManager.apiBaseUrl}/api/business/v1/businesses/logo"
    }

    fun businessLogoThumbnailUrl(): String {
        return "${ConfigurationManager.apiBaseUrl}/api/business/v1/businesses/logo/thumbnail"
    }

    fun businessImageUrl(imageUid: String): String {
        return "${ConfigurationManager.apiBaseUrl}/api/business/v1/businesses/images/$imageUid/file"
    }

    fun businessImageThumbnailUrl(imageUid: String): String {
        return "${ConfigurationManager.apiBaseUrl}/api/business/v1/businesses/images/$imageUid/thumbnail"
    }

    fun orderUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/order/$cleanPath"
    }

    fun invoiceUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/invoice/$cleanPath"
    }

    fun inventoryUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/inventory/$cleanPath"
    }

    fun formUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/form/$cleanPath"
    }

    fun appUpdateUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/core/v1/app-updates/$cleanPath"
    }

    fun subscriptionUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/subscription/$cleanPath"
    }

    fun taxUrl(path: String, queryParams: Map<String, String> = emptyMap()): String {
        val cleanPath = path.removePrefix("/")
        val baseUrl = "${ConfigurationManager.apiBaseUrl}/api/tax/$cleanPath"

        return if (queryParams.isEmpty()) {
            baseUrl
        } else {
            val queryString = queryParams.entries.joinToString("&") { (key, value) ->
                "$key=$value"
            }
            "$baseUrl?$queryString"
        }
    }

    fun fileUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/file/$cleanPath"
    }

    fun wsUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.current.wsBaseUrl}/$cleanPath"
    }

    fun buildCompleteUrl(path: String): String {
        return if (path.startsWith("http")) {
            path
        } else {
            "${ConfigurationManager.apiBaseUrl}${if (!path.startsWith("/")) "/" else ""}${path}"
        }
    }
}
