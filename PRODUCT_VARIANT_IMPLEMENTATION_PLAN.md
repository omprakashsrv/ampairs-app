# Product Variant Implementation - Remaining Work

## ✅ Completed (Phases 1-3)

### Phase 1: Database Foundation
- ✅ ProductType & ServiceType enums
- ✅ ProductVariantEntity with flexible 3-attribute system
- ✅ VariantAttributeEntity for searchable attributes
- ✅ ProductVariantDao & VariantAttributeDao with all queries
- ✅ Migration1To2 (v1 → v2 database upgrade)
- ✅ Platform migration support (Android/iOS/Desktop)

### Phase 2: Domain Models
- ✅ ProductVariant domain model with conversions
- ✅ Product model with classification fields
- ✅ Display name generation from attributes
- ✅ Effective pricing with fallbacks
- ✅ Stock management & low stock checks

### Phase 3: Repository Layer
- ✅ ProductRepository variant methods (create/update/delete/observe)
- ✅ Offline-first implementation with background sync
- ✅ Variant attribute indexing for search
- ✅ ProductModule Koin updates with factory scoping

## 🚧 Remaining Work (Phases 4-6)

### Phase 4: ViewModels (Next Step)

#### 4.1 Create VariantManagementViewModel
**Location:** `/composeApp/src/commonMain/kotlin/com/ampairs/product/ui/variant/VariantManagementViewModel.kt`

**State:**
```kotlin
data class VariantManagementUiState(
    val isLoading: Boolean = false,
    val variants: List<ProductVariant> = emptyList(),
    val productName: String = "",
    val totalStock: Double = 0.0,
    val errorMessage: String? = null,
    val variantToDelete: ProductVariant? = null,
    val isDeleting: Boolean = false
)
```

**Methods:**
- `loadVariants(productId: String)` - Observe variants reactively
- `deleteVariant(variantId: String)` - Soft delete with confirmation
- `showDeleteDialog(variant: ProductVariant)`
- `hideDeleteDialog()`

**Pattern:** Similar to MyTaxCodesViewModel

#### 4.2 Create VariantFormViewModel
**Location:** `/composeApp/src/commonMain/kotlin/com/ampairs/product/ui/variant/VariantFormViewModel.kt`

**Form State:**
```kotlin
data class VariantFormState(
    val sku: String = "",
    val variantName: String = "",
    val attribute1Name: String? = null,
    val attribute1Value: String? = null,
    val attribute2Name: String? = null,
    val attribute2Value: String? = null,
    val attribute3Name: String? = null,
    val attribute3Value: String? = null,
    val mrp: Double? = null,
    val dealerPrice: Double? = null,
    val sellingPrice: Double? = null,
    val stockQuantity: Double = 0.0,
    val lowStockAlert: Double? = null,
    val active: Boolean = true,
    val skuError: String? = null,
    val nameError: String? = null,
    val priceError: String? = null
) {
    val isValid: Boolean get() =
        sku.isNotBlank() && variantName.isNotBlank() &&
        skuError == null && nameError == null
}
```

**Methods:**
- `loadVariant(variantId: String?)` - Load existing variant or new
- `updateForm(newState: VariantFormState)`
- `saveVariant(onSuccess: () -> Unit)` - Generate UID, validate, save
- `loadAttributeOptions(productId: String)` - Get available attribute names/values

**UID Generation:** Use `UidGenerator.generateUid("VAR")` in ViewModel before repository call

**Pattern:** Similar to ProductFormViewModel

### Phase 5: UI Screens

#### 5.1 Create VariantManagementScreen
**Location:** `/composeApp/src/commonMain/kotlin/com/ampairs/product/ui/variant/VariantManagementScreen.kt`

**UI Structure:**
```
TopAppBar("Product Variants - {productName}")
├─ Actions: Sync button (optional)
└─ Content:
    ├─ Summary Card: Total Stock, Variant Count
    ├─ LazyColumn:
    │   └─ VariantCard (for each variant):
    │       ├─ SKU, Variant Name
    │       ├─ Attributes (Size: Large, Color: Blue)
    │       ├─ Pricing (MRP: $100, Selling: $80)
    │       ├─ Stock (50 units) with low stock badge
    │       └─ Actions: Edit icon, Delete icon
    └─ FAB: Add Variant
```

**Features:**
- Reactive variant list from Flow
- Edit navigation: `navigate(Route.VariantForm(productId, variantId))`
- Delete with confirmation dialog
- Stock status badges (low stock, out of stock)
- Empty state when no variants

**Pattern:** Similar to MyTaxCodesScreen

#### 5.2 Create VariantFormScreen
**Location:** `/composeApp/src/commonMain/kotlin/com/ampairs/product/ui/variant/VariantFormScreen.kt`

**Form Sections:**

1. **Basic Information:**
   - SKU (required, unique check)
   - Variant Name (required)

2. **Attributes (Flexible):**
   - Attribute 1: Name dropdown + Value dropdown
   - Attribute 2: Name dropdown + Value dropdown
   - Attribute 3: Name dropdown + Value dropdown
   - Load options from `getAttributeNames()` and `getAttributeValues()`

3. **Pricing (Optional Overrides):**
   - MRP (override base product MRP)
   - Dealer Price (override base DP)
   - Selling Price (override base selling price)
   - Show "(using base product price)" when null

4. **Stock Management:**
   - Stock Quantity (required, numeric keyboard)
   - Low Stock Alert (optional)

5. **Status:**
   - Active/Inactive dropdown

**Validation:**
- SKU required, format validation
- Variant Name required
- Attribute pairs must have both name AND value or both null
- Prices must be positive if set

**Keyboard Navigation:**
- `ImeAction.Next` for all fields except last
- `ImeAction.Done` for last field
- `singleLine = true` for text fields

**Pattern:** Similar to ProductFormScreen

#### 5.3 Update ProductFormScreen
**Location:** Update existing `/composeApp/src/commonMain/kotlin/com/ampairs/product/ui/create/ProductFormScreen.kt`

**Add Classification Section (after Basic Information):**
```kotlin
FormSection(title = "Classification") {
    // Product Type Dropdown
    ExposedDropdownMenuBox(...) {
        ProductType.values().forEach { type ->
            DropdownMenuItem(text = { Text(type.displayName) }, ...)
        }
    }

    // Service Type Dropdown
    ExposedDropdownMenuBox(...) {
        ServiceType.values().forEach { type ->
            DropdownMenuItem(text = { Text(type.displayName) }, ...)
        }
    }

    // Has Variants Checkbox
    Row {
        Checkbox(checked = formState.hasVariants, ...)
        Text("Product has variants")
    }
}
```

**Update FormState:**
```kotlin
data class ProductFormState(
    // ... existing fields ...
    val productType: ProductType? = null,
    val serviceType: ServiceType? = null,
    val hasVariants: Boolean = false
)
```

#### 5.4 Update ProductDetailsScreen
**Location:** Update existing `/composeApp/src/commonMain/kotlin/com/ampairs/product/ui/details/ProductDetailsScreen.kt`

**Add Variants Section (after pricing):**
```kotlin
if (product.hasVariants) {
    FormSection(title = "Variants") {
        Card {
            Column {
                // Summary
                Row {
                    Text("Total Stock: ${product.totalStock}")
                    Text("${product.variants?.size ?: 0} variants")
                }

                // First 3 variants preview
                product.variants?.take(3)?.forEach { variant ->
                    VariantListItem(variant)
                }

                // Manage button
                Button(
                    onClick = {
                        navigate(Route.VariantManagement(productId, product.name))
                    }
                ) {
                    Text("Manage Variants")
                }
            }
        }
    }
}
```

### Phase 6: Navigation & Integration

#### 6.1 Update Navigation Routes
**Location:** Update route definitions file

**Add Routes:**
```kotlin
@Serializable
data class VariantManagement(
    val productId: String,
    val productName: String
) : Route

@Serializable
data class VariantForm(
    val productId: String,
    val variantId: String? = null
) : Route
```

#### 6.2 Add Route Composables
**Location:** Update navigation graph

```kotlin
composable<Route.VariantManagement> { backStackEntry ->
    val route = backStackEntry.toRoute<Route.VariantManagement>()
    VariantManagementScreen(
        productId = route.productId,
        productName = route.productName,
        onNavigateToForm = { variantId ->
            navController.navigate(Route.VariantForm(route.productId, variantId))
        },
        onNavigateBack = { navController.popBackStack() }
    )
}

composable<Route.VariantForm> { backStackEntry ->
    val route = backStackEntry.toRoute<Route.VariantForm>()
    VariantFormScreen(
        productId = route.productId,
        variantId = route.variantId,
        onSaveSuccess = { navController.popBackStack() }
    )
}
```

#### 6.3 Update ProductModule Koin
**Add ViewModels:**
```kotlin
viewModel { (productId: String) ->
    VariantManagementViewModel(productId, get())
}
viewModel { (productId: String, variantId: String?) ->
    VariantFormViewModel(productId, variantId, get())
}
```

## 🎯 Testing Checklist

### Database & Repository
- [x] Migration runs successfully on existing installations
- [x] Variant CRUD operations work offline
- [x] Attribute indexing works correctly
- [ ] Workspace switching isolates variant data

### ViewModels
- [ ] Variant list loads reactively
- [ ] Form validation works correctly
- [ ] UID generation happens in ViewModel
- [ ] Save operations work offline-first

### UI
- [ ] Variant management screen displays correctly
- [ ] Variant form with all sections works
- [ ] Product form has classification dropdowns
- [ ] Product details shows variant summary
- [ ] Navigation flows work end-to-end
- [ ] Empty states display correctly
- [ ] Error states display correctly
- [ ] Loading states display correctly

### Integration
- [ ] Create product → Add variants → View in details
- [ ] Edit variant → Stock updates → Sync works
- [ ] Delete variant → Cascade works correctly
- [ ] Switch workspace → Data isolated correctly

## 📝 Implementation Notes

### Key Patterns to Follow

1. **UID Generation:** Always in ViewModel, never in Repository
   ```kotlin
   val variantId = UidGenerator.generateUid("VAR")
   val variant = formState.toProductVariant(productId, variantId)
   ```

2. **Offline-First:** Database save first, background sync after
   ```kotlin
   variantDao.insertVariant(entity) // Immediate
   try { api.sync() } catch { /* Graceful failure */ }
   ```

3. **Factory Scoping:** All workspace-aware components use `factory`
   ```kotlin
   factory { dao }, factory { repository }, factory { store }
   ```

4. **KMP Compatibility:** Use `Clock.System.now()` not `System.currentTimeMillis()`

5. **Form Navigation:** Use `ImeAction.Next` with `FocusManager`

### Estimated Effort
- Phase 4 (ViewModels): 2-3 hours
- Phase 5 (UI Screens): 4-6 hours
- Phase 6 (Navigation): 1-2 hours
- Testing & Bug Fixes: 2-3 hours
- **Total: 9-14 hours** (1-2 working days)

## 🔗 Reference Files

- **Tax Code Details:** Similar variant display pattern
- **ProductFormScreen:** Form structure and validation
- **CustomerFormViewModel:** Form state management pattern
- **MyTaxCodesViewModel:** List management with delete
- **TaxCodeDetailViewModel:** Reactive data loading

All patterns established in tax and customer modules should be followed for consistency.
