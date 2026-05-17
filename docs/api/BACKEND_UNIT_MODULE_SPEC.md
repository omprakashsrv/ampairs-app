# Backend Unit Module Implementation Specification

## Overview

This document provides complete specifications for implementing the Unit Management Module on the Spring Boot backend to support the mobile app's unit management and conversion features.

---

## Table of Contents

1. [Database Schema](#database-schema)
2. [API Endpoints](#api-endpoints)
3. [Request/Response DTOs](#requestresponse-dtos)
4. [Business Logic Requirements](#business-logic-requirements)
5. [Validation Rules](#validation-rules)
6. [Multi-Tenancy & Security](#multi-tenancy--security)
7. [Sync & Pagination](#sync--pagination)
8. [Error Handling](#error-handling)

---

## 1. Database Schema

### Domain Model Pattern

**IMPORTANT**: All entities must extend `OwnableBaseDomain` base class.

`OwnableBaseDomain` provides:
- `id: String` - Primary key (UID)
- `owner_id: String` - Owner isolation (workspace/tenant)
- `created_at: Timestamp` - Creation timestamp
- `updated_at: Timestamp` - Last update timestamp
- `created_by: String` - User who created
- `updated_by: String` - User who last updated
- `active: Boolean` - Soft delete flag

**Implementation**: Entity classes should only define domain-specific fields. Base fields are inherited.

---

### 1.1 Units Table

**Table Name**: `units`

**Extends**: `OwnableBaseDomain` (inherits: id, owner_id, created_at, updated_at, created_by, updated_by, active)

```sql
CREATE TABLE units (
    id VARCHAR(32) PRIMARY KEY,                    -- UID (UNT prefix)
    owner_id VARCHAR(32) NOT NULL,                 -- Owner isolation (workspace/tenant)
    name VARCHAR(100) NOT NULL,                    -- Unit name (e.g., "Kilogram")
    short_name VARCHAR(20) NOT NULL,               -- Symbol (e.g., "kg")
    decimal_places INT NOT NULL DEFAULT 2,         -- Precision (0-10)
    description TEXT,                              -- Optional description
    category VARCHAR(50),                          -- Category (e.g., "Weight", "Volume")
    active BOOLEAN NOT NULL DEFAULT TRUE,          -- Active status
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(32),                        -- User who created
    updated_by VARCHAR(32),                        -- User who last updated

    -- Indexes
    INDEX idx_units_owner (owner_id),
    INDEX idx_units_name (owner_id, name),
    INDEX idx_units_active (owner_id, active),
    INDEX idx_units_updated (owner_id, updated_at),

    -- Unique constraint per owner
    UNIQUE KEY uk_units_owner_name (owner_id, name),
    UNIQUE KEY uk_units_owner_short_name (owner_id, short_name)
);
```

### 1.2 Unit Conversions Table

**Table Name**: `unit_conversions`

**Domain Pattern**: Also extends `OwnableBaseDomain` for consistency

```sql
CREATE TABLE unit_conversions (
    id VARCHAR(32) PRIMARY KEY,                    -- UID (UCN prefix)
    owner_id VARCHAR(32) NOT NULL,                 -- Owner isolation (workspace/tenant)
    product_id VARCHAR(32) NOT NULL,               -- Product this conversion applies to
    base_unit_id VARCHAR(32) NOT NULL,             -- Base unit (e.g., PCS)
    derived_unit_id VARCHAR(32) NOT NULL,          -- Derived unit (e.g., BOX)
    multiplier DECIMAL(20, 6) NOT NULL,            -- Conversion factor
    active BOOLEAN NOT NULL DEFAULT TRUE,          -- Active status
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(32),
    updated_by VARCHAR(32),

    -- Foreign Keys
    FOREIGN KEY (base_unit_id) REFERENCES units(id),
    FOREIGN KEY (derived_unit_id) REFERENCES units(id),
    FOREIGN KEY (product_id) REFERENCES products(id),

    -- Indexes
    INDEX idx_conversions_owner (owner_id),
    INDEX idx_conversions_product (product_id),
    INDEX idx_conversions_base (base_unit_id),
    INDEX idx_conversions_derived (derived_unit_id),
    INDEX idx_conversions_updated (owner_id, updated_at),

    -- Unique constraint: one conversion per product-base-derived combo
    UNIQUE KEY uk_conversion_product_units (owner_id, product_id, base_unit_id, derived_unit_id)
);
```

**Important Notes**:
- Use `VARCHAR(32)` for IDs (supports mobile-generated UIDs)
- Use `DECIMAL(20, 6)` for multiplier to avoid floating-point precision issues
- `updated_at` must be updated on every modification (for sync)
- Add triggers to auto-update `updated_at` timestamp

---

## 2. API Endpoints

### Base URL
```
/unit/v1
```

All endpoints require:
- **Authentication**: Bearer JWT token
- **Headers**:
  - `Authorization: Bearer {token}`
  - `X-Workspace-ID: {workspace_id}` (for multi-tenant context)

---

### 2.1 GET /unit/v1

**Description**: Get paginated list of units with optional filtering and sync support

**Query Parameters**:
```
?page=0                     // Page number (0-indexed)
&size=100                   // Items per page (default: 100, max: 1000)
&lastSyncTime=              // ISO 8601 timestamp for incremental sync (optional)
&sortBy=updatedAt           // Sort field (name, shortName, updatedAt)
&sortDirection=ASC          // Sort direction (ASC, DESC)
&active=true                // Filter by active status (optional)
&category=Weight            // Filter by category (optional)
&search=kg                  // Search in name/shortName (optional)
```

**Response**: `200 OK`
```json
{
  "data": {
    "content": [
      {
        "uid": "UNT20250118123456ABC123XYZ789",
        "name": "Kilogram",
        "short_name": "kg",
        "decimal_places": 3,
        "description": "Metric unit of mass",
        "category": "Weight",
        "active": true,
        "created_at": "2025-01-18T12:34:56",
        "updated_at": "2025-01-18T12:34:56"
      }
    ],
    "page": 0,
    "size": 100,
    "total_elements": 150,
    "total_pages": 2,
    "has_next": true,
    "has_previous": false
  },
  "error": null,
  "timestamp": "2025-01-18T14:30:00"
}
```

**Sync Behavior**:
- If `lastSyncTime` provided: Return only units modified after that timestamp
- Sort by `updated_at ASC` for predictable sync ordering
- Mobile uses this for batch sync (100 items per page, max 10,000 records)

---

### 2.2 POST /unit/v1/search

**Description**: Search units with advanced filtering (alternative to GET with complex filters)

**Request Body**:
```json
{
  "query": "kilo",           // Search term (name, short_name)
  "categories": ["Weight"],  // Filter by categories
  "active": true,            // Filter by active status
  "page": 0,
  "size": 50
}
```

**Response**: Same pagination format as GET

---

### 2.3 GET /unit/v1/{id}

**Description**: Get single unit by ID

**Path Parameters**:
- `id`: Unit UID

**Response**: `200 OK`
```json
{
  "data": {
    "uid": "UNT20250118123456ABC123XYZ789",
    "name": "Kilogram",
    "short_name": "kg",
    "decimal_places": 3,
    "description": "Metric unit of mass",
    "category": "Weight",
    "active": true,
    "created_at": "2025-01-18T12:34:56",
    "updated_at": "2025-01-18T12:34:56"
  },
  "error": null,
  "timestamp": "2025-01-18T14:30:00"
}
```

**Error Responses**:
- `404 Not Found`: Unit not found
- `403 Forbidden`: Unit belongs to different tenant

---

### 2.4 POST /unit/v1

**Description**: Create new unit

**Request Body**:
```json
{
  "uid": "UNT20250118123456ABC123XYZ789",  // Client-generated (32 chars)
  "name": "Kilogram",
  "short_name": "kg",
  "decimal_places": 3,
  "description": "Metric unit of mass",
  "category": "Weight",
  "active": true
}
```

**Important**:
- `uid` is **client-generated** on mobile (offline-first pattern)
- Backend must accept and preserve the provided `uid`
- If `uid` conflicts, return `409 Conflict` error

**Response**: `201 Created`
```json
{
  "data": {
    "uid": "UNT20250118123456ABC123XYZ789",
    "name": "Kilogram",
    "short_name": "kg",
    "decimal_places": 3,
    "description": "Metric unit of mass",
    "category": "Weight",
    "active": true,
    "created_at": "2025-01-18T12:34:56",
    "updated_at": "2025-01-18T12:34:56"
  },
  "error": null,
  "timestamp": "2025-01-18T14:30:00"
}
```

**Error Responses**:
- `400 Bad Request`: Validation errors (see validation section)
- `409 Conflict`: Unit with same UID, name, or short_name already exists
- `422 Unprocessable Entity`: Business rule violations

---

### 2.5 PUT /unit/v1/{id}

**Description**: Update existing unit

**Path Parameters**:
- `id`: Unit UID

**Request Body**: Same as POST (all fields)

**Response**: `200 OK` (same format as GET)

**Notes**:
- Must update `updated_at` timestamp
- Cannot change `uid` after creation
- Name/short_name must remain unique within tenant

---

### 2.6 DELETE /unit/v1/{id}

**Description**: Soft delete unit (set active = false)

**Path Parameters**:
- `id`: Unit UID

**Response**: `200 OK`
```json
{
  "data": {
    "uid": "UNT20250118123456ABC123XYZ789",
    "active": false,
    "updated_at": "2025-01-18T15:00:00"
  },
  "error": null,
  "timestamp": "2025-01-18T15:00:00"
}
```

**Business Rules**:
- Do NOT hard delete (preserve historical data)
- Set `active = false`
- Update `updated_at` timestamp
- Prevent deletion if unit is referenced in active products/conversions

---

### 2.7 Unit Conversions API

#### GET /unit/v1/conversions

Get paginated unit conversions

**Query Parameters**:
```
?productId=PROD001          // Filter by product (optional)
&baseUnitId=UNT001          // Filter by base unit (optional)
&page=0
&size=100
&lastSyncTime=              // ISO 8601 for sync
```

**Response**: Similar pagination format with UnitConversion objects

---

#### POST /unit/v1/conversions

Create unit conversion

**Request Body**:
```json
{
  "uid": "UCN20250118123456ABC123XYZ789",
  "product_id": "PROD001",
  "base_unit_id": "UNT001",      // PCS
  "derived_unit_id": "UNT002",   // BOX
  "multiplier": 24.0,
  "active": true
}
```

**Validation**:
- `multiplier` must be > 0
- `base_unit_id` ≠ `derived_unit_id`
- Both units must exist and be active
- Product must exist
- Unique constraint: (product_id, base_unit_id, derived_unit_id)

**Response**: `201 Created` with full conversion object

---

#### PUT /unit/v1/conversions/{id}

Update conversion (change multiplier, active status)

---

#### DELETE /unit/v1/conversions/{id}

Soft delete conversion (set active = false)

---

#### POST /unit/v1/conversions/convert

**Description**: Convert quantity between units for a product

**Request Body**:
```json
{
  "product_id": "PROD001",
  "from_unit_id": "UNT002",    // BOX
  "to_unit_id": "UNT001",      // PCS
  "quantity": 2.0
}
```

**Response**: `200 OK`
```json
{
  "data": {
    "original_quantity": 2.0,
    "original_unit_id": "UNT002",
    "converted_quantity": 48.0,
    "converted_unit_id": "UNT001",
    "multiplier": 24.0
  },
  "error": null,
  "timestamp": "2025-01-18T14:30:00"
}
```

---

## 3. Request/Response DTOs

### 3.1 UnitDTO (Java/Kotlin)

```kotlin
@Serializable
data class UnitDTO(
    @SerialName("uid")
    val uid: String,

    @SerialName("name")
    val name: String,

    @SerialName("short_name")
    val shortName: String,

    @SerialName("decimal_places")
    val decimalPlaces: Int,

    @SerialName("description")
    val description: String? = null,

    @SerialName("category")
    val category: String? = null,

    @SerialName("active")
    val active: Boolean = true,

    @SerialName("created_at")
    val createdAt: String,  // ISO 8601 timestamp

    @SerialName("updated_at")
    val updatedAt: String   // ISO 8601 timestamp
)
```

### 3.2 UnitConversionDTO

```kotlin
@Serializable
data class UnitConversionDTO(
    @SerialName("uid")
    val uid: String,

    @SerialName("product_id")
    val productId: String,

    @SerialName("base_unit_id")
    val baseUnitId: String,

    @SerialName("derived_unit_id")
    val derivedUnitId: String,

    @SerialName("multiplier")
    val multiplier: Double,

    @SerialName("active")
    val active: Boolean = true,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String
)
```

### 3.3 PageResponseDTO

```kotlin
@Serializable
data class PageResponseDTO<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    @SerialName("total_elements")
    val totalElements: Long,
    @SerialName("total_pages")
    val totalPages: Int,
    @SerialName("has_next")
    val hasNext: Boolean,
    @SerialName("has_previous")
    val hasPrevious: Boolean
)
```

### 3.4 ApiResponseDTO

```kotlin
@Serializable
data class ApiResponseDTO<T>(
    val data: T?,
    val error: ErrorDTO?,
    val timestamp: String  // ISO 8601
)

@Serializable
data class ErrorDTO(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null
)
```

---

## 4. Business Logic Requirements

### 4.1 UID Generation Strategy

**Mobile-First Pattern**:
- Mobile generates UIDs offline using format: `{PREFIX}{YYYYMMDDHHMMSS}{RANDOM}`
- Backend **MUST accept** client-provided UIDs
- Backend should NOT regenerate UIDs on conflict (return 409 instead)

**Prefix Standards**:
- Units: `UNT`
- Unit Conversions: `UCN`

**Example UID**: `UNT20250118143056ABC123XYZ789DEF456` (32 characters)

### 4.2 Timestamp Format

**Use ISO 8601** string format for all timestamps:
```
Format: "yyyy-MM-dd'T'HH:mm:ss"
Example: "2025-01-18T14:30:56"
```

**Why strings, not Unix timestamps?**:
- Natural string comparison for sync: `"2025-01-18T15:00:00" > "2025-01-18T14:00:00"`
- No timezone confusion
- Database-agnostic

### 4.3 Sync Logic

**Server-Authoritative Timestamps**:
- Always use server's `updated_at` timestamp (not client-provided)
- Mobile tracks last sync time from server response
- Next sync requests: `lastSyncTime=<server_updated_at>`

**Batch Sync Pattern**:
```
1. Mobile: GET /units?lastSyncTime=2025-01-18T10:00:00&page=0&size=100&sortBy=updatedAt&sortDirection=ASC
2. Server: Return units modified after 10:00:00, sorted by updated_at ASC
3. Mobile: Process page, track max(updated_at) from response
4. Mobile: If hasNext=true, request page=1
5. Repeat until hasNext=false
6. Mobile saves max(updated_at) as new lastSyncTime
```

### 4.4 Conflict Resolution

**UID Conflicts**:
- If mobile sends UID that already exists: return `409 Conflict`
- Mobile should handle by accepting server's entity (local UID was offline-generated)

**Data Conflicts** (same entity modified on mobile + server):
- Last-Write-Wins strategy
- Server's `updated_at` is authoritative
- Mobile marks conflicts and re-syncs

### 4.5 Conversion Calculation

**Formula**: `derived_quantity = base_quantity * multiplier`

**Example**:
```
Product: Soap Bars
Base Unit: PCS
Derived Unit: BOX
Multiplier: 24.0

Conversion:
2 BOX → ? PCS
= 2 * 24.0 = 48 PCS
```

**Inverse Conversion**:
```
48 PCS → ? BOX
= 48 / 24.0 = 2 BOX
```

**Edge Cases**:
- If no conversion exists: return error or null
- Support both directions (BOX→PCS and PCS→BOX)

---

## 5. Validation Rules

### 5.1 Unit Validation

**Required Fields**:
- `uid`: Required, exactly 32 characters, must start with "UNT"
- `name`: Required, 1-100 characters
- `short_name`: Required, 1-20 characters
- `decimal_places`: Required, 0-10

**Business Rules**:
- `name` unique per tenant (case-insensitive)
- `short_name` unique per tenant (case-insensitive)
- `decimal_places`: Must be 0-10
- `category`: Max 50 characters (optional)
- `description`: Max 1000 characters (optional)

**Error Response Example**:
```json
{
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": {
      "name": "Unit name already exists",
      "decimal_places": "Must be between 0 and 10"
    }
  },
  "timestamp": "2025-01-18T14:30:00"
}
```

### 5.2 Unit Conversion Validation

**Required Fields**:
- All fields required

**Business Rules**:
- `multiplier` > 0
- `base_unit_id` ≠ `derived_unit_id`
- Both units must exist and belong to same tenant
- Product must exist and belong to same tenant
- Unique: (product_id, base_unit_id, derived_unit_id) per tenant

**Validation Errors**:
- `INVALID_MULTIPLIER`: "Multiplier must be greater than 0"
- `SAME_UNITS`: "Base unit and derived unit cannot be the same"
- `UNIT_NOT_FOUND`: "Unit with ID {id} not found"
- `PRODUCT_NOT_FOUND`: "Product with ID {id} not found"
- `DUPLICATE_CONVERSION`: "Conversion already exists for this product and unit pair"

---

## 6. Multi-Tenancy & Security

### 6.1 Tenant Isolation

**Every query must filter by owner_id**:
```sql
-- Good
SELECT * FROM units WHERE owner_id = ? AND name = ?

-- Bad (security vulnerability)
SELECT * FROM units WHERE name = ?
```

**Extract tenant from**:
- JWT token claims
- `X-Workspace-ID` header (validated against user's workspaces)

**Enforce**:
- Row-level security at database level (if supported)
- Service layer checks before every DB operation
- Unit tests for cross-tenant access prevention

### 6.2 Authorization

**Permission Matrix**:

| Role         | Create | Read | Update | Delete |
|--------------|--------|------|--------|--------|
| Admin        | ✅     | ✅   | ✅     | ✅     |
| Manager      | ✅     | ✅   | ✅     | ❌     |
| User         | ❌     | ✅   | ❌     | ❌     |

**Implement**:
- Spring Security annotations: `@PreAuthorize("hasRole('ADMIN')")`
- Custom permission checks for tenant ownership

---

## 7. Sync & Pagination

### 7.1 Pagination Standards

**Default**: 100 items per page
**Maximum**: 1000 items per page
**Safety Limit**: 10,000 total records per sync

**Query Optimization**:
```sql
-- Use indexed columns for sorting
SELECT * FROM units
WHERE owner_id = ?
  AND updated_at > ?
ORDER BY updated_at ASC
LIMIT ? OFFSET ?;
```

### 7.2 Incremental Sync

**Mobile Sync Flow**:
```
1. Push local unsynced changes to server
   POST /units (for new units)
   PUT /units/{id} (for updated units)

2. Pull server changes since last sync
   GET /units?lastSyncTime=2025-01-18T10:00:00

3. Save server's max(updated_at) as new lastSyncTime
```

**Server Requirements**:
- Sort by `updated_at ASC` for consistent ordering
- Return same entity only once across pages
- Handle deleted entities (active=false) in sync

---

## 8. Error Handling

### 8.1 HTTP Status Codes

| Status | Use Case                          |
|--------|-----------------------------------|
| 200    | Success (GET, PUT, DELETE)        |
| 201    | Created (POST)                    |
| 400    | Bad Request (validation errors)   |
| 401    | Unauthorized (invalid token)      |
| 403    | Forbidden (wrong tenant)          |
| 404    | Not Found                         |
| 409    | Conflict (duplicate UID/name)     |
| 422    | Unprocessable (business rule)     |
| 500    | Internal Server Error             |

### 8.2 Error Response Format

**Consistent Structure**:
```json
{
  "data": null,
  "error": {
    "code": "UNIT_NOT_FOUND",
    "message": "Unit with ID UNT123 not found",
    "details": {
      "requested_id": "UNT123"
    }
  },
  "timestamp": "2025-01-18T14:30:00"
}
```

**Error Codes**:
- `VALIDATION_ERROR`: Field validation failures
- `UNIT_NOT_FOUND`: Unit doesn't exist
- `DUPLICATE_UNIT`: Name/short_name already exists
- `DUPLICATE_UID`: UID already exists (409)
- `UNIT_IN_USE`: Cannot delete (referenced by products)
- `CONVERSION_NOT_FOUND`: No conversion exists
- `INVALID_MULTIPLIER`: Multiplier validation failed
- `UNAUTHORIZED`: Invalid/expired token
- `FORBIDDEN`: Tenant mismatch

---

## 9. Testing Requirements

### 9.1 Unit Tests

**Coverage Requirements**: Minimum 80%

**Test Cases**:
- Create unit with valid data
- Create unit with duplicate name → 409
- Create unit with duplicate UID → 409
- Update unit - normal flow
- Update unit - change name to existing → 409
- Delete unit with no references
- Delete unit with product references → 422
- Get units with pagination
- Get units with lastSyncTime filter
- Search units by name/shortName
- Tenant isolation (cross-tenant access blocked)

### 9.2 Integration Tests

**Scenarios**:
- Full sync flow (push local → pull server)
- Batch sync with 500 units (5 pages of 100)
- Concurrent updates (race conditions)
- UID conflict handling
- Conversion calculations (direct + inverse)

### 9.3 Performance Tests

**Benchmarks**:
- Get 1000 units: < 500ms
- Create unit: < 100ms
- Batch sync 10,000 units: < 10 seconds
- Convert units: < 50ms

---

## 10. Migration & Deployment

### 10.1 Database Migration Script

**Flyway/Liquibase Script**:

```sql
-- V1__create_units_tables.sql

-- Units table
CREATE TABLE units (
    id VARCHAR(32) PRIMARY KEY,
    owner_id VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL,
    short_name VARCHAR(20) NOT NULL,
    decimal_places INT NOT NULL DEFAULT 2,
    description TEXT,
    category VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(32),
    updated_by VARCHAR(32),

    INDEX idx_units_tenant (owner_id),
    INDEX idx_units_name (owner_id, name),
    INDEX idx_units_active (owner_id, active),
    INDEX idx_units_updated (owner_id, updated_at),

    UNIQUE KEY uk_units_tenant_name (owner_id, name),
    UNIQUE KEY uk_units_tenant_short_name (owner_id, short_name)
);

-- Unit conversions table
CREATE TABLE unit_conversions (
    id VARCHAR(32) PRIMARY KEY,
    owner_id VARCHAR(32) NOT NULL,
    product_id VARCHAR(32) NOT NULL,
    base_unit_id VARCHAR(32) NOT NULL,
    derived_unit_id VARCHAR(32) NOT NULL,
    multiplier DECIMAL(20, 6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(32),
    updated_by VARCHAR(32),

    FOREIGN KEY (base_unit_id) REFERENCES units(id),
    FOREIGN KEY (derived_unit_id) REFERENCES units(id),
    FOREIGN KEY (product_id) REFERENCES products(id),

    INDEX idx_conversions_tenant (owner_id),
    INDEX idx_conversions_product (product_id),
    INDEX idx_conversions_base (base_unit_id),
    INDEX idx_conversions_derived (derived_unit_id),
    INDEX idx_conversions_updated (owner_id, updated_at),

    UNIQUE KEY uk_conversion_product_units (owner_id, product_id, base_unit_id, derived_unit_id)
);
```

### 10.2 Seed Data (Optional)

**Common Units**:
```sql
INSERT INTO units (id, owner_id, name, short_name, decimal_places, category, active) VALUES
('UNT_SEED_001', 'DEFAULT', 'Pieces', 'PCS', 0, 'Count', TRUE),
('UNT_SEED_002', 'DEFAULT', 'Kilogram', 'KG', 3, 'Weight', TRUE),
('UNT_SEED_003', 'DEFAULT', 'Gram', 'GM', 3, 'Weight', TRUE),
('UNT_SEED_004', 'DEFAULT', 'Liter', 'L', 3, 'Volume', TRUE),
('UNT_SEED_005', 'DEFAULT', 'Milliliter', 'ML', 3, 'Volume', TRUE),
('UNT_SEED_006', 'DEFAULT', 'Box', 'BOX', 0, 'Count', TRUE),
('UNT_SEED_007', 'DEFAULT', 'Dozen', 'DZN', 0, 'Count', TRUE);
```

---

## 11. API Documentation (Swagger/OpenAPI)

**Generate using**:
- Springdoc OpenAPI
- Swagger annotations

**Example Swagger Config**:
```kotlin
@Tag(name = "Units", description = "Unit management and conversion APIs")
@RestController
@RequestMapping("/unit/v1")
class UnitController {

    @Operation(
        summary = "Get all units",
        description = "Returns paginated list of units with optional filtering and sync support"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    @GetMapping
    fun getUnits(
        @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "100") size: Int,
        @Parameter(description = "Last sync timestamp (ISO 8601)") @RequestParam(required = false) lastSyncTime: String?
    ): ResponseEntity<ApiResponseDTO<PageResponseDTO<UnitDTO>>>
}
```

---

## 12. Implementation Checklist

### Phase 1: Database & Entities
- [ ] Create database tables with migrations
- [ ] Create JPA entities (Unit, UnitConversion)
- [ ] Add owner_id filtering to all queries
- [ ] Create database indexes
- [ ] Add seed data (optional)

### Phase 2: DTOs & Mappers
- [ ] Create UnitDTO
- [ ] Create UnitConversionDTO
- [ ] Create PageResponseDTO<T>
- [ ] Create ApiResponseDTO<T>
- [ ] Implement entity ↔ DTO mappers

### Phase 3: Repositories
- [ ] Create UnitRepository with custom queries
- [ ] Create UnitConversionRepository
- [ ] Add pagination support
- [ ] Add lastSyncTime filtering

### Phase 4: Services
- [ ] Implement UnitService (CRUD operations)
- [ ] Implement UnitConversionService
- [ ] Add validation logic
- [ ] Add business rules (unique names, etc.)
- [ ] Implement conversion calculation

### Phase 5: Controllers
- [ ] Create UnitController with all endpoints
- [ ] Create UnitConversionController
- [ ] Add exception handlers
- [ ] Add Swagger annotations

### Phase 6: Security
- [ ] Add tenant isolation checks
- [ ] Add role-based permissions
- [ ] Add JWT validation
- [ ] Test cross-tenant access prevention

### Phase 7: Testing
- [ ] Write unit tests (80% coverage)
- [ ] Write integration tests
- [ ] Test sync flow end-to-end
- [ ] Test UID conflict handling
- [ ] Performance testing

### Phase 8: Documentation
- [ ] Generate Swagger/OpenAPI docs
- [ ] Write API usage examples
- [ ] Document error codes
- [ ] Create Postman collection

---

## 13. Example Implementation (Spring Boot + Kotlin)

### UnitService.kt

```kotlin
@Service
class UnitService(
    private val unitRepository: UnitRepository,
    private val ownerContext: OwnerContext
) {

    fun getUnits(
        page: Int,
        size: Int,
        lastSyncTime: String?,
        sortBy: String,
        sortDirection: Sort.Direction
    ): Page<Unit> {
        val ownerId = ownerContext.getCurrentOwnerId()
        val pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy))

        return if (lastSyncTime != null) {
            val syncTime = LocalDateTime.parse(lastSyncTime)
            unitRepository.findByOwnerIdAndUpdatedAtAfter(ownerId, syncTime, pageable)
        } else {
            unitRepository.findByOwnerId(ownerId, pageable)
        }
    }

    fun createUnit(dto: UnitDTO): Unit {
        val ownerId = ownerContext.getCurrentOwnerId()

        // Validate UID uniqueness
        if (unitRepository.existsByIdAndOwnerId(dto.uid, ownerId)) {
            throw DuplicateUidException("Unit with UID ${dto.uid} already exists")
        }

        // Validate name uniqueness (case-insensitive)
        if (unitRepository.existsByOwnerIdAndNameIgnoreCase(ownerId, dto.name)) {
            throw DuplicateUnitException("Unit with name '${dto.name}' already exists")
        }

        val unit = dto.toEntity().copy(
            ownerId = ownerId,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        return unitRepository.save(unit)
    }

    fun updateUnit(id: String, dto: UnitDTO): Unit {
        val ownerId = ownerContext.getCurrentOwnerId()

        val existing = unitRepository.findByIdAndOwnerId(id, ownerId)
            ?: throw UnitNotFoundException("Unit with ID $id not found")

        // Validate name uniqueness (excluding current unit)
        if (dto.name != existing.name &&
            unitRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(ownerId, dto.name, id)) {
            throw DuplicateUnitException("Unit with name '${dto.name}' already exists")
        }

        val updated = existing.copy(
            name = dto.name,
            shortName = dto.shortName,
            decimalPlaces = dto.decimalPlaces,
            description = dto.description,
            category = dto.category,
            active = dto.active,
            updatedAt = LocalDateTime.now()
        )

        return unitRepository.save(updated)
    }

    fun deleteUnit(id: String): Unit {
        val ownerId = ownerContext.getCurrentOwnerId()

        val unit = unitRepository.findByIdAndOwnerId(id, ownerId)
            ?: throw UnitNotFoundException("Unit with ID $id not found")

        // Check if unit is referenced by products or conversions
        if (unitRepository.isUnitInUse(id)) {
            throw UnitInUseException("Cannot delete unit that is in use")
        }

        val deleted = unit.copy(
            active = false,
            updatedAt = LocalDateTime.now()
        )

        return unitRepository.save(deleted)
    }
}
```

---

## 14. Mobile-Backend Sync Contract

### Critical Requirements

1. **Client-Generated UIDs**
   - Mobile generates UIDs offline
   - Backend MUST accept and preserve client UIDs
   - On conflict, return 409 (don't regenerate)

2. **Timestamp Consistency**
   - Use ISO 8601 string format
   - Server is authoritative for `updated_at`
   - Mobile uses server timestamps for sync

3. **Batch Sync**
   - Default page size: 100
   - Sort by `updated_at ASC`
   - Mobile loops until `hasNext = false`

4. **Offline-First**
   - Mobile saves locally first (synced=false)
   - Background sync with server
   - Graceful fallback on sync failure

5. **Conflict Resolution**
   - Last-Write-Wins (server timestamp wins)
   - Mobile marks conflicts for user review

---

## 15. Support & Contact

**For Implementation Questions**:
- Mobile Team Lead: [Contact Info]
- Backend Team Lead: [Contact Info]
- API Documentation: [Swagger URL]
- Code Repository: [Git URL]

---

## Appendix A: Sample cURL Commands

### Create Unit
```bash
curl -X POST https://api.ampairs.com/unit/v1 \
  -H "Authorization: Bearer {token}" \
  -H "X-Workspace-ID: {workspace_id}" \
  -H "Content-Type: application/json" \
  -d '{
    "uid": "UNT20250118143056ABC123XYZ789",
    "name": "Kilogram",
    "short_name": "kg",
    "decimal_places": 3,
    "category": "Weight",
    "active": true
  }'
```

### Get Units (Sync)
```bash
curl -X GET "https://api.ampairs.com/unit/v1?lastSyncTime=2025-01-18T10:00:00&page=0&size=100&sortBy=updatedAt&sortDirection=ASC" \
  -H "Authorization: Bearer {token}" \
  -H "X-Workspace-ID: {workspace_id}"
```

### Convert Units
```bash
curl -X POST https://api.ampairs.com/unit/v1/conversions/convert \
  -H "Authorization: Bearer {token}" \
  -H "X-Workspace-ID: {workspace_id}" \
  -H "Content-Type: application/json" \
  -d '{
    "product_id": "PROD001",
    "from_unit_id": "UNT002",
    "to_unit_id": "UNT001",
    "quantity": 2.0
  }'
```

---

**Document Version**: 1.0
**Last Updated**: 2025-01-18
**Author**: Mobile Development Team
**Review Status**: Ready for Backend Implementation
