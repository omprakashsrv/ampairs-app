package com.ampairs.product.data.repository

import com.ampairs.common.di.AppScope
import com.ampairs.product.util.ProductLogger
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size

/**
 * Result of a file picker selection for product images.
 */
data class ProductImagePickerResult(
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    val imageData: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as ProductImagePickerResult
        if (fileName != other.fileName) return false
        if (fileSize != other.fileSize) return false
        if (contentType != other.contentType) return false
        if (!imageData.contentEquals(other.imageData)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + imageData.contentHashCode()
        return result
    }
}

/**
 * Interface for picking product image files cross-platform.
 */
interface ProductImageFilePicker {
    suspend fun pickImage(): ProductImagePickerResult?
    suspend fun pickMultipleImages(maxCount: Int): List<ProductImagePickerResult>
}

/**
 * FileKit-based implementation of ProductImageFilePicker.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class FileKitProductImagePicker : ProductImageFilePicker {

    companion object {
        private val SUPPORTED_IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024L // 10MB
        private const val TAG = "ProductImageFilePicker"
    }

    override suspend fun pickImage(): ProductImagePickerResult? {
        return try {
            ProductLogger.d(TAG, "Starting single image picker")
            val file = FileKit.openFilePicker(type = FileKitType.Image)
            if (file == null) {
                ProductLogger.d(TAG, "User cancelled file selection")
                return null
            }
            val result = processPickedFile(file)
            ProductLogger.d(TAG, "Successfully picked image: ${result?.fileName}")
            result
        } catch (e: Exception) {
            ProductLogger.e(TAG, "Error picking image", e)
            null
        }
    }

    override suspend fun pickMultipleImages(maxCount: Int): List<ProductImagePickerResult> {
        return try {
            ProductLogger.d(TAG, "Starting multiple image picker (max: $maxCount)")
            val files = FileKit.openFilePicker(
                type = FileKitType.Image,
                mode = FileKitMode.Multiple()
            )
            if (files == null) {
                ProductLogger.d(TAG, "User cancelled file selection")
                return emptyList()
            }
            val results = files.take(maxCount).mapNotNull { file ->
                processPickedFile(file)
            }
            ProductLogger.d(TAG, "Successfully picked ${results.size} images")
            results
        } catch (e: Exception) {
            ProductLogger.e(TAG, "Error picking multiple images", e)
            emptyList()
        }
    }

    private suspend fun processPickedFile(file: PlatformFile): ProductImagePickerResult? {
        return try {
            val fileName = file.name
            val fileSize = file.size()

            val extension = fileName.substringAfterLast(".", "").lowercase()
            if (extension !in SUPPORTED_IMAGE_EXTENSIONS) {
                ProductLogger.w(TAG, "Unsupported file type: $extension for file: $fileName")
                return null
            }

            if (fileSize > MAX_FILE_SIZE) {
                ProductLogger.w(TAG, "File too large: ${fileSize}B for file: $fileName (max: ${MAX_FILE_SIZE}B)")
                return null
            }

            val contentType = when (extension) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                "bmp" -> "image/bmp"
                else -> "image/*"
            }

            val imageData = file.readBytes()

            ProductImagePickerResult(
                fileName = fileName,
                contentType = contentType,
                fileSize = fileSize,
                imageData = imageData,
            )
        } catch (e: Exception) {
            ProductLogger.e(TAG, "Error processing picked file: ${file.name}", e)
            null
        }
    }
}
