# Tax Module Backend API Implementation Guide

## Overview

This guide provides complete API specifications for implementing the Tax Module V2 backend in Spring Boot. The mobile app expects these endpoints to support offline-first architecture with multi-country tax calculations.

## Architecture Requirements

### Core Principles
1. **Multi-Tenant**: All endpoints workspace-scoped with `X-Workspace-ID` header
2. **Country-Based**: Tax configurations and codes filtered by workspace country
3. **Offline-First Support**: Incremental sync with `modifiedAfter` timestamps
4. **Master/Workspace Pattern**: Global master tax codes + workspace subscriptions

### Technology Stack
- **Framework**: Spring Boot 3.x with Kotlin
- **Security**: JWT authentication with workspace context
- **Database**: PostgreSQL with multi-tenant isolation
- **API Style**: RESTful with standard `ApiResponse<T>` wrapper

### Naming Convention
**All endpoints follow singular resource naming:**

| Resource Type | Endpoint Pattern |
|--------------|------------------|
| Master Tax Code | `/api/v1/tax/master-code/*` |
| Workspace Tax Code | `/api/v1/workspaces/{id}/tax/code/*` |
| Tax Rule | `/api/v1/workspaces/{id}/tax/rule/*` |
| Tax Component | `/api/v1/workspaces/{id}/tax/component/*` |

---

## 1. Tax Configuration APIs

### 1.1 Get Workspace Tax Configuration

**Endpoint**: `GET /api/v1/workspaces/{workspaceId}/tax/configuration`

**Description**: Retrieve workspace-level tax configuration including country, strategy, and default settings.

**Request Headers**:
```
Authorization: Bearer {jwt_token}
X-Workspace-ID: {workspace_id}
```

**Response**:
```kotlin
data class WorkspaceTaxConfigurationDto(
    val id: String,
    val workspaceId: String,
    val countryCode: String,              // ISO 3166-1 alpha-2 (IN, US, GB, etc.)
    val taxStrategy: String,              // INDIA_GST, USA_SALES_TAX, UK_VAT, etc.
    val defaultTaxCodeSystem: String,     // HSN_CODE, SAC_CODE, TAX_CATEGORY
    val taxJurisdictions: List<String>,   // ["MH", "GJ", "DL"]
    val industry: String?,                // RETAIL_GROCERY, SERVICES, etc.
    val autoSubscribeNewCodes: Boolean,   // Auto-add popular codes
    val syncedAt: Long,
    val metadata: Map<String, String>
)
```

**Example Response**:
```json
{
  "success": true,
  "data": {
    "id": "WTC_WS001",
    "workspaceId": "WS_001",
    "countryCode": "IN",
    "taxStrategy": "INDIA_GST",
    "defaultTaxCodeSystem": "HSN_CODE",
    "taxJurisdictions": ["MH", "GJ", "DL"],
    "industry": "RETAIL_GROCERY",
    "autoSubscribeNewCodes": true,
    "syncedAt": 1733270400000,
    "metadata": {}
  },
  "error": null
}
```

**Spring Boot Implementation**:
```kotlin
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/tax")
class TaxConfigurationController(
    private val taxConfigService: TaxConfigurationService
) {

    @GetMapping("/configuration")
    fun getWorkspaceTaxConfiguration(
        @PathVariable workspaceId: String,
        @RequestHeader("X-Workspace-ID") headerWorkspaceId: String
    ): ApiResponse<WorkspaceTaxConfigurationDto> {
        // Validate workspace ID matches
        require(workspaceId == headerWorkspaceId) { "Workspace ID mismatch" }

        val config = taxConfigService.getConfiguration(workspaceId)
            ?: return ApiResponse.error("Tax configuration not found")

        return ApiResponse.success(config.toDto())
    }
}
```

---

### 1.2 Update Workspace Tax Configuration

**Endpoint**: `PUT /api/v1/workspaces/{workspaceId}/tax/configuration`

**Request Body**:
```json
{
  "countryCode": "IN",
  "taxStrategy": "INDIA_GST",
  "defaultTaxCodeSystem": "HSN_CODE",
  "taxJurisdictions": ["MH", "GJ", "DL"],
  "industry": "RETAIL_GROCERY"
}
```

**Response**: Same as GET configuration

---

## 2. Master Tax Code APIs

### 2.1 Search Master Tax Codes

**Endpoint**: `GET /api/v1/tax/master-code/search`

**Description**: Search global tax code registry filtered by country. This is the primary endpoint for mobile to discover tax codes.

**Query Parameters**:
- `query` (required): Search term (code or description)
- `countryCode` (required): ISO country code (IN, US, GB)
- `codeType` (optional): HSN_CODE, SAC_CODE, TAX_CATEGORY, etc.
- `category` (optional): Filter by category/industry
- `page` (default: 0): Page number
- `size` (default: 50): Page size (max: 100)

**Response**:
```kotlin
data class MasterTaxCodeDto(
    val id: String,                       // "HSN_12345678", "SAC_998314"
    val countryCode: String,              // "IN", "US", "GB"
    val codeType: String,                 // "HSN_CODE", "SAC_CODE", "TAX_CATEGORY"
    val code: String,                     // "12345678", "998314", "GROCERY"
    val description: String,              // Full description
    val shortDescription: String,         // Short description
    val chapter: String? = null,          // HSN Chapter (e.g., "12")
    val heading: String? = null,          // HSN Heading (e.g., "1234")
    val subHeading: String? = null,       // HSN Sub-heading
    val category: String? = null,         // Category/Industry
    val defaultTaxRate: Double? = null,   // Suggested default rate (e.g., 18.0)
    val defaultTaxSlabId: String? = null, // Default slab reference
    val isActive: Boolean = true,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Long,
    val updatedAt: Long
)

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean
)
```

**Example Request**:
```
GET /api/v1/tax/master-code/search?query=oil&countryCode=IN&codeType=HSN_CODE&page=0&size=20
```

**Example Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "HSN_12345678",
        "countryCode": "IN",
        "codeType": "HSN_CODE",
        "code": "12345678",
        "description": "Oil seeds and oleaginous fruits; miscellaneous grains, seeds and fruit",
        "shortDescription": "Oil seeds",
        "chapter": "12",
        "heading": "1234",
        "subHeading": "123456",
        "category": "AGRICULTURE",
        "defaultTaxRate": 5.0,
        "defaultTaxSlabId": "GST_5",
        "isActive": true,
        "metadata": {},
        "createdAt": 1700000000000,
        "updatedAt": 1733270400000
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 45,
    "totalPages": 3,
    "hasNext": true
  },
  "error": null
}
```

**Spring Boot Implementation**:
```kotlin
@RestController
@RequestMapping("/api/v1/tax/master-code")
class MasterTaxCodeController(
    private val masterTaxCodeService: MasterTaxCodeService
) {

    @GetMapping("/search")
    fun searchMasterTaxCodes(
        @RequestParam query: String,
        @RequestParam countryCode: String,
        @RequestParam(required = false) codeType: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ApiResponse<PageResponse<MasterTaxCodeDto>> {

        // Validate page size
        val validSize = minOf(size, 100)

        val result = masterTaxCodeService.searchCodes(
            query = query,
            countryCode = countryCode.uppercase(),
            codeType = codeType,
            category = category,
            page = page,
            size = validSize
        )

        return ApiResponse.success(result)
    }
}
```

**Database Query Example**:
```kotlin
@Repository
interface MasterTaxCodeRepository : JpaRepository<MasterTaxCode, String> {

    @Query("""
        SELECT m FROM MasterTaxCode m
        WHERE m.countryCode = :countryCode
        AND m.isActive = true
        AND (
            LOWER(m.code) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(m.shortDescription) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        AND (:codeType IS NULL OR m.codeType = :codeType)
        AND (:category IS NULL OR m.category = :category)
        ORDER BY m.code ASC
    """)
    fun searchCodes(
        query: String,
        countryCode: String,
        codeType: String?,
        category: String?,
        pageable: Pageable
    ): Page<MasterTaxCode>
}
```

---

### 2.2 Get Popular Tax Codes

**Endpoint**: `GET /api/v1/tax/master-code/popular`

**Description**: Get popular tax codes for a country/industry to help users discover commonly used codes.

**Query Parameters**:
- `countryCode` (required): ISO country code
- `industry` (optional): Industry filter
- `limit` (default: 20): Number of codes

**Response**: `List<MasterTaxCodeDto>`

**Example**:
```
GET /api/v1/tax/master-code/popular?countryCode=IN&industry=RETAIL_GROCERY&limit=20
```

---

## 3. Workspace Tax Code APIs (Subscriptions)

### 3.1 Subscribe to Tax Code

**Endpoint**: `POST /api/v1/workspaces/{workspaceId}/tax/code/subscribe`

**Description**: Subscribe workspace to a master tax code. Creates a workspace-specific copy with custom configurations.

**Request Body**:
```kotlin
data class SubscribeTaxCodeRequest(
    val masterTaxCodeId: String,          // FK to master_tax_codes
    val customTaxRuleId: String? = null,  // Override default rule
    val isFavorite: Boolean = false,
    val notes: String? = null
)
```

**Response**:
```kotlin
data class WorkspaceTaxCodeDto(
    val id: String,                       // "WTC_001_HSN_12345678"
    val workspaceId: String,
    val masterTaxCodeId: String,

    // Cached master data for offline access
    val code: String,
    val codeType: String,
    val description: String,
    val shortDescription: String,

    // Workspace-specific configuration
    val customTaxRuleId: String?,
    val usageCount: Int = 0,
    val lastUsedAt: Long?,
    val isFavorite: Boolean,
    val notes: String?,

    val isActive: Boolean,
    val addedAt: Long,
    val updatedAt: Long,
    val syncStatus: String = "SYNCED"     // SYNCED, PENDING
)
```

**Example Request**:
```json
{
  "masterTaxCodeId": "HSN_12345678",
  "isFavorite": false,
  "notes": "Used for cooking oil products"
}
```

**Spring Boot Implementation**:
```kotlin
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/tax/code")
class WorkspaceTaxCodeController(
    private val workspaceTaxCodeService: WorkspaceTaxCodeService
) {

    @PostMapping("/subscribe")
    fun subscribeToTaxCode(
        @PathVariable workspaceId: String,
        @RequestHeader("X-Workspace-ID") headerWorkspaceId: String,
        @RequestBody request: SubscribeTaxCodeRequest
    ): ApiResponse<WorkspaceTaxCodeDto> {

        require(workspaceId == headerWorkspaceId) { "Workspace ID mismatch" }

        val workspaceTaxCode = workspaceTaxCodeService.subscribe(
            workspaceId = workspaceId,
            request = request
        )

        return ApiResponse.success(workspaceTaxCode.toDto())
    }
}
```

**Service Logic**:
```kotlin
@Service
class WorkspaceTaxCodeService(
    private val masterTaxCodeRepository: MasterTaxCodeRepository,
    private val workspaceTaxCodeRepository: WorkspaceTaxCodeRepository,
    private val uidGenerator: UidGenerator
) {

    fun subscribe(workspaceId: String, request: SubscribeTaxCodeRequest): WorkspaceTaxCode {
        // 1. Fetch master tax code
        val masterCode = masterTaxCodeRepository.findById(request.masterTaxCodeId)
            .orElseThrow { NotFoundException("Master tax code not found") }

        // 2. Check if already subscribed
        val existing = workspaceTaxCodeRepository.findByWorkspaceIdAndMasterTaxCodeId(
            workspaceId, request.masterTaxCodeId
        )
        if (existing != null) {
            throw ConflictException("Already subscribed to this tax code")
        }

        // 3. Create workspace tax code
        val workspaceTaxCode = WorkspaceTaxCode(
            id = uidGenerator.generate("WTC"),
            workspaceId = workspaceId,
            masterTaxCodeId = masterCode.id,

            // Cache master data
            code = masterCode.code,
            codeType = masterCode.codeType,
            description = masterCode.description,
            shortDescription = masterCode.shortDescription,

            // Workspace config
            customTaxRuleId = request.customTaxRuleId,
            usageCount = 0,
            lastUsedAt = null,
            isFavorite = request.isFavorite,
            notes = request.notes,

            isActive = true,
            addedAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            syncStatus = "SYNCED"
        )

        return workspaceTaxCodeRepository.save(workspaceTaxCode)
    }
}
```

---

### 3.2 Get Workspace Tax Codes (Incremental Sync)

**Endpoint**: `GET /api/v1/workspaces/{workspaceId}/tax/code`

**Description**: Get all workspace subscribed tax codes with incremental sync support.

**Query Parameters**:
- `modifiedAfter` (optional): Timestamp in milliseconds for incremental sync
- `page` (default: 0): Page number
- `size` (default: 1000): Page size (large for sync)

**Response**: `PageResponse<WorkspaceTaxCodeDto>`

**Example**:
```
GET /api/v1/workspaces/WS_001/tax/code?modifiedAfter=1733000000000&page=0&size=1000
```

**Spring Boot Implementation**:
```kotlin
@GetMapping
fun getWorkspaceTaxCodes(
    @PathVariable workspaceId: String,
    @RequestHeader("X-Workspace-ID") headerWorkspaceId: String,
    @RequestParam(required = false) modifiedAfter: Long?,
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "1000") size: Int
): ApiResponse<PageResponse<WorkspaceTaxCodeDto>> {

    require(workspaceId == headerWorkspaceId) { "Workspace ID mismatch" }

    val pageable = PageRequest.of(page, size, Sort.by("updatedAt").ascending())

    val result = if (modifiedAfter != null) {
        workspaceTaxCodeRepository.findByWorkspaceIdAndUpdatedAtAfter(
            workspaceId, modifiedAfter, pageable
        )
    } else {
        workspaceTaxCodeRepository.findByWorkspaceId(workspaceId, pageable)
    }

    return ApiResponse.success(result.toPageResponse())
}
```

---

### 3.3 Unsubscribe from Tax Code

**Endpoint**: `DELETE /api/v1/workspaces/{workspaceId}/tax/code/{taxCodeId}`

**Description**: Unsubscribe workspace from a tax code (soft delete).

**Response**: `ApiResponse<Unit>`

---

### 3.4 Update Tax Code Configuration

**Endpoint**: `PATCH /api/v1/workspaces/{workspaceId}/tax/code/{taxCodeId}`

**Description**: Update workspace-specific tax code settings (notes, favorite, custom rule).

**Request Body**:
```json
{
  "isFavorite": true,
  "notes": "Updated notes",
  "customTaxRuleId": "RULE_CUSTOM_001"
}
```

---

### 3.5 Increment Usage Count

**Endpoint**: `POST /api/v1/workspaces/{workspaceId}/tax/code/{taxCodeId}/usage`

**Description**: Increment usage count when tax code is used in calculations/transactions.

**Request Body**:
```json
{
  "timestamp": 1733270400000
}
```

---

## 4. Tax Rule APIs

### 4.1 Get Tax Rules

**Endpoint**: `GET /api/v1/workspaces/{workspaceId}/tax/rule`

**Description**: Get tax rules for workspace with incremental sync support.

**Query Parameters**:
- `modifiedAfter` (optional): Timestamp for incremental sync
- `taxCode` (optional): Filter by specific tax code
- `page`, `size`: Pagination

**Response**:
```kotlin
data class TaxRuleDto(
    val id: String,
    val workspaceId: String,
    val countryCode: String,

    // Tax code reference
    val workspaceTaxCodeId: String,
    val taxCode: String,
    val taxCodeType: String,
    val taxCodeDescription: String,

    // Jurisdiction
    val jurisdiction: String,
    val jurisdictionLevel: String,           // COUNTRY, STATE, COUNTY, CITY

    // Component composition for different scenarios
    val componentComposition: Map<String, ComponentCompositionDto>,

    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

data class ComponentCompositionDto(
    val scenario: String,                    // "intraState", "interState", "standard"
    val components: List<ComponentReferenceDto>,
    val totalRate: Double
)

data class ComponentReferenceDto(
    val id: String,
    val name: String,
    val rate: Double,
    val order: Int
)
```

**Example Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "RULE_IN_HSN_12345_18",
        "workspaceId": "WS_001",
        "countryCode": "IN",
        "workspaceTaxCodeId": "WTC_001_HSN_12345678",
        "taxCode": "12345678",
        "taxCodeType": "HSN_CODE",
        "taxCodeDescription": "Oil seeds",
        "jurisdiction": "MH",
        "jurisdictionLevel": "STATE",
        "componentComposition": {
          "intraState": {
            "scenario": "intraState",
            "components": [
              {
                "id": "CGST_MH_9",
                "name": "CGST",
                "rate": 9.0,
                "order": 1
              },
              {
                "id": "SGST_MH_9",
                "name": "SGST",
                "rate": 9.0,
                "order": 2
              }
            ],
            "totalRate": 18.0
          },
          "interState": {
            "scenario": "interState",
            "components": [
              {
                "id": "IGST_18",
                "name": "IGST",
                "rate": 18.0,
                "order": 1
              }
            ],
            "totalRate": 18.0
          }
        },
        "isActive": true,
        "createdAt": 1700000000000,
        "updatedAt": 1733270400000
      }
    ],
    "page": 0,
    "size": 1000,
    "totalElements": 150,
    "totalPages": 1,
    "hasNext": false
  },
  "error": null
}
```

---

### 4.2 Get Tax Components

**Endpoint**: `GET /api/v1/workspaces/{workspaceId}/tax/component`

**Description**: Get workspace tax components (CGST, SGST, IGST, VAT, etc.).

**Response**:
```kotlin
data class WorkspaceTaxComponentDto(
    val id: String,
    val workspaceId: String,
    val componentTypeId: String,             // FK to tax_component_types
    val componentName: String,               // "CGST", "SGST", "IGST"
    val componentDisplayName: String,        // Display name
    val taxType: String,                     // "GST", "VAT", "SALES_TAX"

    // Jurisdiction
    val jurisdiction: String,
    val jurisdictionLevel: String,

    // Rate configuration
    val ratePercentage: Double,
    val isCompound: Boolean,
    val calculationMethod: String,           // PERCENTAGE, FLAT

    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
```

---

## 5. Tax Calculation API (Optional - Can be Mobile-Only)

### 5.1 Calculate Tax (Server-Side)

**Endpoint**: `POST /api/v1/workspaces/{workspaceId}/tax/calculate`

**Description**: Server-side tax calculation for validation or complex scenarios.

**Request Body**:
```kotlin
data class TaxCalculationRequest(
    val taxCode: String,
    val taxCodeType: String,
    val baseAmount: Double,
    val quantity: Int,
    val sourceLocation: JurisdictionDto,
    val destinationLocation: JurisdictionDto,
    val transactionType: String,             // B2B, B2C, EXPORT, IMPORT
    val transactionContext: TransactionContextDto
)

data class JurisdictionDto(
    val country: String,
    val state: String?,
    val county: String?,
    val city: String?,
    val postalCode: String?
)

data class TransactionContextDto(
    val businessType: String = "REGULAR",    // REGULAR, COMPOSITION, SEZ
    val isReverseCharge: Boolean = false,
    val exemptionReason: String?,
    val specialConditions: Map<String, String>
)
```

**Response**:
```kotlin
data class TaxCalculationResultDto(
    val taxCode: String,
    val codeType: String,
    val baseAmount: Double,
    val quantity: Int,
    val taxComponents: List<TaxComponentResultDto>,
    val totalTaxAmount: Double,
    val totalAmount: Double,
    val jurisdiction: JurisdictionDto,
    val countryCode: String,
    val metadata: Map<String, String>
)

data class TaxComponentResultDto(
    val componentId: String,
    val componentName: String,
    val taxType: String,
    val ratePercentage: Double,
    val taxableAmount: Double,
    val taxAmount: Double,
    val description: String,
    val isCompound: Boolean
)
```

---

## 6. Bulk Operations

### 6.1 Bulk Subscribe Tax Codes

**Endpoint**: `POST /api/v1/workspaces/{workspaceId}/tax/code/bulk-subscribe`

**Description**: Subscribe to multiple tax codes in one request.

**Request Body**:
```json
{
  "masterTaxCodeIds": [
    "HSN_12345678",
    "HSN_10063000",
    "SAC_998314"
  ],
  "applyDefaultRules": true
}
```

**Response**:
```kotlin
data class BulkSubscribeResultDto(
    val successCount: Int,
    val failureCount: Int,
    val subscribedCodes: List<WorkspaceTaxCodeDto>,
    val errors: List<BulkOperationErrorDto>
)

data class BulkOperationErrorDto(
    val masterTaxCodeId: String,
    val errorMessage: String
)
```

---

## 7. Database Schema

### 7.1 Master Tax Codes Table
```sql
CREATE TABLE master_tax_codes (
    id VARCHAR(255) PRIMARY KEY,
    country_code VARCHAR(2) NOT NULL,
    code_type VARCHAR(50) NOT NULL,
    code VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    short_description VARCHAR(500),
    chapter VARCHAR(10),
    heading VARCHAR(10),
    sub_heading VARCHAR(20),
    category VARCHAR(100),
    default_tax_rate DECIMAL(5,2),
    default_tax_slab_id VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    metadata JSONB,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,

    INDEX idx_country_code (country_code),
    INDEX idx_code_type (code_type),
    INDEX idx_code (code),
    FULLTEXT INDEX idx_description (description, short_description)
);
```

### 7.2 Workspace Tax Codes Table
```sql
CREATE TABLE workspace_tax_codes (
    id VARCHAR(255) PRIMARY KEY,
    workspace_id VARCHAR(255) NOT NULL,
    master_tax_code_id VARCHAR(255) NOT NULL,

    -- Cached master data
    code VARCHAR(100) NOT NULL,
    code_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    short_description VARCHAR(500),

    -- Workspace config
    custom_tax_rule_id VARCHAR(255),
    usage_count INT DEFAULT 0,
    last_used_at BIGINT,
    is_favorite BOOLEAN DEFAULT false,
    notes TEXT,

    is_active BOOLEAN DEFAULT true,
    added_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    sync_status VARCHAR(50) DEFAULT 'SYNCED',

    FOREIGN KEY (master_tax_code_id) REFERENCES master_tax_codes(id),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_updated_at (updated_at),
    UNIQUE KEY unique_workspace_master (workspace_id, master_tax_code_id)
);
```

### 7.3 Tax Rules Table
```sql
CREATE TABLE tax_rules_v2 (
    id VARCHAR(255) PRIMARY KEY,
    workspace_id VARCHAR(255) NOT NULL,
    country_code VARCHAR(2) NOT NULL,

    workspace_tax_code_id VARCHAR(255) NOT NULL,
    tax_code VARCHAR(100) NOT NULL,
    tax_code_type VARCHAR(50) NOT NULL,
    tax_code_description TEXT,

    jurisdiction VARCHAR(100) NOT NULL,
    jurisdiction_level VARCHAR(50),

    component_composition JSONB NOT NULL,

    is_active BOOLEAN DEFAULT true,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,

    FOREIGN KEY (workspace_tax_code_id) REFERENCES workspace_tax_codes(id),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_tax_code (tax_code),
    INDEX idx_updated_at (updated_at)
);
```

### 7.4 Tax Components Table
```sql
CREATE TABLE workspace_tax_components (
    id VARCHAR(255) PRIMARY KEY,
    workspace_id VARCHAR(255) NOT NULL,
    component_type_id VARCHAR(255) NOT NULL,
    component_name VARCHAR(100) NOT NULL,
    component_display_name VARCHAR(200),
    tax_type VARCHAR(50) NOT NULL,

    jurisdiction VARCHAR(100) NOT NULL,
    jurisdiction_level VARCHAR(50),

    rate_percentage DECIMAL(5,2) NOT NULL,
    is_compound BOOLEAN DEFAULT false,
    calculation_method VARCHAR(50) DEFAULT 'PERCENTAGE',

    is_active BOOLEAN DEFAULT true,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,

    INDEX idx_workspace_id (workspace_id),
    INDEX idx_component_type_id (component_type_id)
);
```

---

## 8. Error Handling

### Standard Error Response Format
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "TAX_CODE_NOT_FOUND",
    "message": "Tax code not found",
    "details": {
      "taxCodeId": "HSN_12345678"
    }
  }
}
```

### Common Error Codes
- `TAX_CODE_NOT_FOUND`: Master tax code not found
- `ALREADY_SUBSCRIBED`: Workspace already subscribed to tax code
- `CONFIGURATION_NOT_FOUND`: Workspace tax configuration not found
- `INVALID_COUNTRY_CODE`: Invalid or unsupported country code
- `INVALID_TAX_CODE_TYPE`: Invalid tax code type
- `WORKSPACE_MISMATCH`: Workspace ID mismatch between path and header

---

## 9. Testing Checklist

### Unit Tests
- [ ] Master tax code search with pagination
- [ ] Subscribe to tax code (success and duplicate)
- [ ] Unsubscribe from tax code
- [ ] Incremental sync with `modifiedAfter`
- [ ] Usage count increment
- [ ] Favorite toggle
- [ ] Tax rule retrieval with component composition

### Integration Tests
- [ ] Complete subscription flow
- [ ] Sync after subscription
- [ ] Multi-workspace isolation
- [ ] Bulk operations

### Performance Tests
- [ ] Search with 100K+ master codes
- [ ] Incremental sync with 1000+ workspace codes
- [ ] Concurrent subscription requests

---

## 10. Sample Data Seeding

### India Master Tax Codes (Sample)
```kotlin
// HSN Codes
MasterTaxCode(
    id = "HSN_12345678",
    countryCode = "IN",
    codeType = "HSN_CODE",
    code = "12345678",
    description = "Oil seeds and oleaginous fruits; miscellaneous grains, seeds and fruit",
    shortDescription = "Oil seeds",
    chapter = "12",
    heading = "1234",
    subHeading = "123456",
    category = "AGRICULTURE",
    defaultTaxRate = 5.0,
    defaultTaxSlabId = "GST_5"
)

// SAC Codes
MasterTaxCode(
    id = "SAC_998314",
    countryCode = "IN",
    codeType = "SAC_CODE",
    code = "998314",
    description = "Information technology design and development services",
    shortDescription = "IT Services",
    category = "SERVICES",
    defaultTaxRate = 18.0,
    defaultTaxSlabId = "GST_18"
)
```

---

## 11. Additional Considerations

### Security
- All endpoints require JWT authentication
- Workspace ID validation on every request
- Rate limiting on search endpoints (100 requests/minute)
- Input validation for SQL injection prevention

### Caching
- Master tax codes: Cache search results for 1 hour
- Workspace configurations: Cache for 15 minutes
- Tax rules: Cache per workspace for 30 minutes

### Monitoring
- Track search query performance
- Monitor subscription rates
- Alert on sync failures
- Log usage count updates for analytics

---

## Contact & Support

For questions or issues with the API implementation:
- Mobile Team: Review `TaxConfigurationApiImpl.kt` for client expectations
- Backend Team: Follow this specification for consistency
- Documentation: Keep this guide updated with API changes

---

**Last Updated**: January 2025
**Version**: 2.0
**Mobile Client**: Kotlin Multiplatform (Android/iOS/Desktop)
