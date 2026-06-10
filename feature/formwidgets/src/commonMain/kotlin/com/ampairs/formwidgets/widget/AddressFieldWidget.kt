package com.ampairs.formwidgets.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import ampairsapp.feature.formwidgets.generated.resources.widget_address_street
import ampairsapp.feature.formwidgets.generated.resources.widget_address_city
import ampairsapp.feature.formwidgets.generated.resources.widget_address_state
import ampairsapp.feature.formwidgets.generated.resources.widget_address_pincode
import ampairsapp.feature.formwidgets.generated.resources.widget_address_country

/** Structured address persisted as the field's JSON string value. */
@Serializable
data class AddressValue(
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val country: String = "",
)

/**
 * Config-driven structured-address value widget (spec 011, T063). Bound to a `data_type = CUSTOM`
 * field whose `widgetKey = "address"`. Collects street/city/state/pincode/country into one JSON
 * string so a single custom field captures a full address.
 */
@Inject
@ContributesIntoSet(WorkspaceScope::class)
class AddressFieldWidget : CustomFieldWidget {

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

        fun emit(updated: AddressValue) = onValueChange(json.encodeToString(AddressValue.serializer(), updated))

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(field.displayName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(current.street, { emit(current.copy(street = it)) }, enabled = enabled, singleLine = true,
                label = { Text(stringResource(Res.string.widget_address_street)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(current.city, { emit(current.copy(city = it)) }, enabled = enabled, singleLine = true,
                label = { Text(stringResource(Res.string.widget_address_city)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(current.state, { emit(current.copy(state = it)) }, enabled = enabled, singleLine = true,
                label = { Text(stringResource(Res.string.widget_address_state)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(current.pincode, { emit(current.copy(pincode = it)) }, enabled = enabled, singleLine = true,
                label = { Text(stringResource(Res.string.widget_address_pincode)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(current.country, { emit(current.copy(country = it)) }, enabled = enabled, singleLine = true,
                label = { Text(stringResource(Res.string.widget_address_country)) }, modifier = Modifier.fillMaxWidth())
            if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }

    private fun decode(value: String?): AddressValue =
        if (value.isNullOrBlank()) AddressValue()
        else runCatching { json.decodeFromString(AddressValue.serializer(), value) }.getOrDefault(AddressValue())

    companion object {
        const val WIDGET_KEY = "address"
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
