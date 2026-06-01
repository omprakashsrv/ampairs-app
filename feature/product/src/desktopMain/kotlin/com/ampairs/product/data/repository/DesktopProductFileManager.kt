package com.ampairs.product.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DesktopProductFileManager : ProductFileManager {

    override suspend fun saveImageToCache(imageId: String, imageData: ByteArray, fileName: String): String =
        withContext(Dispatchers.IO) {
            val cacheDir = File(getCacheDir())
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val ext = fileName.substringAfterLast('.', "")
            val cacheFileName = if (ext.isNotEmpty()) "$imageId.$ext" else imageId
            val file = File(cacheDir, cacheFileName)
            FileOutputStream(file).use { it.write(imageData) }
            file.absolutePath
        }

    override suspend fun deleteFile(filePath: String): Unit = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) file.delete()
    }

    override suspend fun fileExists(filePath: String): Boolean = withContext(Dispatchers.IO) {
        File(filePath).exists()
    }

    override suspend fun readFile(filePath: String): ByteArray = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) throw IllegalArgumentException("File not found: $filePath")
        file.readBytes()
    }

    private fun getCacheDir(): String {
        return try {
            val base = com.ampairs.common.desktop.DataDirectoryManager.getCacheDir()
            val dir = File(base, "product_images")
            dir.mkdirs()
            dir.absolutePath
        } catch (_: IllegalStateException) {
            val fallback = File(System.getProperty("user.home"), ".ampairs/cache/product_images")
            fallback.mkdirs()
            fallback.absolutePath
        }
    }
}
