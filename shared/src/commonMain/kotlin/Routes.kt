import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
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
    data object Subscription : Route

    @Serializable
    data object Unit : Route

    @Serializable
    data class FormConfig(
        val entityType: String = ""
    ) : Route

    @Serializable
    data object Agent : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object More : Route
}

// Auth routes
@Serializable
sealed interface AuthRoute : NavKey {
    @Serializable
    data object LoginRoot : AuthRoute

    @Serializable
    data object UserSelection : AuthRoute

    @Serializable
    data object Phone : AuthRoute

    @Serializable
    data class Otp(
        val sessionId: String,
        val verificationId: String = "",
        val phoneNumber: String = ""
    ) : AuthRoute

    @Serializable
    data object UserUpdate : AuthRoute

    @Serializable
    data object AccountDeletion : AuthRoute

    @Serializable
    data object AccountRestore : AuthRoute

    @Serializable
    data object DesktopBrowserAuth : AuthRoute  // Desktop browser-based authentication
}

// Workspace routes
@Serializable
sealed interface WorkspaceRoute : NavKey {
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
        val workspaceId: String = "",
        val workspaceSlug: String = "",
    ) : WorkspaceRoute

    @Serializable
    data class ModuleStore(
        val workspaceId: String = ""
    ) : WorkspaceRoute

    @Serializable
    data object SyncStatus : WorkspaceRoute
}

// Product routes
@Serializable
sealed interface ProductRoute : NavKey {
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
    data class VariantManagement(
        val productId: String = "",
        val productName: String = ""
    ) : ProductRoute

    @Serializable
    data class VariantForm(
        val productId: String = "",
        val variantId: String? = null
    ) : ProductRoute

    @Serializable
    data object TaxInfo : ProductRoute

    @Serializable
    data object TaxCode : ProductRoute

    @Serializable
    data object Brands : ProductRoute

    @Serializable
    data object Categories : ProductRoute

    @Serializable
    data object SubCategories : ProductRoute

    @Serializable
    data class CatalogItemForm(
        val catalogType: String = "",
        val itemId: String? = null,
    ) : ProductRoute
}

// Ecom storefront routes (customer-facing standalone surface)
@Serializable
sealed interface EcomRoute : NavKey {
    /** Entry point — resolves the access gate, then hosts the shop shell. */
    @Serializable
    data class Storefront(val slug: String) : EcomRoute

    @Serializable
    data object Browse : EcomRoute

    @Serializable
    data class DrillDown(
        val category: String? = null,
        val brand: String? = null,
        val subcategory: String? = null,
        val query: String? = null,
    ) : EcomRoute

    @Serializable
    data class ProductDetail(val productId: String) : EcomRoute

    @Serializable
    data object Cart : EcomRoute

    @Serializable
    data object Checkout : EcomRoute

    @Serializable
    data class OrderPlaced(val orderRef: String) : EcomRoute

    @Serializable
    data object Orders : EcomRoute

    @Serializable
    data class OrderTracking(val orderRef: String) : EcomRoute

    @Serializable
    data object Account : EcomRoute

    @Serializable
    data object Addresses : EcomRoute
}

// Customer routes
@Serializable
sealed interface CustomerRoute : NavKey {
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
sealed interface InventoryRoute : NavKey {
    @Serializable
    data object Inventory : InventoryRoute
}

// Order routes
@Serializable
sealed interface OrderRoute : NavKey {
    @Serializable
    data class Root(
        val customer: String = "",
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
sealed interface InvoiceRoute : NavKey {
    @Serializable
    data class Root(
        val customer: String = "",
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
sealed interface BusinessRoute : NavKey {
    @Serializable
    data object Overview : BusinessRoute

    @Serializable
    data object Profile : BusinessRoute

    @Serializable
    data object Operations : BusinessRoute

    @Serializable
    data object CustomAttributes : BusinessRoute

    @Serializable
    data object Images : BusinessRoute
}

// Subscription routes
@Serializable
sealed interface SubscriptionRoute : NavKey {
    @Serializable
    data object Root : SubscriptionRoute

    @Serializable
    data object Plans : SubscriptionRoute

    @Serializable
    data class PlanDetails(
        val planCode: String = ""
    ) : SubscriptionRoute

    @Serializable
    data object Usage : SubscriptionRoute

    @Serializable
    data object PaymentHistory : SubscriptionRoute

    @Serializable
    data object PaymentMethods : SubscriptionRoute

    @Serializable
    data class Checkout(
        val planCode: String = "",
        val billingCycle: String = "MONTHLY"
    ) : SubscriptionRoute

    @Serializable
    data object Devices : SubscriptionRoute

    @Serializable
    data object Invoices : SubscriptionRoute

    @Serializable
    data class InvoiceDetail(
        val invoiceUid: String = ""
    ) : SubscriptionRoute
}