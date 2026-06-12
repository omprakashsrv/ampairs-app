package com.ampairs.sequence.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ampairs.sequence.domain.model.SequenceScope
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.sequence.generated.resources.Res
import ampairsapp.feature.sequence.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SequenceDefinitionFormScreen(
    definitionUid: String? = null,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SequenceDefinitionFormViewModel =
        assistedMetroViewModel<SequenceDefinitionFormViewModel, SequenceDefinitionFormViewModel.Factory>(
            key = definitionUid ?: "new"
        ) { create(definitionUid) },
) {
    val formState by viewModel.formState.collectAsState()
    val isEditing = definitionUid != null

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    if (isEditing) stringResource(Res.string.sequence_form_title_edit)
                    else stringResource(Res.string.sequence_form_title_new)
                )
            },
            actions = {
                TextButton(
                    onClick = { viewModel.save(onSaveSuccess) },
                    enabled = !formState.isLoading && formState.isValid
                ) {
                    if (formState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(Res.string.sequence_form_save))
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            formState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.sequence_form_section_scheme),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = formState.entityType,
                        onValueChange = viewModel::updateEntityType,
                        label = { Text(stringResource(Res.string.sequence_form_entity_type)) },
                        placeholder = { Text(stringResource(Res.string.sequence_form_entity_type_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isEditing
                    )

                    Column {
                        Text(
                            text = stringResource(Res.string.sequence_form_scope_label),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = formState.scope == SequenceScope.WORKSPACE,
                                onClick = { viewModel.updateScope(SequenceScope.WORKSPACE) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text(stringResource(Res.string.sequence_scope_workspace))
                            }
                            SegmentedButton(
                                selected = formState.scope == SequenceScope.USER,
                                onClick = { viewModel.updateScope(SequenceScope.USER) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text(stringResource(Res.string.sequence_scope_user))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.sequence_form_scope_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (formState.scope == SequenceScope.USER) {
                        MemberDropdown(
                            members = formState.members,
                            selectedUserName = formState.selectedUserName,
                            onSelect = viewModel::updateSelectedUser
                        )
                    }
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.sequence_form_section_format),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = formState.prefix,
                            onValueChange = viewModel::updatePrefix,
                            label = { Text(stringResource(Res.string.sequence_form_prefix)) },
                            placeholder = { Text(stringResource(Res.string.sequence_form_prefix_hint)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = formState.suffix,
                            onValueChange = viewModel::updateSuffix,
                            label = { Text(stringResource(Res.string.sequence_form_suffix)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = formState.paddingLength,
                        onValueChange = viewModel::updatePaddingLength,
                        label = { Text(stringResource(Res.string.sequence_form_padding)) },
                        supportingText = { Text(stringResource(Res.string.sequence_form_padding_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = formState.startValue,
                            onValueChange = viewModel::updateStartValue,
                            label = { Text(stringResource(Res.string.sequence_form_start_value)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = formState.incrementStep,
                            onValueChange = viewModel::updateIncrementStep,
                            label = { Text(stringResource(Res.string.sequence_form_increment)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Text(
                        text = stringResource(Res.string.sequence_form_preview) + ": " + formState.preview,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.sequence_form_active),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(Res.string.sequence_form_active_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = formState.active,
                            onCheckedChange = viewModel::updateActive
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberDropdown(
    members: List<MemberOption>,
    selectedUserName: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedUserName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.sequence_form_user)) },
            placeholder = { Text(stringResource(Res.string.sequence_form_user_placeholder)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            members.forEach { member ->
                DropdownMenuItem(
                    text = { Text(member.name) },
                    onClick = {
                        onSelect(member.userId)
                        expanded = false
                    }
                )
            }
        }
    }
}
