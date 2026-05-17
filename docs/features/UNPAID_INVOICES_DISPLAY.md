# Unpaid Invoices Display on Subscription Screen

**Date:** December 2, 2025
**Status:** ✅ Complete

---

## Overview

Added prominent display of unpaid invoices on the main SubscriptionScreen to provide immediate visibility of outstanding payments and encourage timely payment action.

---

## Implementation Details

### 1. **SubscriptionScreen.kt** - Main Integration

**Changes:**
- Injected `InvoiceViewModel` to access invoice data
- Added `onNavigateToInvoiceDetail` parameter for direct navigation
- Load invoice summary on screen entry
- Load overdue invoices for quick display
- Display `UnpaidInvoicesCard` when unpaid invoices exist

**Code Addition:**
```kotlin
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel,
    // ... existing parameters ...
    onNavigateToInvoiceDetail: ((String) -> Unit)? = null,  // NEW
    invoiceViewModel: InvoiceViewModel = koinInject()       // NEW
) {
    // ... existing states ...

    // Invoice states
    val invoiceSummary by invoiceViewModel.invoiceSummary.collectAsState()
    val unpaidInvoices by invoiceViewModel.invoices.collectAsState()

    // Load invoice summary on screen entry
    LaunchedEffect(Unit) {
        invoiceViewModel.loadInvoiceSummary()
        invoiceViewModel.loadInvoices(status = InvoiceStatus.OVERDUE)
    }

    // ... in LazyColumn ...
    // Unpaid Invoices Alert (if any)
    item {
        if (invoiceSummary != null && (invoiceSummary!!.overdueInvoices > 0 || invoiceSummary!!.pendingInvoices > 0)) {
            UnpaidInvoicesCard(
                summary = invoiceSummary!!,
                onViewInvoices = onNavigateToInvoices,
                onViewInvoiceDetail = onNavigateToInvoiceDetail
            )
        }
    }
}
```

---

### 2. **UnpaidInvoicesCard Component** - New UI Component

**Location:** `SubscriptionScreen.kt` (bottom of file)

**Features:**
- **Error Container Background:** Red background for overdue invoices
- **Secondary Container Background:** Blue/gray background for pending invoices
- **Warning Icon:** Visual alert with warning or info icon
- **Count Badge:** Shows total number of unpaid invoices
- **Urgent Message:** Context-aware message based on status
- **Total Outstanding Amount:** Formatted currency display
- **Next Due Date:** When the next payment is due
- **Call-to-Action Button:** "Pay Now" (overdue) or "View Invoices" (pending)

**Visual Design:**
```
┌─────────────────────────────────────────────┐
│ ⚠️ Overdue Invoices                    [2]  │
│                                             │
│ You have 2 overdue invoices. Please pay    │
│ to avoid service interruption.              │
│ ─────────────────────────────────────────── │
│ Total Outstanding      Next Due             │
│ ₹5,000.00             2025-11-25            │
│                                             │
│ [💳 Pay Now]                                │
└─────────────────────────────────────────────┘
```

**Color Coding:**
- **Overdue:** Red error container with error colors
- **Pending:** Secondary container with primary colors

---

### 3. **Navigation.kt** - Route Integration

**Changes:**
- Added `onNavigateToInvoiceDetail` parameter to SubscriptionScreen call
- Navigation to InvoiceDetail screen with invoice UID

```kotlin
SubscriptionScreen(
    viewModel = viewModel,
    // ... existing callbacks ...
    onNavigateToInvoiceDetail = { invoiceUid ->
        navController.navigate(SubscriptionRoute.InvoiceDetail(invoiceUid))
    },
    // ...
)
```

---

## User Experience Flow

### Scenario 1: Overdue Invoices
1. User opens Subscription screen
2. Red alert card appears prominently below subscription status
3. Shows urgent message: "You have X overdue invoices. Please pay to avoid service interruption."
4. Displays total outstanding amount in red
5. Shows next due date
6. **"Pay Now"** button navigates to Invoices screen

### Scenario 2: Pending Invoices (No Overdue)
1. User opens Subscription screen
2. Blue/gray alert card appears below subscription status
3. Shows informational message: "You have X pending invoices. Payment due soon."
4. Displays total outstanding amount in primary color
5. Shows next due date
6. **"View Invoices"** button navigates to Invoices screen

### Scenario 3: No Unpaid Invoices
1. User opens Subscription screen
2. No invoice alert card is displayed
3. User sees only their subscription status and usage

---

## Technical Details

### Data Loading
- **Automatic:** Invoice summary loaded on screen entry via `LaunchedEffect`
- **Filtered:** Only overdue invoices pre-loaded for performance
- **Reactive:** StateFlow updates UI when invoice status changes

### Performance Optimization
- Only loads summary data (lightweight)
- Full invoice list loaded on-demand when user navigates
- No blocking operations on main subscription screen

### State Management
- `InvoiceViewModel` manages all invoice-related state
- Separate from `SubscriptionViewModel` for separation of concerns
- Shared via Koin dependency injection

---

## Benefits

### For Users:
✅ **Immediate Visibility:** Can't miss unpaid invoices
✅ **Contextual Urgency:** Clear differentiation between overdue and pending
✅ **Quick Action:** Direct navigation to payment with one tap
✅ **Financial Awareness:** Always aware of outstanding amounts

### For Business:
✅ **Improved Collections:** Prominent display encourages timely payment
✅ **Reduced Churn:** Users pay before service interruption
✅ **Better UX:** Proactive notification within app flow
✅ **Clear Communication:** No surprises about payment status

---

## Display Priority

The UnpaidInvoicesCard appears in this order on SubscriptionScreen:
1. Pre-Launch Banner (if eligible)
2. Seasonal Discount Banner (if active)
3. Current Subscription Card
4. **Unpaid Invoices Alert** ← NEW (high priority placement)
5. Usage Summary Card
6. Quick Actions
7. Available Plans

This ensures financial obligations are surfaced immediately after subscription status.

---

## Integration with Existing Features

- **Works with:** InvoiceListScreen, InvoiceDetailScreen, InvoiceViewModel
- **Complements:** Quick Actions "Invoices" button
- **Uses:** Existing navigation infrastructure
- **No Breaking Changes:** All existing functionality preserved

---

## Files Modified

1. ✅ **SubscriptionScreen.kt** - Added UnpaidInvoicesCard component and integration
2. ✅ **Navigation.kt** - Added onNavigateToInvoiceDetail route

**Total Lines Added:** 183 lines

---

## Testing Checklist

- [ ] Display card when overdue invoices exist
- [ ] Display card when pending invoices exist (no overdue)
- [ ] Hide card when no unpaid invoices
- [ ] "Pay Now" button navigates to Invoices screen (overdue)
- [ ] "View Invoices" button navigates to Invoices screen (pending)
- [ ] Currency formatting displays correctly
- [ ] Next due date displays correctly
- [ ] Card colors match invoice status (red for overdue, blue for pending)
- [ ] Invoice count badge displays correctly
- [ ] Responsive layout on different screen sizes

---

## Future Enhancements

**Potential Improvements:**
- Add "Pay Specific Invoice" action for direct payment
- Show list of invoice numbers in card
- Add dismiss/remind-me-later functionality
- Display estimated service interruption date for overdue
- Add push notification for overdue invoices

---

**Status:** ✅ Ready for testing and review

This implementation provides excellent visibility for unpaid invoices while maintaining a clean, non-intrusive design that encourages timely payment action.
