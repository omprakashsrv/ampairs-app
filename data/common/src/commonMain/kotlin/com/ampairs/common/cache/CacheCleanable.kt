package com.ampairs.common.cache

interface CacheCleanable {
    suspend fun clearCache()
}
