package com.ampairs.customer.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.customer.generated.resources.Res
import ampairsapp.feature.customer.generated.resources.customer_form_title_new
import ampairsapp.feature.customer.generated.resources.customer_form_title_edit
import ampairsapp.feature.customer.generated.resources.customer_save
import ampairsapp.feature.customer.generated.resources.customer_saving
import ampairsapp.feature.customer.generated.resources.customer_import_contact
import ampairsapp.feature.customer.generated.resources.customer_importing
import ampairsapp.feature.customer.generated.resources.customer_save_action
import ampairsapp.feature.customer.generated.resources.customer_select_type
import ampairsapp.feature.customer.generated.resources.customer_select_group
import ampairsapp.feature.customer.generated.resources.customer_no_types
import ampairsapp.feature.customer.generated.resources.customer_no_groups
import ampairsapp.feature.customer.generated.resources.customer_tab_details
import ampairsapp.feature.customer.generated.resources.customer_tab_images
import ampairsapp.feature.customer.generated.resources.customer_section_basic
import ampairsapp.feature.customer.generated.resources.customer_section_business
import ampairsapp.feature.customer.generated.resources.customer_section_credit
import ampairsapp.feature.customer.generated.resources.customer_section_address
import ampairsapp.feature.customer.generated.resources.customer_section_location
import ampairsapp.feature.customer.generated.resources.customer_section_billing
import ampairsapp.feature.customer.generated.resources.customer_section_shipping
import ampairsapp.feature.customer.generated.resources.customer_section_attributes
import ampairsapp.feature.customer.generated.resources.customer_section_status
import ampairsapp.feature.customer.generated.resources.customer_label_name
import ampairsapp.feature.customer.generated.resources.customer_label_email
import ampairsapp.feature.customer.generated.resources.customer_label_landline
import ampairsapp.feature.customer.generated.resources.customer_label_credit_limit
import ampairsapp.feature.customer.generated.resources.customer_label_credit_days
import ampairsapp.feature.customer.generated.resources.customer_label_address
import ampairsapp.feature.customer.generated.resources.customer_label_street
import ampairsapp.feature.customer.generated.resources.customer_label_street2
import ampairsapp.feature.customer.generated.resources.customer_label_city
import ampairsapp.feature.customer.generated.resources.customer_label_pincode
import ampairsapp.feature.customer.generated.resources.customer_label_state
import ampairsapp.feature.customer.generated.resources.customer_label_country
import ampairsapp.feature.customer.generated.resources.customer_label_gst
import ampairsapp.feature.customer.generated.resources.customer_label_pan
import ampairsapp.feature.customer.generated.resources.customer_label_billing_street
import ampairsapp.feature.customer.generated.resources.customer_label_billing_city
import ampairsapp.feature.customer.generated.resources.customer_label_billing_pin
import ampairsapp.feature.customer.generated.resources.customer_label_billing_state
import ampairsapp.feature.customer.generated.resources.customer_label_billing_country
import ampairsapp.feature.customer.generated.resources.customer_label_shipping_street
import ampairsapp.feature.customer.generated.resources.customer_label_shipping_city
import ampairsapp.feature.customer.generated.resources.customer_label_shipping_pin
import ampairsapp.feature.customer.generated.resources.customer_label_shipping_state
import ampairsapp.feature.customer.generated.resources.customer_label_shipping_country
import ampairsapp.feature.customer.generated.resources.customer_location_current
import ampairsapp.feature.customer.generated.resources.customer_location_change
import ampairsapp.feature.customer.generated.resources.customer_location_set
import ampairsapp.feature.customer.generated.resources.customer_location_hint
import ampairsapp.feature.customer.generated.resources.customer_same_as_main_address
import ampairsapp.feature.customer.generated.resources.customer_location_lat_format
import ampairsapp.feature.customer.generated.resources.customer_location_lon_format
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.ampairs.customer.domain.CustomerGroup
import com.ampairs.common.components.Phone
import com.ampairs.customer.ui.components.StateAutocomplete
import com.ampairs.customer.ui.components.StringAutocomplete
import com.ampairs.customer.ui.components.images.CustomerImageManagementScreen
import com.ampairs.customer.ui.components.location.LocationPickerDialog
import com.ampairs.customer.ui.components.location.LocationData
import com.ampairs.customer.ui.components.location.AddressData
import com.ampairs.customer.domain.State
import com.ampairs.customer.ui.components.images.CustomerImageViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import com.ampairs.customer.ui.components.location.LocationService
import com.ampairs.customer.util.CustomerConstants.LABEL_CUSTOMER_TYPE
import com.ampairs.customer.util.CustomerConstants.LABEL_CUSTOMER_GROUP
import com.ampairs.customer.util.CustomerConstants.LABEL_STATUS
import com.ampairs.customer.util.CustomerConstants.STATUS_ACTIVE
import com.ampairs.customer.util.CustomerConstants.STATUS_INACTIVE
import com.ampairs.customer.util.CustomerConstants.STATUS_SUSPENDED
import com.ampairs.customer.domain.CustomerType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormScreen(
    customerId: String? = null,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerFormViewModel = assistedMetroViewModel<CustomerFormViewModel, CustomerFormViewModel.Factory> { create(customerId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formSchema by viewModel.formSchema.collectAsStateWithLifecycle()

    LaunchedEffect(customerId) {
        if (customerId != null) {
            viewModel.loadCustomer()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (customerId == null) stringResource(Res.string.customer_form_title_new) else stringResource(Res.string.customer_form_title_edit)) },
            actions = {
                TextButton(
                    onClick = {
                        viewModel.saveCustomer { onSaveSuccess() }
                    },
                    enabled = uiState.canSave && !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(Res.string.customer_save))
                    }
                }
            }
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                CustomerForm(
                    customerId = customerId,
                    formState = uiState.formState,
                    onFormChange = viewModel::updateForm,
                    error = uiState.error,
                    onSave = { viewModel.saveCustomer { onSaveSuccess() } },
                    canSave = uiState.canSave && !uiState.isSaving,
                    isSaving = uiState.isSaving,
                    states = uiState.states,
                    onStateSelected = viewModel::onStateSelected,
                    cities = uiState.cities,
                    pincodes = uiState.pincodes,
                    customerTypes = uiState.customerTypes,
                    onCustomerTypeSelected = viewModel::onCustomerTypeSelected,
                    customerGroups = uiState.customerGroups,
                    onCustomerGroupSelected = viewModel::onCustomerGroupSelected,
                    entityConfig = uiState.entityConfig,
                    customAttributesContent = { attrs, onChange ->
                        CustomerCustomAttributes(
                            schema = formSchema,
                            optionRegistry = viewModel.optionRegistry,
                            widgetRegistry = viewModel.widgetRegistry,
                            attributes = attrs,
                            onAttributesChange = onChange,
                        )
                    },
                    standardFieldsContent = {
                        CustomerStandardFields(
                            schema = formSchema,
                            optionRegistry = viewModel.optionRegistry,
                            widgetRegistry = viewModel.widgetRegistry,
                            formState = uiState.formState,
                            onFormChange = viewModel::updateForm,
                            reseedKey = uiState.contactImportCount,
                        )
                    },
                    showCustomerImages = uiState.showCustomerImages,
                    customerImagesReadOnly = uiState.customerImagesReadOnly,
                    isContactPickerAvailable = viewModel.isContactPickerAvailable,
                    isImportingContact = uiState.isImportingContact,
                    contactImportError = uiState.contactImportError,
                    onPickContact = viewModel::pickContactAndImport,
                    locationService = viewModel.locationService,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerForm(
    customerId: String?,
    formState: CustomerFormState,
    onFormChange: (CustomerFormState) -> Unit,
    error: String?,
    onSave: () -> Unit,
    canSave: Boolean,
    isSaving: Boolean,
    states: List<State>,
    onStateSelected: (State) -> Unit,
    cities: List<String>,
    pincodes: List<String>,
    customerTypes: List<CustomerType>,
    onCustomerTypeSelected: (CustomerType) -> Unit,
    customerGroups: List<CustomerGroup>,
    onCustomerGroupSelected: (CustomerGroup) -> Unit,
    entityConfig: com.ampairs.form.domain.EntityConfigSchema?,
    customAttributesContent: (@Composable (Map<String, String>, (Map<String, String>) -> Unit) -> Unit)? = null,
    standardFieldsContent: (@Composable () -> Unit)? = null,
    showCustomerImages: Boolean,
    customerImagesReadOnly: Boolean,
    isContactPickerAvailable: Boolean,
    isImportingContact: Boolean,
    contactImportError: String?,
    onPickContact: () -> Unit,
    locationService: LocationService,
    modifier: Modifier = Modifier
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    if (customerId != null && isExpanded) {
        CustomerFormSideBySideLayout(
            customerId = customerId,
            formState = formState,
            onFormChange = onFormChange,
            error = error,
            onSave = onSave,
            canSave = canSave,
            isSaving = isSaving,
            states = states,
            onStateSelected = onStateSelected,
            cities = cities,
            pincodes = pincodes,
            customerTypes = customerTypes,
            onCustomerTypeSelected = onCustomerTypeSelected,
            customerGroups = customerGroups,
            onCustomerGroupSelected = onCustomerGroupSelected,
            entityConfig = entityConfig,
            customAttributesContent = customAttributesContent,
            standardFieldsContent = standardFieldsContent,
            showCustomerImages = showCustomerImages,
            customerImagesReadOnly = customerImagesReadOnly,
            isContactPickerAvailable = isContactPickerAvailable,
            isImportingContact = isImportingContact,
            contactImportError = contactImportError,
            onPickContact = onPickContact,
            locationService = locationService,
            modifier = modifier
        )
    } else if (customerId != null) {
        CustomerFormTabLayout(
            customerId = customerId,
            formState = formState,
            onFormChange = onFormChange,
            error = error,
            onSave = onSave,
            canSave = canSave,
            isSaving = isSaving,
            states = states,
            onStateSelected = onStateSelected,
            cities = cities,
            pincodes = pincodes,
            customerTypes = customerTypes,
            onCustomerTypeSelected = onCustomerTypeSelected,
            customerGroups = customerGroups,
            onCustomerGroupSelected = onCustomerGroupSelected,
            entityConfig = entityConfig,
            customAttributesContent = customAttributesContent,
            standardFieldsContent = standardFieldsContent,
            showCustomerImages = showCustomerImages,
            customerImagesReadOnly = customerImagesReadOnly,
            isContactPickerAvailable = isContactPickerAvailable,
            isImportingContact = isImportingContact,
            contactImportError = contactImportError,
            onPickContact = onPickContact,
            locationService = locationService,
            modifier = modifier
        )
    } else {
        CustomerFormFields(
            formState = formState,
            onFormChange = onFormChange,
            error = error,
            onSave = onSave,
            canSave = canSave,
            isSaving = isSaving,
            states = states,
            onStateSelected = onStateSelected,
            cities = cities,
            pincodes = pincodes,
            customerTypes = customerTypes,
            onCustomerTypeSelected = onCustomerTypeSelected,
            customerGroups = customerGroups,
            onCustomerGroupSelected = onCustomerGroupSelected,
            entityConfig = entityConfig,
            customAttributesContent = customAttributesContent,
            standardFieldsContent = standardFieldsContent,
            isContactPickerAvailable = isContactPickerAvailable,
            isImportingContact = isImportingContact,
            contactImportError = contactImportError,
            onPickContact = onPickContact,
            locationService = locationService,
            modifier = modifier
        )
    }
}

@Composable
private fun CustomerFormTabLayout(
    customerId: String,
    formState: CustomerFormState,
    onFormChange: (CustomerFormState) -> Unit,
    error: String?,
    onSave: () -> Unit,
    canSave: Boolean,
    isSaving: Boolean,
    states: List<State>,
    onStateSelected: (State) -> Unit,
    cities: List<String>,
    pincodes: List<String>,
    customerTypes: List<CustomerType>,
    onCustomerTypeSelected: (CustomerType) -> Unit,
    customerGroups: List<CustomerGroup>,
    onCustomerGroupSelected: (CustomerGroup) -> Unit,
    entityConfig: com.ampairs.form.domain.EntityConfigSchema?,
    customAttributesContent: (@Composable (Map<String, String>, (Map<String, String>) -> Unit) -> Unit)? = null,
    standardFieldsContent: (@Composable () -> Unit)? = null,
    showCustomerImages: Boolean,
    customerImagesReadOnly: Boolean,
    isContactPickerAvailable: Boolean,
    isImportingContact: Boolean,
    contactImportError: String?,
    onPickContact: () -> Unit,
    locationService: LocationService,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }

    val tabDetails = stringResource(Res.string.customer_tab_details)
    val tabImages = stringResource(Res.string.customer_tab_images)
    val tabs = buildList {
        add(tabDetails)
        if (showCustomerImages && customerId.isNotBlank()) {
            add(tabImages)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> CustomerFormFields(
                formState = formState,
                onFormChange = onFormChange,
                error = error,
                onSave = onSave,
                canSave = canSave,
                isSaving = isSaving,
                states = states,
                onStateSelected = onStateSelected,
                cities = cities,
                pincodes = pincodes,
                customerTypes = customerTypes,
                onCustomerTypeSelected = onCustomerTypeSelected,
                customerGroups = customerGroups,
                onCustomerGroupSelected = onCustomerGroupSelected,
                entityConfig = entityConfig,
                customAttributesContent = customAttributesContent,
                standardFieldsContent = standardFieldsContent,
                isContactPickerAvailable = isContactPickerAvailable,
                isImportingContact = isImportingContact,
                contactImportError = contactImportError,
                onPickContact = onPickContact,
                locationService = locationService,
                modifier = Modifier.fillMaxSize()
            )
            1 -> if (tabs.size > 1) {
                CustomerImageManagementScreen(
                    customerId = customerId,
                    readOnly = customerImagesReadOnly,
                    viewModel = assistedMetroViewModel<CustomerImageViewModel, CustomerImageViewModel.Factory> { create(customerId) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomerFormSideBySideLayout(
    customerId: String,
    formState: CustomerFormState,
    onFormChange: (CustomerFormState) -> Unit,
    error: String?,
    onSave: () -> Unit,
    canSave: Boolean,
    isSaving: Boolean,
    states: List<State>,
    onStateSelected: (State) -> Unit,
    cities: List<String>,
    pincodes: List<String>,
    customerTypes: List<CustomerType>,
    onCustomerTypeSelected: (CustomerType) -> Unit,
    customerGroups: List<CustomerGroup>,
    onCustomerGroupSelected: (CustomerGroup) -> Unit,
    entityConfig: com.ampairs.form.domain.EntityConfigSchema?,
    customAttributesContent: (@Composable (Map<String, String>, (Map<String, String>) -> Unit) -> Unit)? = null,
    standardFieldsContent: (@Composable () -> Unit)? = null,
    showCustomerImages: Boolean,
    customerImagesReadOnly: Boolean,
    isContactPickerAvailable: Boolean,
    isImportingContact: Boolean,
    contactImportError: String?,
    onPickContact: () -> Unit,
    locationService: LocationService,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedCard(
            modifier = Modifier
                .weight(if (!showCustomerImages || customerId.isBlank()) 1f else 0.6f)
                .fillMaxHeight()
        ) {
            CustomerFormFields(
                formState = formState,
                onFormChange = onFormChange,
                error = error,
                onSave = onSave,
                canSave = canSave,
                isSaving = isSaving,
                states = states,
                onStateSelected = onStateSelected,
                cities = cities,
                pincodes = pincodes,
                customerTypes = customerTypes,
                onCustomerTypeSelected = onCustomerTypeSelected,
                customerGroups = customerGroups,
                onCustomerGroupSelected = onCustomerGroupSelected,
                entityConfig = entityConfig,
                customAttributesContent = customAttributesContent,
                standardFieldsContent = standardFieldsContent,
                isContactPickerAvailable = isContactPickerAvailable,
                isImportingContact = isImportingContact,
                contactImportError = contactImportError,
                onPickContact = onPickContact,
                locationService = locationService,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showCustomerImages && customerId.isNotBlank()) {
            OutlinedCard(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
            ) {
                CustomerImageManagementScreen(
                    customerId = customerId,
                    readOnly = customerImagesReadOnly,
                    viewModel = assistedMetroViewModel<CustomerImageViewModel, CustomerImageViewModel.Factory> { create(customerId) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerFormFields(
    formState: CustomerFormState,
    onFormChange: (CustomerFormState) -> Unit,
    error: String?,
    onSave: () -> Unit,
    canSave: Boolean,
    isSaving: Boolean,
    states: List<State>,
    onStateSelected: (State) -> Unit,
    cities: List<String>,
    pincodes: List<String>,
    customerTypes: List<CustomerType>,
    onCustomerTypeSelected: (CustomerType) -> Unit,
    customerGroups: List<CustomerGroup>,
    onCustomerGroupSelected: (CustomerGroup) -> Unit,
    entityConfig: com.ampairs.form.domain.EntityConfigSchema?,
    customAttributesContent: (@Composable (Map<String, String>, (Map<String, String>) -> Unit) -> Unit)? = null,
    standardFieldsContent: (@Composable () -> Unit)? = null,
    isContactPickerAvailable: Boolean,
    isImportingContact: Boolean,
    contactImportError: String?,
    onPickContact: () -> Unit,
    locationService: LocationService,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    val resLabelName = stringResource(Res.string.customer_label_name)
    val resLabelEmail = stringResource(Res.string.customer_label_email)
    val resLabelLandline = stringResource(Res.string.customer_label_landline)
    val resLabelGst = stringResource(Res.string.customer_label_gst)
    val resLabelPan = stringResource(Res.string.customer_label_pan)
    val resLabelCreditLimit = stringResource(Res.string.customer_label_credit_limit)
    val resLabelCreditDays = stringResource(Res.string.customer_label_credit_days)
    val resLabelAddress = stringResource(Res.string.customer_label_address)
    val resLabelStreet = stringResource(Res.string.customer_label_street)
    val resLabelStreet2 = stringResource(Res.string.customer_label_street2)
    val resLabelCity = stringResource(Res.string.customer_label_city)
    val resLabelPincode = stringResource(Res.string.customer_label_pincode)
    val resLabelState = stringResource(Res.string.customer_label_state)
    val resLabelCountry = stringResource(Res.string.customer_label_country)

    fun isFieldVisible(fieldName: String): Boolean {
        return entityConfig?.isFieldVisible(fieldName) ?: true
    }

    fun isFieldMandatory(fieldName: String): Boolean {
        return entityConfig?.isFieldMandatory(fieldName) ?: false
    }

    fun getFieldLabel(fieldName: String, defaultLabel: String): String {
        val config = entityConfig?.getFieldConfig(fieldName)
        val label = config?.displayName ?: defaultLabel
        val isMandatory = isFieldMandatory(fieldName)
        return if (isMandatory) "$label *" else label
    }

    fun getFieldPlaceholder(fieldName: String): String? {
        return entityConfig?.getFieldConfig(fieldName)?.placeholder
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (error != null) {
            OutlinedCard(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Contact Import Error
        if (contactImportError != null) {
            OutlinedCard(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = contactImportError,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Contact import (only on Android/iOS, only for new customers)
        if (isContactPickerAvailable && formState.uid.isBlank()) {
            OutlinedButton(
                onClick = onPickContact,
                enabled = !isImportingContact,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isImportingContact) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.customer_importing))
                } else {
                    Icon(Icons.Default.ContactPhone, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.customer_import_contact))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Standard fields — config-driven via the unified FormSchema renderer (spec 011, US1).
        // Sections, order, visibility, labels, required and validation all come from Form Config.
        if (standardFieldsContent != null) {
            standardFieldsContent()
        } else {
        // Basic Information
        FormSection(title = stringResource(Res.string.customer_section_basic)) {
            // Import from Contact Button (only on Android/iOS, only for new customers)
            if (isContactPickerAvailable && formState.uid.isBlank()) {
                OutlinedButton(
                    onClick = onPickContact,
                    enabled = !isImportingContact,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isImportingContact) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.customer_importing))
                    } else {
                        Icon(Icons.Default.ContactPhone, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.customer_import_contact))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Name field - with config integration
            if (isFieldVisible("name")) {
                OutlinedTextField(
                    value = formState.name,
                    onValueChange = { onFormChange(formState.copy(name = it)) },
                    label = { Text(getFieldLabel("name", resLabelName)) },
                    placeholder = getFieldPlaceholder("name")?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    isError = formState.nameError != null,
                    supportingText = formState.nameError?.let { { Text(it) } },
                    singleLine = true
                )
            }

            // Email field - with config integration
            if (isFieldVisible("email")) {
                OutlinedTextField(
                    value = formState.email,
                    onValueChange = { onFormChange(formState.copy(email = it)) },
                    label = { Text(getFieldLabel("email", resLabelEmail)) },
                    placeholder = getFieldPlaceholder("email")?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    isError = formState.emailError != null,
                    supportingText = formState.emailError?.let { { Text(it) } },
                    singleLine = true
                )
            }

            // Customer Type Dropdown
            var customerTypeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = customerTypeExpanded,
                onExpandedChange = { customerTypeExpanded = !customerTypeExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = formState.customerTypeName.ifBlank { stringResource(Res.string.customer_select_type) },
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(LABEL_CUSTOMER_TYPE) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerTypeExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = customerTypeExpanded,
                    onDismissRequest = { customerTypeExpanded = false }
                ) {
                    customerTypes.forEach { customerType ->
                        DropdownMenuItem(
                            text = { Text(customerType.name) },
                            onClick = {
                                onCustomerTypeSelected(customerType)
                                customerTypeExpanded = false
                            }
                        )
                    }
                    if (customerTypes.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.customer_no_types)) },
                            onClick = { }
                        )
                    }
                }
            }

            // Customer Group Dropdown
            var customerGroupExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = customerGroupExpanded,
                onExpandedChange = { customerGroupExpanded = !customerGroupExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = formState.customerGroupName.ifBlank { stringResource(Res.string.customer_select_group) },
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(LABEL_CUSTOMER_GROUP) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerGroupExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = customerGroupExpanded,
                    onDismissRequest = { customerGroupExpanded = false }
                ) {
                    customerGroups.forEach { customerGroup ->
                        DropdownMenuItem(
                            text = { Text(customerGroup.name) },
                            onClick = {
                                onCustomerGroupSelected(customerGroup)
                                customerGroupExpanded = false
                            }
                        )
                    }
                    if (customerGroups.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.customer_no_groups)) },
                            onClick = { }
                        )
                    }
                }
            }

            // Phone field
            if (isFieldVisible("phone")) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Phone(
                        countryCode = formState.countryCode,
                        phone = formState.phone,
                        onValueChange = { phone ->
                            onFormChange(formState.copy(phone = phone))
                        },
                        onValidChange = { /* Validation handled in ViewModel */ }
                    )

                    formState.phoneError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
            }

            // Landline field (separate row)
            if (isFieldVisible("landline")) {
                OutlinedTextField(
                    value = formState.landline,
                    onValueChange = { onFormChange(formState.copy(landline = it)) },
                    label = { Text(getFieldLabel("landline", resLabelLandline)) },
                    placeholder = getFieldPlaceholder("landline")?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true,
                    isError = formState.landlineError != null,
                    supportingText = formState.landlineError?.let { error ->
                        {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )
            }

        }

        // Business Information
        FormSection(title = stringResource(Res.string.customer_section_business)) {
            if (isFieldVisible("gstNumber")) {
                OutlinedTextField(
                    value = formState.gstNumber,
                    onValueChange = { onFormChange(formState.copy(gstNumber = it)) },
                    label = { Text(getFieldLabel("gstNumber", resLabelGst)) },
                    placeholder = getFieldPlaceholder("gstNumber")?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true
                )
            }

            if (isFieldVisible("panNumber")) {
                OutlinedTextField(
                    value = formState.panNumber,
                    onValueChange = { onFormChange(formState.copy(panNumber = it)) },
                    label = { Text(getFieldLabel("panNumber", resLabelPan)) },
                    placeholder = getFieldPlaceholder("panNumber")?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true
                )
            }
        }

        // Credit Management
        FormSection(title = stringResource(Res.string.customer_section_credit)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = if (formState.creditLimit > 0) formState.creditLimit.toString() else "",
                    onValueChange = { value ->
                        val creditLimit = value.toDoubleOrNull() ?: 0.0
                        onFormChange(formState.copy(creditLimit = creditLimit))
                    },
                    label = { Text(resLabelCreditLimit) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = if (formState.creditDays > 0) formState.creditDays.toString() else "",
                    onValueChange = { value ->
                        val creditDays = value.toIntOrNull() ?: 0
                        onFormChange(formState.copy(creditDays = creditDays))
                    },
                    label = { Text(resLabelCreditDays) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true
                )
            }
        }

        // Address Information
        FormSection(title = stringResource(Res.string.customer_section_address)) {
            OutlinedTextField(
                value = formState.address,
                onValueChange = { onFormChange(formState.copy(address = it)) },
                label = { Text(resLabelAddress) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                )
            )

            OutlinedTextField(
                value = formState.street,
                onValueChange = { onFormChange(formState.copy(street = it)) },
                label = { Text(resLabelStreet) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = formState.street2,
                onValueChange = { onFormChange(formState.copy(street2 = it)) },
                label = { Text(resLabelStreet2) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StringAutocomplete(
                    value = formState.city,
                    onValueChange = { onFormChange(formState.copy(city = it)) },
                    suggestions = cities,
                    label = resLabelCity,
                    modifier = Modifier.weight(1f),
                    imeAction = ImeAction.Next
                )

                StringAutocomplete(
                    value = formState.pincode,
                    onValueChange = { onFormChange(formState.copy(pincode = it)) },
                    suggestions = pincodes,
                    label = resLabelPincode,
                    modifier = Modifier.weight(1f),
                    imeAction = ImeAction.Next
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StateAutocomplete(
                    value = formState.state,
                    onValueChange = { onFormChange(formState.copy(state = it)) },
                    onStateSelected = onStateSelected,
                    states = states,
                    modifier = Modifier.weight(1f),
                    label = resLabelState,
                    imeAction = ImeAction.Next
                )

                OutlinedTextField(
                    value = formState.country,
                    onValueChange = { onFormChange(formState.copy(country = it)) },
                    label = { Text(resLabelCountry) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true
                )
            }
        }

        }

        // Location Section
        FormSection(title = stringResource(Res.string.customer_section_location)) {
            LocationSection(
                latitude = formState.latitude,
                longitude = formState.longitude,
                locationService = locationService,
                onLocationSelected = { latitude, longitude, address ->
                    var updatedForm = formState.copy(
                        latitude = latitude,
                        longitude = longitude
                    )

                    // Auto-populate address if provided and main address is empty
                    address?.let { addr ->
                        if (formState.address.isBlank() && formState.street.isBlank() && formState.city.isBlank()) {
                            updatedForm = updatedForm.copy(
                                address = addr.formattedAddress,
                                street = addr.street ?: "",
                                city = addr.city ?: "",
                                state = addr.state ?: "",
                                pincode = addr.pincode ?: "",
                                country = addr.country ?: "India"
                            )
                        }
                    }

                    onFormChange(updatedForm)
                }
            )
        }

        // Billing Address Section
        FormSection(title = stringResource(Res.string.customer_section_billing)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = formState.useBillingAsMainAddress,
                    onCheckedChange = { onFormChange(formState.copy(useBillingAsMainAddress = it)) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.customer_same_as_main_address),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!formState.useBillingAsMainAddress) {
                OutlinedTextField(
                    value = formState.billingStreet,
                    onValueChange = { onFormChange(formState.copy(billingStreet = it)) },
                    label = { Text(stringResource(Res.string.customer_label_billing_street)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StringAutocomplete(
                        value = formState.billingCity,
                        onValueChange = { onFormChange(formState.copy(billingCity = it)) },
                        suggestions = cities,
                        label = stringResource(Res.string.customer_label_billing_city),
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Next
                    )
                    StringAutocomplete(
                        value = formState.billingPincode,
                        onValueChange = { onFormChange(formState.copy(billingPincode = it)) },
                        suggestions = pincodes,
                        label = stringResource(Res.string.customer_label_billing_pin),
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Next
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StateAutocomplete(
                        value = formState.billingState,
                        onValueChange = { onFormChange(formState.copy(billingState = it)) },
                        onStateSelected = { state ->
                            onFormChange(formState.copy(billingState = state.name))
                        },
                        states = states,
                        modifier = Modifier.weight(1f),
                        label = stringResource(Res.string.customer_label_billing_state),
                        imeAction = ImeAction.Next
                    )
                    OutlinedTextField(
                        value = formState.billingCountry,
                        onValueChange = { onFormChange(formState.copy(billingCountry = it)) },
                        label = { Text(stringResource(Res.string.customer_label_billing_country)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) }
                        ),
                        singleLine = true
                    )
                }
            }
        }

        // Shipping Address Section
        FormSection(title = stringResource(Res.string.customer_section_shipping)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = formState.useShippingAsMainAddress,
                    onCheckedChange = { onFormChange(formState.copy(useShippingAsMainAddress = it)) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.customer_same_as_main_address),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!formState.useShippingAsMainAddress) {
                OutlinedTextField(
                    value = formState.shippingStreet,
                    onValueChange = { onFormChange(formState.copy(shippingStreet = it)) },
                    label = { Text(stringResource(Res.string.customer_label_shipping_street)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StringAutocomplete(
                        value = formState.shippingCity,
                        onValueChange = { onFormChange(formState.copy(shippingCity = it)) },
                        suggestions = cities,
                        label = stringResource(Res.string.customer_label_shipping_city),
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Next
                    )
                    StringAutocomplete(
                        value = formState.shippingPincode,
                        onValueChange = { onFormChange(formState.copy(shippingPincode = it)) },
                        suggestions = pincodes,
                        label = stringResource(Res.string.customer_label_shipping_pin),
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Next
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StateAutocomplete(
                        value = formState.shippingState,
                        onValueChange = { onFormChange(formState.copy(shippingState = it)) },
                        onStateSelected = { state ->
                            onFormChange(formState.copy(shippingState = state.name))
                        },
                        states = states,
                        modifier = Modifier.weight(1f),
                        label = stringResource(Res.string.customer_label_shipping_state),
                        imeAction = ImeAction.Done
                    )
                    OutlinedTextField(
                        value = formState.shippingCountry,
                        onValueChange = { onFormChange(formState.copy(shippingCountry = it)) },
                        label = { Text(stringResource(Res.string.customer_label_shipping_country)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        singleLine = true
                    )
                }
            }
        }

        // Custom Attributes — config-driven via the unified FormSchema renderer (spec 011, US1).
        // Falls back to the legacy editor when no renderer slot is provided.
        if (customAttributesContent != null) {
            customAttributesContent(formState.attributes) { newAttributes ->
                onFormChange(formState.copy(attributes = newAttributes))
            }
        } else {
            val attributeDefinitions = entityConfig?.attributeDefinitions?.filter { it.visible } ?: emptyList()
            if (attributeDefinitions.isNotEmpty()) {
                FormSection(title = stringResource(Res.string.customer_section_attributes)) {
                    AttributesEditor(
                        attributes = formState.attributes,
                        attributeDefinitions = attributeDefinitions,
                        onAttributesChange = { newAttributes ->
                            onFormChange(formState.copy(attributes = newAttributes))
                        }
                    )
                }
            }
        }

        if (standardFieldsContent == null) {
        // Status Information
        FormSection(title = stringResource(Res.string.customer_section_status)) {
            var statusExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = !statusExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = formState.status,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(LABEL_STATUS) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    listOf(STATUS_ACTIVE, STATUS_INACTIVE, STATUS_SUSPENDED).forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status) },
                            onClick = {
                                onFormChange(formState.copy(status = status))
                                statusExpanded = false
                            }
                        )
                    }
                }
            }
        }


        }

        // Save Button Section
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.customer_saving))
                } else {
                    Text(stringResource(Res.string.customer_save_action))
                }
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun AttributesEditor(
    attributes: Map<String, String>,
    attributeDefinitions: List<com.ampairs.form.domain.EntityAttributeDefinition>,
    onAttributesChange: (Map<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Display only pre-configured attributes
        attributeDefinitions
            .filter { it.visible && it.enabled }
            .sortedBy { it.displayOrder }
            .forEach { definition ->
                val currentValue = attributes[definition.attributeKey] ?: ""

                ConfiguredAttributeField(
                    definition = definition,
                    value = currentValue,
                    onValueChange = { newValue ->
                        val updatedAttributes = attributes.toMutableMap()
                        if (newValue.isBlank()) {
                            updatedAttributes.remove(definition.attributeKey)
                        } else {
                            updatedAttributes[definition.attributeKey] = newValue
                        }
                        onAttributesChange(updatedAttributes)
                    },
                    focusManager = focusManager
                )
            }
    }
}

@Composable
private fun ConfiguredAttributeField(
    definition: com.ampairs.form.domain.EntityAttributeDefinition,
    value: String,
    onValueChange: (String) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    modifier: Modifier = Modifier
) {
    val label = if (definition.mandatory) "${definition.displayName} *" else definition.displayName

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = definition.placeholder?.let { { Text(it) } },
        supportingText = definition.helpText?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next,
            keyboardType = when (definition.dataType) {
                "number" -> KeyboardType.Number
                "email" -> KeyboardType.Email
                "phone" -> KeyboardType.Phone
                else -> KeyboardType.Text
            }
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) }
        ),
        singleLine = definition.dataType != "text_multiline"
    )
}

@Composable
private fun LocationSection(
    latitude: Double?,
    longitude: Double?,
    locationService: LocationService,
    onLocationSelected: (latitude: Double, longitude: Double, address: AddressData?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showLocationPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (latitude != null && longitude != null) {
            // Show current location
            OutlinedCard(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.customer_location_current),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(Res.string.customer_location_lat_format, latitude.toString().take(10)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(Res.string.customer_location_lon_format, longitude.toString().take(10)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (latitude != null && longitude != null) {
                OutlinedButton(
                    onClick = { showLocationPicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.customer_location_change))
                }
            } else {
                Button(
                    onClick = { showLocationPicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.customer_location_set))
                }
            }
        }

        if (latitude == null && longitude == null) {
            Text(
                text = stringResource(Res.string.customer_location_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Location picker dialog
    LocationPickerDialog(
        showDialog = showLocationPicker,
        currentLocation = if (latitude != null && longitude != null) {
            LocationData(latitude = latitude, longitude = longitude)
        } else null,
        onLocationSelected = { location, address ->
            onLocationSelected(location.latitude, location.longitude, address)
            showLocationPicker = false
        },
        onDismiss = {
            showLocationPicker = false
        },
        locationService = locationService
    )
}