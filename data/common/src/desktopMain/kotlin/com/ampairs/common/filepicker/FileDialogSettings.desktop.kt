package com.ampairs.common.filepicker

import io.github.vinceglb.filekit.dialogs.FileKitDialogParent
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings

actual fun defaultFileDialogSettings(title: String?): FileKitDialogSettings =
    FileKitDialogSettings(
        title = title,
        parent = DesktopWindowRegistry.activeWindow?.let { FileKitDialogParent.awt(it) },
    )
