# Implementation Plan: Scalable Customer Search (FTS)

**Branch**: `001-customer-search-fts` (dev branch: `claude/trusting-fermi-7fvvpa`) | **Date**: 2026-06-15 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-customer-search-fts/spec.md`

## Summary

Replace leading-wildcard `LIKE` customer search with **Room-managed `@Fts4`** full-text search in
`commonMain`, keeping search **client-side** against the local Room DB. Add a `CustomerFts`
contentEntity over `CustomerEntity` (columns: name, email, phone, gstNumber, address, street, city,
state), bump the customer DB to v11 with a hand-written migration that creates the FTS table +
Room's content-sync triggers + `'rebuild'`. A repository-level router builds the FTS MATCH string
(prefix `term*`; digits-only queries collapse to a single space-stripped token for phone) and
dispatches: blank term → paginated browse; non-blank → FTS-MATCH Flow capped at 100. Existing
state/type/group filters, debounce, and MVI/Metro wiring are preserved. No backend or sync changes.

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.4.0
**Primary Dependencies**: Room KMP 2.8.4 (`androidx.room`, `@Fts4`), `androidx.sqlite:sqlite-bundled` 2.6.2 (FTS-enabled on all targets), Compose Multiplatform 1.11.1, Metro DI 1.1.1, AndroidX Paging 3.3.6 (+ project KMP paging wrapper `thirdparty/androidx/paging/compose`), kotlinx.coroutines 1.11.0
**Storage**: Per-workspace Room SQLite (bundled driver) — `customer` module DB, currently v10
**Testing**: Compile gates on all 3 targets; runtime FTS smoke on Android/iOS/Desktop; targeted `commonTest` for the query-builder/router
**Target Platform**: Android (minSdk 24), iOS 16.0, Desktop (JVM), Wasm
**Project Type**: Mobile (KMP multi-module) — feature lives in `feature/customer`
**Performance Goals**: As-you-type search < 100 ms at 100k rows
**Constraints**: `commonMain` only (no `java.*`/`android.*`); bounded result sets; in-place migration; no sync-semantics change
**Scale/Scope**: 20k–100k+ customers per workspace; ~6 files touched in `feature/customer` + 3 platform DB factories

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

> Note: `constitution.md` v1.0.0 predates the codebase's migration off Koin/Store5/`composeApp`
> to Metro DI / `CentralSyncService` / multi-module `feature/`. Principles are mapped to current
> equivalents (e.g. "workspace-scoped `factory`" → `@SingleIn(WorkspaceScope::class)` DB + unscoped
> `@Inject` DAOs/repos). No principle is violated by this feature.

| Principle | Status | Notes |
|---|---|---|
| I. KMP Platform Compatibility | PASS | All new code in `commonMain`; FTS via bundled SQLite works on every target; no platform APIs. |
| II. Offline-First Architecture | PASS | Search is read-only over local Room; write path & `markPendingPush` unchanged; FTS index maintained by Room triggers. |
| III. Workspace-Scoped DB Isolation | PASS | FTS table lives inside the existing `@SingleIn(WorkspaceScope::class)` customer DB; no new DB; closes with the workspace graph. |
| IV. Material Design 3 | PASS | No new UI framework; reuses existing search bar / filter sheet / adaptive list. |
| V. Backend API Alignment | N/A | No backend contract change (client-side search). |
| VI. Store5 Pattern | N/A | Codebase uses `CentralSyncService` + reactive DAO `Flow`, not Store5; unchanged. |
| VII. DTO Migration | N/A | No DTO changes. |
| X. Time/Date Handling | PASS | No new timestamp handling. |

**Gate result: PASS** — no Complexity Tracking entries required.

Re-check after Phase 1 design: **still PASS** — design introduces no platform APIs, no new DB, no
sync change, no UI framework. See `data-model.md` / `research.md`.

## Project Structure

### Documentation (this feature)

```
specs/001-customer-search-fts/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (no API contract — see contracts/README.md)
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```
feature/customer/src/
├── commonMain/kotlin/com/ampairs/customer/
│   ├── data/db/
│   │   ├── CustomerEntity.kt           # unchanged columns (phone/gstNumber already present)
│   │   ├── CustomerFts.kt              # NEW @Fts4(contentEntity = CustomerEntity::class)
│   │   ├── CustomerDatabase.kt         # +CustomerFts entity, version 10 → 11
│   │   ├── CustomerDao.kt              # + browse(PagingSource) + searchByFts(Flow); retire LIKE methods
│   │   └── migrations/
│   │       └── Migration10To11.kt      # NEW: gst index + FTS vtable + triggers + 'rebuild'
│   ├── data/repository/CustomerRepository.kt   # + buildFtsQuery() + searchAndFilter() router
│   ├── domain/CustomerStore.kt         # pass-through to searchAndFilter()
│   └── ui/list/
│       ├── CustomersListViewModel.kt   # route to searchAndFilter(); paged browse + capped search
│       └── CustomersListScreen.kt      # LazyPagingItems for browse; "first 100" hint for search
├── androidMain/.../di/CustomerPlatformModule.android.kt   # register CUSTOMER_MIGRATION_10_11
├── iosMain/.../di/CustomerPlatformModule.ios.kt           # register CUSTOMER_MIGRATION_10_11
├── desktopMain/.../di/CustomerPlatformModule.desktop.kt   # register CUSTOMER_MIGRATION_10_11
└── schemas/.../11.json                 # committed exported Room schema (v11)
```

**Structure Decision**: Mobile KMP multi-module. All feature work is contained in
`feature/customer`; the only files outside `commonMain` are the three platform DI modules that
register the new migration. No changes to `shared/`, `data/common`, or the backend.

## Phases

### Phase 0 — Research
Resolve the FTS-engine choice, the Room-FTS4 migration mechanics, the phone-tokenization approach,
the result-bounding strategy, and the bundled-SQLite FTS availability. → `research.md`.

### Phase 1 — Design & Contracts
- `data-model.md`: `CustomerFts` schema, its relationship to `CustomerEntity`, the migration DDL
  shape, and the search query/router contract.
- `contracts/`: documents that there is **no API contract** (client-side feature) and pins the
  internal DAO/repository search contract.
- `quickstart.md`: how to validate the feature (compile gates, runtime FTS smoke, manual test
  script at scale).

### Phase 2 — Tasks
Generated by `/speckit.tasks` (not part of `/speckit.plan`).

## Complexity Tracking

*No constitution violations — section intentionally empty.*
