package com.ampairs.workspace.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.workspace.api.model.WorkspaceRole
import com.ampairs.workspace.viewmodel.WorkspaceInvitationsViewModel
import com.ampairs.common.validation.ValidationResult
import com.ampairs.common.validation.phone.PhoneNumberValidator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Create workspace invitation screen with comprehensive form
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceInvitationCreateScreen(
    workspaceId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: WorkspaceInvitationsViewModel = koinViewModel { parametersOf(workspaceId) }
    val state by viewModel.state.collectAsState()

    // Form state - Phone only for now
    var recipientMobile by remember { mutableStateOf("") }
    val countryCode = "+91" // India country code - read only
    val phoneValidator = remember { PhoneNumberValidator() }
    var recipientName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("MEMBER") }
    var customMessage by remember { mutableStateOf("") }
    var expiresInDays by remember { mutableStateOf("7") }
    var department by remember { mutableStateOf("") }

    // Form validation
    var mobileError by remember { mutableStateOf("") }
    var roleError by remember { mutableStateOf("") }
    var daysError by remember { mutableStateOf("") }

    // UI state
    var isCreating by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(workspaceId) {
        viewModel.loadAvailableRoles()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                
                Text(
                    text = "Create Invitation",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Form content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recipient Information Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Recipient Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    // Mobile number with country code
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = countryCode,
                            onValueChange = { /* Read only - no changes allowed */ },
                            label = { Text("Country") },
                            readOnly = true,
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(0.3f)
                        )
                        
                        OutlinedTextField(
                            value = recipientMobile,
                            onValueChange = { 
                                recipientMobile = it.filter { char -> char.isDigit() || char == ' ' || char == '-' }
                                mobileError = ""
                                
                                // Real-time validation for better UX
                                if (it.isNotEmpty()) {
                                    val cleanMobile = it.replace("\\D".toRegex(), "")
                                    when (val validationResult = phoneValidator.validate(cleanMobile)) {
                                        is ValidationResult.Invalid -> {
                                            if (cleanMobile.length >= 10) {
                                                mobileError = "Please enter a valid Indian mobile number starting with 6, 7, 8, or 9"
                                            }
                                        }
                                        is ValidationResult.Valid -> {
                                            mobileError = ""
                                        }
                                    }
                                }
                            },
                            label = { Text("Mobile Number *") },
                            placeholder = { Text("9876543210") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = mobileError.isNotEmpty(),
                            supportingText = if (mobileError.isNotEmpty()) {
                                { Text(mobileError, color = MaterialTheme.colorScheme.error) }
                            } else null,
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null)
                            },
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    // Name field (optional)
                    OutlinedTextField(
                        value = recipientName,
                        onValueChange = { recipientName = it },
                        label = { Text("Full Name (Optional)") },
                        placeholder = { Text("John Smith") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Department field (optional)
                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("Department (Optional)") },
                        placeholder = { Text("Engineering, Sales, Marketing...") },
                        leadingIcon = {
                            Icon(Icons.Default.Business, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Role Selection Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Role Assignment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    // Role selection
                    RoleSelectionSection(
                        availableRoles = state.availableRoles,
                        selectedRole = selectedRole,
                        onRoleSelected = { 
                            selectedRole = it
                            roleError = ""
                        },
                        error = roleError
                    )
                }
            }

            // Invitation Settings Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Invitation Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    // Expiration days
                    OutlinedTextField(
                        value = expiresInDays,
                        onValueChange = { value ->
                            if (value.all { it.isDigit() } && value.length <= 2) {
                                expiresInDays = value
                                daysError = ""
                            }
                        },
                        label = { Text("Expires in (days) *") },
                        placeholder = { Text("7") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = daysError.isNotEmpty(),
                        supportingText = if (daysError.isNotEmpty()) {
                            { Text(daysError, color = MaterialTheme.colorScheme.error) }
                        } else {
                            { Text("Valid range: 1-30 days") }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Custom message
                    OutlinedTextField(
                        value = customMessage,
                        onValueChange = { customMessage = it },
                        label = { Text("Custom Message (Optional)") },
                        placeholder = { Text("Welcome to our team! Looking forward to working with you.") },
                        maxLines = 3,
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                }
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f),
                    enabled = !isCreating
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (validateForm(recipientMobile, selectedRole, expiresInDays) { mobile, role, days ->
                            mobileError = mobile
                            roleError = role
                            daysError = days
                        }) {
                            isCreating = true
                            
                            // Extract country code as Int (removing the '+' prefix)
                            val countryCodeInt = countryCode.removePrefix("+").toInt()
                            
                            viewModel.createInvitation(
                                countryCode = countryCodeInt,
                                phone = recipientMobile,
                                recipientName = recipientName.takeIf { it.isNotBlank() },
                                invitedRole = selectedRole,
                                customMessage = customMessage.takeIf { it.isNotBlank() },
                                expiresInDays = expiresInDays.toInt()
                            )
                            isCreating = false
                            showSuccessDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isCreating
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Send Invitation")
                }
            }
        }
    }

    // Success dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Invitation Sent!") },
            text = { 
                val recipientInfo = "$countryCode${recipientMobile.replace("\\D".toRegex(), "")}"
                Text("The invitation has been sent to $recipientInfo. They will receive an SMS with instructions to join the workspace.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun RoleSelectionSection(
    availableRoles: List<WorkspaceRole>,
    selectedRole: String,
    onRoleSelected: (String) -> Unit,
    error: String,
) {
    Column {
        Text(
            text = "Select Role *",
            style = MaterialTheme.typography.labelMedium,
            color = if (error.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (availableRoles.isEmpty()) {
            // Fallback roles if not loaded
            val fallbackRoles = listOf("ADMIN", "MANAGER", "MEMBER", "VIEWER")
            Column(
                modifier = Modifier.selectableGroup()
            ) {
                fallbackRoles.forEach { role ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedRole == role,
                                onClick = { onRoleSelected(role) }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedRole == role,
                            onClick = { onRoleSelected(role) }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column {
                            Text(
                                text = role,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = getRoleDescription(role),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            // Use actual roles from API
            Column(
                modifier = Modifier.selectableGroup()
            ) {
                availableRoles.filter { it.name != "OWNER" }.forEach { role ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedRole == role.name,
                                onClick = { onRoleSelected(role.name) }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedRole == role.name,
                            onClick = { onRoleSelected(role.name) }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column {
                            Text(
                                text = role.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = role.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

private fun getRoleDescription(role: String): String {
    return when (role) {
        "ADMIN" -> "Full workspace management permissions"
        "MANAGER" -> "Can manage team members and projects"
        "MEMBER" -> "Standard workspace access with collaboration features"
        "VIEWER" -> "Read-only access to workspace content"
        else -> "Standard workspace permissions"
    }
}



private fun validateForm(
    mobile: String,
    role: String,
    days: String,
    onErrors: (String, String, String) -> Unit
): Boolean {
    var hasErrors = false
    var mobileError = ""
    var roleError = ""
    var daysError = ""

    // Mobile number validation using PhoneNumberValidator
    if (mobile.isEmpty()) {
        mobileError = "Mobile number is required"
        hasErrors = true
    } else {
        val cleanMobile = mobile.replace("\\D".toRegex(), "")
        val phoneValidator = PhoneNumberValidator()
        when (val validationResult = phoneValidator.validate(cleanMobile)) {
            is ValidationResult.Invalid -> {
                mobileError = "Please enter a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9"
                hasErrors = true
            }
            is ValidationResult.Valid -> {
                // Valid mobile number
            }
        }
    }

    // Role validation
    if (role.isEmpty()) {
        roleError = "Please select a role"
        hasErrors = true
    }

    // Days validation
    if (days.isEmpty()) {
        daysError = "Expiration days is required"
        hasErrors = true
    } else {
        val daysInt = days.toIntOrNull()
        if (daysInt == null || daysInt < 1 || daysInt > 30) {
            daysError = "Please enter a valid number between 1 and 30"
            hasErrors = true
        }
    }

    onErrors(mobileError, roleError, daysError)
    return !hasErrors
}