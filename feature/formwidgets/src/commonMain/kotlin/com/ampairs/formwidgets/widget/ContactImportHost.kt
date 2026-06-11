package com.ampairs.formwidgets.widget

import androidx.compose.runtime.staticCompositionLocalOf
import com.ampairs.formwidgets.contact.ContactData

/**
 * Bridge that lets the contact import widget (an *action* widget that has no single value of its own)
 * push the picked contact's fields into the surrounding host form. The host (e.g. the customer create
 * screen) provides a handler mapping [ContactData] → its own field state; the widget invokes it after
 * a successful pick.
 *
 * Defaults to `null` (no host wired) — the widget then disables itself, so it degrades gracefully when
 * rendered in a form that hasn't opted into contact import.
 */
val LocalContactImportHandler = staticCompositionLocalOf<((ContactData) -> Unit)?> { null }
