package com.ampairs.sfa.ui.leave

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ampairsapp.feature.sfa.generated.resources.Res
import ampairsapp.feature.sfa.generated.resources.sfa_cancel
import ampairsapp.feature.sfa.generated.resources.sfa_leave_date
import ampairsapp.feature.sfa.generated.resources.sfa_leave_form_title_edit
import ampairsapp.feature.sfa.generated.resources.sfa_leave_form_title_new
import ampairsapp.feature.sfa.generated.resources.sfa_leave_reason
import ampairsapp.feature.sfa.generated.resources.sfa_ok
import ampairsapp.feature.sfa.generated.resources.sfa_pick_date
import ampairsapp.feature.sfa.generated.resources.sfa_save
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveFormScreen(
    leaveId: String?,
    onSaveSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: LeaveFormViewModel = assistedMetroViewModel<LeaveFormViewModel, LeaveFormViewModel.Factory>(key = leaveId) {
        create(leaveId)
    },
    modifier: Modifier = Modifier,
) {
    val state by viewModel.formState.collectAsState()
    val title = if (leaveId == null) stringResource(Res.string.sfa_leave_form_title_new)
    else stringResource(Res.string.sfa_leave_form_title_edit)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LeaveDateField(value = state.leaveDate, onChange = viewModel::updateLeaveDate)
            OutlinedTextField(
                value = state.reason,
                onValueChange = viewModel::updateReason,
                label = { Text(stringResource(Res.string.sfa_leave_reason)) },
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { viewModel.save(onSaveSuccess) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.sfa_save), textAlign = TextAlign.Center)
            }
        }
    }
}

/** Read-only leave-date field backed by a Material 3 date picker (no manual typing). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaveDateField(value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.sfa_leave_date)) },
            trailingIcon = { Icon(Icons.Filled.Event, contentDescription = stringResource(Res.string.sfa_pick_date)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // Transparent overlay so a tap anywhere on the field opens the picker.
        Box(modifier = Modifier.matchParentSize().clickable { open = true })
    }
    if (open) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = isoDateToMillis(value))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onChange(millisToIsoDate(it)) }
                    open = false
                }) { Text(stringResource(Res.string.sfa_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(Res.string.sfa_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Parse a stored `yyyy-MM-dd` (or ISO timestamp prefix) to UTC-midnight millis, or null if blank/invalid. */
@OptIn(kotlin.time.ExperimentalTime::class)
private fun isoDateToMillis(value: String): Long? {
    val datePart = value.substringBefore('T').trim()
    if (datePart.isBlank()) return null
    return try {
        kotlinx.datetime.LocalDate.parse(datePart)
            .atStartOfDayIn(kotlinx.datetime.TimeZone.UTC)
            .toEpochMilliseconds()
    } catch (_: Exception) {
        null
    }
}

/** Selected millis (UTC) → `yyyy-MM-dd`. */
@OptIn(kotlin.time.ExperimentalTime::class)
private fun millisToIsoDate(millis: Long): String =
    kotlin.time.Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
        .date
        .toString()
