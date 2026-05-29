# Architecture & State Management

> **Project pattern:** MVI with `StateFlow<UiState>` + `Channel<UiEffect>`. Metro DI. Preserve existing conventions — do not migrate working screens.

## Source of Truth Per Screen

| Concern | Owner |
|---|---|
| Screen behavior | `StateFlow<ScreenState>` in ViewModel |
| Persisted data | Repository → Room → Ktor |
| Local visual-only state | Local Compose state (`remember`) |

## State Modeling — Four Buckets

Split state into four buckets; keep them distinct:

1. **Editable input** — raw text/choice values exactly as the user edits (`"12."`, `""`)
2. **Derived/computed** — parsed, validated, calculated values (computed property or stored)
3. **Persisted snapshot** — existing saved entity for dirty-checking comparisons
4. **Transient UI-only** — scroll position, expansion toggle — local Compose state only

```kotlin
data class CustomerFormState(
    // Bucket 1 — raw input
    val nameText: String = "",
    val emailText: String = "",
    // Bucket 2 — derived/computed
    val errors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    // Computed property (not stored)
    val canSave: Boolean get() = nameText.isNotBlank() && emailText.isNotBlank(),
)
```

**Avoid duplicated state:** don't store `total` + `formattedTotal` + `totalText` — keep one canonical value.

## Where Logic Belongs

| Logic | Where |
|---|---|
| Validation | ViewModel/domain — never composable body |
| Calculations | Pure calculator/domain service called by ViewModel |
| Async orchestration | ViewModel — launch/cancel, debounce, stale |
| One-off effects | ViewModel via `Channel<Effect>` |
| Local UI state | Composable — `LazyListState`, focus, expansion |

Not acceptable in composables: validation, derived totals, data loading, submit enablement, business decisions.

## Effect Delivery

`Channel<Effect>(Channel.BUFFERED)` with `receiveAsFlow()` — buffers for reliable delivery, single consumer, no replay.

```kotlin
class CustomerListViewModel(private val repo: CustomerRepository) : ViewModel() {
    private val _state = MutableStateFlow(CustomerListState())
    val state: StateFlow<CustomerListState> = _state.asStateFlow()

    private val _effects = Channel<CustomerListEffect>(Channel.BUFFERED)
    val effects: Flow<CustomerListEffect> = _effects.receiveAsFlow()

    fun onEvent(event: CustomerListEvent) {
        when (event) {
            is CustomerListEvent.DeleteCustomer -> deleteCustomer(event.uid)
            is CustomerListEvent.Search -> updateSearch(event.query)
        }
    }

    private fun deleteCustomer(uid: String) {
        viewModelScope.launch {
            repo.deleteCustomer(uid)
            _effects.send(CustomerListEffect.ShowSnackbar("Deleted"))
        }
    }
}
```

## Domain Layer Rules

- Zero platform imports — runs in `commonTest` without emulators
- Domain models ≠ DTOs or entities — decouple from API/DB schema
- Repository **interfaces** in domain, implementations in data layer
- Map DTOs to domain models at the repository boundary
- Use cases only for multi-step orchestration — don't wrap single repo calls

```kotlin
// Domain model (no @Serializable, no Room annotations)
data class Customer(val uid: String, val name: String, val email: String?)

// Repository interface in domain
interface CustomerRepository {
    fun observeCustomers(): Flow<List<Customer>>
    suspend fun createCustomer(customer: Customer): Result<Customer>
}
```

## Inter-Feature Communication

| Need | Pattern |
|---|---|
| React to event from another feature | `SharedFlow` event bus |
| Navigate to another feature | Feature API contract (`:api` module) |
| Shared data stream (current workspace) | Repository in `data/common` |
| Pass data back | Callback via feature API |

**Anti-patterns:** importing another feature's ViewModel; cross-feature data via `CompositionLocal`; God event bus with 50+ events.

## Module Dependency Rules

```text
shared/           → feature:*:commonMain, data/common
feature/{name}/   → data/common (never → other feature impl)
data/common/      → (no feature dependencies)
```

The `shared/` module only wires navigation + DI — no feature logic.

## State Collection and Slicing

Collect once at the route/screen boundary, slice downward to leaves:

```kotlin
// Route/Screen — collects full state once
@Composable
fun CustomerListScreen(viewModel: CustomerListViewModel = metroViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) { ... }
        }
    }

    CustomerListContent(
        items = state.customers,
        isLoading = state.isLoading,
        onEvent = viewModel::onEvent,
    )
}

// Leaf — receives only what it renders
@Composable
fun CustomerRow(customer: Customer, onDelete: () -> Unit) { ... }
```

Reusable leaf components must not know your event contract or ViewModel type.
