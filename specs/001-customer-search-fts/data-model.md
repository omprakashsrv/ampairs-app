# Phase 1 Data Model — Scalable Customer Search (FTS)

## Entities

### CustomerEntity (existing — unchanged)
Table `customers`, PK `id: String`. No column changes for this feature. Relevant searchable columns
already present: `name`, `email`, `phone`, `gstNumber`, `address`, `street`, `city`, `state`,
`active`, `synced`. Existing indexes: `id`(unique), `name`, `ref_id`, `state`, `customer_type`,
`customer_group`.

### CustomerFts (NEW — Room-managed FTS4 contentEntity)
External-content FTS4 virtual table mirroring a subset of `customers`. Room auto-generates the table
and the insert/update/delete content-sync triggers.

```kotlin
@Fts4(contentEntity = CustomerEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "customer_fts")
data class CustomerFts(
    val name: String,
    val email: String?,
    val phone: String?,
    val gstNumber: String?,
    val address: String?,
    val street: String?,
    val city: String?,
    val state: String?,
)
```

- Every FTS column name must correspond to a column on `CustomerEntity` (contentEntity contract).
- `tokenizer = unicode61` (diacritic-insensitive word tokenization).
- The FTS table joins back to `customers` via `customers.rowid = customer_fts.rowid`.

**Relationship**: `customer_fts` is an index over `customers` (no independent data). Room keeps it in
sync via generated triggers on insert/update/delete of `customers`. Soft-deletes (`active = 0`) and
hard-deletes both flow through automatically; the search query filters `active = 1`.

## Database

`CustomerDatabase`: add `CustomerFts::class` to `entities`; bump `version` **10 → 11**;
`exportSchema = true` (commit `11.json`).

## Migration: `Migration10To11`

No `ALTER TABLE` (no new base columns). Steps:
1. `CREATE INDEX IF NOT EXISTS customer_gst_idx ON customers(gstNumber);` (optional exact lookups)
2. `CREATE VIRTUAL TABLE customer_fts USING fts4(...)` — DDL **copied verbatim** from generated `11.json`.
3. Create the 3 content-sync triggers (names/bodies **verbatim** from `11.json`).
4. `INSERT INTO customer_fts(customer_fts) VALUES('rebuild');` — populate the index from existing rows.

Register `CUSTOMER_MIGRATION_10_11` in the Android/iOS/Desktop `CustomerPlatformModule` factories'
`migrations = listOf(...)`.

## Search contract (DAO + repository)

### DAO (`CustomerDao`)
Two new methods; both apply `active = 1` + the existing `hasStates/hasTypes/hasGroups` filter trio
and order by `name ASC`.

```kotlin
// Browse (blank query) — paginated
@Query("""
    SELECT * FROM customers
    WHERE active = 1
      AND (:hasStates = 0 OR state IN (:states))
      AND (:hasTypes  = 0 OR customer_type IN (:types))
      AND (:hasGroups = 0 OR customer_group IN (:groups))
    ORDER BY name ASC
""")
fun browse(states: List<String>, hasStates: Int, types: List<String>, hasTypes: Int,
           groups: List<String>, hasGroups: Int): PagingSource<Int, CustomerEntity>

// Search (non-blank query) — FTS MATCH, capped
@Query("""
    SELECT c.* FROM customers c
    JOIN customer_fts f ON c.rowid = f.rowid
    WHERE customer_fts MATCH :ftsQuery
      AND c.active = 1
      AND (:hasStates = 0 OR c.state IN (:states))
      AND (:hasTypes  = 0 OR c.customer_type IN (:types))
      AND (:hasGroups = 0 OR c.customer_group IN (:groups))
    ORDER BY c.name ASC
    LIMIT 100
""")
fun searchByFts(ftsQuery: String, states: List<String>, hasStates: Int, types: List<String>,
                hasTypes: Int, groups: List<String>, hasGroups: Int): Flow<List<CustomerEntity>>
```

The legacy `searchCustomers` / `filterCustomers` LIKE methods are retired once callers move over.

### Repository (`CustomerRepository`)

```kotlin
// FTS MATCH string builder — prefix match; phone collapse for digit queries
fun buildFtsQuery(input: String): String {
    val trimmed = input.trim()
    val digitsOnly = trimmed.all { it.isDigit() || it.isWhitespace() } && trimmed.any { it.isDigit() }
    return if (digitsOnly) {
        "\"${trimmed.filter { it.isDigit() }}\"*"               // single phone token, prefix
    } else {
        trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
            .joinToString(" ") { "\"${it.replace("\"", "\"\"")}\"*" }  // per-token prefix
    }
}

// Router: blank → paginated browse; else → FTS-MATCH capped Flow
// (returns either a PagingSource-backed stream or Flow<List<CustomerListItem>> per branch)
fun searchAndFilter(query: String, filters: CustomerFilter): ...
```

`CustomerStore` exposes `searchAndFilter` as a pass-through. The ViewModel routes its debounced
query + filter state through it; browse renders via `LazyPagingItems`, search via a capped `Flow`
with a "showing first 100 — refine search" hint.

## Validation rules / invariants

- Only `active = 1` rows are searchable/browsable.
- Filters (state/type/group) are AND-combined with the search term and with browse.
- Phone search is space-insensitive (query normalized to a single digit token).
- FTS index is never written by feature code — Room triggers own it (consistency invariant).

## State transitions

None — search/browse is a read-only projection of `customers`. Customer lifecycle (create/update/
soft-delete/sync) is unchanged and out of scope.
