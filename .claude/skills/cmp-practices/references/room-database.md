# Room 3 Database (KMP)

Room 3 KMP (`androidx.room3:room3-runtime` 3.0.0) for SQLite persistence across Android, iOS, and Desktop.

> **Ampairs DI/scoping is NOT covered here — it is covered authoritatively in `/metro-di` and
> `/offline-sync`.** Workspace-aware databases MUST be `@Provides @SingleIn(WorkspaceScope::class)`
> in a `@ContributesTo(WorkspaceScope::class)` platform module, created via
> `WorkspaceAwareDatabaseFactory` and registered with `WorkspaceClosableRegistry`. Do NOT provide a
> workspace DB in `AppScope`. This file covers the **Room API surface only** (entities, DAOs,
> transactions, migrations, testing).

References:
- [Room KMP setup](https://developer.android.com/kotlin/multiplatform/room)
- [Save data in Room](https://developer.android.com/training/data-storage/room)

## Project Setup (already configured — for reference)

```toml
# gradle/libs.versions.toml
androidx-room = "3.0.0"
androidx-sqlite-bundled = "2.7.0"
ksp = "2.3.9"

room-runtime  = { module = "androidx.room3:room3-runtime",  version.ref = "androidx-room" }
room-compiler = { module = "androidx.room3:room3-compiler", version.ref = "androidx-room" }
room-paging   = { module = "androidx.room3:room3-paging",   version.ref = "androidx-room" }
sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "androidx-sqlite-bundled" }

ksp  = { id = "com.google.devtools.ksp", version.ref = "ksp" }
room = { id = "androidx.room3",          version.ref = "androidx-room" }
```

Imports come from `androidx.room3.*` (NOT `androidx.room.*`) and `androidx.sqlite.*`.

```kotlin
// feature/{name}/build.gradle.kts
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
}
room3 { schemaDirectory("$projectDir/schemas") }
```

## Database Definition

```kotlin
import androidx.room3.Database
import androidx.room3.ConstructedBy
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(entities = [CustomerEntity::class], version = 1, exportSchema = true)
@ConstructedBy(CustomerDatabaseConstructor::class)
abstract class CustomerDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
}

@Suppress("KotlinNoActualForExpect")
expect object CustomerDatabaseConstructor : RoomDatabaseConstructor<CustomerDatabase> {
    override fun initialize(): CustomerDatabase
}
```

Room generates the `actual` implementations per platform. `@ConstructedBy` is required for KMP.
In Ampairs the `RoomDatabase.Builder` is produced by `WorkspaceAwareDatabaseFactory` — see `/metro-di`.
Never hand-build the builder in a feature module.

```kotlin
// Ampairs builder finalization (inside WorkspaceAwareDatabaseFactory)
builder
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)   // Dispatchers.IO is safe here (commonMain expect)
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

The `AgentCatalogDatabase` is a disposable cache — bump the version and use
`.fallbackToDestructiveMigration(dropAllTables = true)` (see `feedback_agent_models.md` Rule 5).
For persistent data DBs, write a real migration:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE customers ADD COLUMN nickname TEXT")
    }
}
builder.addMigrations(MIGRATION_1_2)
```

`@AutoMigration(from = 1, to = 2)` handles simple structural changes. Export schemas to VCS
(`room3 { schemaDirectory(...) }`). Never use destructive fallback for persistent (non-cache) data.

## Domain Mapping

Map entities to domain models at the repository boundary. Never pass `@Entity` classes to ViewModels or UI.

```kotlin
fun CustomerEntity.toDomain() = Customer(uid = uid, name = name, updatedAt = updatedAt)
fun Customer.toEntity() = CustomerEntity(uid = uid, name = name, updatedAt = updatedAt)
```

## Testing

```kotlin
class CustomerDaoTest {
    private lateinit var db: CustomerDatabase
    private lateinit var dao: CustomerDao

    @BeforeTest fun setup() {
        db = Room.inMemoryDatabaseBuilder<CustomerDatabase>()
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
| Workspace DB provided in `AppScope` | `@SingleIn(WorkspaceScope::class)` + `WorkspaceAwareDatabaseFactory` (`/metro-di`) |
| `import androidx.room.*` | `import androidx.room3.*` — this project is on Room 3 |
| `allowMainThreadQueries()` | `suspend` + `Flow` |
| `SELECT *` for projections | Projection data classes |
| Missing indexes on queried columns | `@Entity(indices = [...])` |
| `@Insert(onConflict = REPLACE)` with FKs | `@Upsert` |
| Blocking DAO functions on KMP | `suspend` or `Flow` |
| `database.withTransaction { }` in `commonMain` | `useWriterConnection { immediateTransaction { } }` |
| DAO `active = 0` update as "delete" | Repo `copy(active = false, synced = false)` so the push sends it |
