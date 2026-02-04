import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.common.UnauthenticatedHandler
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import com.ampairs.common.firebase.performance.FirebasePerformance
import com.ampairs.common.firebase.performance.PerformanceAttributes
import com.ampairs.common.firebase.performance.PerformanceTraces
import com.ampairs.common.firebase.performance.Trace
import com.ampairs.common.ui.GlobalAppLayoutNav3
import com.ampairs.navigation.combinedEntryProvider
import com.ampairs.navigation.createNav3SavedStateConfig
import com.ampairs.workspace.db.OfflineFirstWorkspaceRepository
import com.ampairs.workspace.integration.WorkspaceContextIntegration
import com.ampairs.workspace.navigation.DynamicModuleNavigationService
import com.ampairs.workspace.navigation.GlobalNavigationManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject

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
    val analytics: FirebaseAnalytics = koinInject()
    val performance: FirebasePerformance = koinInject()
    val appPreferences: AppPreferencesDataStore = koinInject()
    val workspaceRepository: OfflineFirstWorkspaceRepository = koinInject()
    val tokenRepository: TokenRepository = koinInject()
    val userWorkspaceRepository: UserWorkspaceRepository = koinInject()

    // State for auto-resume: null = checking, true = auto-resume, false = normal flow
    var autoResumeState by remember { mutableStateOf<Pair<Boolean, String?>?>(null) }

    // Check for auto-resume on initial load - BEFORE rendering NavDisplay
    LaunchedEffect(Unit) {
        val lastUserId = appPreferences.getLastUserId().first()
        val lastWorkspaceId = appPreferences.getLastWorkspaceId().first()

        if (!lastUserId.isNullOrBlank() && !lastWorkspaceId.isNullOrBlank()) {
            println("AppNavigationNav3: Checking auto-resume for user: $lastUserId, workspace: $lastWorkspaceId")

            // Verify user exists and set as current
            tokenRepository.setCurrentUser(lastUserId)
            val hasWorkspace =
                userWorkspaceRepository.getWorkspaceIdForUser(lastUserId).isNotBlank()

            if (hasWorkspace) {
                // Load workspace from local database
                val workspace = workspaceRepository.getWorkspaceById(lastWorkspaceId)
                if (workspace != null) {
                    println("AppNavigationNav3: Auto-resume: Setting workspace context for ${workspace.name}")
                    // Set workspace context BEFORE navigation
                    WorkspaceContextIntegration.setWorkspaceFromDomain(workspace)
                    GlobalNavigationManager.getInstance().onWorkspaceSelected()
                    autoResumeState = Pair(true, lastWorkspaceId)
                } else {
                    println("AppNavigationNav3: Workspace not found, clearing preferences")
                    appPreferences.clearLastWorkspaceId()
                    autoResumeState = Pair(false, null)
                }
            } else {
                println("AppNavigationNav3: User has no workspace, clearing preferences")
                appPreferences.clearLastWorkspaceId()
                autoResumeState = Pair(false, null)
            }
        } else {
            println("AppNavigationNav3: No auto-resume data found, normal flow")
            autoResumeState = Pair(false, null)
        }
    }

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

    // Track active performance traces
    var activeScreenTrace: Trace? = null

    // Track screen views with Firebase Analytics and Performance
    LaunchedEffect(backStack) {
        snapshotFlow { backStack.lastOrNull() }
            .collectLatest { currentRoute ->
                if (currentRoute != null) {
                    // Stop previous screen trace
                    activeScreenTrace?.let { trace ->
                        trace.stop()
                        println("Performance: Screen load trace stopped")
                    }

                    // Extract screen name from route
                    val screenName = extractScreenNameNav3(currentRoute.toString())
                    val screenClass = currentRoute::class.simpleName ?: "Unknown"

                    // Log screen view to Firebase Analytics
                    analytics.setCurrentScreen(screenName, screenClass)
                    println("Analytics: Screen view tracked - $screenName")

                    // Start new screen load performance trace
                    activeScreenTrace = performance.newTrace(PerformanceTraces.SCREEN_LOAD).apply {
                        putAttribute(PerformanceAttributes.SCREEN_NAME, screenName)
                        putAttribute(PerformanceAttributes.SCREEN_CLASS, screenClass)
                        start()
                        println("Performance: Screen load trace started - $screenName")
                    }

                    // Auto-stop trace after 3 seconds
                    kotlinx.coroutines.delay(3000)
                    activeScreenTrace?.let { trace ->
                        trace.putAttribute(PerformanceAttributes.SUCCESS, "true")
                        trace.stop()
                        activeScreenTrace = null
                        println("Performance: Screen load trace auto-stopped - $screenName")
                    }
                }
            }
    }

    // Handle unauthenticated events
    LaunchedEffect(Unit) {
        UnauthenticatedHandler.onUnauthenticated.collectLatest {
            // Clear workspace context and navigation service on logout
            WorkspaceContextIntegration.clearWorkspaceContext()
            // Clear last workspace ID to prevent auto-resume after logout
            appPreferences.clearLastWorkspaceId()
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
                    onNavigationServiceReady = onNavigationServiceReady
                )
            },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(globalPaddingValues)
        )
    }
}

/**
 * Extract a human-readable screen name from a Navigation 3 route
 */
private fun extractScreenNameNav3(route: String): String {
    return when {
        route.contains("Login") -> "Login"
        route.contains("UserSelection") -> "UserSelection"
        route.contains("Phone") -> "Phone"
        route.contains("Otp") -> "Otp"
        route.contains("UserUpdate") -> "UserUpdate"
        route.contains("AccountDeletion") -> "AccountDeletion"
        route.contains("AccountRestore") -> "AccountRestore"
        route.contains("DesktopBrowserAuth") -> "DesktopBrowserAuth"
        route.contains("WorkspaceRoute.Root") -> "Workspace_List"
        route.contains("WorkspaceRoute.Create") -> "Workspace_Create"
        route.contains("WorkspaceRoute.Edit") -> "Workspace_Edit"
        route.contains("WorkspaceRoute.Members") -> "Workspace_Members"
        route.contains("WorkspaceRoute.MemberDetail") -> "Workspace_MemberDetail"
        route.contains("WorkspaceRoute.Modules") -> "Workspace_Modules"
        route.contains("WorkspaceRoute.ModuleStore") -> "Workspace_ModuleStore"
        route.contains("CustomerList") -> "Customer_List"
        route.contains("CustomerDetails") -> "Customer_Details"
        route.contains("CustomerCreate") -> "Customer_Create"
        route.contains("CustomerType") -> "Customer_Type"
        route.contains("CustomerGroup") -> "Customer_Group"
        route.contains("StateList") -> "Customer_States"
        route.contains("ProductRoute.Products") -> "Product_List"
        route.contains("ProductRoute.ProductDetails") -> "Product_Details"
        route.contains("ProductRoute.ProductForm") -> "Product_Form"
        route.contains("VariantManagement") -> "Product_Variants"
        route.contains("VariantForm") -> "Product_VariantForm"
        route.contains("TaxList") -> "Tax_List"
        route.contains("TaxCalculator") -> "Tax_Calculator"
        route.contains("TaxConfiguration") -> "Tax_Configuration"
        route.contains("MyTaxCodes") -> "Tax_MyCodes"
        route.contains("TaxCodeSearch") -> "Tax_Search"
        route.contains("TaxCodeDetail") -> "Tax_Detail"
        route.contains("BusinessRoute.Overview") -> "Business_Overview"
        route.contains("BusinessRoute.Profile") -> "Business_Profile"
        route.contains("BusinessRoute.Operations") -> "Business_Operations"
        route.contains("BusinessRoute.TaxConfig") -> "Business_TaxConfig"
        route.contains("BusinessRoute.CustomAttributes") -> "Business_CustomAttributes"
        route.contains("BusinessRoute.Images") -> "Business_Images"
        route.contains("SubscriptionRoute.Root") -> "Subscription_Root"
        route.contains("SubscriptionRoute.Plans") -> "Subscription_Plans"
        route.contains("SubscriptionRoute.Usage") -> "Subscription_Usage"
        route.contains("SubscriptionRoute.PaymentHistory") -> "Subscription_PaymentHistory"
        route.contains("SubscriptionRoute.Devices") -> "Subscription_Devices"
        route.contains("SubscriptionRoute.Invoices") -> "Subscription_Invoices"
        route.contains("SubscriptionRoute.InvoiceDetail") -> "Subscription_InvoiceDetail"
        route.contains("FormConfig") -> "FormConfig"
        else -> route.substringAfterLast(".").substringBefore("(")
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
