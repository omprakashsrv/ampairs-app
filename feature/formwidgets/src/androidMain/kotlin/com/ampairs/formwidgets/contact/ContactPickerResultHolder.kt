package com.ampairs.formwidgets.contact

import android.net.Uri

/**
 * Holds callbacks for contact picker results and permission requests.
 * This is needed because startActivityForResult requires a callback to be set
 * before the activity result is received.
 */
object ContactPickerResultHolder {
    private var contactCallback: ((Uri?) -> Unit)? = null
    private var permissionCallback: ((Boolean) -> Unit)? = null

    fun setCallback(callback: (Uri?) -> Unit) {
        contactCallback = callback
    }

    fun setPermissionCallback(callback: (Boolean) -> Unit) {
        permissionCallback = callback
    }

    fun onContactResult(uri: Uri?) {
        contactCallback?.invoke(uri)
        contactCallback = null
    }

    fun onPermissionResult(granted: Boolean) {
        permissionCallback?.invoke(granted)
        permissionCallback = null
    }

    fun clearCallbacks() {
        contactCallback = null
        permissionCallback = null
    }
}
