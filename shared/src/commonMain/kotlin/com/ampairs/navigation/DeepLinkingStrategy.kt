package com.ampairs.navigation

import com.ampairs.workspace.context.WorkspaceContextManager
import kotlinx.serialization.Serializable

/**
 * Deep linking strategy that preserves workspace context
 *
 * This approach enables deep links like:
 * - /customer/edit/123 (with workspace context from app state)
 * - /order/view/456 (with workspace context from app state)
 * - /product/create (with workspace context from app state)
 *
 * Instead of URL-based workspace context like:
 * - /w/acme-corp/customer/edit/123 (workspace in URL - complex to manage)
 */
object DeepLinkingStrategy {

    /**
     * Handle incoming deep link by preserving workspace context
     *
     * Flow:
     * 1. Parse the deep link route
     * 2. Check if workspace context exists in app state
     * 3. If no workspace context, redirect to workspace selection with return path
     * 4. If workspace context exists, navigate directly to the target route
     */
    fun handleDeepLink(
        deepLinkUrl: String,
        onNavigateToWorkspaceSelection: (returnPath: String) -> Unit,
        onNavigateToTarget: (route: Any) -> Unit
    ) {
        val workspaceManager = WorkspaceContextManager.getInstance()
        val currentWorkspace = workspaceManager.currentWorkspace.value

        // Parse the deep link to extract the target route
        val targetRoute = parseDeepLinkToRoute(deepLinkUrl)

        if (currentWorkspace != null) {
            // Workspace context exists, navigate directly to target
            onNavigateToTarget(targetRoute)
        } else {
            // No workspace context, redirect to workspace selection with return path
            onNavigateToWorkspaceSelection(deepLinkUrl)
        }
    }

    /**
     * Generate deep link URL for a route (workspace-agnostic)
     *
     * This generates clean URLs without workspace context:
     * - /customer/edit/123
     * - /order/view/456
     * - /product/create
     *
     * Workspace context is maintained in app state, not URL
     */
    fun generateDeepLink(route: Any): String {
        return when (route) {
            // Customer routes
            is CustomerDeepLinkRoute.Edit -> "/customer/edit/${route.customerId}"
            is CustomerDeepLinkRoute.View -> "/customer/view/${route.customerId}"
            is CustomerDeepLinkRoute.List -> "/customer/list"

            // Product routes
            is ProductDeepLinkRoute.Edit -> "/product/edit/${route.productId}"
            is ProductDeepLinkRoute.View -> "/product/view/${route.productId}"
            is ProductDeepLinkRoute.Create -> "/product/create"
            is ProductDeepLinkRoute.List -> "/product/list"

            // Order routes
            is OrderDeepLinkRoute.Edit -> "/order/edit/${route.orderId}"
            is OrderDeepLinkRoute.View -> "/order/view/${route.orderId}"
            is OrderDeepLinkRoute.Create -> "/order/create"
            is OrderDeepLinkRoute.List -> "/order/list"

            // Invoice routes
            is InvoiceDeepLinkRoute.Edit -> "/invoice/edit/${route.invoiceId}"
            is InvoiceDeepLinkRoute.View -> "/invoice/view/${route.invoiceId}"
            is InvoiceDeepLinkRoute.Create -> "/invoice/create"
            is InvoiceDeepLinkRoute.List -> "/invoice/list"

            // Module routes (dynamic modules)
            is ModuleDeepLinkRoute -> "/module/${route.moduleCode}/${route.path}"

            else -> "/home"
        }
    }

    /**
     * Parse deep link URL to route object
     */
    private fun parseDeepLinkToRoute(deepLinkUrl: String): Any {
        val pathSegments = deepLinkUrl.trimStart('/').split('/')

        return when {
            pathSegments.size >= 2 && pathSegments[0] == "customer" -> {
                when (pathSegments[1]) {
                    "edit" -> CustomerDeepLinkRoute.Edit(pathSegments.getOrElse(2) { "" })
                    "view" -> CustomerDeepLinkRoute.View(pathSegments.getOrElse(2) { "" })
                    "list" -> CustomerDeepLinkRoute.List
                    else -> CustomerDeepLinkRoute.List
                }
            }

            pathSegments.size >= 2 && pathSegments[0] == "product" -> {
                when (pathSegments[1]) {
                    "edit" -> ProductDeepLinkRoute.Edit(pathSegments.getOrElse(2) { "" })
                    "view" -> ProductDeepLinkRoute.View(pathSegments.getOrElse(2) { "" })
                    "create" -> ProductDeepLinkRoute.Create
                    "list" -> ProductDeepLinkRoute.List
                    else -> ProductDeepLinkRoute.List
                }
            }

            pathSegments.size >= 2 && pathSegments[0] == "order" -> {
                when (pathSegments[1]) {
                    "edit" -> OrderDeepLinkRoute.Edit(pathSegments.getOrElse(2) { "" })
                    "view" -> OrderDeepLinkRoute.View(pathSegments.getOrElse(2) { "" })
                    "create" -> OrderDeepLinkRoute.Create
                    "list" -> OrderDeepLinkRoute.List
                    else -> OrderDeepLinkRoute.List
                }
            }

            pathSegments.size >= 2 && pathSegments[0] == "invoice" -> {
                when (pathSegments[1]) {
                    "edit" -> InvoiceDeepLinkRoute.Edit(pathSegments.getOrElse(2) { "" })
                    "view" -> InvoiceDeepLinkRoute.View(pathSegments.getOrElse(2) { "" })
                    "create" -> InvoiceDeepLinkRoute.Create
                    "list" -> InvoiceDeepLinkRoute.List
                    else -> InvoiceDeepLinkRoute.List
                }
            }

            pathSegments.size >= 3 && pathSegments[0] == "module" -> {
                val moduleCode = pathSegments[1]
                val modulePath = pathSegments.drop(2).joinToString("/")
                ModuleDeepLinkRoute(moduleCode, modulePath)
            }

            else -> HomeDeepLinkRoute.Dashboard
        }
    }
}

/**
 * Deep link route definitions (workspace-agnostic)
 */

@Serializable
sealed interface CustomerDeepLinkRoute {
    @Serializable data class Edit(val customerId: String) : CustomerDeepLinkRoute
    @Serializable data class View(val customerId: String) : CustomerDeepLinkRoute
    @Serializable data object List : CustomerDeepLinkRoute
}

@Serializable
sealed interface ProductDeepLinkRoute {
    @Serializable data class Edit(val productId: String) : ProductDeepLinkRoute
    @Serializable data class View(val productId: String) : ProductDeepLinkRoute
    @Serializable data object Create : ProductDeepLinkRoute
    @Serializable data object List : ProductDeepLinkRoute
}

@Serializable
sealed interface OrderDeepLinkRoute {
    @Serializable data class Edit(val orderId: String) : OrderDeepLinkRoute
    @Serializable data class View(val orderId: String) : OrderDeepLinkRoute
    @Serializable data object Create : OrderDeepLinkRoute
    @Serializable data object List : OrderDeepLinkRoute
}

@Serializable
sealed interface InvoiceDeepLinkRoute {
    @Serializable data class Edit(val invoiceId: String) : InvoiceDeepLinkRoute
    @Serializable data class View(val invoiceId: String) : InvoiceDeepLinkRoute
    @Serializable data object Create : InvoiceDeepLinkRoute
    @Serializable data object List : InvoiceDeepLinkRoute
}

@Serializable
data class ModuleDeepLinkRoute(
    val moduleCode: String,
    val path: String
)

@Serializable
sealed interface HomeDeepLinkRoute {
    @Serializable data object Dashboard : HomeDeepLinkRoute
}