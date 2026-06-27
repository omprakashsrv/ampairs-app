# SelectionHelper — Ambiguous Parameter Resolution (FR-017)

## Overview

`SelectionHelper` eliminates the need to force users to re-type exact IDs when an action parameter resolves to multiple candidates. Instead of returning `ActionResult.NeedsInput("Please specify the exact customer ID")`, return `ActionResult.Selection` to show a tappable dialog with numbered options.

This works across:
- **Text path**: User taps an option in the dialog
- **Button path**: Option buttons with numbers
- **Voice path**: User says the number (e.g., "1", "2", "3")

## When to Use SelectionHelper

Use `SelectionHelper.resolveWithSelection()` whenever:
1. A user provides a search hint (e.g., "customer John")
2. Multiple entities match the hint
3. An exact match is impossible without picking one

**Examples:**
- Customer action: "get customer John" → multiple Johns in the DB
- Product action: "add 3 widgets" → multiple product SKUs named "widget"
- Order action: "create order for Acme" → multiple companies named "Acme"

## How SelectionHelper Works

```kotlin
SelectionHelper.resolveWithSelection(
    candidates: List<SelectionOption>,     // The matches (max 5 displayed)
    question: String,                      // Header text: "Which customer?"
    paramName: String,                     // Parameter to fill: "customerId"
    action: AgentAction,                   // The pending action
    fallbackNeedsInput: String? = null,    // Fallback if selection fails
)
```

**Returns:**
- `0 candidates` → `ActionResult.NeedsInput` (ask for more details)
- `1 candidate` → `ActionResult.Success` (auto-pick, no dialog needed)
- `2+ candidates` → `ActionResult.Selection` (show dialog)

## SelectionOption Structure

```kotlin
data class SelectionOption(
    val id: String,                      // Unique identifier (e.g., customer.uid)
    val label: String,                   // Primary display (e.g., customer.name)
    val secondaryLabel: String? = null,  // Disambiguation (e.g., phone or city)
    val metadata: Map<String, Any> = emptyMap(),  // Optional extra data
)
```

## Example: Customer Action with Selection

### Before (NeedsInput)
```kotlin
private suspend fun getCustomer(params: Map<String, String>): ActionResult {
    val query = params["searchName"] ?: return ActionResult.NeedsInput(...)
    val results = customerRepository.searchCustomers(query).first()
    
    return when {
        results.isEmpty() -> ActionResult.Success("No matches")
        results.size > 1 -> {
            // Old way: force user to type the exact ID
            ActionResult.NeedsInput(
                "Please specify the customer ID",
                listOf("customerId")
            )
        }
        else -> /* ... */
    }
}
```

### After (SelectionHelper)
```kotlin
private suspend fun getCustomer(params: Map<String, String>): ActionResult {
    val query = params["searchName"] ?: return ActionResult.NeedsInput(...)
    val results = customerRepository.searchCustomers(query).first()
    
    val options = results.take(5).map { c ->
        SelectionOption(
            id = c.uid,
            label = c.name,
            secondaryLabel = c.phone ?: c.city,  // Helps disambiguate
        )
    }
    
    return SelectionHelper.resolveWithSelection(
        candidates = options,
        question = "Which customer?",
        paramName = "customerId",
        action = AgentAction(
            moduleName = "customer",
            actionType = ActionType.READ,
            params = params,
        ),
        fallbackNeedsInput = "Which customer? (found ${results.size} matches)",
    )
}
```

## Example: Product Action (Cart Add-Item)

When adding items to a cart, if the product name is ambiguous:

```kotlin
private suspend fun addProductToCart(params: Map<String, String>): ActionResult {
    val productName = params["product"] ?: return ActionResult.NeedsInput(...)
    val products = productRepository.searchByName(productName).first()
    
    val options = products.take(5).map { p ->
        SelectionOption(
            id = p.uid,
            label = p.name,
            secondaryLabel = p.sku,  // SKU helps distinguish variants
            metadata = mapOf("category" to p.category),
        )
    }
    
    return SelectionHelper.resolveWithSelection(
        candidates = options,
        question = "Which product?",
        paramName = "productId",
        action = AgentAction(
            moduleName = "order",
            actionType = ActionType.ADD_ITEM,
            params = params,  // Retains quantity and other context
        ),
    )
}
```

## Example: Order Creation (Customer Resolution)

In the order handler, when multiple customers match the search:

```kotlin
val customerName = params["customer"]?.trim() ?: return ActionResult.NeedsInput(...)
val candidates = orderRepository.customerDataService.listCustomers(customerName)

if (candidates.size > 1) {
    val options = candidates.take(5).map { c ->
        SelectionOption(
            id = c.id,
            label = c.name,
            secondaryLabel = c.phone,  // Phone number helps identify the right customer
        )
    }
    
    return SelectionHelper.resolveWithSelection(
        candidates = options,
        question = "Which customer?",
        paramName = "customerId",
        action = AgentAction(
            moduleName = "order",
            actionType = ActionType.CREATE,
            params = params,  // Action retains product/quantity context
        ),
        fallbackNeedsInput = "Which customer did you mean?",
    )
}
```

## Voice Integration

When the Selection dialog is displayed and the user is in **voice mode**:

1. The dialog shows number badges (1, 2, 3...) on each option
2. A voice hint displays: "Tap an option or say the number (1, 2, 3...)"
3. Voice input parser (in `ChatViewModel.listenOnce()`) intercepts numeric utterances
4. Calls `viewModel.selectOption(selectedOptionId)` to fill the parameter and re-dispatch

**Minimal implementation in voice loop:**
```kotlin
// Inside ChatViewModel.listenOnce()
val pendingSelection = _uiState.value.pendingSelection
if (pendingSelection != null && transcript.matches(Regex("^[1-9]$"))) {
    val index = transcript.toInt() - 1
    if (index < pendingSelection.options.size) {
        selectOption(pendingSelection.options[index].id)
        return
    }
}
```

## Checklist for Adding Selection to a Handler

- [ ] Import `SelectionOption` and `SelectionHelper`
- [ ] Identify search/lookup methods that return multiple candidates
- [ ] Map candidates to `SelectionOption` (id, label, secondary label)
- [ ] Replace `ActionResult.NeedsInput` with `SelectionHelper.resolveWithSelection()`
- [ ] Test with 1, 2, and 5+ candidates to verify all paths (auto-pick, selection, fallback)
- [ ] Add secondary labels that disambiguate (phone, SKU, category, city)
- [ ] Ensure the pending action retains context (quantity, other params)

## Modules Already Updated

The following handlers have been updated to use SelectionHelper:
- **CustomerActionHandler** — getCustomer, updateCustomer, deleteCustomer (when name matches multiple)
- **OrderActionHandler** — proposeOrder (customer and product resolution)

## Remaining Candidates for Selection

These handlers could benefit from SelectionHelper:
- **ProductActionHandler** — any search or lookup returning multiple products
- **InvoiceActionHandler** — customer search during invoice creation
- **PaymentActionHandler** — customer/invoice search for payment entry
- **InventoryActionHandler** — product search during stock adjustment
- **CartActionHandler** — any product addition with ambiguous name

## Testing the Selection UI

### Manual Test (Android/Desktop)

1. Start the app, go to Chat, enable Voice
2. Say or type: "get customer john" (assuming multiple "John" customers exist)
3. Verify the Selection dialog appears with:
   - Header: "Which customer?"
   - Numbered options (1, 2, 3...)
   - Secondary labels (phone or city)
   - Voice hint text
4. Tap an option → dialog closes, action re-dispatches with the selected ID
5. Optional: Say "1" or "2" in voice mode → option is selected by number

### Unit Test Pattern

```kotlin
@Test
fun `multiple customer matches returns selection`() = runTest {
    // Setup: 3 customers named "John"
    val handler = CustomerActionHandler(mockRepository, mockDao, mockSync)
    val result = handler.execute(
        AgentAction(
            moduleName = "customer",
            actionType = ActionType.READ,
            params = mapOf("searchName" to "john"),
        )
    )
    
    // Verify: Selection is returned with 3 options
    assertThat(result).isInstanceOf(ActionResult.Selection::class.java)
    val selection = result as ActionResult.Selection
    assertThat(selection.question).isEqualTo("Which customer?")
    assertThat(selection.options).hasSize(3)
    assertThat(selection.paramName).isEqualTo("customerId")
}
```

## See Also

- **SelectionDialog.kt** — UI composable that renders the selection modal
- **ChatViewModel.selectOption()** — ViewModel handler for user selection
- **AgentOrchestrator.selectOption()** — Re-dispatches action with selected ID
- **PendingSelection** — State class holding pending selection context
