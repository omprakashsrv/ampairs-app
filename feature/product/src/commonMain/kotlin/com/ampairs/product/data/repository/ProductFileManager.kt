package com.ampairs.product.data.repository

interface ProductFileManager {
    suspend fun saveImageToCache(imageId: String, imageData: ByteArray, fileName: String): String
    suspend fun deleteFile(filePath: String)
    suspend fun fileExists(filePath: String): Boolean
    suspend fun readFile(filePath: String): ByteArray
}
