package com.ampairs.common.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.ampairs.common.navigation.NavigationUtils.navigateBack

/**
 * Common interface for handling back navigation across platforms
 */
interface BackNavigationHandler {
    @Composable
    fun HandleBackNavigation(
        enabled: Boolean = true,
        onBackPressed: () -> Unit,
    )
}

/**
 * Utility functions for navigation
 */
object NavigationUtils {
    /**
     * Safe back navigation that handles edge cases
     */
    fun NavController.navigateBack(): Boolean {
        return if (previousBackStackEntry != null) {
            popBackStack()
            true
        } else {
            false
        }
    }

    /**
     * Navigate back to a specific route, or fallback to another route if not found
     */
    fun NavController.navigateBackTo(route: Any, fallbackRoute: Any? = null) {
        val success = popBackStack(route, inclusive = false)
        if (!success && fallbackRoute != null) {
            navigate(fallbackRoute) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
    }
}

/**
 * Composable that provides platform-specific back navigation handling
 */
@Composable
expect fun PlatformBackHandler(
    enabled: Boolean = true,
    onBackPressed: () -> Unit,
)

/**
 * Common composable for handling back navigation with NavController
 */
@Composable
fun BackNavigationHandler(
    navController: NavController?,
    enabled: Boolean = true,
    fallbackRoute: Any? = null,
    onBackPressed: (() -> Unit)? = null,
) {
    PlatformBackHandler(enabled = enabled) {
        if (onBackPressed != null) {
            onBackPressed()
        } else if (navController != null) {
            val success = navController.navigateBack()
            if (!success && fallbackRoute != null) {
                navController.navigate(fallbackRoute) {
                    popUpTo(0)
                    launchSingleTop = true
                }
            }
        }
    }
}