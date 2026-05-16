# Subscription Limit Enforcement Integration Guide

This guide shows how to integrate subscription limit checks into your feature modules (Customer, Product, etc.).

## Overview

When users try to create new resources (customers, products, invoices), the app should:
1. Check current usage against subscription limits
2. If limit exceeded → Show upgrade dialog (read-only mode)
3. If approaching limit (80%+) → Show warning dialog
4. If OK → Allow operation

## Integration Steps

### 1. Inject SubscriptionEnforcement in ViewModel

```kotlin
class CustomerViewModel(
    private val repository: CustomerRepository,
    private val subscriptionEnforcement: SubscriptionEnforcement, // Add this
    private val workspaceIdProvider: () -> String
) : ViewModel() {
    // ... existing code
}
```

### 2. Add Enforcement Check Before Create

```kotlin
fun createCustomer(customer: Customer) {
    viewModelScope.launch {
        val workspaceId = workspaceIdProvider()

        // Get current count
        val currentCount = repository.getCustomerCount(workspaceId)

        // Check if creation is allowed
        val decision = subscriptionEnforcement.canCreateResource(
            workspaceId = workspaceId,
            resourceType = LimitChecker.ResourceType.CUSTOMER,
            currentCount = currentCount
        )

        when (decision) {
            is EnforcementDecision.Blocked -> {
                // Show upgrade dialog
                _events.emit(CustomerEvent.LimitExceeded(decision))
            }
            is EnforcementDecision.Warning -> {
                // Show warning dialog (optional - can proceed)
                _events.emit(CustomerEvent.LimitWarning(decision))
            }
            is EnforcementDecision.Allowed -> {
                // Proceed with creation
                repository.createCustomer(customer).fold(
                    onSuccess = { _events.emit(CustomerEvent.CustomerCreated(it)) },
                    onFailure = { _events.emit(CustomerEvent.Error(it.message)) }
                )
            }
        }
    }
}
```

### 3. Add Events to Handle Enforcement

```kotlin
sealed class CustomerEvent {
    data class CustomerCreated(val customer: Customer) : CustomerEvent()
    data class LimitExceeded(val decision: EnforcementDecision.Blocked) : CustomerEvent()
    data class LimitWarning(val decision: EnforcementDecision.Warning) : CustomerEvent()
    data class Error(val message: String?) : CustomerEvent()
}
```

### 4. Show Dialogs in UI

```kotlin
@Composable
fun CustomerFormScreen(
    viewModel: CustomerViewModel,
    onNavigateToPlans: () -> Unit
) {
    var showLimitDialog by remember { mutableStateOf<EnforcementDecision.Blocked?>(null) }
    var showWarningDialog by remember { mutableStateOf<EnforcementDecision.Warning?>(null) }
    var pendingCustomer by remember { mutableStateOf<Customer?>(null) }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is CustomerEvent.LimitExceeded -> {
                    showLimitDialog = event.decision
                }
                is CustomerEvent.LimitWarning -> {
                    showWarningDialog = event.decision
                    pendingCustomer = getCurrentCustomerFromForm()
                }
                is CustomerEvent.CustomerCreated -> {
                    // Success - navigate back
                }
            }
        }
    }

    // Limit exceeded dialog
    showLimitDialog?.let { decision ->
        LimitExceededDialog(
            decision = decision,
            onDismiss = { showLimitDialog = null },
            onUpgrade = {
                showLimitDialog = null
                onNavigateToPlans()
            }
        )
    }

    // Warning dialog
    showWarningDialog?.let { decision ->
        LimitWarningDialog(
            decision = decision,
            onDismiss = {
                showWarningDialog = null
                pendingCustomer = null
            },
            onContinue = {
                // User wants to proceed anyway
                pendingCustomer?.let { viewModel.forceCreateCustomer(it) }
                showWarningDialog = null
                pendingCustomer = null
            },
            onViewPlans = {
                showWarningDialog = null
                pendingCustomer = null
                onNavigateToPlans()
            }
        )
    }

    // ... rest of form UI
}
```

## Resource Type Mapping

Use these constants when checking limits:

```kotlin
LimitChecker.ResourceType.CUSTOMER  // For customer creation
LimitChecker.ResourceType.PRODUCT   // For product creation
LimitChecker.ResourceType.INVOICE   // For invoice creation
LimitChecker.ResourceType.MEMBER    // For workspace member invitation
LimitChecker.ResourceType.DEVICE    // For device registration
```

## Read-Only Mode Check

To check workspace mode before showing forms:

```kotlin
val workspaceMode = subscriptionEnforcement.getWorkspaceMode(workspaceId)

when (workspaceMode) {
    is WorkspaceMode.Normal -> {
        // Allow form
    }
    is WorkspaceMode.ReadOnly -> {
        // Show read-only banner
        Text(
            text = "Read-only: ${workspaceMode.exceededResources.joinToString(", ")} limits exceeded",
            color = MaterialTheme.colorScheme.error
        )
    }
}
```

## Example: Complete Customer Form Integration

See the full example below:

```kotlin
@Composable
fun CustomerFormScreen(
    customerId: String?,
    viewModel: CustomerViewModel = koinViewModel { parametersOf(customerId) },
    onNavigateBack: () -> Unit,
    onNavigateToPlans: () -> Unit
) {
    var showLimitDialog by remember { mutableStateOf<EnforcementDecision.Blocked?>(null) }
    var showWarningDialog by remember { mutableStateOf<EnforcementDecision.Warning?>(null) }

    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is CustomerEvent.CustomerCreated -> {
                    onNavigateBack()
                }
                is CustomerEvent.LimitExceeded -> {
                    showLimitDialog = event.decision
                }
                is CustomerEvent.LimitWarning -> {
                    showWarningDialog = event.decision
                }
                is CustomerEvent.Error -> {
                    // Show error snackbar
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (customerId == null) "New Customer" else "Edit Customer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Form fields
            OutlinedTextField(
                value = formState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Customer Name") },
                modifier = Modifier.fillMaxWidth()
            )

            // ... more fields

            Spacer(Modifier.weight(1f))

            // Save button
            Button(
                onClick = { viewModel.saveCustomer() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && formState.isValid
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Save")
                }
            }
        }
    }

    // Limit exceeded dialog
    showLimitDialog?.let { decision ->
        LimitExceededDialog(
            decision = decision,
            onDismiss = { showLimitDialog = null },
            onUpgrade = {
                showLimitDialog = null
                onNavigateToPlans()
            }
        )
    }

    // Warning dialog
    showWarningDialog?.let { decision ->
        LimitWarningDialog(
            decision = decision,
            onDismiss = { showWarningDialog = null },
            onContinue = {
                viewModel.saveCustomerForce()
                showWarningDialog = null
            },
            onViewPlans = {
                showWarningDialog = null
                onNavigateToPlans()
            }
        )
    }
}
```

## Koin Integration

Update your module's Koin configuration:

```kotlin
val customerModule = module {
    // ... existing dependencies

    viewModel { parameters ->
        CustomerViewModel(
            repository = get(),
            subscriptionEnforcement = get(), // Add this
            workspaceIdProvider = { WorkspaceContextManager.getInstance().currentWorkspace.value?.id ?: "" }
        )
    }
}
```

## Testing

When testing, you can mock the enforcement:

```kotlin
@Test
fun `should show limit dialog when customer limit exceeded`() = runTest {
    val mockEnforcement = mockk<SubscriptionEnforcement>()

    coEvery {
        mockEnforcement.canCreateResource(any(), any(), any())
    } returns EnforcementDecision.Blocked(
        resourceType = "CUSTOMER",
        currentCount = 50,
        limit = 50,
        message = "Limit reached",
        upgradeSuggestion = null
    )

    val viewModel = CustomerViewModel(
        repository = mockRepository,
        subscriptionEnforcement = mockEnforcement,
        workspaceIdProvider = { "workspace-123" }
    )

    viewModel.createCustomer(testCustomer)

    val event = viewModel.events.first()
    assertTrue(event is CustomerEvent.LimitExceeded)
}
```

## Summary

1. ✅ Inject `SubscriptionEnforcement` in ViewModel
2. ✅ Check limits before create operations
3. ✅ Handle `EnforcementDecision` events
4. ✅ Show appropriate dialogs
5. ✅ Navigate to plans screen for upgrades

This pattern ensures consistent limit enforcement across all modules while keeping the UI responsive and user-friendly.
