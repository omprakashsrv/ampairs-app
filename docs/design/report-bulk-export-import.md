# App Design: Data Export & Bulk Upload (`feature/export`)

Companion to the backend spec `ampairs/specs/015-report-bulk-export-import/`. This document covers the **Kotlin Multiplatform client** design. Read alongside `/offline-sync`, `/metro-di`, and `/cmp-practices`.

## Goal

A module-agnostic **Export (download)** and **Import (bulk upload)** capability in the app, working **fully offline against Room** for everyday use, with an optional **SERVER** path for scale / rich Excel / validated jobs. The headline flow: export customers (with `uid`) → bulk-edit the file → re-import → each customer updated by `uid` — delivered entirely through the **existing offline-sync push** (`POST /{module}/v1/{resource}/sync`).

## Why this is mostly "free" on the client

1. Every module already holds its full dataset in **Room** → CLIENT export is a local read.
2. The `CentralSyncService` already pushes `synced=false` rows to each module's `/sync` upsert → CLIENT import is just "write rows to Room as unsynced." **No new write API, no new sync delegate per imported module.**
3. `feature/file` already wraps **FileKit** (pick) and `FileManager` (platform save/read) → file I/O exists.

So the new module is: a generic **engine** (format read/write + a per-module exporter SPI), a **templates** store (synced), local **job tracking**, and **UI**.

## Module layout

```
feature/export/src/
├── commonMain/kotlin/com/ampairs/export/
│   ├── engine/
│   │   ├── ModuleExporter.kt            # SPI: per-module columns + Room read + Room write(synced=false)
│   │   ├── ModuleExporterRegistry.kt    # Map<moduleKey, ModuleExporter> (Metro multibinding)
│   │   ├── ModuleExporterKey.kt         # @MapKey
│   │   └── format/
│   │       ├── FormatWriter.kt / FormatReader.kt        # dispatch by ExportFormat
│   │       ├── CsvWriter.kt / CsvReader.kt              # pure commonMain
│   │       ├── JsonWriter.kt / JsonReader.kt            # kotlinx.serialization
│   │       ├── XmlWriter.kt / XmlReader.kt              # pure commonMain string building
│   │       └── SpreadsheetWriter.kt / SpreadsheetReader.kt   # expect  (Excel)
│   ├── data/db/      ExportTemplateEntity/Dao, DataJobEntity/Dao, ExportDatabase
│   ├── data/repository/  ExportTemplateRepository (local-only + markPendingPush), DataJobRepository (local)
│   ├── data/api/     ExportTemplateApi(+Impl)  # /report/v1/templates/sync ;  DataJobApi  # server jobs + status + download
│   ├── domain/       ExportFormat, GenerationLocation, ImportMode, ExportColumn, ExportFilter, ExportTemplate, DataJob, ImportOutcome
│   ├── sync/         ExportTemplateSyncDelegate.kt   # @SyncEntityKey(EXPORT_TEMPLATE)
│   ├── di/           ExportModule.kt
│   └── ui/           ExportScreen, ImportScreen, TemplateEditorScreen, JobStatusScreen + ViewModels
├── androidMain/   ExportModule.android.kt (DB) · SpreadsheetWriter.android.kt (Apache POI) · file save/share
├── desktopMain/   ExportModule.desktop.kt (DB) · SpreadsheetWriter.desktop.kt (Apache POI)
├── iosMain/       ExportModule.ios.kt (DB) · SpreadsheetWriter.ios.kt (pure-Kotlin OOXML, or SERVER fallback)
└── commonMain/composeResources/values/strings.xml
```

## The per-module exporter SPI (extensibility)

Each feature module contributes one exporter — mirrors the agent module's `ModuleQueryExecutor` registration exactly.

```kotlin
// feature/customer/.../export/CustomerExporter.kt
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ModuleExporterKey("customer")
class CustomerExporter(
    private val dao: CustomerDao,
    private val syncStateDao: SyncStateDao,   // for markPendingPush after import write
) : ModuleExporter {
    override val moduleKey = "customer"
    override val columns = listOf(
        ExportColumn("uid", "UID", ColumnType.STRING, isMatchKey = true),
        ExportColumn("name", "Name", ColumnType.STRING),
        ExportColumn("phone", "Phone", ColumnType.STRING),
        ExportColumn("group_uid", "Group", ColumnType.FK_UID),
        ExportColumn("active", "Active", ColumnType.BOOLEAN, isActiveFlag = true),
        // display-only example (export-only, ignored on import):
        ExportColumn("balance_display", "Balance", ColumnType.STRING, isDisplayOnly = true),
    )

    override suspend fun readRows(f: List<ExportFilter>, sort: ExportSort?, includeInactive: Boolean):
        List<Map<String, String?>> = dao.query(f, sort, includeInactive).map { it.toExportRow() }

    // IMPORT: write to Room as unsynced + flag pending — the existing CustomerSyncDelegate push delivers it.
    override suspend fun writeRows(rows: List<Map<String, String?>>, mode: ImportMode): ImportOutcome {
        var updated = 0; var skipped = 0; val errors = mutableListOf<RowError>()
        rows.forEachIndexed { i, row ->
            val uid = row["uid"]?.takeIf { it.isNotBlank() }
            when {
                uid == null && mode == ImportMode.UPDATE_ONLY -> skipped++
                else -> {
                    val existing = uid?.let { dao.getCustomerById(it) }
                    if (existing == null && mode == ImportMode.UPDATE_ONLY) { skipped++; return@forEachIndexed }
                    val entity = (existing ?: newCustomerEntity(uid ?: UidGenerator.generateUid("CUS")))
                        .applyExportRow(row)            // validates; throws → errors.add(...)
                        .copy(synced = false)
                    dao.insertCustomer(entity); updated++
                }
            }
        }
        syncStateDao.markPendingPush(SyncEntity.CUSTOMER, Clock.System.now().toEpochMilliseconds())
        return ImportOutcome(updated = updated, skipped = skipped, errors = errors)
    }
}
```

**Onboarding a new module = add one such file** (columns + `toExportRow`/`applyExportRow`). The engine and UI need no change → satisfies the "extensible to any module" requirement (spec SC-006). A module is import-capable iff it already has a sync delegate (it does, if it's on the `/sync` contract).

> Boundary note: `ModuleExporter` and `ModuleExporterKey` live in **`data/common`** (like `ModuleQueryExecutor`) so `feature/export` doesn't depend on every feature module, and feature modules depend only on `data/common`.

## Formats — platform strategy

| Format | Where | Notes |
|---|---|---|
| CSV | `commonMain` | RFC-4180 quoting; streamed to a `Sink` in batches. |
| JSON | `commonMain` | kotlinx.serialization; array of row objects. |
| XML | `commonMain` | simple `<rows><row><col/></row></rows>`; pure string building. |
| Excel `.xlsx` | **expect/actual** | `androidMain`/`desktopMain` → Apache POI (`poi-ooxml`, JVM). `iosMain` → minimal pure-Kotlin OOXML writer over a KMP zip, **or** force `GenerationLocation.SERVER` for Excel on iOS (backend POI). CSV/JSON/XML stay offline on iOS regardless. |

POI is JVM-only, so it may only be referenced from `androidMain`/`desktopMain` — never `commonMain`. See research R4 in the backend spec for the iOS OOXML approach and the SERVER fallback.

## Generation location flag

`ExportTemplate.defaultLocation` and a per-run override drive a `GenerationLocation` (CLIENT | SERVER):
- **CLIENT** → engine runs locally (offline). Default below a row threshold or when offline.
- **SERVER** → `DataJobApi` starts a backend job (`POST /report/v1/exports` or multipart `POST /report/v1/imports/{module}`); the app tracks status via poll + the existing STOMP workspace-events channel and downloads the artifact/error report.

Heuristic default: offline or `rowCount < threshold` ⇒ CLIENT; Excel-on-iOS-without-native-writer ⇒ SERVER; otherwise user's choice.

## Templates (custom reports) — synced

- `ExportTemplateEntity` in the workspace-scoped `export` Room DB (`@SingleIn(WorkspaceScope::class)`, registered with `WorkspaceClosableRegistry`).
- `ExportTemplateRepository` is **local-only**: writes `synced=false` + `markPendingPush(SyncEntity.EXPORT_TEMPLATE)`.
- `ExportTemplateSyncDelegate` (`@ContributesIntoMap(WorkspaceScope::class) @SyncEntityKey(EXPORT_TEMPLATE)`) does the bulk push + batched pull against `/report/v1/templates/sync` — copy `CustomerSyncDelegate`.
- Add `EXPORT_TEMPLATE` to `SyncEntity`.

So a report configured on web/desktop appears on mobile and drives **offline** CLIENT export.

## Local job tracking

`DataJobEntity` is **local-only (not synced)** — tracks CLIENT job progress and caches SERVER job status so the UI survives navigation/process death. `DataJobRepository` is pure Room.

## UI / MVI

- **ExportScreen**: module picker → template picker (or "Standard report") → format → location toggle → Run. Progress + Save/Share on completion. (`AppScreenWithHeader`, `metroViewModel()`.)
- **ImportScreen**: module picker → FileKit pick → preview first N rows + detected columns → mode (Update-only / Upsert) → location toggle → Run → result summary (total/updated/created/skipped/failed) + "Download error report".
- **TemplateEditorScreen**: column checklist + reorder, typed filter rows, sort, default format/location → save (drives sync).
- All strings via `stringResource(Res.string.*)`; money/date display columns via `formatMoney`/`formatDate` with `LocalAppLocale.current` (display-only columns only — machine columns stay raw, see research R9).

Export/Import are surfaced as actions on each module's list screen (overflow menu) and/or a dedicated `Route.Export`. Either way add the route + entry provider and `reportUrl()` to `ApiUrlBuilder`.

## Wire-up checklist

- [ ] `settings.gradle.kts`: add `:feature:export`.
- [ ] `ApiUrlBuilder`: add `fun reportUrl(path: String)`.
- [ ] `SyncEntity`: add `EXPORT_TEMPLATE`.
- [ ] `data/common`: add `ModuleExporter` + `ModuleExporterKey` SPI (next to `ModuleQueryExecutor`).
- [ ] `feature/export`: engine, formats (incl. expect/actual Excel), templates DB/sync, local jobs, UI.
- [ ] Each onboarded feature module: one `*Exporter` (+ `toExportRow`/`applyExportRow`).
- [ ] `Routes.kt` + entry provider (+ `ModuleRegistry` if it gets its own nav entry).
- [ ] Compile all 3 targets: `androidApp:compileDebugKotlinAndroid`, `shared:compileKotlinIosSimulatorArm64`, `desktopApp:compileKotlin`.

## Offline answer (the user's explicit question), client view

- **CSV/JSON/XML export** and **all imports**: fully offline on the client DB. Import writes unsynced rows; the existing push reconciles on reconnect. This is the primary, recommended path for everyday volumes.
- **Excel on JVM (Android/Desktop)**: offline via POI. **Excel on iOS**: offline if the native OOXML writer is built; otherwise SERVER.
- **Large / strictly-validated jobs**: SERVER (backend POI + row-level validation + error report), online.
