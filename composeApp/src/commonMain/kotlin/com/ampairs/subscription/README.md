# Subscription Module

Complete subscription management system with offline-first architecture, limit enforcement, and read-only mode.

## Features

### 📱 Screens
1. **Main Subscription Screen** - Current plan overview with quick actions
2. **Plan Comparison Screen** - Compare all plans with billing cycles and features
3. **Usage Details Screen** - Detailed resource usage breakdown with upgrade prompts
4. **Payment History Screen** - Transaction history and billing summary
5. **Device Management Screen** - Manage registered devices with access modes

### 🔒 Limit Enforcement
- **Read-Only Mode** - Workspace becomes read-only when limits exceeded
- **Pre-Creation Checks** - Validates limits before allowing resource creation
- **Instant Feedback** - Shows upgrade dialog immediately when limit reached
- **Warning System** - Alerts when approaching limits (80%+)

### ⚡ Key Capabilities
- Offline-first subscription state management
- Client-side limit validation
- Device-based offline enforcement (7-day tokens)
- Usage tracking and metrics
- Plan upgrade suggestions
- Trial period support
- Multi-platform billing (Google Play, App Store, Razorpay, Stripe)

## Architecture

```
UI Layer (Compose)
    ↓
ViewModel (MVI Pattern)
    ↓
SubscriptionEnforcement ← LimitChecker
    ↓
SubscriptionRepository
    ↓
┌─────────────────┬──────────────────┐
│  Room Database  │  Ktor HTTP API   │
│  (Local Cache)  │  (Backend Sync)  │
└─────────────────┴──────────────────┘
```

## Usage Example

### Check Limits Before Creation

```kotlin
// In CustomerViewModel
class CustomerViewModel(
    private val subscriptionEnforcement: SubscriptionEnforcement
) : ViewModel() {

    fun createCustomer(customer: Customer) {
        viewModelScope.launch {
            val currentCount = repository.getCustomerCount()

            when (val decision = subscriptionEnforcement.canCreateResource(
                workspaceId = workspaceId,
                resourceType = LimitChecker.ResourceType.CUSTOMER,
                currentCount = currentCount
            )) {
                is EnforcementDecision.Blocked -> {
                    // Show upgrade dialog - read-only mode
                    _events.emit(CustomerEvent.LimitExceeded(decision))
                }
                is EnforcementDecision.Warning -> {
                    // Show warning - can proceed
                    _events.emit(CustomerEvent.LimitWarning(decision))
                }
                is EnforcementDecision.Allowed -> {
                    // Proceed with creation
                    repository.createCustomer(customer)
                }
            }
        }
    }
}
```

### Show Enforcement Dialogs

```kotlin
@Composable
fun CustomerFormScreen(
    viewModel: CustomerViewModel,
    onNavigateToPlans: () -> Unit
) {
    var showLimitDialog by remember { mutableStateOf<EnforcementDecision.Blocked?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is CustomerEvent.LimitExceeded -> {
                    showLimitDialog = event.decision
                }
            }
        }
    }

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
}
```

## Read-Only Mode Behavior

When a resource limit is exceeded:

| Resource | Read-Only Behavior |
|----------|-------------------|
| **Customers** | ❌ Cannot create new customers<br>✅ Can view/edit existing customers |
| **Products** | ❌ Cannot create new products<br>✅ Can view/edit existing products |
| **Invoices** | ❌ Cannot create new invoices<br>✅ Can view existing invoices |
| **Devices** | ❌ Cannot register new devices<br>✅ Existing devices continue working |

## Resource Types

```kotlin
LimitChecker.ResourceType.CUSTOMER   // Max customers
LimitChecker.ResourceType.PRODUCT    // Max products
LimitChecker.ResourceType.INVOICE    // Max invoices per month
LimitChecker.ResourceType.MEMBER     // Max workspace members
LimitChecker.ResourceType.DEVICE     // Max registered devices
LimitChecker.ResourceType.STORAGE_GB // Max storage in GB
```

## Subscription Plans

| Plan | Customers | Products | Invoices/Mo | Devices | Price (Annual) |
|------|-----------|----------|-------------|---------|----------------|
| **FREE** | 50 | 50 | 20 | 2 | Free |
| **STARTER** | 500 | 500 | 200 | 5 | Save 20% |
| **PROFESSIONAL** | Unlimited | Unlimited | Unlimited | 15 | Save 25% |
| **ENTERPRISE** | Unlimited | Unlimited | Unlimited | Unlimited | Save 30% |

## Integration

### 1. Add to Koin Module

```kotlin
val yourModule = module {
    viewModel {
        YourViewModel(
            repository = get(),
            subscriptionEnforcement = get() // Inject this
        )
    }
}
```

### 2. Check Before Create Operations

See [INTEGRATION_GUIDE.md](./INTEGRATION_GUIDE.md) for complete examples.

### 3. Handle Events

```kotlin
sealed class YourEvent {
    data class LimitExceeded(val decision: EnforcementDecision.Blocked) : YourEvent()
    data class LimitWarning(val decision: EnforcementDecision.Warning) : YourEvent()
}
```

## Backend APIs

### Required APIs (Implemented ✅)
- `GET /api/v1/subscription/plans` - Get all plans
- `GET /api/v1/subscription/current` - Get current subscription
- `GET /api/v1/subscription/usage` - Get usage metrics
- `POST /api/v1/subscription/trial` - Start trial
- `POST /api/v1/subscription/change-plan` - Change plan
- `GET /api/v1/subscription/devices` - List devices
- `POST /api/v1/subscription/sync` - Full offline sync

### Needed APIs (To Implement ❌)
- `GET /api/v1/subscription/payments` - Payment history
- `GET /api/v1/subscription/payment-methods` - Saved payment methods

See [SUBSCRIPTION_BACKEND_API_REQUIREMENTS.md](./SUBSCRIPTION_BACKEND_API_REQUIREMENTS.md) for full API specs.

## Files Structure

```
subscription/
├── api/
│   ├── SubscriptionApi.kt          # API interface
│   └── SubscriptionApiImpl.kt      # Ktor implementation
├── db/
│   ├── SubscriptionDatabase.kt     # Room database
│   ├── SubscriptionDao.kt          # Database queries
│   └── SubscriptionEntities.kt     # Database entities
├── domain/
│   ├── dto/
│   │   └── SubscriptionApiDtos.kt  # API response DTOs
│   └── model/
│       ├── SubscriptionModels.kt   # Domain models
│       ├── SubscriptionEnums.kt    # Enums (Plan, Status, etc.)
│       ├── PaymentModels.kt        # Payment models
│       └── DeviceRegistration.kt   # Device models
├── feature/
│   ├── FeatureGate.kt             # Feature availability
│   ├── LimitChecker.kt            # Limit checking logic
│   └── SubscriptionEnforcement.kt # Read-only enforcement
├── repository/
│   └── SubscriptionRepository.kt   # Data layer
├── ui/
│   ├── components/
│   │   ├── SubscriptionComponents.kt    # Reusable components
│   │   ├── SubscriptionDialogs.kt       # Action dialogs
│   │   └── LimitExceededDialog.kt       # Enforcement dialogs
│   └── screens/
│       ├── SubscriptionScreen.kt        # Main screen
│       ├── PlanComparisonScreen.kt      # Plan comparison
│       ├── UsageDetailsScreen.kt        # Usage breakdown
│       ├── PaymentHistoryScreen.kt      # Billing history
│       └── DeviceManagementScreen.kt    # Device management
├── viewmodel/
│   └── SubscriptionViewModel.kt    # Business logic
├── di/
│   └── SubscriptionModule.kt       # Koin configuration
├── Navigation.kt                   # Navigation graph
├── INTEGRATION_GUIDE.md           # Integration examples
├── SUBSCRIPTION_BACKEND_API_REQUIREMENTS.md  # API documentation
└── README.md                       # This file
```

## Testing

See [INTEGRATION_GUIDE.md](./INTEGRATION_GUIDE.md) for testing examples with mocked enforcement.

## Next Steps

1. ✅ Implement backend payment history APIs
2. ✅ Integrate enforcement in Customer module
3. ✅ Integrate enforcement in Product module
4. ✅ Integrate enforcement in Invoice module
5. ✅ Add usage analytics dashboard
6. ✅ Implement usage export functionality

## License

Part of the Ampairs Mobile Application.
