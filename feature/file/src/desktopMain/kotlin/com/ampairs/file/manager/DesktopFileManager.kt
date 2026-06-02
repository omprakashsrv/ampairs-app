package com.ampairs.file.manager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DesktopFileManager : FileManager {

    override suspend fun saveToCache(fileId: String, imageData: ByteArray, fileName: String): String =
        withContext(Dispatchers.IO) {
            val cacheDir = File(getCacheDir()).also { it.mkdirs() }
            val ext = fileName.substringAfterLast('.', "")
            val name = if (ext.isNotEmpty()) "$fileId.$ext" else fileId
            val file = File(cacheDir, name)
            FileOutputStream(file).use { it.write(imageData) }
            file.absolutePath
        }

    override suspend fun readFile(filePath: String): ByteArray = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) throw IllegalArgumentException("File not found: $filePath")
        file.readBytes()
    }

    override suspend fun deleteFile(filePath: String): Unit = withContext(Dispatchers.IO) {
        File(filePath).let { if (it.exists()) it.delete() }
    }

    override suspend fun fileExists(filePath: String): Boolean = withContext(Dispatchers.IO) {
        File(filePath).exists()
    }

    private fun getCacheDir(): String = try {
        val base = com.ampairs.common.desktop.DataDirectoryManager.getCacheDir()
        File(base, "file_images").also { it.mkdirs() }.absolutePath
    } catch (_: IllegalStateException) {
        File(System.getProperty("user.home"), ".ampairs/cache/file_images").also { it.mkdirs() }.absolutePath
    }
}
