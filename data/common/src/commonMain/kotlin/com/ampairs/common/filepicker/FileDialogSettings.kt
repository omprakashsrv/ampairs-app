package com.ampairs.common.filepicker

import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings

/**
 * Desktop's native file dialog has no owner window unless one is explicitly supplied — without it,
 * Windows can render the dialog behind the app's own (maximized) window instead of on top of it.
 * Android/iOS have no such concept, so this just forwards to the platform default there.
 */
expect fun defaultFileDialogSettings(title: String? = null): FileKitDialogSettings
