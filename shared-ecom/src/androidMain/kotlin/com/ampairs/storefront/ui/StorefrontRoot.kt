package com.ampairs.storefront.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ampairs.storefront.di.StorefrontAppGraph
import com.ampairs.storefront.nav.StorefrontRoute
import com.ampairs.storefront.nav.storefrontEntryProvider
import com.ampairs.common.locale.AppLocale
import com.ampairs.common.locale.LocalAppLocale
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

/**
 * Root of the customer storefront ecom app, in one of two modes decided by [workspaceSlug]:
 *
 * - **Pinned** (`workspaceSlug != null`) — a white-label client build (`:clientApp`). Activates that
 *   single workspace graph at startup and lands on its storefront. No picker.
 * - **Directory** (`workspaceSlug == null`) — the common multi-store app (`:marketplaceApp`). No graph
 *   is activated at startup; the user is shown a storefront picker and a store's isolated graph is
 *   activated only when one is selected.
 *
 * Login is forced first in both modes: the start destination is [StorefrontRoute.Phone] until an
 * access token exists, then the storefront (pinned) or the directory (common) loads.
 *
 * Brand-neutral: the concrete app supplies its [workspaceSlug] (or none). [seedColor] is the initial
 * Material 3 seed; in directory mode the selected store's own brand color overrides it dynamically.
 *
 * @param workspaceSlug the pinned storefront/workspace slug, or `null` for the multi-store directory app.
 * @param seedColor the initial brand seed color. Defaults to the Ampairs green.
 */
@Composable
fun StorefrontRoot(
    graph: StorefrontAppGraph,
    workspaceSlug: String? = null,
    seedColor: Color = DefaultSeedColor,
) {
    val directoryMode = workspaceSlug == null
    val session by graph.workspaceManager.session.collectAsStateWithLifecycle()

    // Pinned mode activates its single workspace once. Directory mode waits for a selection.
    LaunchedEffect(workspaceSlug) {
        if (workspaceSlug != null) {
            graph.workspaceManager.activate(
                workspaceId = workspaceSlug,
                workspaceSlug = workspaceSlug,
            )
        }
    }

    // A selected store overrides the seed; pinned builds keep the color they were launched with.
    var seedColorState by remember { mutableStateOf(seedColor) }

    // Decide the entry screen after the token is known (login-first in both modes).
    var startDestination by remember { mutableStateOf<StorefrontRoute?>(null) }
    LaunchedEffect(session, directoryMode) {
        if (startDestination != null) return@LaunchedEffect
        // Directory mode has no startup session; pinned mode waits for the activated session.
        if (directoryMode || session != null) {
            val token = graph.tokenRepository.getAccessToken()
            startDestination = when {
                token.isNullOrBlank() -> StorefrontRoute.Phone
                directoryMode -> StorefrontRoute.Directory
                else -> StorefrontRoute.Storefront(workspaceSlug!!)
            }
        }
    }

    StorefrontTheme(seedColor = seedColorState) {
        // Surface (not Modifier.background) so LocalContentColor defaults to onSurface — Text()
        // without an explicit color would otherwise fall back to Compose's hardcoded black,
        // invisible against a dark surface in dark mode.
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
            color = MaterialTheme.colorScheme.surface,
        ) {
            val activeSession = session
            val start = startDestination
            // Pinned mode also blocks on the session (its VMs are workspace-scoped). Directory mode
            // renders on the app graph until a store is picked.
            if (start == null || (!directoryMode && activeSession == null)) {
                Loading()
                return@Surface
            }

            // Before a store is activated (directory mode) resolve VMs from the app graph; once a
            // session exists, its workspace graph (which inherits app bindings) takes over.
            val viewModelFactory = activeSession?.graph?.metroViewModelFactory ?: graph.metroViewModelFactory

            CompositionLocalProvider(
                LocalMetroViewModelFactory provides viewModelFactory,
                LocalAppLocale provides AppLocale.Default,
            ) {
                val backStack = remember(start) { mutableStateListOf<NavKey>(start) }

                val authStore = remember { ViewModelStore() }
                val authStoreOwner = remember(authStore) {
                    object : ViewModelStoreOwner {
                        override val viewModelStore: ViewModelStore = authStore
                    }
                }

                val onAuthenticated: () -> Unit = {
                    authStore.clear()
                    backStack.clear()
                    backStack.add(if (directoryMode) StorefrontRoute.Directory else StorefrontRoute.Storefront(workspaceSlug!!))
                }

                // Directory: activate the picked store's isolated graph, apply its brand color, open it.
                val onStorefrontSelected: (String, Long?) -> Unit = { slug, argb ->
                    graph.workspaceManager.activate(workspaceId = slug, workspaceSlug = slug)
                    seedColorState = argb?.let { Color(it) } ?: DefaultSeedColor
                    backStack.add(StorefrontRoute.Storefront(slug))
                }

                // Directory mode only: leaving a store (Swiggy/Zomato-style back) returns to the
                // picker and tears down that store's graph so its sync stops and the next store is
                // clean. Null in a pinned build (the store is the app's root — back exits the app).
                val onExitStore: (() -> Unit)? = if (directoryMode) {
                    {
                        graph.workspaceManager.clear()
                        seedColorState = seedColor
                        backStack.clear()
                        backStack.add(StorefrontRoute.Directory)
                    }
                } else {
                    null
                }

                // Remount on workspace switch so per-store ViewModelStores/DBs never leak across stores.
                key(activeSession?.generation ?: 0L) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        entryProvider = { navKey ->
                            storefrontEntryProvider(
                                key = navKey,
                                backStack = backStack,
                                authStoreOwner = authStoreOwner,
                                onAuthenticated = onAuthenticated,
                                onStorefrontSelected = onStorefrontSelected,
                                onExitStore = onExitStore,
                            ) ?: error("Unknown route: $navKey")
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .consumeWindowInsets(WindowInsets.systemBars)
                            .imePadding(),
                    )
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}
