# feature:tax

Multi-region tax calculation engine. Supports India GST, USA Sales Tax, UK VAT, EU VAT, Canada GST/HST, and Australia GST with configurable rules and components.

## Responsibilities

- Calculate taxes for line items based on region, product type, and tax code
- Store and manage tax codes, components, and rules in Room
- Provide a UI for browsing, searching, and applying tax codes
- Admin UI for configuring custom tax rules

## Architecture

```
TaxCalculationEngine
    ↓
TaxStrategy (per region)
    ├── IndiaGSTStrategy   (CGST / SGST / IGST)
    ├── USASalesTaxStrategy
    ├── UKVATStrategy
    ├── EUVATStrategy
    ├── CanadaGSTHSTStrategy
    └── AustraliaGSTStrategy
```

## Key Classes

| Class | Purpose |
|---|---|
| `TaxCalculationEngine` | Selects strategy and computes tax components |
| `TaxCodeRepository` | CRUD for tax codes |
| `TaxComponentRepository` | Tax component definitions (rate, type) |
| `TaxRuleRepository` | Conditional tax rules (HSN-based, etc.) |
| `TaxConfigurationRepository` | Per-workspace tax config |
| `TaxCalculatorViewModel` | Interactive tax calculator UI |
| `MyTaxCodesViewModel` | Workspace tax code list |
| `TaxCodeSearchViewModel` | Searchable tax code picker |

## Koin Module

```kotlin
taxModule          // in com.ampairs.tax.di
taxPlatformModule  // platform-specific (DB factory)
```

## Navigation Routes

```kotlin
TaxRoute.TaxCodes        // my tax codes
TaxRoute.TaxCodeSearch   // picker for forms
TaxRoute.TaxCalculator   // interactive calculator
TaxRoute.TaxConfig       // admin configuration
```

## Database

`TaxRoomDatabase` — workspace-scoped (`factory` scope).

Tables: tax_codes, tax_components, tax_rules, tax_configuration
