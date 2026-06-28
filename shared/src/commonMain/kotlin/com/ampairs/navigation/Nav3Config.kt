package com.ampairs.navigation

import AuthRoute
import BusinessRoute
import CustomerRoute
import InventoryRoute
import InvoiceRoute
import OrderRoute
import PaymentRoute
import ProductRoute
import Route
import SubscriptionRoute
import WorkspaceRoute
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.ampairs.customer.ui.CustomerCreateRoute
import com.ampairs.customer.ui.CustomerDetailsRoute
import com.ampairs.customer.ui.CustomerGroupCreateRoute
import com.ampairs.customer.ui.CustomerGroupListRoute
import com.ampairs.customer.ui.CustomerListRoute
import com.ampairs.customer.ui.CustomerTypeCreateRoute
import com.ampairs.customer.ui.CustomerTypeListRoute
import com.ampairs.customer.ui.StateListRoute
import com.ampairs.supplier.ui.SupplierCreateRoute
import com.ampairs.supplier.ui.SupplierDetailsRoute
import com.ampairs.supplier.ui.SupplierListRoute
import com.ampairs.purchase.ui.PurchaseRoute
import com.ampairs.tax.ui.navigation.MyTaxCodesRoute
import com.ampairs.tax.ui.navigation.TaxCalculatorRoute
import com.ampairs.tax.ui.navigation.TaxCodeDetailRoute
import com.ampairs.tax.ui.navigation.TaxCodeSearchRoute
import com.ampairs.tax.ui.navigation.TaxConfigurationRoute
import com.ampairs.tax.ui.navigation.TaxListRoute
import com.ampairs.unit.ui.UnitFormRoute
import com.ampairs.unit.ui.UnitListRoute
import com.ampairs.printing.ui.PrinterListRoute
import com.ampairs.printing.ui.PrintQueueRoute
import com.ampairs.printing.ui.TemplateEditRoute
import com.ampairs.printing.ui.TemplateListRoute
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Creates the SavedStateConfiguration for Navigation 3 with polymorphic serialization.
 * All route classes must be registered here for proper state restoration.
 */
fun createNav3SavedStateConfig(): SavedStateConfiguration {
    return SavedStateConfiguration {
        serializersModule = SerializersModule {
            polymorphic(NavKey::class) {
                // Main Routes
                subclass(Route.Login::class)
                subclass(Route.Workspace::class)
                subclass(Route.Customer::class)
                subclass(Route.Supplier::class)
                subclass(Route.Product::class)
                subclass(Route.Inventory::class)
                subclass(Route.Order::class)
                subclass(Route.Invoice::class)
                subclass(Route.Purchase::class)
                subclass(Route.Tax::class)
                subclass(Route.Business::class)
                subclass(Route.Subscription::class)
                subclass(Route.Unit::class)
                subclass(Route.Storefront::class)
                subclass(Route.FormConfig::class)
                subclass(Route.Agent::class)
                subclass(Route.More::class)
                subclass(Route.Printing::class)
                subclass(Route.Payment::class)

                // Auth Routes
                subclass(AuthRoute.LoginRoot::class)
                subclass(AuthRoute.UserSelection::class)
                subclass(AuthRoute.Phone::class)
                subclass(AuthRoute.Otp::class)
                subclass(AuthRoute.UserUpdate::class)
                subclass(AuthRoute.AccountDeletion::class)
                subclass(AuthRoute.AccountRestore::class)
                subclass(AuthRoute.DesktopBrowserAuth::class)

                // Workspace Routes
                subclass(WorkspaceRoute.Root::class)
                subclass(WorkspaceRoute.Create::class)
                subclass(WorkspaceRoute.Edit::class)
                subclass(WorkspaceRoute.Detail::class)
                subclass(WorkspaceRoute.Members::class)
                subclass(WorkspaceRoute.MemberDetail::class)
                subclass(WorkspaceRoute.Invitations::class)
                subclass(WorkspaceRoute.CreateInvitation::class)
                subclass(WorkspaceRoute.AcceptInvitation::class)
                subclass(WorkspaceRoute.Modules::class)
                subclass(WorkspaceRoute.ModuleStore::class)

                // Product Routes
                subclass(ProductRoute.Group::class)
                subclass(ProductRoute.Product::class)
                subclass(ProductRoute.ProductEdit::class)
                subclass(ProductRoute.Products::class)
                subclass(ProductRoute.ProductDetails::class)
                subclass(ProductRoute.ProductForm::class)
                subclass(ProductRoute.VariantManagement::class)
                subclass(ProductRoute.VariantForm::class)
                subclass(ProductRoute.TaxInfo::class)
                subclass(ProductRoute.TaxCode::class)

                // Customer Routes (sealed interface)
                subclass(CustomerRoute.Root::class)
                subclass(CustomerRoute.CustomerView::class)
                subclass(CustomerRoute.CustomerEdit::class)
                subclass(CustomerRoute.Redirect::class)

                // Customer Navigation Routes (standalone)
                subclass(CustomerListRoute::class)
                subclass(CustomerDetailsRoute::class)
                subclass(CustomerCreateRoute::class)
                subclass(StateListRoute::class)
                subclass(CustomerTypeListRoute::class)
                subclass(CustomerTypeCreateRoute::class)
                subclass(CustomerGroupListRoute::class)
                subclass(CustomerGroupCreateRoute::class)

                // Supplier Navigation Routes (standalone)
                subclass(SupplierListRoute::class)
                subclass(SupplierDetailsRoute::class)
                subclass(SupplierCreateRoute::class)

                // Purchase Routes
                subclass(PurchaseRoute.PurchaseListRoute::class)
                subclass(PurchaseRoute.PurchaseDetailsRoute::class)
                subclass(PurchaseRoute.PurchaseCreateRoute::class)

                // Inventory Routes
                subclass(InventoryRoute.Dashboard::class)
                subclass(InventoryRoute.Items::class)
                subclass(InventoryRoute.ItemForm::class)
                subclass(InventoryRoute.PhysicalCount::class)
                subclass(InventoryRoute.Ledger::class)
                subclass(InventoryRoute.LowStock::class)
                subclass(InventoryRoute.Settings::class)

                // Order Routes
                subclass(OrderRoute.Root::class)
                subclass(OrderRoute.OrderView::class)
                subclass(OrderRoute.Orders::class)

                // Invoice Routes
                subclass(InvoiceRoute.Root::class)
                subclass(InvoiceRoute.InvoiceView::class)
                subclass(InvoiceRoute.Invoices::class)

                // Payment Routes
                subclass(PaymentRoute.Dashboard::class)
                subclass(PaymentRoute.SelectParty::class)
                subclass(PaymentRoute.Record::class)
                subclass(PaymentRoute.Adjustment::class)
                subclass(PaymentRoute.OpeningBalance::class)
                subclass(PaymentRoute.Statement::class)
                subclass(PaymentRoute.PartyPayments::class)

                // Business Routes
                subclass(BusinessRoute.Overview::class)
                subclass(BusinessRoute.Profile::class)
                subclass(BusinessRoute.Operations::class)
                subclass(BusinessRoute.CustomAttributes::class)
                subclass(BusinessRoute.Images::class)

                // Subscription Routes
                subclass(SubscriptionRoute.Root::class)
                subclass(SubscriptionRoute.Plans::class)
                subclass(SubscriptionRoute.PlanDetails::class)
                subclass(SubscriptionRoute.Usage::class)
                subclass(SubscriptionRoute.PaymentHistory::class)
                subclass(SubscriptionRoute.PaymentMethods::class)
                subclass(SubscriptionRoute.Checkout::class)
                subclass(SubscriptionRoute.Devices::class)
                subclass(SubscriptionRoute.Invoices::class)
                subclass(SubscriptionRoute.InvoiceDetail::class)

                // Tax Navigation Routes (standalone)
                subclass(TaxListRoute::class)
                subclass(TaxConfigurationRoute::class)
                subclass(TaxCalculatorRoute::class)
                subclass(MyTaxCodesRoute::class)
                subclass(TaxCodeSearchRoute::class)
                subclass(TaxCodeDetailRoute::class)

                // Unit Navigation Routes (standalone)
                subclass(UnitListRoute::class)
                subclass(UnitFormRoute::class)

                // Printing Routes
                subclass(PrinterListRoute::class)
                subclass(PrintQueueRoute::class)
                subclass(TemplateListRoute::class)
                subclass(TemplateEditRoute::class)
            }
        }
    }
}
