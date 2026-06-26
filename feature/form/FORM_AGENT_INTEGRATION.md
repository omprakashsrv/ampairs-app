# Form Agentic Integration Guide

This document explains how to integrate the FormAgentChatBubble into form edit screens to enable voice-to-form field filling.

## Overview

The form agent system allows users to fill form fields via voice commands. For example:
- "Set customer name to John Smith"
- "Fill email with john@example.com"
- "What fields do I need?"

The integration consists of:
1. **FormActionHandler** — Routes voice intents to form field operations
2. **FormAgentChatBubble** — UI component with voice input and chat interface
3. **FormFieldFillIntent** — Data model for field fill requests

## Architecture

```
User Voice Input
    ↓
FormAgentChatBubble (collects transcript)
    ↓
Agent Module (processes intent via FormActionHandler)
    ↓
FormActionHandler.fillField() (returns metadata with fieldKey + value)
    ↓
onFieldFill callback → Form ViewModel updates state
    ↓
Form field re-renders with new value
```

## Integration Steps

### 1. Add Chat Bubble to Form Edit Screen

Add the `FormAgentChatBubble` composable to the top-level layout of your form edit screen.

**Example: CustomerGroupFormScreen**

```kotlin
@Composable
fun CustomerGroupFormScreen(
    customerGroupId: String? = null,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerGroupFormViewModel = assistedMetroViewModel<CustomerGroupFormViewModel, CustomerGroupFormViewModel.Factory> { create(customerGroupId) }
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        Column {
            TopAppBar(
                title = { Text("Customer Group") },
                // ... existing actions
            )
            // Your form content here
            // DynamicFormRenderer or custom fields
        }

        // Add chat bubble in top-right
        FormAgentChatBubble(
            entityType = "customer_group",
            onFieldFill = { fieldKey, value ->
                // Update your form state when a field is filled
                viewModel.updateField(fieldKey, value)
            },
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}
```

### 2. Implement Field Update in ViewModel

Update your ViewModel to handle field fills from the agent:

```kotlin
class CustomerGroupFormViewModel(
    private val repository: ConfigRepository,
    // ... other deps
) : ViewModel() {
    private val _formState = MutableStateFlow(CustomerGroupFormState())
    val formState: StateFlow<CustomerGroupFormState> = _formState.asStateFlow()

    fun updateField(fieldKey: String, value: String) {
        val currentState = _formState.value
        _formState.value = when (fieldKey) {
            "name" -> currentState.copy(name = value)
            "description" -> currentState.copy(description = value)
            "code" -> currentState.copy(code = value)
            // ... handle all form fields
            else -> currentState
        }
    }
}
```

### 3. Entity Type Naming Convention

When adding the chat bubble, use the backend entity type consistently:
- "customer" — for customers
- "customer_group" — for customer groups
- "customer_type" — for customer types
- "product" — for products
- "order" — for orders
- "invoice" — for invoices
- "business" — for business profile

The entity type is used by the FormActionHandler to retrieve the correct form schema.

### 4. Handling Agent Responses

The FormActionHandler returns field fill requests with metadata:

```kotlin
// Example response from FormActionHandler.fillField()
ActionResult.Success(
    summary = "Filled field 'Customer Name' with value 'John'.",
    data = mapOf(
        "entityType" to "customer",
        "fieldKey" to "name",
        "fieldValue" to "John",
        "fieldDisplayName" to "Customer Name",
    )
)
```

Your ViewModel's `updateField()` method extracts `fieldKey` and `fieldValue` to update the form state.

## Voice Command Examples

Once integrated, users can speak:

### Create/Update Actions
- "Set customer name to Acme Corp"
- "Fill email with admin@acme.com"
- "Set phone to 9876543210"
- "Add city Mumbai"

### Query/Discovery
- "What fields do I need to fill?" → Returns mandatory fields
- "Show me the form schema" → Lists all available fields with types

### Validation
- Field values are validated against the form schema rules (required, min/max length, etc.)
- Invalid values return an error with guidance

## Field Types Supported

The agent supports filling fields of type:
- **TEXT** — name, email, city, etc.
- **PARAGRAPH** — descriptions, notes
- **NUMBER** — quantities, prices
- **DATE** — dates (voice: "set date to 15th January 2025")
- **CHOICE** — dropdowns (voice: "set status to active")
- **YESNO** — toggles (voice: "set active to yes")

For **CHOICE** / **MULTI_CHOICE** fields, the agent displays available options if the user's value is ambiguous.

## Testing

### Manual Testing Checklist

1. **Chat Bubble Appearance**
   - [ ] Chat bubble FAB appears in top-right of form edit screen
   - [ ] Tapping FAB opens the chat dialog
   - [ ] Dialog shows entity type context ("Editing: customer")

2. **Voice Input (when integrated with SpeechToText)**
   - [ ] Mic button responds to tap (listening state)
   - [ ] Transcript updates as user speaks
   - [ ] Stop button cancels listening

3. **Field Filling**
   - [ ] Agent correctly fills text fields
   - [ ] Choice fields map user input to valid options
   - [ ] Mandatory field warnings appear for empty required fields
   - [ ] Invalid values are rejected with error messages

4. **Cross-Platform**
   - [ ] Chat bubble renders correctly on Android, iOS, Desktop
   - [ ] Dialog sizing adapts to screen sizes
   - [ ] Voice input works on all platforms (with appropriate STT backend)

## Future Enhancements

### Planned Features
1. **Voice confirmation** — "Did I get that right?" before committing field fills
2. **Multi-field filling** — "Set customer name to John, email to john@acme.com, city to Mumbai"
3. **Field suggestions** — "Next, you should fill [required field]"
4. **Undo/redo** — Quick revert of agent-filled fields
5. **Form completion progress** — "3 of 5 required fields filled"

### Integration with Other Modules
- **Agent chat** — Integrate with ChatViewModel for richer assistant conversation context
- **Permission system** — Respect workspace roles; restrict field fills based on user permissions
- **Audit logging** — Track which fields were filled via voice for compliance

## Troubleshooting

### Chat Bubble Not Appearing
- Verify `feature/form/build.gradle.kts` includes `implementation(projects.feature.agent)`
- Check that `FormActionHandler` is properly annotated with `@ContributesIntoMap(WorkspaceScope::class)`

### Fields Not Filling
- Ensure ViewModel's `updateField()` handles all form field keys
- Verify entity type matches the backend form schema (case-sensitive)
- Check FormValueState is correctly bound to UI field components

### Voice Input Not Working (Future)
- Verify SpeechToText adapter is configured for the platform
- Check RECORD_AUDIO permission is granted on Android/iOS
- Ensure microphone access is enabled in device settings

## Code References

- **Handler**: `feature/form/src/commonMain/kotlin/com/ampairs/form/agent/FormActionHandler.kt`
- **UI Component**: `feature/form/src/commonMain/kotlin/com/ampairs/form/ui/FormAgentChatBubble.kt`
- **Intent Model**: `feature/form/src/commonMain/kotlin/com/ampairs/form/ui/FormFieldFillIntent.kt`
- **Form Schema**: `feature/form-api/src/commonMain/kotlin/com/ampairs/form/domain/FormSchema.kt`

## Related Patterns

- **Customer Agent Handler**: `feature/customer/src/commonMain/kotlin/com/ampairs/customer/agent/CustomerActionHandler.kt`
- **Agent Module Integration**: `/metro-di` and `/offline-sync` guides for dependency setup
- **Form Rendering**: `DynamicFormRenderer` in form module for field rendering pipeline
