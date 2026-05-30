package com.ampairs.customer.ui.customertype

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import ampairsapp.feature.customer.generated.resources.Res
import ampairsapp.feature.customer.generated.resources.customer_active
import ampairsapp.feature.customer.generated.resources.customer_save
import ampairsapp.feature.customer.generated.resources.customer_saving
import ampairsapp.feature.customer.generated.resources.customer_section_basic
import ampairsapp.feature.customer.generated.resources.customer_section_additional
import ampairsapp.feature.customer.generated.resources.customer_label_description
import ampairsapp.feature.customer.generated.resources.customer_label_metadata
import ampairsapp.feature.customer.generated.resources.customer_type_form_title_new
import ampairsapp.feature.customer.generated.resources.customer_type_form_title_edit
import ampairsapp.feature.customer.generated.resources.customer_type_name_label
import ampairsapp.feature.customer.generated.resources.customer_type_name_hint
import ampairsapp.feature.customer.generated.resources.customer_type_desc_hint
import ampairsapp.feature.customer.generated.resources.customer_type_code_label
import ampairsapp.feature.customer.generated.resources.customer_type_code_hint
import ampairsapp.feature.customer.generated.resources.customer_type_section_display
import ampairsapp.feature.customer.generated.resources.customer_type_display_order
import ampairsapp.feature.customer.generated.resources.customer_type_order_hint
import ampairsapp.feature.customer.generated.resources.customer_type_section_credit
import ampairsapp.feature.customer.generated.resources.customer_type_credit_limit_label
import ampairsapp.feature.customer.generated.resources.customer_type_credit_limit_hint
import ampairsapp.feature.customer.generated.resources.customer_type_credit_days_label
import ampairsapp.feature.customer.generated.resources.customer_type_credit_days_hint
import ampairsapp.feature.customer.generated.resources.customer_type_metadata_hint
import ampairsapp.feature.customer.generated.resources.customer_type_save
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerTypeFormScreen(
    customerTypeId: String? = null,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerTypeFormViewModel = assistedMetroViewModel<CustomerTypeFormViewModel, CustomerTypeFormViewModel.Factory> { create(customerTypeId) }
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    val isEditing = customerTypeId != null

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    if (isEditing) stringResource(Res.string.customer_type_form_title_edit)
                    else stringResource(Res.string.customer_type_form_title_new)
                )
            },
            actions = {
                TextButton(
                    onClick = { viewModel.saveCustomerType(onSaveSuccess) },
                    enabled = !formState.isLoading && formState.name.isNotBlank()
                ) {
                    if (formState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(Res.string.customer_save))
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
                        text = stringResource(Res.string.customer_section_basic),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = formState.name,
                        onValueChange = viewModel::updateName,
                        label = { Text(stringResource(Res.string.customer_type_name_label)) },
                        placeholder = { Text(stringResource(Res.string.customer_type_name_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        isError = formState.error != null && formState.name.isBlank()
                    )

                    OutlinedTextField(
                        value = formState.description,
                        onValueChange = viewModel::updateDescription,
                        label = { Text(stringResource(Res.string.customer_label_description)) },
                        placeholder = { Text(stringResource(Res.string.customer_type_desc_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )

                    OutlinedTextField(
                        value = formState.typeCode,
                        onValueChange = viewModel::updateTypeCode,
                        label = { Text(stringResource(Res.string.customer_type_code_label)) },
                        placeholder = { Text(stringResource(Res.string.customer_type_code_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.customer_type_section_display),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = formState.displayOrder,
                        onValueChange = viewModel::updateDisplayOrder,
                        label = { Text(stringResource(Res.string.customer_type_display_order)) },
                        placeholder = { Text(stringResource(Res.string.customer_type_order_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.customer_type_section_credit),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = formState.defaultCreditLimit,
                        onValueChange = viewModel::updateDefaultCreditLimit,
                        label = { Text(stringResource(Res.string.customer_type_credit_limit_label)) },
                        placeholder = { Text(stringResource(Res.string.customer_type_credit_limit_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )

                    OutlinedTextField(
                        value = formState.defaultCreditDays,
                        onValueChange = viewModel::updateDefaultCreditDays,
                        label = { Text(stringResource(Res.string.customer_type_credit_days_label)) },
                        placeholder = { Text(stringResource(Res.string.customer_type_credit_days_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.customer_section_additional),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = formState.metadata,
                        onValueChange = viewModel::updateMetadata,
                        label = { Text(stringResource(Res.string.customer_label_metadata)) },
                        placeholder = { Text(stringResource(Res.string.customer_type_metadata_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(Res.string.customer_active),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(checked = formState.active, onCheckedChange = viewModel::updateActive)
                    }
                }
            }

            Button(
                onClick = { viewModel.saveCustomerType(onSaveSuccess) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = !formState.isLoading && formState.name.isNotBlank()
            ) {
                if (formState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.customer_saving))
                } else {
                    Text(stringResource(Res.string.customer_type_save))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
