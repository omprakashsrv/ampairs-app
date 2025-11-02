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
     * Build complete API URL for user endpoints
     */
    fun userUrl(path: String): String {
        return "${ConfigurationManager.apiBaseUrl}/user/$path"
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
