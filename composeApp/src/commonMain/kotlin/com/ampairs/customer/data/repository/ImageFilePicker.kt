package com.ampairs.customer.data.repository

/**
 * Platform-agnostic interface for image file picking.
 * Uses FileKit for cross-platform file selection.
 */
interface ImageFilePicker {

    /**
     * Pick a single image file from the device.
     *
     * @return ImageFilePickerResult containing the selected image data and metadata,
     *         or null if user cancelled or no file was selected
     */
    suspend fun pickImage(): ImageFilePickerResult?

    /**
     * Pick multiple image files from the device.
     *
     * @param maxCount Maximum number of images to select (default 10)
     * @return List of ImageFilePickerResult containing selected images,
     *         or empty list if user cancelled or no files were selected
     */
    suspend fun pickMultipleImages(maxCount: Int = 10): List<ImageFilePickerResult>
}