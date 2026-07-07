package com.ampairs.sfa.ui.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ampairsapp.feature.sfa.generated.resources.Res
import ampairsapp.feature.sfa.generated.resources.sfa_attendance_check_in
import ampairsapp.feature.sfa.generated.resources.sfa_attendance_check_out
import ampairsapp.feature.sfa.generated.resources.sfa_attendance_checked_in
import ampairsapp.feature.sfa.generated.resources.sfa_attendance_not_checked_in
import ampairsapp.feature.sfa.generated.resources.sfa_attendance_open
import ampairsapp.feature.sfa.generated.resources.sfa_attendance_recent
import ampairsapp.feature.sfa.generated.resources.sfa_attendance_since
import ampairsapp.feature.sfa.generated.resources.sfa_attendance_title
import com.ampairs.common.locale.AppLocale
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatDateTime
import com.ampairs.sfa.domain.model.Attendance
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onBack: () -> Unit,
    viewModel: AttendanceViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val locale = LocalAppLocale.current
    val open = uiState.openRecord

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.sfa_attendance_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (open != null) {
                        Text(
                            text = stringResource(Res.string.sfa_attendance_checked_in),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(Res.string.sfa_attendance_since, formatDateTime(open.checkInAt, locale)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.sfa_attendance_not_checked_in),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            Button(
                onClick = { if (open != null) viewModel.checkOut() else viewModel.checkIn() },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (open != null) Res.string.sfa_attendance_check_out else Res.string.sfa_attendance_check_in,
                    ),
                )
            }

            uiState.error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

            Text(stringResource(Res.string.sfa_attendance_recent), style = MaterialTheme.typography.labelLarge)
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(uiState.recent, key = { it.uid }) { record ->
                    AttendanceRow(record = record, locale = locale)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AttendanceRow(record: Attendance, locale: AppLocale) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = formatDateTime(record.checkInAt, locale), style = MaterialTheme.typography.bodyMedium)
        val out = record.checkOutAt
        Text(
            text = if (!out.isNullOrBlank()) formatDateTime(out, locale) else stringResource(Res.string.sfa_attendance_open),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
