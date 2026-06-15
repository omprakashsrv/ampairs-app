# Contracts — Scalable Customer Search (FTS)

## No external API contract

This feature is **client-side only**. Search runs against the local Room database; there is **no new
backend endpoint** and **no change to any existing REST contract**. The customer `/sync` contract
(`GET/POST v1/customers/sync`) and the `CentralSyncService` / `CustomerSyncDelegate` push/pull flow
are untouched.

Therefore there are no OpenAPI/GraphQL schemas in this directory.

## Internal contracts (pinned)

The feature's contracts are internal (DAO ↔ repository ↔ store ↔ ViewModel). They are specified in
[`../data-model.md`](../data-model.md):

- **DAO**: `browse(...): PagingSource<Int, CustomerEntity>` and
  `searchByFts(ftsQuery, ...): Flow<List<CustomerEntity>>` — both apply `active = 1` + state/type/
  group filters, order by `name ASC`; search is `LIMIT 100`.
- **Repository**: `buildFtsQuery(input): String` (prefix terms; digit-query → single phone token)
  and `searchAndFilter(query, filters)` router (blank → browse, else → FTS).
- **FTS index contract**: `customer_fts` is Room-managed (auto triggers); feature code never writes
  to it.

## Migration contract

`Migration10To11` must reproduce Room's generated v11 `customer_fts` table + content-sync trigger
DDL **verbatim** (from the committed `11.json`), or Room's open-time schema validation fails. See
[`../data-model.md`](../data-model.md) and [`../research.md`](../research.md) R9.
