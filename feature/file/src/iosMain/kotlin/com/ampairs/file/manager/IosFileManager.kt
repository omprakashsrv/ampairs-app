package com.ampairs.file.manager

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithBytes
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
class IosFileManager : FileManager {

    override suspend fun saveToCache(fileId: String, imageData: ByteArray, fileName: String): String =
        withContext(Dispatchers.Default) {
            val cacheDir = getCacheDir()
            val ext = fileName.substringAfterLast('.', "")
            val name = if (ext.isNotEmpty()) "$fileId.$ext" else fileId
            val filePath = "$cacheDir/$name"
            imageData.toNSData().writeToFile(filePath, atomically = true)
            filePath
        }

    override suspend fun readFile(filePath: String): ByteArray = withContext(Dispatchers.Default) {
        if (!NSFileManager.defaultManager.fileExistsAtPath(filePath)) {
            throw IllegalArgumentException("File not found: $filePath")
        }
        NSData.dataWithContentsOfFile(filePath)?.toByteArray()
            ?: throw IllegalArgumentException("Could not read file: $filePath")
    }

    override suspend fun deleteFile(filePath: String): Unit = withContext(Dispatchers.Default) {
        val fm = NSFileManager.defaultManager
        if (fm.fileExistsAtPath(filePath)) fm.removeItemAtPath(filePath, error = null)
    }

    override suspend fun fileExists(filePath: String): Boolean = withContext(Dispatchers.Default) {
        NSFileManager.defaultManager.fileExistsAtPath(filePath)
    }

    private fun getCacheDir(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        val cachesPath = paths.first() as String
        val dir = "$cachesPath/file_images"
        val fm = NSFileManager.defaultManager
        if (!fm.fileExistsAtPath(dir)) {
            fm.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
        }
        return dir
    }

    private fun ByteArray.toNSData(): NSData =
        usePinned { pinned -> NSData.dataWithBytes(bytes = pinned.addressOf(0), length = size.toULong()) }

    private fun NSData.toByteArray(): ByteArray {
        val length = this.length.toInt()
        val result = ByteArray(length)
        if (length > 0) {
            result.usePinned { pinned -> memcpy(pinned.addressOf(0), this.bytes, this.length) }
        }
        return result
    }
}
