package com.ampairs.form.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.form.generated.resources.Res
import ampairsapp.feature.form.generated.resources.form_agent_chat_title
import ampairsapp.feature.form.generated.resources.form_agent_close_cd
import ampairsapp.feature.form.generated.resources.form_agent_editing
import ampairsapp.feature.form.generated.resources.form_agent_empty
import ampairsapp.feature.form.generated.resources.form_agent_input_hint
import ampairsapp.feature.form.generated.resources.form_agent_open_cd
import ampairsapp.feature.form.generated.resources.form_agent_processing
import ampairsapp.feature.form.generated.resources.form_agent_send_cd
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Chat bubble FAB for AI-assisted form filling. Sits in the top-right of an edit screen (customer,
 * product, etc.). On tap it opens a chat where the user describes the record in plain words and the
 * on-device assistant predicts values for the matching form fields — each prediction is delivered to
 * [onFieldFill] so the host screen can update its form state.
 *
 * @param entityType the entity being edited (e.g., "customer", "product") — also the form-schema key.
 * @param onFieldFill called once per predicted field with (fieldKey, value).
 * @param modifier optional Modifier for positioning.
 */
@Composable
fun FormAgentChatBubble(
    entityType: String,
    onFieldFill: (fieldKey: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FormAgentViewModel = assistedMetroViewModel<FormAgentViewModel, FormAgentViewModel.Factory>(
        key = entityType,
    ) { create(entityType) },
) {
    var showDialog by remember { mutableStateOf(false) }

    // Apply every predicted fill to the host form, regardless of whether the dialog is open.
    LaunchedEffect(viewModel) {
        viewModel.fills.collect { fill -> onFieldFill(fill.fieldKey, fill.value) }
    }

    Box(modifier = modifier.fillMaxWidth().padding(end = 16.dp, top = 8.dp)) {
        FloatingActionButton(
            onClick = { showDialog = !showDialog },
            modifier = Modifier.align(Alignment.TopEnd),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = stringResource(Res.string.form_agent_open_cd),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }

    if (showDialog) {
        FormAgentChatDialog(
            entityType = entityType,
            viewModel = viewModel,
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun FormAgentChatDialog(
    entityType: String,
    viewModel: FormAgentViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    fun send() {
        val text = input.trim()
        if (text.isBlank()) return
        viewModel.submit(text)
        input = ""
    }

    Surface(
        modifier = modifier.fillMaxWidth(0.95f).padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.form_agent_chat_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.form_agent_close_cd))
                }
            }

            Text(
                text = stringResource(Res.string.form_agent_editing, entityType),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 240.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.messages.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.form_agent_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(state.messages) { message -> ChatMessageBubble(message) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(Res.string.form_agent_input_hint)) },
                    singleLine = true,
                    enabled = !state.isProcessing,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { send() }),
                )
                if (state.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    IconButton(onClick = { send() }, enabled = input.isNotBlank()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(Res.string.form_agent_send_cd),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (state.isProcessing) {
                Text(
                    text = stringResource(Res.string.form_agent_processing),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: FormChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(
                topStart = if (message.isUser) 16.dp else 0.dp,
                topEnd = if (message.isUser) 0.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp,
            ),
            color = if (message.isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}
