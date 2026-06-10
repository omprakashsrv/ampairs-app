package com.ampairs.customer.ui.create

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ampairsapp.feature.customer.generated.resources.Res
import ampairsapp.feature.customer.generated.resources.customer_section_attributes
import ampairsapp.feature.customer.generated.resources.customer_images_save_first
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.customer.ui.components.images.CustomerImageManagementScreen
import com.ampairs.customer.ui.components.images.CustomerImageViewModel
import com.ampairs.form.domain.FormField
import com.ampairs.form.render.CustomFieldWidget
import com.ampairs.form.render.LocalFormEntityUid
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Config-driven image-gallery widget (spec 011, T031). Bound to a `data_type = CUSTOM` customer
 * field whose `widgetKey = "image_gallery"`. Reuses the existing customer image management control,
 * binding it to the customer currently being edited via [LocalFormEntityUid] (the widget value
 * contract doesn't carry the entity uid). Shows a hint until the customer is saved (no uid yet).
 */
@Inject
@ContributesIntoSet(WorkspaceScope::class)
class CustomerImageGalleryWidget : CustomFieldWidget {

    override val widgetKey: String = "image_gallery"

    @Composable
    override fun Render(
        field: FormField,
        value: String?,
        onValueChange: (String?) -> Unit,
        enabled: Boolean,
        error: String?,
    ) {
        val customerId = LocalFormEntityUid.current
        Column(Modifier.fillMaxWidth()) {
            Text(
                field.displayName.ifBlank { stringResource(Res.string.customer_section_attributes) },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (customerId.isBlank()) {
                Text(
                    stringResource(Res.string.customer_images_save_first),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                CustomerImageManagementScreen(
                    customerId = customerId,
                    readOnly = !enabled,
                    viewModel = assistedMetroViewModel<CustomerImageViewModel, CustomerImageViewModel.Factory>(key = customerId) { create(customerId) },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}
