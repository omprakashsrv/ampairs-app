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

    fun sequenceUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/sequence/$cleanPath"
    }

    fun settingUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/setting/$cleanPath"
    }

    fun eventUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/event/$cleanPath"
    }

    fun printingUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/printing/$cleanPath"
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

    fun paymentUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/payment/$cleanPath"
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

    /**
     * Authenticated customer-account ecom endpoints. The ecom module uses the
     * `/api/v1/ecom/{path}` convention (v1 right after /api — see the mobile contract),
     * unlike the older `/api/{module}/v{n}` modules.
     * e.g. ecomUrl("account/addresses"), ecomUrl("account/orders")
     */
    fun ecomUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/v1/ecom/$cleanPath"
    }

    /**
     * Public storefront endpoints scoped to a slug: /api/v1/store/{slug}/{path}
     * e.g. storeUrl("green-mart", "catalog-meta"), storeUrl("green-mart", "products")
     */
    fun storeUrl(slug: String, path: String = ""): String {
        val cleanPath = path.removePrefix("/")
        val base = "${ConfigurationManager.apiBaseUrl}/api/v1/store/$slug"
        return if (cleanPath.isBlank()) base else "$base/$cleanPath"
    }

    fun fileUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/api/file/$cleanPath"
    }

    fun wsUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.current.wsBaseUrl}/api/$cleanPath"
    }

    fun buildCompleteUrl(path: String): String {
        return if (path.startsWith("http")) {
            path
        } else {
            "${ConfigurationManager.apiBaseUrl}${if (!path.startsWith("/")) "/" else ""}${path}"
        }
    }
}
