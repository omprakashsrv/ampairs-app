package com.ampairs.di

import com.ampairs.agent.ui.ChatViewModel
import com.ampairs.auth.api.AuthApi
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.db.UserRepository
import com.ampairs.auth.db.dao.UserDao
import com.ampairs.auth.firebase.FirebaseAuthRepository
import com.ampairs.auth.viewmodel.AccountDeletionViewModel
import com.ampairs.auth.viewmodel.DeviceManagementViewModel
import com.ampairs.auth.viewmodel.LoginViewModel
import com.ampairs.auth.viewmodel.UserSelectionViewModel
import com.ampairs.auth.viewmodel.UserUpdateViewModel
import com.ampairs.business.data.api.BusinessApi
import com.ampairs.business.data.repository.BusinessRepository
import com.ampairs.business.ui.BusinessCustomAttributesViewModel
import com.ampairs.business.ui.BusinessImagesViewModel
import com.ampairs.business.ui.BusinessOperationsViewModel
import com.ampairs.business.ui.BusinessOverviewViewModel
import com.ampairs.business.ui.BusinessProfileViewModel
import com.ampairs.business.ui.BusinessTaxConfigViewModel
import com.ampairs.common.DeviceService
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import com.ampairs.common.firebase.crashlytics.FirebaseCrashlytics
import com.ampairs.common.firebase.messaging.FirebaseMessaging
import com.ampairs.common.firebase.performance.FirebasePerformance
import com.ampairs.common.localization.LocaleManager
import com.ampairs.common.theme.ThemeManager
import com.ampairs.customer.data.repository.CustomerGroupRepository
import com.ampairs.customer.data.repository.CustomerImageRepository
import com.ampairs.customer.data.repository.CustomerRepository
import com.ampairs.customer.data.repository.CustomerTypeRepository
import com.ampairs.customer.data.repository.ImageFilePicker
import com.ampairs.customer.ui.components.contact.ContactPickerService
import com.ampairs.customer.ui.components.location.LocationService
import com.ampairs.customer.domain.CustomerGroupStore
import com.ampairs.customer.domain.CustomerStore
import com.ampairs.customer.domain.CustomerTypeStore
import com.ampairs.customer.domain.StateStore
import com.ampairs.customer.ui.customergroup.CustomerGroupListViewModel
import com.ampairs.customer.ui.customertype.CustomerTypeListViewModel
import com.ampairs.customer.ui.list.CustomersListViewModel
import com.ampairs.customer.ui.state.StateListViewModel
import com.ampairs.form.data.repository.ConfigRepository
import com.ampairs.inventory.db.InventoryRepository
import com.ampairs.inventory.viewmodel.InventoryListViewModel
import com.ampairs.invoice.db.InvoiceRepository
import com.ampairs.invoice.viewmodel.InvoicesViewModel
import com.ampairs.order.db.OrderRepository
import com.ampairs.order.viewmodel.OrdersViewModel
import com.ampairs.product.data.repository.ProductRepository
import com.ampairs.product.domain.ProductStore
import com.ampairs.product.ui.list.ProductsListViewModel
import com.ampairs.subscription.repository.InvoiceRepository as SubscriptionInvoiceRepository
import com.ampairs.subscription.util.SubscriptionOnboardingManager
import com.ampairs.subscription.viewmodel.InvoiceViewModel as SubscriptionInvoiceViewModel
import com.ampairs.subscription.viewmodel.SubscriptionViewModelFactory
import com.ampairs.tax.calculation.TaxCalculationEngine
import com.ampairs.tax.data.repository.TaxCodeRepository
import com.ampairs.tax.data.repository.TaxComponentRepository
import com.ampairs.tax.data.repository.TaxConfigurationRepository
import com.ampairs.tax.data.repository.TaxRuleRepository
import com.ampairs.tax.ui.calculator.TaxCalculatorViewModel
import com.ampairs.tax.ui.configuration.TaxConfigurationViewModel
import com.ampairs.tax.ui.list.MyTaxCodesViewModel
import com.ampairs.tax.ui.search.TaxCodeSearchViewModel
import com.ampairs.unit.data.repository.UnitRepository
import com.ampairs.unit.ui.list.UnitListViewModel
import com.ampairs.workspace.db.OfflineFirstRolesPermissionsRepository
import com.ampairs.workspace.db.OfflineFirstWorkspaceInvitationRepository
import com.ampairs.workspace.db.OfflineFirstWorkspaceRepository
import com.ampairs.workspace.db.UserInvitationRepository
import com.ampairs.workspace.db.WorkspaceMemberRepository
import com.ampairs.workspace.db.WorkspaceModuleRepository
import com.ampairs.workspace.db.WorkspaceRepository
import com.ampairs.workspace.store.WorkspaceMemberStore
import com.ampairs.workspace.store.WorkspaceMemberUpdateStoreFactory
import com.ampairs.workspace.store.WorkspacePermissionsStore
import com.ampairs.workspace.store.WorkspaceRolesStore
import com.ampairs.workspace.viewmodel.WorkspaceCreateViewModel
import com.ampairs.workspace.viewmodel.WorkspaceListViewModel
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import io.ktor.client.engine.HttpClientEngine

/**
 * Root application graph interface.
 * Exposes all dependencies that Compose code (entry providers, screens) needs to access.
 * Platform-specific @DependencyGraph implementations are in each platform source set.
 */
interface AppGraph : ViewModelGraph {

    // ── Infrastructure ────────────────────────────────────────────────────────
    val themeManager: ThemeManager
    val localeManager: LocaleManager
    val appPreferences: AppPreferencesDataStore
    val analytics: FirebaseAnalytics
    val performance: FirebasePerformance
    val crashlytics: FirebaseCrashlytics
    val messaging: FirebaseMessaging
    val deviceService: DeviceService
    val httpEngine: HttpClientEngine

    // ── Auth ──────────────────────────────────────────────────────────────────
    val tokenRepository: TokenRepository
    val userWorkspaceRepository: UserWorkspaceRepository
    val userRepository: UserRepository
    val authApi: AuthApi
    val userDao: UserDao
    val firebaseAuthRepository: FirebaseAuthRepository

    // ── Workspace ─────────────────────────────────────────────────────────────
    val offlineFirstWorkspaceRepository: OfflineFirstWorkspaceRepository
    val workspaceRepository: WorkspaceRepository
    val workspaceMemberStore: WorkspaceMemberStore
    val workspaceMemberUpdateStoreFactory: WorkspaceMemberUpdateStoreFactory
    val workspaceMemberRepository: WorkspaceMemberRepository
    val workspaceRolesStore: WorkspaceRolesStore
    val workspacePermissionsStore: WorkspacePermissionsStore
    val workspaceModuleRepository: WorkspaceModuleRepository
    val workspaceInvitationRepository: OfflineFirstWorkspaceInvitationRepository
    val workspaceRolesPermissionsRepository: OfflineFirstRolesPermissionsRepository
    val userInvitationRepository: UserInvitationRepository

    // ── Customer ──────────────────────────────────────────────────────────────
    val customerStore: CustomerStore
    val stateStore: StateStore
    val customerTypeStore: CustomerTypeStore
    val customerGroupStore: CustomerGroupStore
    val customerRepository: CustomerRepository
    val customerGroupRepository: CustomerGroupRepository
    val customerTypeRepository: CustomerTypeRepository
    val customerImageRepository: CustomerImageRepository
    val imagePicker: ImageFilePicker
    val locationService: LocationService
    val contactPickerService: ContactPickerService

    // ── Product ───────────────────────────────────────────────────────────────
    val productStore: ProductStore
    val productRepository: ProductRepository

    // ── Order ─────────────────────────────────────────────────────────────────
    val orderRepository: OrderRepository

    // ── Invoice (feature) ─────────────────────────────────────────────────────
    val invoiceRepository: InvoiceRepository

    // ── Tax ───────────────────────────────────────────────────────────────────
    val taxCodeRepository: TaxCodeRepository
    val taxRuleRepository: TaxRuleRepository
    val taxComponentRepository: TaxComponentRepository
    val taxCalculationEngine: TaxCalculationEngine
    val taxConfigurationRepository: TaxConfigurationRepository

    // ── Unit ──────────────────────────────────────────────────────────────────
    val unitRepository: UnitRepository

    // ── Form ──────────────────────────────────────────────────────────────────
    val configRepository: ConfigRepository

    // ── Business ──────────────────────────────────────────────────────────────
    val businessRepository: BusinessRepository
    val businessApi: BusinessApi

    // ── Inventory ─────────────────────────────────────────────────────────────
    val inventoryRepository: InventoryRepository

    // ── Subscription ──────────────────────────────────────────────────────────
    val subscriptionViewModelFactory: SubscriptionViewModelFactory
    val subscriptionOnboardingManager: SubscriptionOnboardingManager
    val subscriptionInvoiceRepository: SubscriptionInvoiceRepository

    // ── ViewModel factories (Metro auto-implements from @Inject constructors) ─
    fun createLoginViewModel(): LoginViewModel
    fun createUserSelectionViewModel(): UserSelectionViewModel
    fun createUserUpdateViewModel(): UserUpdateViewModel
    fun createAccountDeletionViewModel(): AccountDeletionViewModel
    fun createDeviceManagementViewModel(): DeviceManagementViewModel
    fun createWorkspaceListViewModel(): WorkspaceListViewModel
    fun createWorkspaceCreateViewModel(): WorkspaceCreateViewModel
    fun createOrdersViewModel(): OrdersViewModel
    fun createUnitListViewModel(): UnitListViewModel
    fun createInvoicesViewModel(): InvoicesViewModel
    fun createCustomerTypeListViewModel(): CustomerTypeListViewModel
    fun createCustomerGroupListViewModel(): CustomerGroupListViewModel
    fun createStateListViewModel(): StateListViewModel
    fun createCustomersListViewModel(): CustomersListViewModel
    fun createProductsListViewModel(): ProductsListViewModel
    fun createTaxConfigurationViewModel(): TaxConfigurationViewModel
    fun createTaxCalculatorViewModel(): TaxCalculatorViewModel
    fun createTaxCodeSearchViewModel(): TaxCodeSearchViewModel
    fun createMyTaxCodesViewModel(): MyTaxCodesViewModel
    fun createBusinessOperationsViewModel(): BusinessOperationsViewModel
    fun createBusinessTaxConfigViewModel(): BusinessTaxConfigViewModel
    fun createBusinessOverviewViewModel(): BusinessOverviewViewModel
    fun createBusinessProfileViewModel(): BusinessProfileViewModel
    fun createBusinessImagesViewModel(): BusinessImagesViewModel
    fun createBusinessCustomAttributesViewModel(): BusinessCustomAttributesViewModel
    fun createInventoryListViewModel(): InventoryListViewModel
    fun createChatViewModel(): ChatViewModel
    fun createSubscriptionInvoiceViewModel(): SubscriptionInvoiceViewModel
}
