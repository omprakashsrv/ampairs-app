# feature:form

Dynamic form configuration engine. Allows workspaces to define custom fields and validation rules for any entity type (customer, product, order, etc.) without requiring an app update.

## Responsibilities

- Fetch and cache entity field configs from the backend
- Store field schemas in a local Room database for offline use
- Render a form config UI for admins to add/edit custom fields
- Provide `EntityFieldConfig` and `EntityAttributeDefinition` to other modules

## Key Classes

| Class | Purpose |
|---|---|
| `ConfigApi` / `ConfigApiImpl` | REST endpoints for form schemas |
| `ConfigRepository` | Offline-first data access |
| `FormDatabase` | Room database for form configs |
| `EntityFieldConfigDao` | CRUD for field configs |
| `EntityAttributeDefinitionDao` | CRUD for attribute definitions |
| `FormConfigViewModel` | Admin form builder UI state |

## Domain Models

`EntityFieldConfig`, `EntityAttributeDefinition`, `EntityConfigSchema`, `SaveConfigSchemaRequest`, `EntityType`, `AttributeDataType`, `AttributeValidationType`, `ValidationParamKeys`, `DefaultFormConfigs`

## Koin Module

```kotlin
formModule          // in com.ampairs.form.di
formPlatformModule  // platform-specific (DB factory)
```

## Database

`FormDatabase` — workspace-scoped (`factory` scope).

Tables: `entity_field_config`, `entity_attribute_definition`
