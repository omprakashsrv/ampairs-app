package com.ampairs.customer.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Android-specific implementation of PlatformFileManager.
 * Handles file operations using Android's internal storage.
 */
class AndroidFileManager(
    private val context: Context
) : PlatformFileManager {

    override suspend fun saveImageToCache(imageId: String, imageData: ByteArray, fileName: String): String =
        withContext(Dispatchers.IO) {
            val cacheDir = File(context.cacheDir, "customer_images")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val fileExtension = fileName.substringAfterLast('.', "")
            val cacheFileName = if (fileExtension.isNotEmpty()) {
                "${imageId}.$fileExtension"
            } else {
                imageId
            }

            val file = File(cacheDir, cacheFileName)
            FileOutputStream(file).use { outputStream ->
                outputStream.write(imageData)
            }

            file.absolutePath
        }

    override suspend fun deleteFile(filePath: String): Unit = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        }
    }

    override suspend fun fileExists(filePath: String): Boolean = withContext(Dispatchers.IO) {
        File(filePath).exists()
    }

    override suspend fun getFileSize(filePath: String): Long = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) file.length() else 0L
    }

    override suspend fun getCacheDirectory(): String = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "customer_images")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        cacheDir.absolutePath
    }

    override suspend fun readFile(filePath: String): ByteArray = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File not found: $filePath")
        }
        file.readBytes()
    }
}