# Kotlin Coroutines & Flow

## StateFlow vs SharedFlow vs Channel

| | StateFlow | SharedFlow | Channel |
|---|---|---|---|
| Holds current value | Yes (replay=1, conflated) | No (configurable) | No |
| New collector gets | Latest value immediately | Replayed values | Nothing (consumed) |
| Delivery | All collectors | All collectors | One receiver |
| Duplicate filtering | Built-in | None | None |
| **Use for** | **UI state** | **Broadcasting** | **One-off effects** |

### Project MVI mapping

```kotlin
class CustomerListViewModel(private val repo: CustomerRepository) : ViewModel() {
    private val _state = MutableStateFlow(CustomerListState())
    val state: StateFlow<CustomerListState> = _state.asStateFlow()

    private val _effects = Channel<CustomerListEffect>(Channel.BUFFERED)
    val effects: Flow<CustomerListEffect> = _effects.receiveAsFlow()
}
```

### When to use which

- **Screen state** (loading, data, errors, form input) → `StateFlow`
- **One-off UI effects** (navigate, snackbar, haptic) → `Channel(BUFFERED)` → `receiveAsFlow()`
- **Broadcasting to multiple collectors** → `SharedFlow(replay=0)` for fire-and-forget
- **Hot data from cold source** → `stateIn(viewModelScope, WhileSubscribed(5000), initial)`

### Common mistakes

- `StateFlow` for one-off events → new collector gets latest → shows twice on config change
- `SharedFlow(replay=0)` for mandatory effects → lost when UI detached
- `Channel()` default (RENDEZVOUS) → suspends sender if no receiver; always use `Channel.BUFFERED`

## Flow Operators Quick Reference

### Transforming
| Operator | Purpose |
|---|---|
| `map { }` | Transform each value |
| `mapNotNull { }` | Transform and drop nulls |
| `filter { }` | Keep matching values |

### Flattening
| Operator | Behavior | Use when |
|---|---|---|
| `flatMapLatest { }` | Cancel previous inner flow | Search queries — only latest matters |
| `flatMapConcat { }` | Sequential | Order matters |
| `flatMapMerge { }` | Concurrent | Parallel, order irrelevant |

### Combining
| Operator | Behavior | Use when |
|---|---|---|
| `combine(a, b) { a, b -> }` | Emit when ANY emits, latest from each | Multiple independent state sources |
| `zip(a, b) { a, b -> }` | Paired emissions only | Synchronized pairs |

> **Gotcha:** `combine` waits until every upstream emits at least once before producing output. Use `onStart { emit(default) }` if needed.

### Timing / Error
| Operator | Purpose |
|---|---|
| `debounce(300)` | Wait for pause — search input |
| `distinctUntilChanged()` | Skip consecutive duplicates |
| `catch { }` | Handle upstream errors, can `emit()` fallback |
| `retry(3)` / `retryWhen { }` | Retry with backoff |
| `onEach { }` | Side effect without changing values |

## Dispatchers

| Dispatcher | Use for | CMP support |
|---|---|---|
| `Dispatchers.Main` | UI state updates | All targets |
| `Dispatchers.IO` | Network, database, file I/O | All targets (coroutines 1.7+) |
| `Dispatchers.Default` | CPU-heavy computation | All targets |

> **iOS note:** `Dispatchers.IO` is available since coroutines 1.7+. For older usage or uncertainty, use `Dispatchers.Default` or `DispatcherProvider.io` (the project's expect/actual wrapper).

**Main-safe rule:** the callee switches dispatchers, not the caller. Inject dispatcher as constructor param for testability.

```kotlin
class CustomerRepository(
    private val dao: CustomerDao,
    private val api: CustomerApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun syncCustomers() = withContext(ioDispatcher) {
        val remote = api.getCustomers()
        dao.insertAll(remote.map { it.toEntity() })
    }
}
```

## Scopes

| Scope | Lifecycle | Use for |
|---|---|---|
| `viewModelScope` | ViewModel cleared | ViewModel coroutines |
| `rememberCoroutineScope()` | Leaves composition | Compose event handlers (UI-local async) |
| `coroutineScope { }` | All children complete | Parallel decomposition — one fails → all cancel |
| `supervisorScope { }` | Children independent | Independent parallel tasks (dashboard sections) |
| Injected external `CoroutineScope` | App lifecycle (manual) | Work that must outlive the current screen |

Never use `GlobalScope` — no lifecycle, memory leak. Inject an external scope instead:

```kotlin
// Repository with app-lifecycle work (e.g., bookmark that must finish even if user navigates back)
class CustomerRepository(
    private val dao: CustomerDao,
    private val externalScope: CoroutineScope,   // inject app-scoped coroutine scope
) {
    suspend fun bookmarkCustomer(uid: String) {
        externalScope.launch { dao.bookmark(uid) }.join()   // survives ViewModel clearing
    }
}
```

### Parallel work in repository

```kotlin
// coroutineScope — both must succeed (one failure cancels the other)
suspend fun getCustomerWithOrders(uid: String): CustomerWithOrders = coroutineScope {
    val customer = async { customerDao.getCustomer(uid) }
    val orders = async { orderDao.getOrders(uid) }
    CustomerWithOrders(customer.await(), orders.await())
}

// supervisorScope — independent (one failure doesn't cancel the other)
suspend fun loadDashboard(): Dashboard = supervisorScope {
    val customers = async { customerRepo.count() }
    val orders = async { orderRepo.pendingCount() }
    Dashboard(
        customers = runCatching { customers.await() }.getOrDefault(0),
        pendingOrders = runCatching { orders.await() }.getOrDefault(0),
    )
}
```

## stateIn Strategies

Convert cold `Flow` to hot `StateFlow`. Always `val`, never per function call.

```kotlin
val customers: StateFlow<List<Customer>> = repo.observeCustomers()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
```

| Strategy | Starts | Stops | Use for |
|---|---|---|---|
| `WhileSubscribed(5000)` | First collector | 5s after last gone | ViewModel state — stops when UI gone |
| `Lazily` | First collector | Never | Expensive-to-restart shared resources |
| `Eagerly` | Immediately | Never | Data needed before first collector |

## Exception Handling

```kotlin
// Always rethrow CancellationException
try { suspendingWork() }
catch (e: CancellationException) { throw e }   // NEVER swallow
catch (e: Exception) { handleError(e) }

// In ViewModel launch
viewModelScope.launch {
    try {
        val data = repo.fetchData()
        _state.update { it.copy(data = data, isLoading = false) }
    } catch (e: IOException) {
        _state.update { it.copy(error = "Network error", isLoading = false) }
    }
}
```

## Cancellation — ensureActive()

Non-suspending loops ignore cancellation. Call `ensureActive()` or check `isActive`:

```kotlin
viewModelScope.launch {
    for (customer in customers) {
        ensureActive()   // throws CancellationException if scope was cancelled
        processCustomer(customer)
    }
}
```

Suspend functions from `kotlinx.coroutines` (e.g., `delay`, `withContext`) are already cancellation-aware.

## Anti-Patterns

| Anti-pattern | Fix |
|---|---|
| `GlobalScope.launch {}` | `viewModelScope` or injected external scope |
| `runBlocking` on Main | `launch` / `async` |
| Swallowing `CancellationException` | Always rethrow |
| Blocking I/O on `Dispatchers.Default` | `Dispatchers.IO` |
| `stateIn` per function call | Declare as `val`, create once |
| Hardcoded `Dispatchers.IO` | Inject dispatcher as constructor param |
| `catch (e: Throwable)` | `catch (e: Exception)` + rethrow `CancellationException` |
| `combine` without initial values | Add `onStart { emit(default) }` |
| Non-suspending loop with no cancellation check | `ensureActive()` at loop start |
| Exposing suspend functions from ViewModel | Expose `StateFlow` instead, launch internally |
