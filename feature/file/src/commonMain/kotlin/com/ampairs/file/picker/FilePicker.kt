package com.ampairs.file.picker

import com.ampairs.file.api.FilePickerResult
import com.ampairs.file.util.FileLogger
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

interface FilePicker {
    suspend fun pickSingleImage(): FilePickerResult?
    suspend fun pickMultipleImages(maxCount: Int = 10): List<FilePickerResult>
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class FileKitFilePicker : FilePicker {

    companion object {
        private val SUPPORTED_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024L
        private const val TAG = "FilePicker"
    }

    override suspend fun pickSingleImage(): FilePickerResult? {
        return try {
            val file = FileKit.openFilePicker(type = FileKitType.Image) ?: return null
            processFile(file.name, file.size(), file.readBytes())
        } catch (e: Exception) {
            FileLogger.e(TAG, "Error picking single image", e)
            null
        }
    }

    override suspend fun pickMultipleImages(maxCount: Int): List<FilePickerResult> {
        return try {
            val files = FileKit.openFilePicker(
                type = FileKitType.Image,
                mode = FileKitMode.Multiple(),
            ) ?: return emptyList()
            files.take(maxCount).mapNotNull { processFile(it.name, it.size(), it.readBytes()) }
        } catch (e: Exception) {
            FileLogger.e(TAG, "Error picking multiple images", e)
            emptyList()
        }
    }

    private fun processFile(name: String, size: Long, bytes: ByteArray): FilePickerResult? {
        val ext = name.substringAfterLast(".", "").lowercase()
        if (ext !in SUPPORTED_EXTENSIONS) {
            FileLogger.w(TAG, "Unsupported file type: $ext")
            return null
        }
        if (size > MAX_FILE_SIZE) {
            FileLogger.w(TAG, "File too large: ${size}B (max: ${MAX_FILE_SIZE}B)")
            return null
        }
        val contentType = when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            else -> "image/*"
        }
        return FilePickerResult(fileName = name, contentType = contentType, fileSize = size, imageData = bytes)
    }
}
