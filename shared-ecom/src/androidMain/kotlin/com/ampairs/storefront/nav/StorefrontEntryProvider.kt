package com.ampairs.storefront.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.auth.ui.AccountRestoreScreen
import com.ampairs.auth.ui.OtpScreen
import com.ampairs.auth.ui.PhoneScreen
import com.ampairs.auth.ui.UserUpdateScreen
import com.ampairs.auth.viewmodel.LoginNavEvent
import com.ampairs.auth.viewmodel.LoginViewModel
import com.ampairs.auth.viewmodel.UserUpdateNavEvent
import com.ampairs.auth.viewmodel.UserUpdateViewModel
import com.ampairs.ecom.ui.EcomStorefrontScreen
import com.ampairs.ecom.ui.account.AccountScreen
import com.ampairs.ecom.ui.account.AddressesScreen
import com.ampairs.ecom.ui.cart.CartScreen
import com.ampairs.ecom.ui.catalog.DrillDownArgs
import com.ampairs.ecom.ui.catalog.DrillDownScreen
import com.ampairs.ecom.ui.catalog.ProductDetailScreen
import com.ampairs.ecom.ui.checkout.CheckoutScreen
import com.ampairs.ecom.ui.checkout.OrderPlacedScreen
import com.ampairs.ecom.ui.directory.StorefrontDirectoryScreen
import com.ampairs.ecom.ui.order.OrderTrackingScreen
import com.ampairs.ecom.ui.order.OrdersListScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Maps [StorefrontRoute]s to the reused feature/auth + feature/ecom screens. Login is forced first
 * (see [StorefrontNavHost]); once authenticated, [onAuthenticated] drops the user on the storefront and
 * the ecom gate resolves store access.
 */
fun storefrontEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
    authStoreOwner: ViewModelStoreOwner,
    onAuthenticated: () -> Unit,
    onStorefrontSelected: (slug: String, brandColorArgb: Long?) -> Unit,
    onExitStore: (() -> Unit)?,
): NavEntry<NavKey>? = when (key) {

    // ── Login ─────────────────────────────────────────────────────────────
    is StorefrontRoute.Phone -> NavEntry(key) {
        val viewModel = loginViewModel(authStoreOwner)
        LaunchedEffect(Unit) {
            viewModel.navEvent.collectLatest { event ->
                when (event) {
                    LoginNavEvent.LoginSuccess, LoginNavEvent.NavigateToWorkspace -> onAuthenticated()
                    LoginNavEvent.NavigateToUserUpdate -> backStack.add(StorefrontRoute.UserUpdate)
                    LoginNavEvent.NavigateToAccountRestore -> backStack.add(StorefrontRoute.AccountRestore)
                    else -> {}
                }
            }
        }
        PhoneScreen(
            viewModel = viewModel,
            onAuthSuccess = { sessionId, verificationId ->
                backStack.add(
                    StorefrontRoute.Otp(
                        sessionId = sessionId,
                        verificationId = verificationId,
                        phoneNumber = viewModel.state.value.phoneNumber,
                    ),
                )
            },
        )
    }

    is StorefrontRoute.Otp -> NavEntry(key) {
        val viewModel = loginViewModel(authStoreOwner)
        LaunchedEffect(Unit) {
            viewModel.navEvent.collectLatest { event ->
                when (event) {
                    LoginNavEvent.LoginSuccess, LoginNavEvent.NavigateToWorkspace -> onAuthenticated()
                    LoginNavEvent.NavigateToUserUpdate -> backStack.add(StorefrontRoute.UserUpdate)
                    LoginNavEvent.NavigateToAccountRestore -> backStack.add(StorefrontRoute.AccountRestore)
                    else -> {}
                }
            }
        }
        OtpScreen(
            viewModel = viewModel,
            sessionId = key.sessionId,
            verificationId = key.verificationId,
            phoneNumber = key.phoneNumber,
            onAuthSuccess = { viewModel.handleOtpSuccess() },
        )
    }

    is StorefrontRoute.UserUpdate -> NavEntry(key) {
        val viewModel: UserUpdateViewModel = metroViewModel()
        LaunchedEffect(Unit) {
            viewModel.navEvent.collectLatest { event ->
                when (event) {
                    is UserUpdateNavEvent.LoginSuccess,
                    is UserUpdateNavEvent.NavigateToWorkspace -> onAuthenticated()
                }
            }
        }
        UserUpdateScreen(onProfilePictureUpdated = {})
    }

    is StorefrontRoute.AccountRestore -> NavEntry(key) {
        AccountRestoreScreen(
            onRestoreSuccess = { backStack.add(StorefrontRoute.UserUpdate) },
            onLogout = {
                backStack.clear()
                backStack.add(StorefrontRoute.Phone)
            },
        )
    }

    // ── Storefront directory / picker (common multi-store app) ────────────
    is StorefrontRoute.Directory -> NavEntry(key) {
        StorefrontDirectoryScreen(onStorefrontSelected = onStorefrontSelected)
    }

    // ── Storefront + ordering ─────────────────────────────────────────────
    is StorefrontRoute.Storefront -> NavEntry(key) {
        EcomStorefrontScreen(
            slug = key.slug,
            onRequireLogin = {
                backStack.clear()
                backStack.add(StorefrontRoute.Phone)
            },
            onOpenSearch = { backStack.add(StorefrontRoute.DrillDown(query = "")) },
            onOpenCart = { backStack.add(StorefrontRoute.Cart) },
            onOpenProduct = { backStack.add(StorefrontRoute.ProductDetail(it)) },
            onOpenDrillDown = { args ->
                backStack.add(StorefrontRoute.DrillDown(args.category, args.brand, args.subcategory, args.query))
            },
            onOpenOrder = { backStack.add(StorefrontRoute.OrderTracking(it)) },
            onOpenAddresses = { backStack.add(StorefrontRoute.Addresses) },
            onExitStore = onExitStore,
        )
    }

    is StorefrontRoute.DrillDown -> NavEntry(key) {
        EcomScaffold { snackbar ->
            DrillDownScreen(
                args = DrillDownArgs(key.category, key.brand, key.subcategory, key.query),
                snackbar = snackbar,
                onBack = { backStack.removeLastOrNull() },
                onOpenProduct = { backStack.add(StorefrontRoute.ProductDetail(it)) },
                onOpenCart = { backStack.add(StorefrontRoute.Cart) },
            )
        }
    }

    is StorefrontRoute.ProductDetail -> NavEntry(key) {
        EcomScaffold { snackbar ->
            ProductDetailScreen(
                productId = key.productId,
                snackbar = snackbar,
                onBack = { backStack.removeLastOrNull() },
                onOpenCart = { backStack.add(StorefrontRoute.Cart) },
            )
        }
    }

    is StorefrontRoute.Cart -> NavEntry(key) {
        EcomScaffold { snackbar ->
            CartScreen(
                snackbar = snackbar,
                onBack = { backStack.removeLastOrNull() },
                onCheckout = { backStack.add(StorefrontRoute.Checkout) },
            )
        }
    }

    is StorefrontRoute.Checkout -> NavEntry(key) {
        EcomScaffold { snackbar ->
            CheckoutScreen(
                snackbar = snackbar,
                onBack = { backStack.removeLastOrNull() },
                onAddAddress = { backStack.add(StorefrontRoute.Addresses) },
                onOrderPlaced = { ref -> backStack.add(StorefrontRoute.OrderPlaced(ref)) },
            )
        }
    }

    is StorefrontRoute.OrderPlaced -> NavEntry(key) {
        OrderPlacedScreen(
            orderRef = key.orderRef,
            onTrack = { backStack.add(StorefrontRoute.OrderTracking(key.orderRef)) },
            onContinue = {
                val idx = backStack.indexOfFirst { it is StorefrontRoute.Storefront }
                if (idx >= 0) while (backStack.size > idx + 1) backStack.removeLastOrNull()
            },
        )
    }

    is StorefrontRoute.OrderTracking -> NavEntry(key) {
        OrderTrackingScreen(orderRef = key.orderRef, onBack = { backStack.removeLastOrNull() })
    }

    is StorefrontRoute.Orders -> NavEntry(key) {
        OrdersListScreen(onOpenOrder = { backStack.add(StorefrontRoute.OrderTracking(it)) })
    }

    is StorefrontRoute.Account -> NavEntry(key) {
        AccountScreen(
            onOpenOrders = { backStack.add(StorefrontRoute.Orders) },
            onOpenAddresses = { backStack.add(StorefrontRoute.Addresses) },
            onLogin = {
                backStack.clear()
                backStack.add(StorefrontRoute.Phone)
            },
            onLoggedOut = { backStack.removeLastOrNull() },
        )
    }

    is StorefrontRoute.Addresses -> NavEntry(key) {
        AddressesScreen(onBack = { backStack.removeLastOrNull() })
    }

    else -> null
}

/** Shared [LoginViewModel] across the Phone + Otp steps (scoped to the auth flow store owner). */
@Composable
private fun loginViewModel(owner: ViewModelStoreOwner): LoginViewModel =
    metroViewModel(viewModelStoreOwner = owner, key = "storefront_auth_login_viewmodel")

@Composable
private fun EcomScaffold(content: @Composable (SnackbarHostState) -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { content(snackbar) }
    }
}
