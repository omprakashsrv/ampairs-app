import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ampairs.common.ui.GlobalAppLayoutNav3
import com.ampairs.navigation.combinedEntryProvider
import com.ampairs.navigation.createNav3SavedStateConfig
import com.ampairs.workspace.navigation.DynamicModuleNavigationService
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Navigation 3 implementation of AppNavigation.
 *
 * Key differences from Nav2:
 * - Uses user-owned back stack (SnapshotStateList) instead of NavController
 * - Uses NavDisplay instead of NavHost
 * - Uses entry providers instead of NavGraphBuilder.composable
 * - Routes are accessed directly (no toRoute<T>() needed)
 */
@Composable
fun AppNavigationNav3(
    onNavigationServiceReady: ((DynamicModuleNavigationService?) -> Unit)? = null,
    onNavigationReady: (((String) -> Unit) -> Unit)? = null
) {
    val viewModel: AppNavigationViewModel = metroViewModel()
    val autoResumeState by viewModel.autoResumeState.collectAsState()

    // Show loading while checking auto-resume
    if (autoResumeState == null) {
        return
    }

    // Determine start destination based on auto-resume state
    val (shouldAutoResume, lastWorkspaceId) = autoResumeState!!
    val startDestination: NavKey = if (shouldAutoResume && lastWorkspaceId != null) {
        WorkspaceRoute.Modules(lastWorkspaceId)
    } else {
        Route.Login
    }

    // Create Nav3 SavedStateConfiguration for polymorphic serialization
    val savedStateConfig = remember { createNav3SavedStateConfig() }

    // Create the user-owned back stack
    // NavBackStack is essentially a SnapshotStateList<NavKey>
    val backStack = rememberNavBackStack(savedStateConfig, startDestination)

    // Track screen views with Firebase Analytics and Performance
    LaunchedEffect(backStack) {
        snapshotFlow { backStack.lastOrNull() }
            .collectLatest { currentRoute ->
                if (currentRoute != null) {
                    viewModel.onScreenChanged(currentRoute.toString())
                }
            }
    }

    // Handle unauthenticated events — business logic in ViewModel, backStack manipulation here
    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collectLatest {
            backStack.clear()
            backStack.add(Route.Login)
        }
    }

    // Set up navigation callback for desktop menu integration
    LaunchedEffect(backStack) {
        val navigationCallback: (String) -> Unit = { route ->
            println("AppNavigationNav3: Received navigation request for: $route")
            navigateToMenuItemNav3(backStack, route)
        }
        onNavigationReady?.invoke(navigationCallback)
    }

    // Clear navigationService when navigating away from workspace modules
    LaunchedEffect(backStack) {
        snapshotFlow { backStack.lastOrNull() }
            .collectLatest { currentRoute ->
                if (currentRoute != null) {
                    val routeName = currentRoute.toString()
                    val isInWorkspaceModules = routeName.contains("Modules")
                    val isInCustomerModule = routeName.contains("Customer")

                    if (!isInWorkspaceModules && !isInCustomerModule) {
                        println("AppNavigationNav3: Clearing navigationService - not in workspace/customer modules")
                        onNavigationServiceReady?.invoke(null)
                    }
                }
            }
    }

    // Create a ViewModelStore specifically for the Auth flow
    // This allows sharing LoginViewModel between Phone and OTP screens
    // while keeping it scoped only to the authentication process.
    val authViewModelStore = remember { ViewModelStore() }
    val authViewModelStoreOwner = remember(authViewModelStore) {
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = authViewModelStore
        }
    }

    // Automatically clear auth ViewModels when leaving the auth flow (e.g., navigating to Workspace)
    LaunchedEffect(backStack) {
        snapshotFlow { backStack.any { it is AuthRoute || it is Route.Login } }
            .collect { isInAuth ->
                if (!isInAuth) {
                    println("AppNavigationNav3: Clearing Auth ViewModelStore")
                    authViewModelStore.clear()
                }
            }
    }

    // Global App Layout wraps NavDisplay - header is rendered ONCE here
    GlobalAppLayoutNav3(
        backStack = backStack
    ) { globalPaddingValues ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = { key ->
                combinedEntryProvider(
                    key = key,
                    backStack = backStack,
                    onLoginSuccess = {
                        backStack.clear()
                        backStack.add(Route.Workspace)
                    },
                    onNavigationServiceReady = onNavigationServiceReady,
                    sharedViewModelStoreOwner = authViewModelStoreOwner
                )
            },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(globalPaddingValues)
        )
    }
}

/**
 * Navigate to a menu item based on its route path or module code (Nav3 version)
 */
fun navigateToMenuItemNav3(
    backStack: MutableList<NavKey>,
    route: String
) {
    when {
        // Handle legacy module codes first (backward compatibility)
        route == "business" -> backStack.add(BusinessRoute.Overview)
        route == "customer" -> backStack.add(Route.Customer)
        route == "product" -> backStack.add(Route.Product)
        route == "order" -> backStack.add(Route.Order)
        route == "invoice" -> backStack.add(Route.Invoice)
        route == "inventory" -> backStack.add(Route.Inventory)
        route == "tax" -> backStack.add(Route.Tax)
        route == "subscription" -> backStack.add(Route.Subscription)

        // Handle specific menu item paths
        route.startsWith("/customers") -> {
            when (route) {
                "/customers" -> backStack.add(Route.Customer)
                "/customers/create" -> backStack.add(com.ampairs.customer.ui.CustomerCreateRoute())
                "/customers/import" -> backStack.add(CustomerRoute.Root)
                "/customers/states" -> backStack.add(com.ampairs.customer.ui.StateListRoute)
                "/customers/types" -> backStack.add(com.ampairs.customer.ui.CustomerTypeListRoute)
                "/customers/groups" -> backStack.add(com.ampairs.customer.ui.CustomerGroupListRoute)
                "/customers/config" -> backStack.add(Route.FormConfig("customer"))
                else -> backStack.add(Route.Customer)
            }
        }

        route.startsWith("/products") -> {
            when (route) {
                "/products" -> backStack.add(Route.Product)
                "/products/create" -> backStack.add(ProductRoute.ProductForm())
                "/products/groups" -> backStack.add(ProductRoute.Group())
                "/products/import" -> backStack.add(Route.Product)
                else -> backStack.add(Route.Product)
            }
        }

        route.startsWith("/orders") -> {
            when (route) {
                "/orders" -> backStack.add(Route.Order)
                "/orders/create" -> backStack.add(OrderRoute.Root())
                "/orders/import" -> backStack.add(Route.Order)
                else -> backStack.add(Route.Order)
            }
        }

        route.startsWith("/invoices") -> {
            when (route) {
                "/invoices" -> backStack.add(Route.Invoice)
                "/invoices/create" -> backStack.add(InvoiceRoute.Root())
                "/invoices/import" -> backStack.add(Route.Invoice)
                else -> backStack.add(Route.Invoice)
            }
        }

        route.startsWith("/inventory") -> {
            backStack.add(Route.Inventory)
        }

        route.startsWith("/tax") -> {
            backStack.add(Route.Tax)
        }

        route.startsWith("/business") -> {
            when (route) {
                "/business/overview", "/business" -> backStack.add(BusinessRoute.Overview)
                "/business/profile" -> backStack.add(BusinessRoute.Profile)
                "/business/operations" -> backStack.add(BusinessRoute.Operations)
                "/business/tax" -> backStack.add(BusinessRoute.TaxConfig)
                else -> backStack.add(BusinessRoute.Overview)
            }
        }

        route.startsWith("/form-config") -> {
            val entityType = route.removePrefix("/form-config/")
            backStack.add(Route.FormConfig(entityType))
        }

        route.startsWith("/subscription") -> {
            when (route) {
                "/subscription" -> backStack.add(Route.Subscription)
                "/subscription/plans" -> backStack.add(SubscriptionRoute.Plans)
                "/subscription/usage" -> backStack.add(SubscriptionRoute.Usage)
                else -> backStack.add(Route.Subscription)
            }
        }

        else -> {
            println("AppNavigationNav3: Unknown route: $route")
        }
    }
}
