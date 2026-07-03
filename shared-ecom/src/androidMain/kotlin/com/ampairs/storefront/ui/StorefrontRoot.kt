package com.ampairs.storefront.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
 * Root of the customer storefront ecom app. Activates the pinned workspace graph, then hosts the
 * login → ecom navigation. Login is forced first: the start destination is [StorefrontRoute.Phone]
 * until an access token exists, after which the storefront loads and its gate resolves store access.
 *
 * Brand-neutral: the concrete app (e.g. `:ambikaApp`) supplies its [workspaceSlug]. This module is
 * reused across enterprise customers, so nothing here is tenant-specific.
 *
 * @param workspaceSlug the storefront/workspace slug this build is pinned to (e.g. "ambika-enterprise").
 * @param seedColor the client's brand seed color; drives the Material 3 scheme. Defaults to the
 *   Ampairs green when a build doesn't supply one.
 */
@Composable
fun StorefrontRoot(
    graph: StorefrontAppGraph,
    workspaceSlug: String,
    seedColor: Color = DefaultSeedColor,
) {
    val session by graph.workspaceManager.session.collectAsStateWithLifecycle()

    // Activate the single pinned workspace once — no picker in the customer app.
    LaunchedEffect(workspaceSlug) {
        graph.workspaceManager.activate(
            workspaceId = workspaceSlug,
            workspaceSlug = workspaceSlug,
        )
    }

    // Decide the entry screen after the token is known.
    var startDestination by remember { mutableStateOf<StorefrontRoute?>(null) }
    LaunchedEffect(session) {
        if (session != null && startDestination == null) {
            val token = graph.tokenRepository.getAccessToken()
            startDestination = if (token.isNullOrBlank()) {
                StorefrontRoute.Phone
            } else {
                StorefrontRoute.Storefront(workspaceSlug)
            }
        }
    }

    StorefrontTheme(seedColor = seedColor) {
        Box(
            Modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            val activeSession = session
            val start = startDestination
            if (activeSession == null || start == null) {
                Loading()
                return@Box
            }

            CompositionLocalProvider(
                LocalMetroViewModelFactory provides activeSession.graph.metroViewModelFactory,
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
                    backStack.add(StorefrontRoute.Storefront(workspaceSlug))
                }

                key(activeSession.generation) {
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
