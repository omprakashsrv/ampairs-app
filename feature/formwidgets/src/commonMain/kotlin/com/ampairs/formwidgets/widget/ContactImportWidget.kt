package com.ampairs.formwidgets.widget

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.form.domain.FormField
import com.ampairs.form.render.CustomFieldWidget
import com.ampairs.formwidgets.contact.ContactPickerService
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.formwidgets.generated.resources.Res
import ampairsapp.feature.formwidgets.generated.resources.widget_contact_import
import ampairsapp.feature.formwidgets.generated.resources.widget_contact_importing

/**
 * Config-driven contact import widget (spec 011). Bound to a `data_type = CUSTOM` field whose
 * `widgetKey = "contact"`. Unlike a value widget, this is an *action* widget: picking a contact fills
 * multiple sibling fields of the surrounding form via [LocalContactImportHandler], which the host
 * provides (the host owns the ContactData → its-own-fields mapping). The widget's own field value is
 * left untouched.
 *
 * Disabled when the platform has no contact picker, or when no host handler is wired.
 */
@Inject
@ContributesIntoSet(WorkspaceScope::class)
class ContactImportWidget(
    private val contactPickerService: ContactPickerService,
) : CustomFieldWidget {

    override val widgetKey: String = WIDGET_KEY

    @Composable
    override fun Render(
        field: FormField,
        value: String?,
        onValueChange: (String?) -> Unit,
        enabled: Boolean,
        error: String?,
    ) {
        val handler = LocalContactImportHandler.current
        val scope = rememberCoroutineScope()
        var importing by remember { mutableStateOf(false) }
        val available = remember { contactPickerService.isAvailable() }

        OutlinedButton(
            onClick = {
                if (importing || handler == null) return@OutlinedButton
                importing = true
                scope.launch {
                    contactPickerService.pickContact()
                        .onSuccess { handler(it) }
                    importing = false
                }
            },
            enabled = enabled && available && handler != null && !importing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (importing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.widget_contact_importing))
            } else {
                Icon(Icons.Default.ContactPhone, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.widget_contact_import))
            }
        }
    }

    companion object {
        const val WIDGET_KEY = "contact"
    }
}
