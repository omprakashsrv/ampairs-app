package com.ampairs.form.render

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The uid of the entity the form is currently editing (e.g. the customer being edited), provided by
 * the host screen. Entity-scoped custom widgets (image gallery, file attachments) read this to bind
 * to the right record — the [CustomFieldWidget] value contract only carries a single field value, not
 * the owning entity's identity. Blank for a not-yet-saved (create) record.
 */
val LocalFormEntityUid = staticCompositionLocalOf { "" }
