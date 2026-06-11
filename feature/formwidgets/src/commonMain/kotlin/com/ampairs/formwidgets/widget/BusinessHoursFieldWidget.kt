package com.ampairs.formwidgets.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.form.domain.FormField
import com.ampairs.form.render.CustomFieldWidget
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.formwidgets.generated.resources.Res
import ampairsapp.feature.formwidgets.generated.resources.widget_hours_closed
import ampairsapp.feature.formwidgets.generated.resources.widget_hours_open_label
import ampairsapp.feature.formwidgets.generated.resources.widget_hours_close_label

/** One day's hours. [open]/[close] are free-text "HH:mm"; [closed] hides the time fields. */
@Serializable
data class DayHours(val open: String = "09:00", val close: String = "18:00", val closed: Boolean = false)

/** Business hours for the week, persisted as the field's JSON string value (Mon→Sun). */
@Serializable
data class BusinessHoursValue(
    val mon: DayHours = DayHours(),
    val tue: DayHours = DayHours(),
    val wed: DayHours = DayHours(),
    val thu: DayHours = DayHours(),
    val fri: DayHours = DayHours(),
    val sat: DayHours = DayHours(closed = true),
    val sun: DayHours = DayHours(closed = true),
)

/**
 * Config-driven business-hours value widget (spec 011, T063). Bound to a `data_type = CUSTOM` field
 * whose `widgetKey = "business_hours"`. Captures per-day open/close (or "closed") for the week into
 * a single JSON string value.
 */
@Inject
@ContributesIntoSet(WorkspaceScope::class)
class BusinessHoursFieldWidget : CustomFieldWidget {

    override val widgetKey: String = WIDGET_KEY

    @Composable
    override fun Render(
        field: FormField,
        value: String?,
        onValueChange: (String?) -> Unit,
        enabled: Boolean,
        error: String?,
    ) {
        val current = remember(value) { decode(value) }
        fun emit(updated: BusinessHoursValue) = onValueChange(json.encodeToString(BusinessHoursValue.serializer(), updated))

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(field.displayName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            DayRow("Mon", current.mon, enabled) { emit(current.copy(mon = it)) }
            DayRow("Tue", current.tue, enabled) { emit(current.copy(tue = it)) }
            DayRow("Wed", current.wed, enabled) { emit(current.copy(wed = it)) }
            DayRow("Thu", current.thu, enabled) { emit(current.copy(thu = it)) }
            DayRow("Fri", current.fri, enabled) { emit(current.copy(fri = it)) }
            DayRow("Sat", current.sat, enabled) { emit(current.copy(sat = it)) }
            DayRow("Sun", current.sun, enabled) { emit(current.copy(sun = it)) }
            if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }

    @Composable
    private fun DayRow(label: String, hours: DayHours, enabled: Boolean, onChange: (DayHours) -> Unit) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(44.dp))
            if (hours.closed) {
                Text(stringResource(Res.string.widget_hours_closed), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            } else {
                OutlinedTextField(hours.open, { onChange(hours.copy(open = it)) }, enabled = enabled, singleLine = true,
                    label = { Text(stringResource(Res.string.widget_hours_open_label)) }, modifier = Modifier.weight(1f))
                OutlinedTextField(hours.close, { onChange(hours.copy(close = it)) }, enabled = enabled, singleLine = true,
                    label = { Text(stringResource(Res.string.widget_hours_close_label)) }, modifier = Modifier.weight(1f))
            }
            Switch(checked = !hours.closed, onCheckedChange = { onChange(hours.copy(closed = !it)) }, enabled = enabled)
        }
    }

    private fun decode(value: String?): BusinessHoursValue =
        if (value.isNullOrBlank()) BusinessHoursValue()
        else runCatching { json.decodeFromString(BusinessHoursValue.serializer(), value) }.getOrDefault(BusinessHoursValue())

    companion object {
        const val WIDGET_KEY = "business_hours"
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
