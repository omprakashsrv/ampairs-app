# Paging 3 — Offline-First with RemoteMediator

Room as the single source of truth, network as the refresh trigger. Uses `thirdparty/androidx/paging/compose/` (project's custom KMP Paging3 wrapper).

Reference: [Network + database paging](https://developer.android.com/topic/libraries/architecture/paging/v3-network-db)

## RemoteMediator — Full Implementation

```kotlin
@OptIn(ExperimentalPagingApi::class)
class CustomerRemoteMediator(
    private val api: CustomerApi,
    private val db: CustomerRoomDatabase,
    private val workspaceId: String,
) : RemoteMediator<Int, CustomerEntity>() {

    override suspend fun initialize(): InitializeAction {
        val lastUpdated = db.remoteKeyDao().getLastUpdated("customers") ?: 0L
        val cacheTimeout = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)
        return if (Clock.System.now().toEpochMilliseconds() - lastUpdated < cacheTimeout) {
            InitializeAction.SKIP_INITIAL_REFRESH   // show cached Room data immediately
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH  // fetch fresh data before showing
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, CustomerEntity>,
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val remoteKey = db.remoteKeyDao().getRemoteKey("customers")
                remoteKey?.nextPage ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        return try {
            val response = api.getCustomers(workspaceId, page, state.config.pageSize)

            db.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    db.customerDao().clearAll()
                    db.remoteKeyDao().delete("customers")
                }
                db.customerDao().insertAll(response.customers.map { it.toEntity() })
                db.remoteKeyDao().insert(
                    RemoteKey(
                        id = "customers",
                        nextPage = if (response.customers.isEmpty()) null else page + 1,
                        lastUpdated = Clock.System.now().toEpochMilliseconds(),
                    )
                )
            }

            MediatorResult.Success(endOfPaginationReached = response.customers.isEmpty())
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
```

> **Project rule:** use `Clock.System.now().toEpochMilliseconds()` not `System.currentTimeMillis()` — KMP compatibility.

## Pager Wiring

```kotlin
@OptIn(ExperimentalPagingApi::class)
val customers: Flow<PagingData<CustomerEntity>> = Pager(
    config = PagingConfig(pageSize = 20, enablePlaceholders = false),
    remoteMediator = CustomerRemoteMediator(api, db, workspaceId),
    pagingSourceFactory = { db.customerDao().pagingSource() },
).flow.cachedIn(viewModelScope)
```

The `PagingSource` reads from Room. The `RemoteMediator` fetches from network and writes to Room. The UI observes the Room-backed `PagingSource`.

## Remote Keys Entity

```kotlin
@Entity(tableName = "remote_keys")
data class RemoteKey(
    @PrimaryKey val id: String,
    val nextPage: Int?,
    val lastUpdated: Long = Clock.System.now().toEpochMilliseconds(),
)

@Dao
interface RemoteKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: RemoteKey)

    @Query("SELECT * FROM remote_keys WHERE id = :id")
    suspend fun getRemoteKey(id: String): RemoteKey?

    @Query("SELECT lastUpdated FROM remote_keys WHERE id = :id")
    suspend fun getLastUpdated(id: String): Long?

    @Query("DELETE FROM remote_keys WHERE id = :id")
    suspend fun delete(id: String)
}
```

## LoadState in UI

```kotlin
// ✅ Use source.refresh, not top-level refresh
val loadState = customers.loadState

when {
    loadState.source.refresh is LoadState.Loading -> CircularProgressIndicator()
    loadState.source.refresh is LoadState.Error -> ErrorView(retry = { customers.retry() })
    else -> { /* show content */ }
}

// Append loading indicator at end of list
if (loadState.append is LoadState.Loading) {
    item { CircularProgressIndicator(modifier = Modifier.padding(8.dp)) }
}
```

> `loadState.source.refresh` not `loadState.refresh` — the top-level convenience may report network completion before Room finishes writing, causing the loading indicator to disappear too early.

## initialize() Decision

| Return | Behavior | Use when |
|---|---|---|
| `LAUNCH_INITIAL_REFRESH` | Fetches fresh data before showing (default) | Cache expired or first load |
| `SKIP_INITIAL_REFRESH` | Shows cached Room data immediately | Cache is fresh (e.g., < 1 hour old) |

## Integration with Offline-First Repository

The RemoteMediator handles paged sync. For non-paged sync (e.g., full workspace sync on login), use the project's standard `syncXxx()` repository pattern and clear the RemoteMediator keys to force refresh next open.

```kotlin
suspend fun forceRefreshCustomers() {
    db.remoteKeyDao().delete("customers")   // clears cache timestamp → LAUNCH_INITIAL_REFRESH on next open
}
```
