# Feature Specification: Scalable Customer Search (FTS)

**Feature ID**: 001-customer-search-fts
**Dev Branch**: `claude/trusting-fermi-7fvvpa` (speckit feature slug: `001-customer-search-fts`)
**Date**: 2026-06-15
**Status**: Planned

> Note: this spec was synthesized from a design discussion (the `/speckit.specify` step
> was skipped). It captures the agreed requirements and decisions so `/speckit.plan` and
> `/speckit.tasks` have a canonical source.

## Summary

Replace the customer list's `LIKE '%query%'` search with a full-text-search (FTS) backed
implementation that stays fast at **20,000–100,000+ customers per workspace**, across all
KMP targets (Android, iOS, Desktop, Wasm). Search remains **client-side only** against the
local Room database — there is **no new backend endpoint** and **no change to sync semantics**.

## Problem

The current search (`CustomerDao.searchCustomers` / `filterCustomers`) uses leading-wildcard
`LIKE '%q%'`, which cannot use an index and forces a full table scan. At a few hundred rows
this is sub-millisecond; at 20k–100k rows it degrades as-you-type typing latency. The list also
loads **all** active customers into memory with no `LIMIT`, which compounds the problem at scale.

## Goals

- Sub-100ms as-you-type search at 100k customers in a workspace.
- Search across **name, email, phone, GSTIN, and address** fields (street/city/state/address line).
- One shared implementation in `commonMain` — identical behavior on Android, iOS, Desktop, Wasm.
- Bounded result sets (no unbounded list loads).
- Preserve existing multi-select filters (state / type / group) and `ORDER BY name`.
- No regression to offline-first sync.

## Non-Goals

- No server-side / backend search endpoint (search stays local).
- No change to the existing `CentralSyncService` / `CustomerSyncDelegate` push/pull contract.
- Custom-field (`attributes_json`) values are **not** searchable (matches current behavior).
- Relevance ranking and mid-word substring matching are **out of scope for v1** (see "Future").

## Users & Use Cases

- A sales user opening the Customers screen and typing a partial **name** to find a customer.
- Looking up a customer by **phone** (typed with or without spaces) or by **GSTIN** (full or prefix).
- Filtering by **state/type/group** while a search term is active.
- Browsing the full customer list when no search term is entered.

## Functional Requirements

- **FR-1** Searching a non-empty term returns customers whose indexed fields **prefix-match** each
  token (e.g. `raj` matches "Rajesh"; `esh` does **not** — prefix, not substring).
- **FR-2** Indexed/searchable fields: `name, email, phone, gstNumber, address, street, city, state`.
- **FR-3** Phone search is space-insensitive: typing `98765 43210` matches a stored `9876543210`.
- **FR-4** GSTIN search matches full or prefix (e.g. `29ABCDE` matches `29ABCDE1234F1Z5`).
- **FR-5** Existing multi-select filters (state, type, group) apply on top of any search term, and
  on the browse (empty-term) list.
- **FR-6** Results are ordered by `name ASC` and bounded: browse is paginated; search is capped
  at the first 100 results with a "refine your search" affordance.
- **FR-7** Only `active = 1` customers appear.
- **FR-8** Search input is debounced (~300 ms) and executed off the main thread (already present).
- **FR-9** Soft-deleted / synced-away customers disappear from results automatically (FTS index
  stays consistent with the base table without manual maintenance in feature code).

## Non-Functional Requirements

- **NFR-1 (Performance)** As-you-type query < 100 ms at 100k rows on a mid-range device.
- **NFR-2 (KMP)** `commonMain` only; no `java.*` / `android.*`; compiles on all targets.
- **NFR-3 (Storage)** FTS index overhead acceptable (~index of the 8 text columns).
- **NFR-4 (Migration safety)** Existing customer data upgrades in place (no destructive reset).
- **NFR-5 (Workspace isolation)** Works within the existing per-workspace DB; no cross-workspace leakage.

## Key Decisions (resolved during design)

| Decision | Choice | Rationale |
|---|---|---|
| Scale target | 20k–100k+ per workspace | Drives the need for FTS over `LIKE`. |
| Platform scope | One `commonMain` impl | Single codebase across Android/iOS/Desktop/Wasm. |
| FTS engine | **Room-managed `@Fts4`** | `@Fts5` exists only in `androidx.room3`; this app is on `androidx.room` 2.8.4. FTS4 is Room-managed (auto table + triggers). |
| Matching | Prefix (`term*`) + `ORDER BY name` | FTS4 has no `rank`; prefix covers as-you-type. |
| Phone | Index existing `phone` column directly; normalize the **query** only | 10-digit rule already keeps stored phone a single token; avoids a redundant column. |
| Custom fields | Excluded from FTS | Avoids indexing JSON keys/noise; matches current behavior. |
| Results | Paging3 for browse; `LIMIT 100` Flow for search | Bounded memory at 100k rows. |

## Future (explicitly deferred)

When the app migrates `androidx.room` → `androidx.room3` (stable), swap `@Fts4` → Room-managed
`@Fts5` to gain **bm25 relevance ranking** (`ORDER BY rank`) and optional **trigram** tokenizer
for **true substring** matching. The design isolates all search logic behind the repository so
this swap touches only the FTS entity + DAO query.

## Acceptance Criteria

- Typing in the search bar on a workspace with 100k customers returns results without perceptible
  lag (< 100 ms), prefix-matching name/email/phone/GSTIN/address.
- Phone search works with and without spaces.
- State/type/group filters combine with the search term and with the browse list.
- Browsing 100k customers scrolls smoothly (paged), not loaded all at once.
- Deleting/syncing a customer removes it from search results without manual index code.
- All three targets compile; FTS verified at runtime on Android, iOS, and Desktop.

## Risks

- First full sync of ~100k rows indexes row-by-row via Room triggers (tens of seconds, background).
- The migration's FTS DDL must match Room's generated v11 schema exactly (validation gotcha).
- Phone single-token assumption depends on the 10-digit rule holding on every write path.
