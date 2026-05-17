# Tax System - Entity and Table Rename Summary

**Date:** December 4, 2025

## Rationale

Since `WorkspaceAwareDatabaseFactory` provides database isolation per workspace, the "Workspace" prefix in entity and table names is redundant. Removing it makes the code cleaner and more concise.

## Renames Completed

### Entities (3 files renamed)

| Old Name | New Name | Table Name Change |
|----------|----------|-------------------|
| `WorkspaceTaxCodeEntity` | `TaxCodeEntity` | `workspace_tax_codes` → `tax_codes` |
| `WorkspaceTaxComponentEntity` | `TaxComponentEntity` | `workspace_tax_components` → `tax_components` |
| `WorkspaceTaxConfigurationEntity` | `TaxConfigurationEntity` | `workspace_tax_configuration` → `tax_configuration` |

### DAOs (3 files renamed)

| Old Name | New Name | Method Renames |
|----------|----------|----------------|
| `WorkspaceTaxCodeDao` | `TaxCodeDao` | `observeWorkspaceTaxCodes()` → `observeTaxCodes()`<br>`searchWorkspaceTaxCodes()` → `searchTaxCodes()` |
| `WorkspaceTaxComponentDao` | `TaxComponentDao` | `observeWorkspaceComponents()` → `observeTaxComponents()` |
| `WorkspaceTaxConfigurationDao` | `TaxConfigurationDao` | No method renames needed |

### Extension Functions Updated

All extension functions updated to use new entity names:

```kotlin
// Old
fun WorkspaceTaxCodeEntity.toDomain(workspaceId: String): WorkspaceTaxCode
fun WorkspaceTaxCode.toEntity(): WorkspaceTaxCodeEntity

// New
fun TaxCodeEntity.toDomain(workspaceId: String): WorkspaceTaxCode
fun WorkspaceTaxCode.toEntity(): TaxCodeEntity
```

## Files Updated

### ✅ Entities (3 files)
- `WorkspaceTaxCodeEntity.kt` → `TaxCodeEntity.kt`
- `WorkspaceTaxComponentEntity.kt` → `TaxComponentEntity.kt`
- `WorkspaceTaxConfigurationEntity.kt` → `TaxConfigurationEntity.kt`

### ✅ DAOs (3 files)
- `WorkspaceTaxCodeDao.kt` → `TaxCodeDao.kt`
- `WorkspaceTaxComponentDao.kt` → `TaxComponentDao.kt`
- `WorkspaceTaxConfigurationDao.kt` → `TaxConfigurationDao.kt`

### ✅ Database (1 file)
- `TaxRoomDatabase.kt` - Updated to reference new entity and DAO names

### ✅ DI Module (1 file)
- `TaxModuleV2.kt` - Updated DAO factory definitions

## Domain Models (Not Renamed)

These keep their "Workspace" prefix because they represent API DTOs:

- `WorkspaceTaxCode` - Still used for API communication
- `WorkspaceTaxComponent` - Still used for API communication
- `WorkspaceTaxConfiguration` - Still used for API communication

## SQL Query Changes

All SQL queries updated to use new table names:

```sql
-- Old
SELECT * FROM workspace_tax_codes WHERE is_active = 1
SELECT * FROM workspace_tax_components WHERE jurisdiction = :jurisdiction
SELECT * FROM workspace_tax_configuration LIMIT 1

-- New
SELECT * FROM tax_codes WHERE is_active = 1
SELECT * FROM tax_components WHERE jurisdiction = :jurisdiction
SELECT * FROM tax_configuration LIMIT 1
```

## Database Schema Impact

**Version:** Remains at v2 (no additional migration needed - fresh implementation)

**Tables Created:**
- `tax_codes` (renamed from `workspace_tax_codes`)
- `tax_components` (renamed from `workspace_tax_components`)
- `tax_configuration` (renamed from `workspace_tax_configuration`)
- `tax_component_types` (unchanged)
- `tax_rules` (unchanged)

## Code Quality Improvements

### Before (Verbose):
```kotlin
@Entity(tableName = "workspace_tax_codes")
data class WorkspaceTaxCodeEntity(...)

val dao: WorkspaceTaxCodeDao
dao.observeWorkspaceTaxCodes()
```

### After (Clean):
```kotlin
@Entity(tableName = "tax_codes")
data class TaxCodeEntity(...)

val dao: TaxCodeDao
dao.observeTaxCodes()
```

## Breaking Changes

### Repository Layer
Repositories need to be updated to use new DAO method names:
- `dao.observeWorkspaceTaxCodes()` → `dao.observeTaxCodes()`
- `dao.searchWorkspaceTaxCodes()` → `dao.searchTaxCodes()`
- `dao.observeWorkspaceComponents()` → `dao.observeTaxComponents()`

### No Changes Needed
- API layer (unchanged - still uses Workspace domain models)
- ViewModels (unchanged - uses repositories)
- UI screens (unchanged - uses ViewModels)

## Testing Checklist

- [x] Verify all entity references updated
- [x] Verify all DAO references updated
- [x] Verify database schema creates correct tables
- [ ] Test workspace switching (data isolation)
- [x] Update repositories to use new DAO method names
- [x] Update API implementation to remove workspaceId parameters
- [x] Run full compilation test

---

**Status:** ✅ Complete - All entity, DAO, repository, and API updates finished
**Completed:** December 5, 2025

## Additional Changes (December 5, 2025)

### Repository Updates
- ✅ `TaxCodeRepository.kt` - Already using new DAO method names (no changes needed)
- ✅ `TaxComponentRepository.kt` - Updated constructor parameter and all method calls from `workspaceTaxComponentDao` to `taxComponentDao`
- ✅ `TaxConfigurationRepository.kt` - Updated to remove `WorkspaceContextManager` dependency and simplified all method signatures

### API Layer Updates
All workspace-scoped endpoints in `TaxConfigurationApiImpl.kt` updated to remove workspace ID from URLs:
- `v1/workspaces/$workspaceId/configuration` → `v1/configuration`
- `v1/workspaces/$workspaceId/workspace-codes` → `v1/workspace-codes`
- `v1/workspaces/$workspaceId/workspace-components` → `v1/workspace-components`
- `v1/workspaces/$workspaceId/tax-rules` → `v1/tax-rules`

**Rationale:** Workspace context is now handled at the HTTP client level via headers, eliminating the need for workspace ID in URL paths.
