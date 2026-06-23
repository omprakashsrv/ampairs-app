import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.common.UnauthenticatedHandler
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.AppScope
import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import com.ampairs.common.firebase.performance.FirebasePerformance
import com.ampairs.common.firebase.performance.PerformanceAttributes
import com.ampairs.common.firebase.performance.PerformanceTraces
import com.ampairs.di.WorkspaceManager
import com.ampairs.workspace.db.OfflineFirstWorkspaceRepository
import com.ampairs.workspace.navigation.GlobalNavigationManager
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class AppNavigationViewModel(
    private val analytics: FirebaseAnalytics,
    private val performance: FirebasePerformance,
    private val appPreferences: AppPreferencesDataStore,
    private val workspaceRepository: OfflineFirstWorkspaceRepository,
    private val tokenRepository: TokenRepository,
    private val userWorkspaceRepository: UserWorkspaceRepository,
    private val workspaceManager: WorkspaceManager,
) : ViewModel() {

    // Triple: (shouldResume, workspaceId, workspaceSlug)
    private val _autoResumeState = MutableStateFlow<Triple<Boolean, String?, String?>?>(null)
    val autoResumeState: StateFlow<Triple<Boolean, String?, String?>?> = _autoResumeState.asStateFlow()

    val workspaceSession: StateFlow<WorkspaceManager.WorkspaceSession?> = workspaceManager.session

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    private var screenTraceJob: Job? = null

    init {
        checkAutoResume()
        observeUnauthenticated()
    }

    private fun checkAutoResume() {
        viewModelScope.launch {
            val lastUserId = appPreferences.getLastUserId().first()
            val lastWorkspaceId = appPreferences.getLastWorkspaceId().first()

            if (!lastUserId.isNullOrBlank() && !lastWorkspaceId.isNullOrBlank()) {
                tokenRepository.setCurrentUser(lastUserId)
                val hasWorkspace = userWorkspaceRepository.getWorkspaceIdForUser(lastUserId).isNotBlank()

                if (hasWorkspace) {
                    val workspace = workspaceRepository.getWorkspaceById(lastWorkspaceId)
                    if (workspace != null) {
                        workspaceManager.activateWorkspace(lastWorkspaceId, workspace.slug, lastUserId)
                        GlobalNavigationManager.getInstance().onWorkspaceSelected()
                        _autoResumeState.value = Triple(true, lastWorkspaceId, workspace.slug)
                    } else {
                        appPreferences.clearLastWorkspaceId()
                        _autoResumeState.value = Triple(false, null, null)
                    }
                } else {
                    appPreferences.clearLastWorkspaceId()
                    _autoResumeState.value = Triple(false, null, null)
                }
            } else {
                _autoResumeState.value = Triple(false, null, null)
            }
        }
    }

    private fun observeUnauthenticated() {
        viewModelScope.launch {
            UnauthenticatedHandler.onUnauthenticated.collectLatest {
                appPreferences.clearLastWorkspaceId()
                workspaceManager.clearSession()
                _logoutEvent.emit(Unit)
            }
        }
    }

    fun onScreenChanged(route: String) {
        screenTraceJob?.cancel()

        val screenName = extractScreenName(route)
        val screenClass = route.substringAfterLast(".").substringBefore("(")

        analytics.setCurrentScreen(screenName, screenClass)

        screenTraceJob = viewModelScope.launch {
            val trace = performance.newTrace(PerformanceTraces.SCREEN_LOAD).apply {
                putAttribute(PerformanceAttributes.SCREEN_NAME, screenName)
                putAttribute(PerformanceAttributes.SCREEN_CLASS, screenClass)
                start()
            }
            delay(3000)
            trace.putAttribute(PerformanceAttributes.SUCCESS, "true")
            trace.stop()
        }
    }

    private fun extractScreenName(route: String): String = when {
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
        route.contains("BusinessRoute.CustomAttributes") -> "Business_CustomAttributes"
        route.contains("BusinessRoute.Images") -> "Business_Images"
        route.contains("SubscriptionRoute.Root") -> "Subscription_Root"
        route.contains("SubscriptionRoute.Plans") -> "Subscription_Plans"
        route.contains("SubscriptionRoute.Usage") -> "Subscription_Usage"
        route.contains("SubscriptionRoute.PaymentHistory") -> "Subscription_PaymentHistory"
        route.contains("SubscriptionRoute.Devices") -> "Subscription_Devices"
        route.contains("SubscriptionRoute.Invoices") -> "Subscription_Invoices"
        route.contains("SubscriptionRoute.InvoiceDetail") -> "Subscription_InvoiceDetail"
        route.contains("OrderRoute.Root") -> "Order_Create"
        route.contains("OrderRoute.OrderView") -> "Order_View"
        route.contains("OrderRoute.Orders") -> "Order_List"
        route.contains("InvoiceRoute.Root") -> "Invoice_Create"
        route.contains("InvoiceRoute.InvoiceView") -> "Invoice_View"
        route.contains("InvoiceRoute.Invoices") -> "Invoice_List"
        route.contains("InventoryRoute.Dashboard") -> "Inventory"
        route.contains("InventoryRoute.Items") -> "Inventory_Items"
        route.contains("InventoryRoute.ItemForm") -> "Inventory_ItemForm"
        route.contains("InventoryRoute.PhysicalCount") -> "Inventory_Count"
        route.contains("InventoryRoute.Ledger") -> "Inventory_Ledger"
        route.contains("InventoryRoute.LowStock") -> "Inventory_LowStock"
        route.contains("InventoryRoute.Settings") -> "Inventory_Settings"
        route.contains("FormConfig") -> "FormConfig"
        else -> route.substringAfterLast(".").substringBefore("(")
    }
}
