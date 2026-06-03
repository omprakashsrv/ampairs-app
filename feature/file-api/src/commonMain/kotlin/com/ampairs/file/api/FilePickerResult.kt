package com.ampairs.file.api

data class FilePickerResult(
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    val imageData: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as FilePickerResult
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
