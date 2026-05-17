# Form Configuration System - Complete Guide

## Overview

The Ampairs platform includes a comprehensive **Dynamic Form Configuration System** that allows administrators to customize form fields without code changes. This system supports both **standard fields** (permanent, code-defined) and **custom attributes** (dynamic, user-defined).

**Last Updated**: January 2025
**Status**: ✅ **Production Ready**

---

## Architecture

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│                    FORM CONFIGURATION SYSTEM                 │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────────┐      ┌──────────────────┐             │
│  │  Standard Fields │      │ Custom Attributes │             │
│  │  (Code-Defined) │      │  (User-Defined)   │             │
│  └────────┬────────┘      └────────┬──────────┘             │
│           │                         │                         │
│           ▼                         ▼                         │
│  ┌──────────────────────────────────────────┐               │
│  │     Backend: ConfigService.kt            │               │
│  │  - seedCustomerDefaults() - 31 fields    │               │
│  │  - createFieldConfig()                   │               │
│  │  - createAttributeDefinition()           │               │
│  └──────────────┬───────────────────────────┘               │
│                 │                                             │
│                 ▼                                             │
│  ┌──────────────────────────────────────────┐               │
│  │       Database: MySQL                     │               │
│  │  - field_config table (31 standard)      │               │
│  │  - attribute_definition table (dynamic)  │               │
│  │  - customer.attributes JSON column        │               │
│  └──────────────┬───────────────────────────┘               │
│                 │                                             │
│                 ▼                                             │
│  ┌──────────────────────────────────────────┐               │
│  │  API: GET /api/v1/form/schema            │               │
│  │  - Auto-seeds on first access            │               │
│  │  - Returns EntityConfigSchemaResponse    │               │
│  └──────────────┬───────────────────────────┘               │
│                 │                                             │
│                 ▼                                             │
│  ┌──────────────────────────────────────────┐               │
│  │   Mobile/Web: FormConfigScreen           │               │
│  │  - Settings icon in module TopAppBar     │               │
│  │  - Edit field configs                    │               │
│  │  - Add custom attributes                 │               │
│  └──────────────┬───────────────────────────┘               │
│                 │                                             │
│                 ▼                                             │
│  ┌──────────────────────────────────────────┐               │
│  │  Form Rendering: CustomerFormScreen      │               │
│  │  - Standard fields + Custom attributes   │               │
│  │  - Dynamic visibility/mandatory          │               │
│  │  - Default values applied                │               │
│  └──────────────────────────────────────────┘               │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

## Part 1: Standard Fields (Code-Defined)

### What Are Standard Fields?

**Standard fields** are permanent form fields that:
- Are part of the entity model (Customer, Product, etc.)
- Have database columns
- Are defined in code by developers
- Can be **configured** (visibility, mandatory, default values) but not deleted
- Available to all workspaces

### Current Standard Customer Fields (31 Total)

| Section | Fields | Count |
|---------|--------|-------|
| Basic Information | name, email, customerType, customerGroup, countryCode, phone, landline | 7 |
| Business Information | gstNumber, panNumber | 2 |
| Credit Management | creditLimit, creditDays | 2 |
| Main Address | address, street, street2, city, pincode, state, country | 7 |
| Location | latitude, longitude | 2 |
| Billing Address | billingStreet, billingCity, billingPincode, billingState, billingCountry | 5 |
| Shipping Address | shippingStreet, shippingCity, shippingPincode, shippingState, shippingCountry | 5 |
| Status | status | 1 |

### How to Add a New Standard Field

**Example**: Adding a "website" field to Customer

#### Step 1: Update Backend Entity

**File**: `/ampairs-backend/customer/src/main/kotlin/com/ampairs/customer/domain/model/Customer.kt`

```kotlin
@Entity
@Table(name = "customers")
class Customer : OwnableBaseDomain() {
    // ... existing fields ...

    @Column(name = "website")
    var website: String? = null
}
```

#### Step 2: Update Backend DTO

**File**: `/ampairs-backend/customer/src/main/kotlin/com/ampairs/customer/domain/dto/CustomerDto.kt`

```kotlin
data class CustomerResponse(
    // ... existing fields ...
    val website: String? = null,
)

fun Customer.asCustomerResponse(): CustomerResponse = CustomerResponse(
    // ... existing mappings ...
    website = website,
)
```

#### Step 3: Add to Backend Default Configuration

**File**: `/ampairs-backend/form/src/main/kotlin/com/ampairs/form/domain/service/ConfigService.kt`

```kotlin
private fun seedCustomerDefaults() {
    val fields = listOf(
        // ... existing 31 fields ...

        // === New Field ===
        createFieldConfig("customer", "website", "Website", 32,
            visible = true, mandatory = false,
            placeholder = "https://example.com",
            helpText = "Customer's website URL"),
    )
}
```

#### Step 4: Update Mobile Domain Model

**File**: `/ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/customer/domain/Customer.kt`

```kotlin
@Serializable
data class Customer(
    // ... existing fields ...
    val website: String? = null,
)
```

#### Step 5: Update Mobile Form State

**File**: `/ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/customer/ui/create/CustomerFormViewModel.kt`

```kotlin
data class CustomerFormState(
    // ... existing fields ...
    val website: String = "",
)

fun CustomerFormState.toCustomer(): Customer = Customer(
    // ... existing mappings ...
    website = website.trim().takeIf { it.isNotBlank() },
)

fun Customer.toFormState(): CustomerFormState = CustomerFormState(
    // ... existing mappings ...
    website = website ?: "",
)
```

#### Step 6: Add UI Field (Config-Aware)

**File**: `/ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/customer/ui/create/CustomerFormScreen.kt`

```kotlin
// Add in appropriate section
val websiteConfig = fieldConfigs.find { it.fieldName == "website" }
if (websiteConfig?.visible == true) {
    ConfiguredTextField(
        value = formState.website,
        onValueChange = { onFormChange(formState.copy(website = it)) },
        label = websiteConfig.displayName,
        placeholder = websiteConfig.placeholder,
        helpText = websiteConfig.helpText,
        mandatory = websiteConfig.mandatory,
        enabled = websiteConfig.enabled,
        focusManager = focusManager
    )
}
```

#### Step 7: Create Database Migration

**File**: `/ampairs-backend/customer/src/main/resources/db/migration/mysql/V1.0.2__add_customer_website.sql`

```sql
ALTER TABLE customers
ADD COLUMN website VARCHAR(255) NULL
COMMENT 'Customer website URL';
```

#### Step 8: Update Mobile Backend Reference

**File**: `/ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/form/domain/DefaultFormConfigs.kt`

```kotlin
@Suppress("unused")
object DefaultFormConfigs {
    fun getDefaultCustomerFieldConfigs(): List<EntityFieldConfig> = listOf(
        // ... existing 31 fields ...

        EntityFieldConfig(
            uid = "customer-field-website",
            entityType = "customer",
            fieldName = "website",
            displayName = "Website",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 32,
            placeholder = "https://example.com",
            helpText = "Customer's website URL"
        ),
    )
}
```

**Result**: The "website" field is now a permanent standard field available to all users.

---

## Part 2: Custom Attributes (User-Defined)

### What Are Custom Attributes?

**Custom attributes** are dynamic fields that:
- Are **NOT** part of the main entity model
- Stored in JSON column (`customer.attributes`)
- Created by **admin users** at runtime (no code deployment)
- Workspace-specific (each workspace has different attributes)
- Can be added/edited/deleted through UI

### Attribute Data Types

| Data Type | Storage | UI Rendering | Examples |
|-----------|---------|--------------|----------|
| STRING | `"value"` | OutlinedTextField | Industry, Notes, Payment Terms |
| NUMBER | `"123.45"` | Number keyboard | Annual Revenue, Employee Count |
| BOOLEAN | `"true"/"false"` | Checkbox | Tax Exempt, Premium Customer |
| DATE | `"2025-01-20"` | Date picker | Contract Start Date |
| ENUM | `"option1"` | Dropdown | Company Size (1-10, 11-50, etc.) |

### How Custom Attributes Work

#### Backend Storage

```sql
-- customers table
CREATE TABLE customers (
    -- ... standard columns ...
    attributes JSON NULL,  -- Stores dynamic attributes
);

-- Example JSON content
{
  "industry": "Retail",
  "annualRevenue": "5000000",
  "companySize": "51-200",
  "taxExempt": "true",
  "notes": "Preferred customer with special pricing"
}
```

#### Backend Definition

```sql
-- attribute_definition table
CREATE TABLE attribute_definition (
    id BIGINT PRIMARY KEY,
    entity_type VARCHAR(50),     -- "customer"
    attribute_key VARCHAR(100),  -- "industry"
    display_name VARCHAR(255),   -- "Industry"
    data_type VARCHAR(50),       -- "STRING"
    visible BOOLEAN,
    mandatory BOOLEAN,
    enabled BOOLEAN,
    display_order INT,
    category VARCHAR(100),       -- "Business"
    placeholder VARCHAR(255),
    help_text TEXT,
    enum_values JSON,            -- For ENUM type
    -- ... workspace/tenant fields ...
);
```

### User Workflow: Adding Custom Attributes

#### Step 1: Access Form Configuration

1. Navigate to **Customers** module
2. Click **Settings icon** (⚙️) in TopAppBar
3. FormConfigScreen opens

#### Step 2: Click "Add Attribute"

- Scroll to "Custom Attributes" section
- Click **"+ Add Attribute"** button
- New empty attribute card appears

#### Step 3: Fill Attribute Definition

**Required Fields**:
- **Attribute Key**: `industry` (unique identifier, camelCase, no spaces)
- **Display Name**: `Industry` (shown in form UI)
- **Data Type**: Select from dropdown (STRING, NUMBER, BOOLEAN, DATE, ENUM)

**Optional Fields**:
- **Category**: `Business` (groups related attributes)
- **Placeholder**: `e.g., Retail, Manufacturing`
- **Help Text**: `Customer's business industry sector`
- **Display Order**: `1` (controls field order in form)
- **Visible**: ☑ (show in form)
- **Mandatory**: ☐ (not required)
- **Enabled**: ☑ (editable)

#### Step 4: Save Configuration

- Click **"Save Changes"** FAB (floating action button)
- Backend saves to `attribute_definition` table
- Configuration applied to workspace

#### Step 5: Use in Customer Form

- Navigate to **Create Customer** or **Edit Customer**
- Scroll to **"Custom Attributes"** section
- New "Industry" field appears dynamically
- User fills value: `Retail`
- On save, stored in `customer.attributes` JSON: `{"industry": "Retail"}`

### How Mobile App Renders Attributes

**File**: `/ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/customer/ui/create/CustomerFormScreen.kt`

```kotlin
// Lines 985-996
val attributeDefinitions = entityConfig?.attributeDefinitions?.filter { it.visible } ?: emptyList()
if (attributeDefinitions.isNotEmpty()) {
    FormSection(title = "Custom Attributes") {
        AttributesEditor(
            attributes = formState.attributes,
            attributeDefinitions = attributeDefinitions,
            onAttributesChange = { newAttributes ->
                onFormChange(formState.copy(attributes = newAttributes))
            }
        )
    }
}
```

**Attribute Renderer** (Lines 1124-1154):

```kotlin
@Composable
private fun ConfiguredAttributeField(
    definition: EntityAttributeDefinition,
    value: String,
    onValueChange: (String) -> Unit,
    focusManager: FocusManager
) {
    val label = if (definition.mandatory) "${definition.displayName} *" else definition.displayName

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = definition.placeholder?.let { { Text(it) } },
        supportingText = definition.helpText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = when (definition.dataType) {
                "number" -> KeyboardType.Number
                "email" -> KeyboardType.Email
                "phone" -> KeyboardType.Phone
                else -> KeyboardType.Text
            }
        )
    )
}
```

**Storage in State**:

```kotlin
// CustomerFormViewModel.kt
data class CustomerFormState(
    // ... standard fields ...
    val attributes: Map<String, String> = emptyMap(),
)

// Saved to backend as JSON
Customer(
    // ... standard fields ...
    attributes = mapOf(
        "industry" to "Retail",
        "annualRevenue" to "5000000",
        "companySize" to "51-200"
    )
)
```

---

## Part 3: Configuration Management

### Accessing Form Configuration

**Path**: Any module → Settings icon → FormConfigScreen

**Supported Modules**:
- Customer (`/customers` → Settings → `/form-config/customer`)
- Product (`/products` → Settings → `/form-config/product`)
- Business (`/business` → Form Configuration card)
- Tax (`/tax/rates` → Settings → `/form-config/tax_rate`)

### Configuration Capabilities

#### Field Configuration (Standard Fields)

**Can Configure**:
- ✅ **Display Name**: Change label text
- ✅ **Placeholder**: Set example text
- ✅ **Default Value**: Pre-fill new records
- ✅ **Help Text**: Add contextual information
- ✅ **Visible**: Show/hide field
- ✅ **Mandatory**: Make required/optional
- ✅ **Enabled**: Make editable/read-only
- ✅ **Display Order**: Reorder fields

**Cannot Configure**:
- ❌ Field name (hardcoded in entity)
- ❌ Data type (defined in database schema)
- ❌ Validation logic (requires code changes)
- ❌ Delete field (permanent part of entity)

#### Attribute Configuration (Custom Fields)

**Can Configure**:
- ✅ **Create new attributes**
- ✅ **Edit all properties**
- ✅ **Delete attributes**
- ✅ **Define data types**
- ✅ **Set enum values** (for dropdown attributes)

### Configuration Persistence

**Database Tables**:

```sql
-- Standard field configurations
CREATE TABLE field_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workspace_id VARCHAR(255),        -- Tenant isolation
    entity_type VARCHAR(50),          -- "customer", "product"
    field_name VARCHAR(100),          -- "name", "email"
    display_name VARCHAR(255),        -- "Customer Name"
    visible BOOLEAN DEFAULT TRUE,
    mandatory BOOLEAN DEFAULT FALSE,
    enabled BOOLEAN DEFAULT TRUE,
    display_order INT,
    validation_type VARCHAR(50),      -- "EMAIL", "PHONE"
    placeholder VARCHAR(255),
    help_text TEXT,
    default_value VARCHAR(500),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Custom attribute definitions
CREATE TABLE attribute_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workspace_id VARCHAR(255),        -- Tenant isolation
    entity_type VARCHAR(50),
    attribute_key VARCHAR(100),       -- "industry"
    display_name VARCHAR(255),        -- "Industry"
    data_type VARCHAR(50),            -- "STRING", "NUMBER"
    visible BOOLEAN,
    mandatory BOOLEAN,
    enabled BOOLEAN,
    display_order INT,
    category VARCHAR(100),
    placeholder VARCHAR(255),
    help_text TEXT,
    default_value VARCHAR(500),
    enum_values JSON,                 -- ["1-10", "11-50", "51-200"]
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

---

## Part 4: API Integration

### Fetching Configuration

**Endpoint**: `GET /api/v1/form/schema?entity_type=customer`

**Response**:

```json
{
  "success": true,
  "data": {
    "fieldConfigs": [
      {
        "uid": "customer-field-name",
        "entity_type": "customer",
        "field_name": "name",
        "display_name": "Customer Name",
        "visible": true,
        "mandatory": true,
        "enabled": true,
        "display_order": 1,
        "placeholder": "Enter customer name",
        "help_text": "Full name of the customer"
      }
      // ... 30 more standard fields
    ],
    "attributeDefinitions": [
      {
        "uid": "customer-attr-industry",
        "entity_type": "customer",
        "attribute_key": "industry",
        "display_name": "Industry",
        "data_type": "STRING",
        "visible": true,
        "mandatory": false,
        "enabled": true,
        "display_order": 1,
        "category": "Business",
        "placeholder": "e.g., Retail, Manufacturing",
        "help_text": "Customer's business industry"
      }
      // ... custom attributes
    ],
    "last_updated": "2025-01-20T10:30:00"
  }
}
```

### Auto-Seeding Behavior

**Backend Logic** (`ConfigService.kt`):

```kotlin
fun getConfigSchema(entityType: String): EntityConfigSchemaResponse {
    var fieldConfigs = fieldConfigRepository.findByEntityTypeOrderByDisplayOrderAsc(entityType)
    var attributeDefinitions = attributeDefinitionRepository.findByEntityTypeOrderByDisplayOrderAsc(entityType)

    // Auto-seed if empty (first time access for this workspace)
    if (fieldConfigs.isEmpty() && attributeDefinitions.isEmpty()) {
        logger.info("No configuration found for entity type: {}, seeding defaults...", entityType)
        seedDefaultConfig(entityType)

        // Fetch again after seeding
        fieldConfigs = fieldConfigRepository.findByEntityTypeOrderByDisplayOrderAsc(entityType)
        attributeDefinitions = attributeDefinitionRepository.findByEntityTypeOrderByDisplayOrderAsc(entityType)
    }

    return EntityConfigSchemaResponse(
        fieldConfigs = fieldConfigs.asFieldConfigResponses(),
        attributeDefinitions = attributeDefinitions.asAttributeDefinitionResponses()
    )
}
```

**When Auto-Seeding Happens**:
1. **First workspace access**: New workspace calls API for first time
2. **Default config**: Backend seeds 31 standard customer fields
3. **No duplicates**: Subsequent calls return existing config
4. **Workspace isolation**: Each workspace gets own config copy

### Saving Configuration

**Endpoint**: `POST /api/v1/form/config`

**Request Body**:

```json
{
  "fieldConfigs": [
    {
      "entity_type": "customer",
      "field_name": "name",
      "display_name": "Company Name",  // Changed from "Customer Name"
      "visible": true,
      "mandatory": true,
      "enabled": true,
      "display_order": 1
    }
  ],
  "attributeDefinitions": [
    {
      "entity_type": "customer",
      "attribute_key": "industry",
      "display_name": "Business Sector",  // Changed
      "data_type": "STRING",
      "visible": true
    }
  ]
}
```

---

## Part 5: Best Practices

### Standard Fields Best Practices

1. **Minimize standard fields**: Only add if needed by ALL customers
2. **Use attributes for business-specific**: Jewelry-specific fields → custom attributes
3. **Consider validation needs**: Complex validation requires standard fields
4. **Database design**: Standard fields get indexed, attributes don't
5. **Performance**: Querying standard fields is faster than JSON attributes

### Custom Attributes Best Practices

1. **Naming convention**: Use camelCase for attribute keys (`annualRevenue`, not `annual_revenue`)
2. **Category organization**: Group related attributes (`Business`, `Financial`, `Tax`)
3. **Reasonable limits**: Don't create 50+ attributes (performance impact)
4. **Data types**: Use correct types (NUMBER for calculations, STRING for text)
5. **Help text**: Always provide context for users

### Configuration Management

1. **Test before production**: Create test workspace, configure fields, test forms
2. **Document changes**: Keep track of custom attributes in workspace documentation
3. **Default values**: Use for common scenarios (country = "India")
4. **Visibility control**: Hide unused fields to simplify forms
5. **Display order**: Logical grouping (contact info together, addresses together)

---

## Part 6: Current Limitations & Future Enhancements

### Current Limitations

1. **Attribute data types**: All stored as strings in mobile `Map<String, String>`
   - Backend: `Map<String, Any>` (supports multiple types)
   - Mobile: Needs type conversion for NUMBER, BOOLEAN, DATE

2. **No advanced validation**: Attributes only support basic input types
   - No regex patterns
   - No custom validation rules
   - No cross-field validation

3. **No conditional visibility**: Can't hide/show fields based on other field values
   - Example: Hide GST if country != "India"

4. **No field groups/sections**: All attributes in single "Custom Attributes" section
   - Could support tabs or collapsible sections

### Planned Enhancements

1. **Advanced attribute rendering**:
   - DATE picker for date attributes
   - Dropdown for ENUM attributes
   - Checkbox for BOOLEAN attributes
   - Number formatting for NUMBER attributes

2. **Conditional logic**:
   - Show/hide fields based on conditions
   - Dynamic mandatory rules
   - Calculated fields

3. **Import/Export**:
   - Export configuration as JSON
   - Import configuration between workspaces
   - Configuration templates

4. **Validation rules**:
   - Regex patterns for STRING
   - Min/max for NUMBER
   - Date ranges for DATE

---

## Summary

### Key Takeaways

✅ **Standard Fields** (31): Code-defined, permanent, configurable
✅ **Custom Attributes** (∞): User-defined, dynamic, fully manageable
✅ **Backend Auto-Seeding**: First access seeds defaults
✅ **UI Integration**: Settings icon → FormConfigScreen
✅ **Workspace Isolation**: Each workspace has own config
✅ **Production Ready**: Fully implemented and tested

### Quick Reference

| Task | Steps |
|------|-------|
| **Add standard field** | 1. Backend entity/DTO<br>2. ConfigService<br>3. Mobile model<br>4. FormState<br>5. UI field<br>6. Migration |
| **Add custom attribute** | 1. Click Settings<br>2. Add Attribute<br>3. Fill definition<br>4. Save<br>5. Use in form |
| **Configure existing field** | 1. Click Settings<br>2. Edit field card<br>3. Change properties<br>4. Save |
| **Hide unused field** | 1. Settings → Find field<br>2. Uncheck "Visible"<br>3. Save |

---

**Documentation**: `/ampairs-mp-app/FORM_CONFIGURATION_GUIDE.md`
**Validation Report**: `/ampairs-mp-app/CUSTOMER_FORM_CONFIG_VALIDATION.md`
**Backend Config**: `/ampairs-backend/form/src/main/kotlin/com/ampairs/form/domain/service/ConfigService.kt`
**Mobile Reference**: `/ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/form/domain/DefaultFormConfigs.kt`
