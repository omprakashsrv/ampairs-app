# Room 3 Database (KMP)

Room 3 KMP (`androidx.room3:room3-runtime` 3.0.3) for SQLite persistence across Android, iOS, and Desktop.

> **Ampairs keeps all Room data in two consolidated databases in `:data:database`** —
> `AmpairsAppDatabase` (AppScope: auth, workspace registry, model catalog) and
> `AmpairsWorkspaceDatabase` (`@SingleIn(WorkspaceScope::class)`: every feature's tables + sync state).
> A feature module never declares a `@Database`, never applies the Room/KSP plugins and never builds a
> database; its entities + DAOs live in `:data:database` under the feature's package. How to add
> tables: CLAUDE.md → *Adding tables for a feature*. DI/scoping: `/metro-di`; sync: `/offline-sync`.
> This file covers the **Room API surface** (entities, DAOs, transactions, migrations, testing).

References:
- [Room KMP setup](https://developer.android.com/kotlin/multiplatform/room)
- [Save data in Room](https://developer.android.com/training/data-storage/room)

## Project Setup (already configured in `:data:database` only — for reference)

```toml
# gradle/libs.versions.toml
androidx-room = "3.0.3"
androidx-sqlite-bundled = "2.7.0"
ksp = "2.3.10"

room-runtime  = { module = "androidx.room3:room3-runtime",  version.ref = "androidx-room" }
room-compiler = { module = "androidx.room3:room3-compiler", version.ref = "androidx-room" }
room-paging   = { module = "androidx.room3:room3-paging",   version.ref = "androidx-room" }
sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "androidx-sqlite-bundled" }

ksp  = { id = "com.google.devtools.ksp", version.ref = "ksp" }
room = { id = "androidx.room3",          version.ref = "androidx-room" }
```

Imports come from `androidx.room3.*` (NOT `androidx.room.*`) and `androidx.sqlite.*`.

```kotlin
// data/database/build.gradle.kts — the ONLY module that runs Room KSP
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
    // never kspCommonMainMetadata for Room — it generates an `actual` in the wrong source set
}
room3 { schemaDirectory("$projectDir/schemas") }
```

A feature module only needs `implementation(projects.data.database)` + `implementation(libs.room.runtime)`.

## Database Definition

```kotlin
// data/database/src/commonMain/.../AmpairsWorkspaceDatabase.kt (abridged)
@Database(
    entities = [
        CustomerEntity::class,
        // …every feature's workspace entities, grouped by feature…
        SyncStateEntity::class,
    ],
    version = 9,              // one shared, append-only version line for all features
    exportSchema = true,
)
@ConstructedBy(AmpairsWorkspaceDatabaseConstructor::class)
abstract class AmpairsWorkspaceDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    // …
}

@Suppress("KotlinNoActualForExpect")
expect object AmpairsWorkspaceDatabaseConstructor : RoomDatabaseConstructor<AmpairsWorkspaceDatabase> {
    override fun initialize(): AmpairsWorkspaceDatabase
}
```

Room generates the `actual` implementations per platform. `@ConstructedBy` is required for KMP.
The builder lives in `data/database/src/{android,desktop,ios}Main/.../di/DatabaseModule.*.kt`:

```kotlin
Room.databaseBuilder<AmpairsWorkspaceDatabase>(/* platform path: workspace_{slug}_main.db / workspace_{slug}/main.db */)
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)   // Dispatchers.IO is safe here (commonMain expect)
    .addMigrations(/* WORKSPACE_MIGRATION_1_2, … */)
    .build()
```

## Critical Performance Rules

| Rule | Why |
|------|-----|
| Index every column in `WHERE`, `ORDER BY`, `JOIN ON` | Avoids full table scan: O(n) → O(log n) |
| Batch writes inside a transaction | Individual inserts each trigger a separate disk sync |
| Select only needed columns (projection data classes) | Reduces memory and I/O vs `SELECT *` |
| `Flow` for reactive reads, `suspend` for writes | Auto-notify on changes; keep main thread free |
| Never `allowMainThreadQueries()` | Blocks UI, causes ANRs |
| `BundledSQLiteDriver` for KMP | Consistent SQLite version across platforms |

## Entity Design

Ampairs sync entities always carry `synced` (push flag) and `active` (soft-delete) columns — see
`/offline-sync`. Keep them out of agent query schemas but never omit them from the entity.

```kotlin
@Entity(
    tableName = "customers",
    indices = [Index("active"), Index("updated_at")],
)
data class CustomerEntity(
    @PrimaryKey val uid: String,
    val name: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,   // ISO 8601 string for sync
    @ColumnInfo(defaultValue = "1") val active: Boolean = true,
    @ColumnInfo(defaultValue = "0") val synced: Boolean = false,
)
```

## DAO Patterns

```kotlin
@Dao
interface CustomerDao {
    @Upsert suspend fun upsert(customer: CustomerEntity)
    @Upsert suspend fun upsertAll(customers: List<CustomerEntity>)
    @Query("DELETE FROM customers WHERE uid = :uid") suspend fun hardDeleteByUid(uid: String)

    @Query("SELECT * FROM customers WHERE active = 1 ORDER BY updated_at DESC")
    fun observeActive(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE uid = :uid")
    suspend fun getByUid(uid: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE synced = 0")
    suspend fun getUnsynced(): List<CustomerEntity>
}
```

- `@Upsert` inserts or updates — prefer over `@Insert(onConflict = REPLACE)` which triggers cascading deletes.
- `Flow` queries auto-invalidate when the table changes.
- **KMP:** all DAO functions must be `suspend` or return `Flow`.
- **Soft delete** is a repository concern (`copy(active = false, synced = false)`), not a DAO
  `active = 0` update — see `/offline-sync` Rule.

## Transactions (KMP)

`database.withTransaction { }` is Android-only and NOT available in `commonMain`. Use the KMP API:

```kotlin
// write transaction
database.useWriterConnection { connection ->
    connection.immediateTransaction {
        customerDao.hardDeleteByUid(oldUid)
        customerDao.upsert(newEntity)
    }
}

// read-only (agent SAFE_QUERY path — see feedback_agent_models.md Rule 7)
database.useReaderConnection { connection ->
    connection.usePrepared("SELECT COUNT(*) FROM customers WHERE active = 1") { /* ... */ }
}
```

## Migrations

Both consolidated databases hold durable data — **`fallbackToDestructiveMigration` is banned** on them.
Every schema change bumps `version` and ships a migration in `data/database/.../migrations/`,
registered in `.addMigrations(...)` on **all three** platforms:

```kotlin
val WORKSPACE_MIGRATION_9_10 = object : Migration(9, 10) {
    override suspend fun migrate(connection: SQLiteConnection) {
        // CREATE statements copied verbatim from schemas/.../AmpairsWorkspaceDatabase/10.json (`createSql`)
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sfa_beats` (`id` TEXT NOT NULL, … PRIMARY KEY(`id`))")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `sfa_beat_id_idx` ON `sfa_beats` (`id`)")
    }
}
```

- Copy new tables/indexes from the exported schema JSON rather than hand-writing SQL — Room validates
  the migrated schema against it and crashes on any mismatch. Commit the new `N.json`.
- Simple column adds can be `ALTER TABLE … ADD COLUMN …` (e.g. `WORKSPACE_MIGRATION_8_9`).
- The model-catalog table (`ai_models` in `AmpairsAppDatabase`) is a disposable cache: change it with a
  migration that `DROP TABLE` + recreates it (the manifest re-pulls) — still not destructive fallback.
- The workspace version line is shared across every feature — check open branches that also bump it.

## Domain Mapping

Map entities to domain models at the repository boundary. Never pass `@Entity` classes to ViewModels or UI.

```kotlin
fun CustomerEntity.toDomain() = Customer(uid = uid, name = name, updatedAt = updatedAt)
fun Customer.toEntity() = CustomerEntity(uid = uid, name = name, updatedAt = updatedAt)
```

## Testing

```kotlin
// Repository/ViewModel tests usually use fake DAOs (e.g. RecordingSyncStateDao in the order/invoice
// tests). For a real DAO test, build the consolidated DB in memory:
class CustomerDaoTest {
    private lateinit var db: AmpairsWorkspaceDatabase
    private lateinit var dao: CustomerDao

    @BeforeTest fun setup() {
        db = Room.inMemoryDatabaseBuilder<AmpairsWorkspaceDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(UnconfinedTestDispatcher())
            .build()
        dao = db.customerDao()
    }

    @AfterTest fun teardown() = db.close()

    @Test fun `upsert and observe`() = runTest {
        dao.upsert(testCustomer)
        dao.observeActive().test { assertThat(awaitItem()).hasSize(1) }
    }
}
```

## Anti-Patterns

| Anti-pattern | Fix |
|---|---|
| A feature module with its own `@Database` / Room plugin | Entities + DAOs in `:data:database`, registered in `AmpairsWorkspaceDatabase` |
| Workspace DB provided in `AppScope` | `@SingleIn(WorkspaceScope::class)` in `DatabaseModule.*.kt` (`/metro-di`) |
| `fallbackToDestructiveMigration` | Version bump + migration from the exported schema JSON |
| `import androidx.room.*` | `import androidx.room3.*` — this project is on Room 3 |
| `allowMainThreadQueries()` | `suspend` + `Flow` |
| `SELECT *` for projections | Projection data classes |
| Missing indexes on queried columns | `@Entity(indices = [...])` |
| `@Insert(onConflict = REPLACE)` with FKs | `@Upsert` |
| Blocking DAO functions on KMP | `suspend` or `Flow` |
| `database.withTransaction { }` in `commonMain` | `useWriterConnection { immediateTransaction { } }` |
| DAO `active = 0` update as "delete" | Repo `copy(active = false, synced = false)` so the push sends it |
