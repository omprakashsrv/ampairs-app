# Testing Strategy

ViewModel-centric testing using Turbine to validate the full intent → state → event cycle. Ampairs
MVI uses `onIntent()` + `StateFlow<UiState>` (`uiState`) + `SharedFlow<UiEvent>` (`events`) — mirror
those names in tests.

## Testing Priority

1. **ViewModel tests** (highest ROI) — full intent → state → event cycle with Turbine
2. **Pure function tests** — validators, calculators, mappers in isolation
3. **Compose UI tests** — critical user flows only
4. **Platform integration tests** — only for genuinely platform-specific behavior

## Setup

```kotlin
// build.gradle.kts
commonTest.dependencies {
    implementation(kotlin("test"))
    implementation(libs.turbine)                    // app.cash.turbine — verify version in catalog
    implementation(libs.kotlinx.coroutines.test)
}
```

## Coroutines Test Harness

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class CustomerListViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest fun setUpDispatcher() { Dispatchers.setMain(testDispatcher) }
    @AfterTest  fun tearDownDispatcher() { Dispatchers.resetMain() }
}
```

## State Testing with Turbine

```kotlin
class CustomerListViewModelTest {
    private val fakeRepository = FakeCustomerRepository()
    private lateinit var viewModel: CustomerListViewModel

    @BeforeTest fun setup() { viewModel = CustomerListViewModel(fakeRepository) }

    @Test
    fun `loading customers emits loading then loaded`() = runTest {
        fakeRepository.setCustomers(listOf(testCustomer))
        viewModel.uiState.test {
            assertThat(awaitItem().isLoading).isTrue()
            val loaded = awaitItem()
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.customers).hasSize(1)
        }
    }

    @Test
    fun `repository error surfaces error state`() = runTest {
        fakeRepository.setError(RuntimeException("Network error"))
        viewModel.uiState.test {
            skipItems(1) // initial
            assertThat(awaitItem().error).isNotNull()
        }
    }
}
```

## One-off Event Testing with Turbine

```kotlin
@Test
fun `save success emits NavigateBack event`() = runTest {
    viewModel.events.test {
        viewModel.onIntent(CustomerFormIntent.OnSaveClick)
        assertThat(awaitItem()).isEqualTo(CustomerFormEvent.NavigateBack)
    }
}
```

## Fake Repositories (Preferred Over Mocks)

Fakes control success/failure without mock-framework complexity, and they honour the offline-first
contract (reactive DAO `Flow` for reads):

```kotlin
class FakeCustomerRepository : CustomerRepository {
    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    private var error: Throwable? = null

    fun setCustomers(list: List<Customer>) { _customers.value = list }
    fun setError(e: Throwable) { error = e }

    override fun observeCustomers(): Flow<List<Customer>> = _customers

    override suspend fun createCustomer(c: Customer): Result<Customer> =
        error?.let { Result.failure(it) } ?: run { _customers.update { it + c }; Result.success(c) }
}
```

## Testing Metro DI

**Never mock the DI framework.** Construct ViewModels directly with fake dependencies:

```kotlin
// GOOD
val viewModel = CustomerListViewModel(fakeRepository)

// BAD: do not manipulate AppGraph / WorkspaceGraph or mock Metro annotations
```

For integration tests that need a full graph, build a test-specific graph with test doubles rather
than reaching into the production graph:

```kotlin
@DependencyGraph(AppScope::class)
interface TestAppGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides repository: CustomerRepository): TestAppGraph
    }
}

val testGraph = createGraphFactory<TestAppGraph.Factory>().create(fakeRepository)
```

## Pure Function Tests

```kotlin
class UidGeneratorTest {
    @Test fun `uid has prefix and 32 char length`() {
        val uid = UidGenerator.generateUid("CUS")
        assertThat(uid).startsWith("CUS")
        assertThat(uid).hasLength(32)
    }
}
```

## Anti-Patterns

| Anti-pattern | Fix |
|---|---|
| Testing private implementation details | Test through `onIntent()` / `uiState` / `events` |
| Mocking DI frameworks (Metro) | Construct ViewModels directly with fakes |
| Shared mutable fixtures between tests | Fresh state per test in `@BeforeTest` |
| UI-only coverage | ViewModel tests first — they catch bugs faster |
| `Thread.sleep()` in coroutine tests | `runTest` + `advanceUntilIdle()` / Turbine |
| Calling `repository.syncXxx()` in a test to assert sync | Assert the local write + `markPendingPush`; the delegate owns the network (`/offline-sync`) |
