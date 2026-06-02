package com.ampairs.file.manager

interface FileManager {
    suspend fun saveToCache(fileId: String, imageData: ByteArray, fileName: String): String
    suspend fun readFile(filePath: String): ByteArray
    suspend fun deleteFile(filePath: String)
    suspend fun fileExists(filePath: String): Boolean
}
