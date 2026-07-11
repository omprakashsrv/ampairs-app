# Database Consolidation Plan

**Status:** Proposed (planning phase — no code changes yet)
**Problem:** Every open Room database costs native RAM — a connection pool (1 writer + reader
connections), each connection carrying its own SQLite page cache (~2 MB default), prepared-statement
cache, and WAL shared-memory mapping, plus Room's per-database `InvalidationTracker` and coroutine
machinery. With one workspace active the app currently holds **26 open databases**, multiplying that
fixed cost ~26×.

---

## 1. Current State (audited)

### AppScope databases — 3 instances, open for the whole app lifetime

| Database | Module | Entities | Notes |
|---|---|---|---|
| `AuthRoomDatabase` | feature/auth | 3 (user, token, session) | exists before workspace selection |
| `WorkspaceRoomDatabase` | feature/workspace | 9 (workspace, members, roles, modules…) | stores the workspace list itself |
| `AgentCatalogDatabase` | feature/agent | 1 (`AiModelEntity`) | disposable cache; **destructive migration** convention |

### WorkspaceScope databases — 23 instances per active workspace

agent_chat (1), business (1), customer (4), ecom (9), file (1), form_v2 (3), inventory (2),
invoice (2), notification (1), offers (1), order (2), payment (5), pricing (3), printing (4),
product (10), purchase (2), sequence (2), store (2), subscription (4), supplier (1),
sync-state (1), tax (5), unit (2) — **~61 tables spread over 23 files**
(`workspace_{slug}_{module}.db` on Android, `workspace_{slug}/{module}.db` on iOS/Desktop).

**Total: 26 open database instances** (and 26 files per workspace on disk, ×3 with `-wal`/`-shm`).

### Audit results (what makes the merge feasible)

- **No table-name collisions.** All explicit `tableName`s are unique across modules (the two
  `TaxCodeEntity` classes map to `taxCodeEntity` in product vs `tax_codes` in tax). No two
  default-named entities share a class simple name.
- **No TypeConverter conflicts.** Converters are `@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter)`
  (same class everywhere — union is a no-op) and tax's `@ColumnTypeConverters(TaxInstantConverters)`
  (column-scoped, conflict-free by construction).
- **DAO wiring is already inverted.** Repositories/ViewModels inject DAO *interfaces*; DAOs are
  produced by Metro `@Provides fun provideXxxDao(db) = db.xxxDao()` modules. Only those provider
  functions change source — **zero changes in repositories, sync delegates, or ViewModels**.
- **Constraint:** `shared-ecom` (used by `clientApp` and `marketplaceApp`) reuses
  feature/auth, feature/ecom, feature/store, feature/file — the consolidated `@Database` classes
  cannot live inside `shared/`; they must live somewhere every composition root can layer on.

---

## 2. Target: 2 databases (down from 26)

| # | Database | Scope | File | Contents |
|---|---|---|---|---|
| 1 | **`AmpairsAppDatabase`** | `AppScope` | `ampairs_app.db` | auth (3) + workspace registry (9) + agent model catalog (1) = 13 tables |
| 2 | **`AmpairsWorkspaceDatabase`** | `WorkspaceScope` | `workspace_{slug}.db` | all 23 current workspace schemas = ~61 tables |

### Why 2 and not 1

App-lifetime data (auth/workspace list/model catalog) and per-workspace data have different
lifecycles. The workspace DB must be a separate *file per workspace* so workspace switch stays
"close old graph, open new file" and a workspace can be wiped by deleting one file. Merging them
would put cross-workspace data behind one file and break the `WorkspaceScope` child-graph model.

### Agent catalog convention change (required by the merge)

`AgentCatalogDatabase` currently relies on `fallbackToDestructiveMigration(dropAllTables = true)`
because it's a re-pullable mirror of the backend model manifest. That flag **must not** be carried
onto the shared app DB — a catalog schema bump would wipe auth and log every user out. Instead, the
"disposable cache" semantics move from the file level to the table level:

- No `fallbackToDestructiveMigration` on `AmpairsAppDatabase` — ever.
- Any future catalog schema change ships as a trivial migration that just
  `DROP TABLE ai_models` + recreates it — the manifest re-pulls on next launch, so no data-mapping
  migration is ever needed for catalog tables.
- Update `.claude/memory/feedback_agent_models.md` Rule 5 accordingly when implementing.

**Runtime result: 2 open databases instead of 26** (~92% fewer connection pools/page caches), one
`InvalidationTracker` spanning the workspace tables, and 1 file per workspace instead of 23.

### Known trade-offs (accepted)

- **Single writer per file.** SQLite allows one writer per database; today 23 workspace DBs can
  write in parallel, after the merge writes serialize on one WAL writer. In practice writes are
  short transactions (sync batches, form saves) and WAL keeps readers unblocked — the paging/list
  read paths are unaffected. If a specific sync burst measurably contends, batching inside one
  transaction (already the delegate pattern) is the fix, not more files.
- **One schema version for ~61 tables.** Any feature schema change bumps the shared version and
  adds a migration in one central place. Adopt an append-only `migrations/` list in the aggregator
  module; conflicts between parallel feature branches become an explicit merge point (this is a
  feature — today per-module versions drift silently).
- **Room KSP processes the big DB in one module** — that module's incremental build cost grows.
  Feature modules themselves compile as before (they keep only entities + DAO interfaces).

### Bonus unlocked (not in scope, but free later)

All workspace tables in one file means the agent's SAFE_QUERY path can eventually support
**cross-module joins** ("sales by customer name" = invoice ⨝ customer), which is impossible today
by construction.

---

## 3. Where the code lives

New module: **`data/database`** (name: `:data:database`), depending on every feature module.
Features keep their `@Entity` classes and `@Dao` interfaces; the aggregator owns:

```
data/database/src/commonMain/.../
├── AmpairsAppDatabase.kt          # @Database(entities = [13 app entities], version = 1)
├── AmpairsWorkspaceDatabase.kt    # @Database(entities = [~61 entities], version = 1)
├── migrations/                    # append-only migration list (starts empty)
├── di/AppDatabaseDaoModule.kt     # @ContributesTo(AppScope): @Provides DAOs from app DB
├── di/WorkspaceDaoModule.kt       # @ContributesTo(WorkspaceScope): @Provides DAOs from workspace DB
└── import/LegacyDatabaseImporter.kt
data/database/src/{androidMain,iosMain,desktopMain}/
└── DatabaseModule.{platform}.kt   # the only 2 platform DB providers left
```

- Both DB classes need their own `@ConstructedBy(...Constructor)` expect/actual objects (Room KMP).
- The `@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter)` and
  `@ColumnTypeConverters(TaxInstantConverters)` annotations move onto the consolidated classes.
- Dependency direction stays acyclic: `feature/* ← data/database ← shared`. No feature depends on
  the aggregator.
- **`shared-ecom` gets its own aggregate** in a later phase (auth + ecom + store + file entities in
  its own 2-DB pair). Until then, the four feature DB classes it consumes
  (`AuthRoomDatabase`, `EcomRoomDatabase`, `StoreDatabase`, `FileRoomDatabase`) are *kept but no
  longer provided* in the main app's graphs — main app providers come only from `data/database`.
  Feature DB classes not used by shared-ecom are deleted outright.

### DI changes (mechanical)

- Delete all 23 per-feature workspace `@Provides ...Database` platform providers + the auth,
  workspace, chat providers; replace with 2 providers in `data/database` platform modules
  (workspace DB still registered via `closableRegistry.register { it.close() }`).
- Rewrite each feature's `XxxDaoModule` provider source: `db.customerDao()` where `db` is now
  `AmpairsWorkspaceDatabase` (or app DB). Signatures/return types unchanged → no downstream edits.
- `WorkspaceGraph.syncStateDatabase` (consumed by `CentralSyncService.start`) becomes a
  `SyncStateDao`-based handle from the consolidated DB — small signature change in
  `WorkspaceManager`/`CentralSyncService`.
- Agent `*QueryExecutor`s inject `AmpairsWorkspaceDatabase` instead of their module DB — the
  reader-connection pattern (`useReaderConnection { usePrepared }`) is unchanged. The per-module
  `ModuleQuerySchema` allow-lists keep query scoping exactly as strict as today.

---

## 4. Existing-data migration (offline-first — no data loss)

Users have unsynced local rows (`synced = false`) that must survive. Wipe-and-repull is **not**
acceptable. One-time import on first open of each consolidated DB:

1. Open the new consolidated DB (fresh, version 1, tables created empty). The agent catalog table
   is **excluded from import** — it's a re-pullable cache, so it starts empty and repopulates from
   the backend manifest on next launch (the bundled `ModelCatalog.kt` covers cold start); the old
   `agent_catalog` file is simply deleted.
2. For each legacy module file that exists on disk:
   a. Open it once through its **old Room class** so any pending per-module migrations run
      (legacy files can be several versions behind if the user hasn't updated in a while), close it.
   b. `ATTACH DATABASE '<legacy path>' AS legacy` on the consolidated connection.
   c. For each table: `INSERT OR REPLACE INTO main.<t> (<cols>) SELECT <cols> FROM legacy.<t>`
      using the column intersection from `PRAGMA table_info` (defensive against drift).
   d. `DETACH`, then delete the legacy file + `-wal` + `-shm`.
3. Record completion (one DataStore flag per scope+slug) so import never re-runs.
4. Import runs inside the DB provider before the instance is handed to the graph, so no feature
   code observes a half-imported state.

Old per-feature `@Database` classes and their `@ConstructedBy` actuals are kept (unreferenced by
DI) for the importer during a 2–3 release deprecation window, then deleted together with the
importer's legacy-open step (later installs import raw or start fresh from sync).

Rollback safety: legacy files are deleted only after their import transaction commits; a crash
mid-import re-runs idempotently (`INSERT OR REPLACE`, flag written last).

---

## 5. Phases

| Phase | Work | Exit criteria |
|---|---|---|
| **0. Audit** *(done)* | table names, converters, DAO wiring, shared-ecom constraint | this document |
| **1. Aggregator module** | `:data:database`, 2 DB classes + constructors, platform providers, schema export baseline | module compiles on all 3 targets |
| **2. DI rewire** | DAO providers re-sourced; per-feature DB providers removed from main-app graphs; `WorkspaceGraph`/`CentralSyncService` sync-state handle; agent query executors | app compiles all 3 targets; fresh install works end-to-end |
| **3. Legacy importer** | `LegacyDatabaseImporter` + completion flags + file cleanup | upgrade from current release preserves unsynced rows (manual test matrix: Android flat paths, iOS/Desktop dir paths) |
| **4. shared-ecom aggregate** | same pattern for clientApp/marketplaceApp; delete remaining feature DB classes | shared-ecom apps compile + run |
| **5. Cleanup** | delete deprecated feature DB classes + importer legacy-open path (after deprecation window) | 26 → 2 DB classes in the tree |

Validation per phase: `androidApp:compileDebugKotlinAndroid`, `shared:compileKotlinIosSimulatorArm64`,
`desktopApp:compileKotlin`, plus workspace-switch and sync push/pull smoke tests
(stale-data regression checklist from `/metro-di` §7 applies verbatim — the consolidated workspace
DB must still be `@SingleIn(WorkspaceScope::class)` + closable-registered).

---

## 6. Decision summary

> **2 databases: one AppScope (`ampairs_app.db`: auth + workspace registry + agent model catalog,
> with drop-and-recreate migrations for the catalog table instead of file-level destructive
> migration), and one per-workspace (`workspace_{slug}.db`: all ~61 feature tables incl. sync
> state and agent chat).**
> Down from 26 open instances to 2; DAO-level DI means the blast radius is confined to DI modules,
> the two new DB classes, and a one-time importer — repositories, sync delegates, and ViewModels
> are untouched.
