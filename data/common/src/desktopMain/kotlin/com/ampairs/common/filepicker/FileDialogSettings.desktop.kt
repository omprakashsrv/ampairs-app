package com.ampairs.common.filepicker

import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings

actual fun defaultFileDialogSettings(title: String?): FileKitDialogSettings =
    FileKitDialogSettings(title = title, parentWindow = DesktopWindowRegistry.activeWindow)
