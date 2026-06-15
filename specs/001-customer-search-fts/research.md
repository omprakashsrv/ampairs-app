# Phase 0 Research — Scalable Customer Search (FTS)

All NEEDS CLARIFICATION items from Technical Context are resolved below.

## R1. Search engine: FTS vs LIKE

- **Decision**: SQLite full-text search (FTS), not `LIKE '%q%'`.
- **Rationale**: The target is 20k–100k+ customers/workspace. Leading-wildcard `LIKE` cannot use an
  index → full table scan, which degrades as-you-type latency at that size. FTS uses an inverted
  index → sub-100ms prefix lookups.
- **Alternatives considered**:
  - Keep `LIKE` + add `LIMIT` — rejected: still scans the whole table per keystroke at 100k.
  - Server-side search endpoint — rejected: violates the offline-first design (search must work
    offline) and adds a backend contract; out of scope.

## R2. FTS version: FTS4 vs FTS5

- **Decision**: Room-managed **`@Fts4`** now; defer `@Fts5` to a later room3 migration.
- **Rationale**:
  - The `@Fts5` annotation exists, is Room-managed (auto table + content-sync triggers), **but only
    in the `androidx.room3` package** (the new KMP-first major). This project is on
    `androidx.room:room-runtime:2.8.4`, whose classic namespace exposes only `@Fts3`/`@Fts4`
    (verified: `androidx.room.Fts4` reference page exists; `androidx.room.Fts5` 404s).
  - Getting Room-managed FTS5 would require migrating the **entire app** (`androidx.room.*` →
    `androidx.room3.*`) across all 17 feature modules onto a pre-stable major — far out of scope.
  - The bundled SQLite **does** ship FTS5, so a *hand-rolled* FTS5 (manual vtable + triggers in a
    migration + raw `@Query`) is possible, but it forfeits Room-managed trigger consistency. Given
    v1 only needs prefix matching + name ordering, FTS4's Room-managed simplicity wins.
- **Tradeoffs accepted with FTS4**: no built-in `rank` (bm25) → order by `name`; prefix-only (no
  trigram substring). Both are acceptable for v1 (see spec "Future").
- **Alternatives considered**: hand-rolled FTS5 (substring + rank, manual triggers) — deferred;
  app-wide room3 migration for Room-managed `@Fts5` — deferred.

## R3. Cross-platform availability of FTS (bundled SQLite)

- **Decision**: Rely on the bundled SQLite driver; no OS-version floor.
- **Rationale**: `WorkspaceAwareDatabaseFactory` builds every platform's Room DB with
  `BundledSQLiteDriver()` (`androidx.sqlite:sqlite-bundled` 2.6.2), declared in `data/common`
  commonMain. The bundled SQLite is compiled with FTS3/4/5 enabled, so FTS works on Android
  (minSdk 24), iOS 16.0, Desktop, and Wasm regardless of the OS's own `libsqlite3`.
- **Note**: This is also why FTS5 (if hand-rolled later) would work cross-platform — the doc's
  "FTS5 = Android-only / API 29+" caveat applies only to the *system* SQLite, which we don't use.

## R4. Phone matching (single-token + query normalization)

- **Decision**: Index the existing `phone` column directly in FTS (no derived `phone_normalized`
  column). Normalize only the **query**: when the trimmed input is digits+spaces only, strip spaces
  and emit a single prefix term.
- **Rationale**: `CustomerDao.nullifyInvalidPhones()` nulls any phone whose length ≠ 10, so stored
  phones are already a single space-free token. A second normalized column would be redundant. FTS
  tokenizes on word boundaries, so a spaced query (`98765 43210`) would otherwise split into two
  prefix terms that can't both match the single stored token `9876543210`; collapsing the query to
  one token (`9876543210*`) fixes that.
- **Risk**: if any write path stores a spaced/formatted phone (bypassing the 10-digit rule), that
  row's phone token splits and phone search misses it → reinstate a derived normalized column.
- **Alternatives considered**: derived `phone_normalized` column + FTS column (extra migration
  backfill, redundant given the 10-digit rule) — rejected for v1.

## R5. GSTIN matching

- **Decision**: Index `gstNumber` in FTS; no separate exact-route. Add a plain `gstNumber` index for
  potential exact lookups/joins.
- **Rationale**: A 15-char GSTIN is a single FTS token; `29ABCDE*` prefix-matches it directly. A
  dedicated exact-match branch adds code for negligible benefit.

## R6. Custom fields (attributes_json) in FTS

- **Decision**: **Excluded.**
- **Rationale**: Product decision (this thread). Indexing raw `attributes_json` would also index JSON
  keys/punctuation as noise; a values-only derived column would need a JSON1 backfill in the
  migration. Current behavior already doesn't search custom fields, so exclusion is no regression and
  keeps the migration plain SQL (no JSON1 dependency).

## R7. Result bounding: Paging3 vs LIMIT

- **Decision**: **Paging3** for the browse list (blank query); **`LIMIT 100` reactive Flow** for
  search results.
- **Rationale**: Browsing 100k rows must not load all into memory → Paging3 (already wrapped in
  `thirdparty/androidx/paging/compose`, Room emits `PagingSource`). Search is naturally bounded;
  top-100 by name with a "refine search" hint matches the doc's "always LIMIT 50–100" and keeps the
  reactive Flow simple.
- **Alternatives considered**: `LIMIT 100` for both (simpler UI, but browse can't scroll past 100) —
  rejected for browse; full Paging3 over FTS results (more UI complexity, little benefit at top-100) —
  rejected for search.

## R8. FTS index maintenance (write path)

- **Decision**: Keep the existing `@Insert(onConflict = REPLACE)` write path; let Room's
  `@Fts4(contentEntity=…)`-generated triggers maintain the FTS index. No `markPendingPush`/sync
  changes.
- **Rationale**: Room auto-generates insert/update/delete content-sync triggers for a contentEntity
  FTS table; the existing repository/`CustomerSyncDelegate` writes (insert/REPLACE, hard-delete)
  fire them automatically. REPLACE churns the FTS row even on unchanged columns, but incremental
  pulls only write changed rows, so steady-state churn is negligible.
- **Risk**: first full 100k pull indexes row-by-row via triggers (tens of seconds, background, as
  rows stream in). Mitigation if needed: raise the pull batch size so each transaction covers more
  rows. Room-managed FTS4 has no drop-trigger/bulk-rebuild escape hatch.

## R9. Migration mechanics (v10 → v11)

- **Decision**: Hand-written `Migration10To11` that creates the FTS virtual table + Room's
  content-sync triggers (DDL copied verbatim from the generated `11.json` schema) + an optional
  `gstNumber` index, then `INSERT INTO customer_fts(customer_fts) VALUES('rebuild')` to populate.
- **Rationale**: Room validates the opened schema against `11.json`; the migration's FTS DDL and
  trigger names must match Room's generated output exactly or open-time validation throws. Workflow:
  build once with `exportSchema = true` to emit `11.json`, copy its `customer_fts` + trigger DDL into
  the migration. Register `CUSTOMER_MIGRATION_10_11` in all three platform DB factories.
- **No `ALTER TABLE`**: no new base-table columns are added (phone/gstNumber already exist), so the
  migration only adds the FTS table/triggers/index.
