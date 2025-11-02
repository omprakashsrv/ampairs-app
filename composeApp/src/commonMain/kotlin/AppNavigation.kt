import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigation.toRoute
import com.ampairs.auth.authNavigation
import com.ampairs.business.businessNavigation
import com.ampairs.common.UnauthenticatedHandler
import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import com.ampairs.common.firebase.performance.FirebasePerformance
import com.ampairs.common.firebase.performance.PerformanceAttributes
import com.ampairs.common.firebase.performance.PerformanceTraces
import com.ampairs.common.firebase.performance.Trace
import com.ampairs.common.ui.AppScreenWithHeader
import com.ampairs.customer.ui.CustomerCreateRoute
import com.ampairs.customer.ui.StateListRoute
import com.ampairs.customer.ui.customerNavigation
import com.ampairs.product.productNavigation
import com.ampairs.tax.ui.navigation.taxNavigation
import com.ampairs.workspace.context.WorkspaceContextManager
import com.ampairs.workspace.integration.WorkspaceContextIntegration
import com.ampairs.workspace.navigation.DynamicModuleNavigationService
import com.ampairs.workspace.navigation.GlobalNavigationManager
import com.ampairs.workspace.workspaceNavigation
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject

@Composable
fun AppNavigation(
    onNavigationServiceReady: ((DynamicModuleNavigationService?) -> Unit)? = null,
    onNavigationReady: (((String) -> Unit) -> Unit)? = null
) {
    val navController = rememberNavController()
    val workspaceManager = WorkspaceContextManager.getInstance()
    val analytics: FirebaseAnalytics = koinInject()
    val performance: FirebasePerformance = koinInject()

    // Track active performance traces
    var activeScreenTrace: Trace? = null

    // Track screen views with Firebase Analytics and Performance
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collectLatest { backStackEntry ->
            val route = backStackEntry.destination.route
            if (route != null) {
                // Stop previous screen trace
                activeScreenTrace?.let { trace ->
                    trace.stop()
                    println("Performance: Screen load trace stopped")
                }

                // Extract screen name from route
                val screenName = extractScreenName(route)
                val screenClass = route.substringBefore("?").substringBefore("/")

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

                // Auto-stop trace after 3 seconds (screens should load faster than this)
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

    // Set up navigation callback for desktop menu integration
    LaunchedEffect(navController) {
        val navigationCallback: (String) -> Unit = { route ->
            println("AppNavigation: Received navigation request for: $route")
            navigateToMenuItem(navController, route)
        }
        onNavigationReady?.invoke(navigationCallback)
    }

    // Get global navigation manager instance
    val globalNavigationManager = GlobalNavigationManager.getInstance()

    LaunchedEffect(Unit) {
        UnauthenticatedHandler.onUnauthenticated.collectLatest {
            // Clear workspace context and navigation service on logout using integration
            WorkspaceContextIntegration.clearWorkspaceContext()
            navController.navigate(Route.Login) {
                popUpTo(0)
            }
        }
    }

    // Clear navigationService when navigating away from workspace modules
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collectLatest { backStackEntry ->
            val currentRoute = backStackEntry.destination.route
            println("AppNavigation: Current route: $currentRoute")

            // Clear navigationService when not in workspace modules or customer modules
            val isInWorkspaceModules = currentRoute?.contains("workspace/modules") == true
            val isInCustomerModule = currentRoute?.contains("Route.Customer") == true ||
                    currentRoute?.contains("com.ampairs.customer") == true

            if (currentRoute != null && !isInWorkspaceModules && !isInCustomerModule) {
                println("AppNavigation: Clearing navigationService - not in workspace/customer modules")
                onNavigationServiceReady?.invoke(null)
            }
        }
    }

    // Remove the automatic workspace selection redirection for now
    // This was causing infinite loops and should be handled differently
    // The workspace selection should be handled by the individual screens that require workspace context

    NavHost(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars),
        navController = navController, startDestination = Route.Login
    ) {
        authNavigation(navController) {
            val options = navOptions {
                popUpTo<AuthRoute.LoginRoot> {
                    this.inclusive = true
                }
                launchSingleTop = true // Avoid multiple instances of the same destination
            }
            navController.navigate(
                route = Route.Workspace,
                navOptions = options
            )
        }
        workspaceNavigation(navController, onNavigationServiceReady)
        // Customer module navigation
        composable<Route.Customer> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                com.ampairs.customer.ui.CustomerScreen(
                    onCustomerClick = { customerId ->
                        navController.navigate(
                            com.ampairs.customer.ui.CustomerDetailsRoute(
                                customerId
                            )
                        )
                    },
                    onCreateCustomer = {
                        navController.navigate(CustomerCreateRoute())
                    },
                    onFormConfig = {
                        println("AppNavigation Route.Customer: Navigating to FormConfig")
                        navController.navigate(Route.FormConfig("customer"))
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Product module navigation
        composable<Route.Product> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                com.ampairs.product.ProductScreen(
                    onProductClick = { productId ->
                        navController.navigate(ProductRoute.ProductDetails(productId))
                    },
                    onCreateProduct = {
                        navController.navigate(ProductRoute.ProductForm())
                    },
                    onFormConfig = {
                        println("AppNavigation Route.Product: Navigating to FormConfig")
                        navController.navigate(Route.FormConfig("product"))
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Tax module navigation
        composable<Route.Tax> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                com.ampairs.tax.ui.navigation.TaxScreen(
                    onNavigateToHsnCodes = {
                        navController.navigate(com.ampairs.tax.ui.navigation.HsnCodesListRoute)
                    },
                    onNavigateToTaxCalculator = {
                        navController.navigate(com.ampairs.tax.ui.navigation.TaxCalculatorRoute)
                    },
                    onNavigateToTaxRates = {
                        navController.navigate(com.ampairs.tax.ui.navigation.TaxRatesRoute)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Form Config navigation
        composable<Route.FormConfig> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.FormConfig>()
            com.ampairs.form.ui.FormConfigScreen(
                entityType = route.entityType,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        customerNavigation(navController)
        productNavigation(navController)
        taxNavigation(navController)
        businessNavigation(navController)
        // Temporarily commented out pending customer integration updates
        // inventoryNavigation(navController) { }
        // orderNavigation(navController) { }
        // invoiceNavigation(navController) { }
    }
}

/**
 * Extract a human-readable screen name from a navigation route
 * Examples:
 * - "Route.Login" → "Login"
 * - "AuthRoute.Phone" → "Phone"
 * - "WorkspaceRoute.Members/{workspaceId}" → "Workspace_Members"
 * - "com.ampairs.customer.ui.CustomerDetailRoute/{customerId}" → "Customer_Detail"
 */
private fun extractScreenName(route: String): String {
    // Remove path parameters and query parameters
    val cleanRoute = route
        .substringBefore("?")
        .substringBefore("/{")
        .substringBefore("/{")

    return when {
        // Handle main routes (Route.*)
        cleanRoute.startsWith("Route.") -> {
            cleanRoute.substringAfter("Route.")
        }
        // Handle auth routes (AuthRoute.*)
        cleanRoute.startsWith("AuthRoute.") -> {
            cleanRoute.substringAfter("AuthRoute.")
        }
        // Handle workspace routes (WorkspaceRoute.*)
        cleanRoute.startsWith("WorkspaceRoute.") -> {
            "Workspace_" + cleanRoute.substringAfter("WorkspaceRoute.")
        }
        // Handle business routes (BusinessRoute.*)
        cleanRoute.startsWith("BusinessRoute.") -> {
            "Business_" + cleanRoute.substringAfter("BusinessRoute.")
        }
        // Handle customer routes (CustomerRoute.* or com.ampairs.customer.ui.*)
        cleanRoute.startsWith("CustomerRoute.") -> {
            "Customer_" + cleanRoute.substringAfter("CustomerRoute.")
        }
        cleanRoute.contains("com.ampairs.customer.ui.") -> {
            val screenName = cleanRoute.substringAfterLast(".")
                .replace("Route", "")
            "Customer_$screenName"
        }
        // Handle product routes
        cleanRoute.startsWith("ProductRoute.") -> {
            "Product_" + cleanRoute.substringAfter("ProductRoute.")
        }
        cleanRoute.contains("com.ampairs.product.") -> {
            val screenName = cleanRoute.substringAfterLast(".")
                .replace("Route", "")
            "Product_$screenName"
        }
        // Handle tax routes
        cleanRoute.startsWith("TaxRoute.") -> {
            "Tax_" + cleanRoute.substringAfter("TaxRoute.")
        }
        cleanRoute.contains("com.ampairs.tax.") -> {
            val screenName = cleanRoute.substringAfterLast(".")
                .replace("Route", "")
            "Tax_$screenName"
        }
        // Default: use the last part of the route
        else -> {
            cleanRoute.substringAfterLast(".")
                .replace("Route", "")
                .ifEmpty { cleanRoute }
        }
    }
}

/**
 * Navigate to a menu item based on its route path or module code
 * Maps menu item paths to type-safe navigation routes
 */
fun navigateToMenuItem(navController: androidx.navigation.NavHostController, route: String) {
    when {
        // Handle legacy module codes first (backward compatibility)
        route == "business" -> navController.navigate(BusinessRoute.Profile)
        route == "customer" -> navController.navigate(Route.Customer)
        route == "product" -> navController.navigate(Route.Product)
        route == "order" -> navController.navigate(Route.Order)
        route == "invoice" -> navController.navigate(Route.Invoice)
        route == "inventory" -> navController.navigate(Route.Inventory)
        route == "tax" -> navController.navigate(Route.Tax)

        // Handle specific menu item paths
        route.startsWith("/customers") -> {
            when (route) {
                "/customers" -> navController.navigate(Route.Customer)
                "/customers/create" -> navController.navigate(CustomerCreateRoute())
                "/customers/import" -> navController.navigate(CustomerRoute.Root)
                "/customers/states" -> navController.navigate(StateListRoute)
                "/customers/types" -> navController.navigate(com.ampairs.customer.ui.CustomerTypeListRoute)
                "/customers/groups" -> navController.navigate(com.ampairs.customer.ui.CustomerGroupListRoute)
                "/customers/config" -> navController.navigate(Route.FormConfig("customer"))
                else -> navController.navigate(Route.Customer)
            }
        }

        route.startsWith("/products") -> {
            when (route) {
                "/products" -> navController.navigate(Route.Product)
                "/products/create" -> navController.navigate(ProductRoute.ProductForm())
                "/products/groups" -> navController.navigate(ProductRoute.Group())
                "/products/import" -> navController.navigate(Route.Product) // Route to main product page
                else -> navController.navigate(Route.Product)
            }
        }

        route.startsWith("/orders") -> {
            when (route) {
                "/orders" -> navController.navigate(Route.Order)
                "/orders/create" -> navController.navigate(OrderRoute.Root())
                "/orders/import" -> navController.navigate(Route.Order)
                else -> navController.navigate(Route.Order)
            }
        }

        route.startsWith("/invoices") -> {
            when (route) {
                "/invoices" -> navController.navigate(Route.Invoice)
                "/invoices/create" -> navController.navigate(InvoiceRoute.Root())
                "/invoices/import" -> navController.navigate(Route.Invoice)
                else -> navController.navigate(Route.Invoice)
            }
        }

        route.startsWith("/inventory") -> {
            navController.navigate(Route.Inventory)
        }

        route.startsWith("/tax") -> {
            navController.navigate(Route.Tax)
        }

        route.startsWith("/business") -> {
            when (route) {
                "/business/overview", "/business" -> navController.navigate(BusinessRoute.Overview)
                "/business/profile" -> navController.navigate(BusinessRoute.Profile)
                "/business/operations" -> navController.navigate(BusinessRoute.Operations)
                "/business/tax" -> navController.navigate(BusinessRoute.TaxConfig)
                else -> navController.navigate(BusinessRoute.Overview)
            }
        }

        route.startsWith("/form-config") -> {
            // Extract entity type from route like /form-config/customer
            val entityType = route.removePrefix("/form-config/")
            navController.navigate(Route.FormConfig(entityType))
        }

        else -> {
            println("AppNavigation: Unknown route: $route")
        }
    }
}
