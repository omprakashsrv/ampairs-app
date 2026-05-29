# Clean Code & Avoiding Overengineering

## Disciplined vs Bloated MVI

| Area | Good | Overengineered |
|---|---|---|
| ViewModel | `ProductViewModel` with `onEvent()` | `BaseMviViewModel<State, Intent, Effect, Result>` with separate `reduce()` |
| Events | One sealed interface per feature | `UserEvent`, `UiEvent`, `SystemEvent`, `InternalEvent` wrappers |
| State updates | `_state.update { it.copy(...) }` in `onEvent()` | Separate `Result` type + pure `reduce()` for simple screens |
| Effects | Only for impure one-shot actions | Effects for synchronous state changes |
| UI | Route + stateless screen + leaf components | Every row/card has its own ViewModel |
| Use cases | Multi-step domain logic, real reuse | One wrapper per repository call |

## When to Use a Use Case

**Useful:**
- Multi-step orchestration (validate → create → sync → notify)
- Logic reused by 3+ call sites
- Complex domain policy worth testing in isolation

**Ceremony:**

```kotlin
// BAD — pure pass-through, no value
class GetCustomersUseCase(private val repository: CustomerRepository) {
    suspend operator fun invoke() = repository.getCustomers()
}
```

## When a ViewModel Needs One

A screen needs a dedicated ViewModel when it has: async data, multi-field editing, validation, derived calculations, navigation effects, retry/refresh logic, draft/original dirty-checking.

Lightweight state holder class is fine for: purely visual tab selection, local expansion toggle, scroll affordance, tooltip visibility.

## Naming Conventions

| Concept | Recommended | Avoid |
|---|---|---|
| Event | `CustomerEvent` | `CustomerActionEventIntent` |
| State | `CustomerState` | `CustomerViewState`, `Contract.State` |
| Effect | `CustomerEffect` | `CustomerCommandSideEffect` |
| ViewModel | `CustomerListViewModel` | `BaseCustomerViewModel` |
| Route entry | `CustomerListScreen` | `CustomerContainerFragment` |
| Leaf | `CustomerRow`, `AddressCard` | `CustomerRowWidgetComponentView` |

## Import Hygiene

**Strict rule:** never write fully qualified package paths inline. Always import at file top. Use `import ... as ...` for name clashes.

```kotlin
// ❌ Inline fully qualified path
val unit = com.ampairs.common.id_generator.UidGenerator.generateUid("CUS")

// ✅ Proper import
import com.ampairs.common.id_generator.UidGenerator
val uid = UidGenerator.generateUid("CUS")

// ✅ Import alias for name clash
import com.ampairs.unit.data.db.entity.UnitEntity as UnitDbEntity
import com.ampairs.unit.domain.model.Unit as DomainUnit
```

**Alias naming convention:** prefix/suffix with layer — `Db`, `Domain`, `Ui`, `Api`, `Dto`, `Entity`.

## Feature-First Organization

Organize by feature, then by layer. Not the other way around.

```text
// ✅ Good — feature-first
feature/customer/src/commonMain/kotlin/com/ampairs/customer/
├── data/api/
├── data/db/
├── data/repository/
├── domain/
├── di/
└── ui/

// ❌ Bad — creates cross-feature navigation maze
presentation/customer/, presentation/product/, presentation/order/
domain/customer/, domain/product/, domain/order/
data/customer/, data/product/, data/order/
```

## Anti-Overengineering Rules

- Do not create nested state holders for every card/section — only when they have independent lifecycle, async, tests, and real reuse
- Do not extract trivial composables (one-line `Text` wrappers, components used once)
- Do not create generic base ViewModels until 10+ feature ViewModels share identical boilerplate
- Do not add an abstraction until it has solved a real problem twice
- Do not force MVI migration on existing working screens — introduce MVI for new features only
- Do not create `Result`/`PartialState` 4th type unless many event sources drive the same transition

## Disciplined MVI Example

```kotlin
// ✅ Clean 3-type MVI
sealed interface CustomerEvent {
    data class OnSelected(val uid: String) : CustomerEvent
    data object OnRefresh : CustomerEvent
}

data class CustomerState(val customers: List<Customer> = emptyList(), val isLoading: Boolean = false)

sealed interface CustomerEffect {
    data class NavigateToDetail(val uid: String) : CustomerEffect
    data class ShowError(val message: String) : CustomerEffect
}

class CustomerListViewModel(private val repo: CustomerRepository) : ViewModel() {
    private val _state = MutableStateFlow(CustomerState())
    val state = _state.asStateFlow()

    private val _effects = Channel<CustomerEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: CustomerEvent) = when (event) {
        is CustomerEvent.OnSelected -> {
            viewModelScope.launch { _effects.send(CustomerEffect.NavigateToDetail(event.uid)) }
        }
        CustomerEvent.OnRefresh -> loadCustomers()
    }

    init { loadCustomers() }

    private fun loadCustomers() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                repo.observeCustomers().collect { customers ->
                    _state.update { it.copy(customers = customers, isLoading = false) }
                }
            } catch (e: Exception) {
                _effects.send(CustomerEffect.ShowError(e.message ?: "Failed to load"))
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
```
