import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Login : Route
    
    @Serializable
    data object Workspace : Route
    
    @Serializable
    data object Customer : Route
    
    @Serializable
    data object Product : Route
    
    @Serializable
    data object Inventory : Route
    
    @Serializable
    data object Order : Route
    
    @Serializable
    data object Invoice : Route

    @Serializable
    data object Tax : Route

    @Serializable
    data object Business : Route

    @Serializable
    data class FormConfig(
        val entityType: String = ""
    ) : Route
}

// Auth routes
@Serializable
sealed interface AuthRoute {
    @Serializable
    data object LoginRoot : AuthRoute

    @Serializable
    data object UserSelection : AuthRoute

    @Serializable
    data object Phone : AuthRoute

    @Serializable
    data class Otp(
        val sessionId: String,
        val verificationId: String = "" // Firebase verification ID (empty for backend API auth)
    ) : AuthRoute

    @Serializable
    data object UserUpdate : AuthRoute

    @Serializable
    data object DesktopBrowserAuth : AuthRoute  // Desktop browser-based authentication
}

// Workspace routes
@Serializable
sealed interface WorkspaceRoute {
    @Serializable
    data object Root : WorkspaceRoute
    
    @Serializable
    data object Create : WorkspaceRoute
    
    @Serializable
    data class Edit(
        val workspaceId: String = "",
    ) : WorkspaceRoute

    @Serializable
    data class Detail(
        val workspaceId: String = "",
    ) : WorkspaceRoute

    @Serializable
    data class Members(
        val workspaceId: String = "",
    ) : WorkspaceRoute

    @Serializable
    data class MemberDetail(
        val workspaceId: String = "",
        val memberId: String = "",
    ) : WorkspaceRoute

    @Serializable
    data class Invitations(
        val workspaceId: String = "",
    ) : WorkspaceRoute

    @Serializable
    data class CreateInvitation(
        val workspaceId: String = "",
    ) : WorkspaceRoute

    @Serializable
    data class AcceptInvitation(
        val token: String = "",
    ) : WorkspaceRoute

    @Serializable
    data class Modules(
        val workspaceId: String = ""
    ) : WorkspaceRoute

    @Serializable
    data class ModuleStore(
        val workspaceId: String = ""
    ) : WorkspaceRoute
}

// Product routes
@Serializable
sealed interface ProductRoute {
    @Serializable
    data class Group(
        val type: String = "GROUP",
        val edit: Boolean = false
    ) : ProductRoute

    @Serializable
    data class Product(
        val groupId: String = ""
    ) : ProductRoute

    @Serializable
    data class ProductEdit(
        val productId: String = ""
    ) : ProductRoute

    @Serializable
    data object Products : ProductRoute

    @Serializable
    data class ProductDetails(
        val productId: String = ""
    ) : ProductRoute

    @Serializable
    data class ProductForm(
        val productId: String? = null
    ) : ProductRoute

    @Serializable
    data object TaxInfo : ProductRoute

    @Serializable
    data object TaxCode : ProductRoute
}

// Customer routes
@Serializable
sealed interface CustomerRoute {
    @Serializable
    data object Root : CustomerRoute
    
    @Serializable
    data object CustomerView : CustomerRoute
    
    @Serializable
    data class CustomerEdit(
        val id: String = ""
    ) : CustomerRoute
    
    @Serializable
    data class Redirect(
        val fromCustomer: String = "",
        val toCustomer: String = ""
    ) : CustomerRoute
}

// Inventory routes
@Serializable
sealed interface InventoryRoute {
    @Serializable
    data object Inventory : InventoryRoute
}

// Order routes
@Serializable
sealed interface OrderRoute {
    @Serializable
    data class Root(
        val fromCustomer: String = "",
        val toCustomer: String = "",
        val id: String = ""
    ) : OrderRoute
    
    @Serializable
    data class OrderView(
        val id: String = ""
    ) : OrderRoute
    
    @Serializable
    data object Orders : OrderRoute
}

// Invoice routes
@Serializable
sealed interface InvoiceRoute {
    @Serializable
    data class Root(
        val fromCustomer: String = "",
        val toCustomer: String = "",
        val id: String = ""
    ) : InvoiceRoute

    @Serializable
    data class InvoiceView(
        val id: String = ""
    ) : InvoiceRoute

    @Serializable
    data object Invoices : InvoiceRoute
}

// Business routes
@Serializable
sealed interface BusinessRoute {
    @Serializable
    data object Overview : BusinessRoute

    @Serializable
    data object Profile : BusinessRoute

    @Serializable
    data object Operations : BusinessRoute

    @Serializable
    data object TaxConfig : BusinessRoute

    @Serializable
    data object CustomAttributes : BusinessRoute
}