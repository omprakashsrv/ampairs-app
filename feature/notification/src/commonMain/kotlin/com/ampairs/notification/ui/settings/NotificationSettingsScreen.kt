package com.ampairs.notification.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.notification.generated.resources.Res
import ampairsapp.feature.notification.generated.resources.notification_cd_back
import ampairsapp.feature.notification.generated.resources.notification_settings_announcements_subtitle
import ampairsapp.feature.notification.generated.resources.notification_settings_announcements_title
import ampairsapp.feature.notification.generated.resources.notification_settings_invoice_subtitle
import ampairsapp.feature.notification.generated.resources.notification_settings_invoice_title
import ampairsapp.feature.notification.generated.resources.notification_settings_master_subtitle
import ampairsapp.feature.notification.generated.resources.notification_settings_master_title
import ampairsapp.feature.notification.generated.resources.notification_settings_order_subtitle
import ampairsapp.feature.notification.generated.resources.notification_settings_order_title
import ampairsapp.feature.notification.generated.resources.notification_settings_section_types
import ampairsapp.feature.notification.generated.resources.notification_settings_title

@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: NotificationSettingsViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.notification_cd_back),
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(Res.string.notification_settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        HorizontalDivider()

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Master switch
            ToggleRow(
                titleRes = Res.string.notification_settings_master_title,
                subtitleRes = Res.string.notification_settings_master_subtitle,
                checked = uiState.notificationsEnabled,
                enabled = true,
                onCheckedChange = viewModel::setNotificationsEnabled,
            )
            HorizontalDivider()

            // Per-type section header
            Text(
                text = stringResource(Res.string.notification_settings_section_types),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            // Per-type toggles — greyed/disabled when the master switch is off.
            val perTypeEnabled = uiState.notificationsEnabled
            ToggleRow(
                titleRes = Res.string.notification_settings_order_title,
                subtitleRes = Res.string.notification_settings_order_subtitle,
                checked = uiState.orderUpdates,
                enabled = perTypeEnabled,
                onCheckedChange = viewModel::setOrderUpdates,
            )
            ToggleRow(
                titleRes = Res.string.notification_settings_invoice_title,
                subtitleRes = Res.string.notification_settings_invoice_subtitle,
                checked = uiState.invoiceUpdates,
                enabled = perTypeEnabled,
                onCheckedChange = viewModel::setInvoiceUpdates,
            )
            ToggleRow(
                titleRes = Res.string.notification_settings_announcements_title,
                subtitleRes = Res.string.notification_settings_announcements_subtitle,
                checked = uiState.announcements,
                enabled = perTypeEnabled,
                onCheckedChange = viewModel::setAnnouncements,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ToggleRow(
    titleRes: StringResource,
    subtitleRes: StringResource,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor =
        if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val subtitleColor =
        if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(subtitleRes),
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}
