# Tasks: Scalable Customer Search (FTS)

**Input**: Design documents from `/specs/001-customer-search-fts/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/
**Dev branch**: `claude/trusting-fermi-7fvvpa`

**Tests**: Included only where the spec explicitly asks (quickstart §6 — `buildCustomerFtsQuery` unit test). No broader test scaffolding is generated.

---

## ⚠️ Implementation status (2026-06-15)

Code was written in a sandbox that **cannot build** (no Android SDK, Room deps not cached,
Gradle toolchain download blocked, and Linux can't compile the iOS target). So all build / schema /
runtime / test tasks are **unverified** and must be completed in CI or a local/macOS dev env.

Two deliberate deviations from the original plan, made because they're unsafe to author blind:
- **F1 resolution simplified**: `searchAndFilter(query, filters): Flow<List<CustomerListItem>>` —
  ONE return type, capped at 200, blank→`browse`, else→`searchByFts`. No Paging3 (avoids new deps +
  a `LazyPagingItems` screen rewrite). Fits the existing ViewModel pipeline with a one-line swap.
- **US4 (Paging3 browse) deferred** to the build env (T019/T020) — needs paging-dependency wiring
  and a screen rewrite. Current browse path is the capped Flow (bounded, name-ordered, filtered).

`[X]` = code written (unverified)  ·  `[~]` = deferred  ·  `[ ]` = needs a build env

- `[X]` T003 CustomerFts, `[X]` T004 DB v11, `[X]` T006 Migration10To11 (DDL needs 11.json diff),
  `[X]` T007 register migration (3 platforms), `[X]` T009 searchByFts, `[X]` T010/T015 buildCustomerFtsQuery
  (incl. phone collapse + F3 degenerate guard), `[X]` T011 searchAndFilter router, `[X]` T012 store,
  `[X]` T013 ViewModel swap, `[X]` T014/T016 BuildCustomerFtsQueryTest, `[X]` T001 commonTest sourceSet.
- `[ ]` T002/T005/T008 schema export + 11.json (needs build), `[ ]` T017/T018 retire legacy filter path
  (kept for order/invoice pickers), `[ ]` T023 compile, `[ ]` T024 runtime smoke, `[ ]` T025 run tests.
- `[~]` T019/T020 Paging3 browse + LazyPagingItems screen, `[~]` T021 retire LIKE methods (still used
  by pickers), `[~]` T022 first-sync batch tuning.

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1–US4 (see spec use cases / functional requirements)
- All paths are under `feature/customer/` unless noted.

## Path base
`CMN = feature/customer/src/commonMain/kotlin/com/ampairs/customer`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Ensure the customer module can build paged DB queries.

- [ ] T001 Verify/add Paging dependencies for the customer module in `feature/customer/build.gradle.kts`: ensure `commonMain` has `libs.room.paging` and the project KMP paging wrapper (`thirdparty/androidx/paging/compose`) is on the dependency path used by `CustomersListScreen`. Add to `gradle/libs.versions.toml` only if an alias is missing (no hardcoded versions).
- [ ] T002 [P] Confirm `feature/customer` exports its Room schema (`exportSchema = true` already set on `CustomerDatabase`) and that the `schemas/` output dir is wired in `build.gradle.kts` (room `schemaDirectory`). Needed so v11 `11.json` is generated in Phase 2.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The FTS index, DB version bump, and migration. **No user story can function until this is done** (every search/browse query joins `customer_fts` or relies on v11).

**⚠️ CRITICAL**: Complete this phase before starting US1–US4.

- [ ] T003 Create `CMN/data/db/CustomerFts.kt` — `@Fts4(contentEntity = CustomerEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)` `@Entity(tableName = "customer_fts")` with columns `name, email, phone, gstNumber, address, street, city, state` (each must name a real `CustomerEntity` column). See data-model.md.
- [ ] T004 Edit `CMN/data/db/CustomerDatabase.kt` — add `CustomerFts::class` to `entities`; bump `version` 10 → 11; keep `exportSchema = true`.
- [ ] T005 Build once to generate the v11 schema: `./gradlew :feature:customer:compileDebugKotlinAndroid`; copy the generated `customer_fts` virtual-table DDL **and** the 3 content-sync trigger DDL/names verbatim out of `schemas/.../11.json` for use in T006.
- [ ] T006 Create `CMN/data/db/migrations/Migration10To11.kt` — `CUSTOMER_MIGRATION_10_11` (`Migration(10, 11)`): `CREATE INDEX IF NOT EXISTS customer_gst_idx ON customers(gstNumber)`; then the FTS vtable + 3 triggers (verbatim from T005); then `INSERT INTO customer_fts(customer_fts) VALUES('rebuild')`. **No `ALTER TABLE`** (no new base columns).
- [ ] T007 Register `CUSTOMER_MIGRATION_10_11` in all three platform factories' `migrations = listOf(...)`: `androidMain/.../di/CustomerPlatformModule.android.kt`, `iosMain/.../di/CustomerPlatformModule.ios.kt`, `desktopMain/.../di/CustomerPlatformModule.desktop.kt`.
- [ ] T008 Commit the exported `feature/customer/schemas/.../11.json`.

**Checkpoint**: DB opens at v11, `customer_fts` populated by `'rebuild'`, Room triggers maintain it on writes. User stories can begin.

---

## Phase 3: User Story 1 — Text search (name/email/GSTIN/address) (Priority: P1) 🎯 MVP

**Goal**: Typing a non-numeric term prefix-matches customers across name/email/gstNumber/address fields, < 100 ms at 100k rows, with `ORDER BY name` and `LIMIT 100`. (FR-1, FR-2, FR-4, FR-6, FR-7, NFR-1)

**Independent Test**: On a seeded DB, typing `raj` returns "Rajesh …"; typing `29ABCDE` returns the matching GSTIN; `esh` returns nothing (prefix-only, expected). Results capped at 100, name-ordered, active-only.

- [ ] T009 [US1] Add `searchByFts(ftsQuery, states, hasStates, types, hasTypes, groups, hasGroups): Flow<List<CustomerEntity>>` to `CMN/data/db/CustomerDao.kt` — `JOIN customer_fts f ON c.rowid = f.rowid WHERE customer_fts MATCH :ftsQuery AND c.active = 1 AND (filter trio) ORDER BY c.name ASC LIMIT 100`. (data-model.md DAO contract)
- [ ] T010 [US1] Add `buildFtsQuery(input): String` to `CMN/data/repository/CustomerRepository.kt` — text branch: split on whitespace, escape `"`→`""`, wrap each token `"<tok>"*` (per-token prefix). (Digit branch added in US2/T015.)
- [ ] T011 [US1] Add `searchAndFilter(query, filters)` router to `CustomerRepository.kt` — non-blank query → `searchByFts(buildFtsQuery(query), …filters…)` mapped to `Flow<List<CustomerListItem>>`. (Blank branch added in US4/T019.)
- [ ] T012 [US1] Expose `searchAndFilter` as a pass-through in `CMN/domain/CustomerStore.kt`.
- [ ] T013 [US1] Wire `CMN/ui/list/CustomersListViewModel.kt` to call `customerStore.searchAndFilter(query, filter)` for non-blank queries (keep existing 300 ms debounce + filter combine); render results in the existing list state.
- [ ] T014 [P] [US1] Add `buildFtsQuery` unit test in `feature/customer/src/commonTest/kotlin/com/ampairs/customer/BuildFtsQueryTest.kt` — cases: `"raj mum"` → `"raj"* "mum"*`; `a"b` → `"a""b"*`. (quickstart §6)

**Checkpoint**: Core text search works through the FTS path independently.

---

## Phase 4: User Story 2 — Phone search, space-insensitive (Priority: P1)

**Goal**: Searching a phone number matches whether or not the user types spaces; `9876543210` and `98765 43210` both find the same customer. (FR-3)

**Independent Test**: With phone indexed in FTS (T003), typing `98765 43210` returns the customer stored as `9876543210`; typing a partial `98765` prefix-matches.

- [ ] T015 [US2] Extend `buildFtsQuery` in `CustomerRepository.kt` — if the trimmed input is digits+spaces only (and has ≥1 digit), strip all non-digits and emit a single prefix term `"<digits>"*` (collapses to one token to match the stored phone token). (research.md R4)
- [ ] T016 [P] [US2] Add phone cases to `BuildFtsQueryTest.kt`: `"98765 43210"` → `"9876543210"*`; mixed text stays per-token.

**Checkpoint**: Phone search works with/without spaces; US1 text search unaffected.

---

## Phase 5: User Story 3 — Filters combine with search (Priority: P2)

**Goal**: State/type/group multi-select filters AND-combine with any active search term (and with browse). (FR-5)

**Independent Test**: With a search term active, selecting a state filter narrows results to that state; clearing the term keeps the filter applied to the browse list.

- [ ] T017 [US3] Ensure `searchAndFilter` (T011) threads `CustomerFilter` (states/types/groups + `hasX` flags) into `searchByFts` and the browse query; remove the legacy `filterCustomers` call from `CustomerStore`/`CustomerRepository` once parity is confirmed.
- [ ] T018 [US3] Update `CustomersListViewModel.kt` so the filter `combine(...)` feeds `searchAndFilter` for both blank and non-blank query states (single code path); verify `getDistinctStates/Types/Groups` still populate the filter sheet.

**Checkpoint**: Filters apply uniformly across search and browse via one path.

---

## Phase 6: User Story 4 — Browse large list (paged) (Priority: P2)

**Goal**: Browsing with no search term scrolls smoothly at 100k rows without loading all into memory. (FR-6, NFR-1, NFR-3)

**Independent Test**: Clear the search box on a 100k-customer workspace; the list scrolls via Paging3 with flat memory; filters still apply.

- [ ] T019 [US4] Add `browse(states, hasStates, types, hasTypes, groups, hasGroups): PagingSource<Int, CustomerEntity>` to `CustomerDao.kt` — `active = 1` + filter trio, `ORDER BY name ASC`. Wire the blank-query branch of `searchAndFilter`/store to a paged stream.
- [ ] T020 [US4] Update `CMN/ui/list/CustomersListScreen.kt` — render the browse list via `LazyPagingItems` (project paging wrapper) with stable `key = { it.id }`; render the search results (capped Flow) with a "showing first 100 — refine search" hint. Keep the existing search bar / filter sheet / adaptive card-table layout.

**Checkpoint**: Browse is paged; search is capped; both honor filters.

---

## Phase 7: Polish & Cross-Cutting

- [ ] T021 Retire the legacy LIKE methods `searchCustomers` / `filterCustomers` from `CustomerDao.kt` and any now-dead pass-throughs in `CustomerRepository.kt` / `CustomerStore.kt`; update remaining call sites (e.g. order/invoice customer pickers) to the new search path.
- [ ] T022 [P] First-full-sync tuning (optional): if 100k initial indexing is slow, raise the customer pull batch size in `CMN/sync/CustomerSyncDelegate.kt` so each insert transaction covers more rows (FTS triggers fire per row). Document the chosen size.
- [ ] T023 Compile all targets: `./gradlew androidApp:compileDebugKotlinAndroid && ./gradlew shared:compileKotlinIosSimulatorArm64 && ./gradlew desktopApp:compileKotlin`.
- [ ] T024 Runtime FTS smoke per quickstart §3–§5 on Android, Desktop, and iOS simulator: v10→v11 migration runs once (no data loss), MATCH returns results, phone/GSTIN/filter/browse behaviors verified.
- [ ] T025 Run `commonTest`: `./gradlew :feature:customer:testDebugUnitTest` (or the KMP test task) — `BuildFtsQueryTest` green.

---

## Dependencies & Execution Order

```
Setup (T001–T002)
   └─> Foundational (T003–T008)   ← BLOCKS everything
          ├─> US1 text search (T009–T014)        🎯 MVP
          │      ├─> US2 phone (T015–T016)        depends on buildFtsQuery (T010)
          │      └─> US3 filters (T017–T018)      depends on searchAndFilter (T011)
          └─> US4 browse (T019–T020)              parallel to US1 (separate browse() path)
   └─> Polish (T021–T025)                          after the stories it touches
```

- **Foundational blocks all stories.** T005 (build) must precede T006 (migration DDL copy).
- **US1 is the MVP.** US2 and US3 extend US1's `buildFtsQuery`/`searchAndFilter` (sequential after US1). US4 (browse) only needs Foundational and can proceed in parallel with US1.
- **Parallel within phases**: `[P]` tasks touch different files (e.g. T014/T016 test file vs. repository).

## Parallel Execution Examples

- After Foundational: start **US1 (T009–T013)** and **US4 (T019)** concurrently — different DAO methods/UI paths.
- Within US1: T014 `[P]` (test file) alongside T009–T013.
- Within US2: T016 `[P]` (test) alongside T015.

## Implementation Strategy

- **MVP = Foundational + US1** (T001–T014): fast text search shipping on all platforms.
- **Increment 2**: US2 phone (T015–T016) — small, high-value.
- **Increment 3**: US3 filters (T017–T018) + US4 browse (T019–T020) — completes parity with the old screen at scale.
- **Finish**: Polish (T021–T025) — retire LIKE, tune first sync, validate all targets.

## Summary

- **Total tasks**: 25
- **Per story**: Setup 2 · Foundational 6 · US1 6 · US2 2 · US3 2 · US4 2 · Polish 5
- **MVP scope**: T001–T014 (Setup + Foundational + US1)
- **Tests**: `BuildFtsQueryTest` (T014, T016) — the only tests the spec explicitly requested.
