package com.ampairs.formwidgets.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ampairs.form.domain.FormField
import com.ampairs.form.render.CustomFieldWidget
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.formwidgets.location.LocationData
import com.ampairs.formwidgets.location.LocationPickerDialog
import com.ampairs.formwidgets.location.LocationService
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.formwidgets.generated.resources.Res
import ampairsapp.feature.formwidgets.generated.resources.widget_location_set
import ampairsapp.feature.formwidgets.generated.resources.widget_location_change

/**
 * Config-driven location value widget (spec 011). Bound to a `data_type = CUSTOM` field whose
 * `widgetKey = "location"`. The field's stored value is the human-readable address — the widget
 * reverse-geocodes the picked coordinates and writes the resolved address back, so a location field
 * "fetches the address as well" (not just lat/long).
 *
 * Registered into the renderer's [CustomFieldWidget] multibinding set; resolved by [widgetKey].
 */
@Inject
@ContributesIntoSet(WorkspaceScope::class)
class LocationFieldWidget(
    private val locationService: LocationService,
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
        var showPicker by remember { mutableStateOf(false) }
        val current = value.orEmpty()

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = field.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (current.isNotBlank()) {
                Text(
                    text = current,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            OutlinedButton(
                onClick = { showPicker = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(
                        if (current.isBlank()) Res.string.widget_location_set
                        else Res.string.widget_location_change
                    )
                )
            }

            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        LocationPickerDialog(
            showDialog = showPicker,
            onLocationSelected = { location: LocationData, address ->
                // Prefer the reverse-geocoded address; fall back to coordinates.
                val resolved = address?.formattedAddress?.takeIf { it.isNotBlank() }
                    ?: location.address?.takeIf { it.isNotBlank() }
                    ?: "${location.latitude}, ${location.longitude}"
                onValueChange(resolved)
                showPicker = false
            },
            onDismiss = { showPicker = false },
            locationService = locationService,
        )
    }

    companion object {
        const val WIDGET_KEY = "location"
    }
}
