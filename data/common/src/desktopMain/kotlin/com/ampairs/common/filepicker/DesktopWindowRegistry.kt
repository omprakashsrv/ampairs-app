package com.ampairs.common.filepicker

import java.awt.Window

/**
 * Tracks the app's frontmost top-level [Window] so native file dialogs can be given an explicit
 * owner. On Windows, an unowned `IFileOpenDialog` has no Z-order relationship to the app and can be
 * shown behind it (notably behind a maximized main window); parenting it fixes that.
 */
object DesktopWindowRegistry {
    @Volatile
    var activeWindow: Window? = null
}
