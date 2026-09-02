package com.ampairs.database.di

import com.ampairs.agent.data.db.dao.ChatMessageDao
import com.ampairs.analytics.data.db.dao.DemandForecastDao
import com.ampairs.business.agent.BusinessAgentDao
import com.ampairs.business.data.db.BusinessDao
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.customer.agent.CustomerAgentDao
import com.ampairs.customer.data.db.CustomerDao
import com.ampairs.customer.data.db.CustomerGroupDao
import com.ampairs.customer.data.db.CustomerTypeDao
import com.ampairs.customer.data.db.StateDao
import com.ampairs.database.AmpairsWorkspaceDatabase
import com.ampairs.form.agent.FormAgentDao
import com.ampairs.form.data.db.FormFieldDao
import com.ampairs.form.data.db.FormSchemaDao
import com.ampairs.form.data.db.FormSectionDao
import com.ampairs.inventory.agent.InventoryAgentDao
import com.ampairs.inventory.data.db.InventoryItemDao
import com.ampairs.inventory.data.db.InventoryTransactionDao
import com.ampairs.invoice.agent.InvoiceAgentDao
import com.ampairs.invoice.db.dao.InvoiceDao
import com.ampairs.invoice.db.dao.InvoiceItemDao
import com.ampairs.notification.data.db.dao.NotificationDao
import com.ampairs.order.agent.OrderAgentDao
import com.ampairs.order.db.dao.OrderDao
import com.ampairs.order.db.dao.OrderItemDao
import com.ampairs.payment.agent.PaymentAgentDao
import com.ampairs.payment.data.db.dao.AdjustmentVoucherDao
import com.ampairs.payment.data.db.dao.LedgerEntryDao
import com.ampairs.payment.data.db.dao.PartyBalanceDao
import com.ampairs.payment.data.db.dao.PaymentAllocationDao
import com.ampairs.payment.data.db.dao.PaymentVoucherDao
import com.ampairs.pricing.data.db.dao.GeoZoneDao
import com.ampairs.pricing.data.db.dao.OfferDao
import com.ampairs.pricing.data.db.dao.PriceListDao
import com.ampairs.pricing.data.db.dao.PriceListItemDao
import com.ampairs.printing.data.db.PrintJobDao
import com.ampairs.printing.data.db.PrintRoutingDao
import com.ampairs.printing.data.db.PrintTemplateDao
import com.ampairs.printing.data.db.PrinterDao
import com.ampairs.product.agent.ProductAgentDao
import com.ampairs.product.db.dao.BrandDao
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.product.db.dao.ProductStandardCostDao
import com.ampairs.product.db.dao.ProductVariantDao
import com.ampairs.product.db.dao.SubCategoryDao
import com.ampairs.product.db.dao.TaxCodeDao as ProductTaxCodeDao
import com.ampairs.product.db.dao.TaxInfoDao
import com.ampairs.product.db.dao.VariantAttributeDao
import com.ampairs.purchase.db.dao.PurchaseDao
import com.ampairs.sequence.data.db.dao.SequenceAllocationDao
import com.ampairs.sequence.data.db.dao.SequenceDefinitionDao
import com.ampairs.subscription.db.SubscriptionDao
import com.ampairs.supplier.data.db.SupplierDao
import com.ampairs.tax.agent.TaxAgentDao
import com.ampairs.tax.data.db.dao.TaxCodeDao
import com.ampairs.tax.data.db.dao.TaxComponentDao
import com.ampairs.tax.data.db.dao.TaxComponentTypeDao
import com.ampairs.tax.data.db.dao.TaxConfigurationDao
import com.ampairs.tax.data.db.dao.TaxRuleDao
import com.ampairs.unit.agent.UnitAgentDao
import com.ampairs.unit.data.db.dao.UnitConversionDao
import com.ampairs.unit.data.db.dao.UnitDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/**
 * Sources every workspace-scoped DAO from the consolidated [AmpairsWorkspaceDatabase]. DAO types
 * are unchanged from the old per-feature modules, so repositories, sync delegates and ViewModels
 * are untouched by the consolidation.
 *
 * The ecom/store/file/sync-state DAOs (and the [com.ampairs.common.agent.WorkspaceDatabaseProvider]
 * binding) are deliberately NOT provided here even though this module also owns those tables —
 * they live in `:shared`'s `EcomFileStoreSyncDaoModule` instead. `:shared-ecom` depends on this
 * module (for the storefront `@Database` class definitions) but does NOT depend on `:shared`, and
 * provides that same DAO subset from its own `StorefrontWorkspaceDatabase`. Since Metro merges every
 * `@ContributesTo(WorkspaceScope::class)` visible on the classpath, keeping those providers here
 * would duplicate-bind them in the storefront apps' graph.
 */
@ContributesTo(WorkspaceScope::class)
interface WorkspaceDatabaseDaoModule {
    companion object {
        // customer
        @Provides
        fun provideCustomerDao(db: AmpairsWorkspaceDatabase): CustomerDao = db.customerDao()

        @Provides
        fun provideStateDao(db: AmpairsWorkspaceDatabase): StateDao = db.stateDao()

        @Provides
        fun provideCustomerTypeDao(db: AmpairsWorkspaceDatabase): CustomerTypeDao = db.customerTypeDao()

        @Provides
        fun provideCustomerGroupDao(db: AmpairsWorkspaceDatabase): CustomerGroupDao = db.customerGroupDao()

        @Provides
        fun provideCustomerAgentDao(db: AmpairsWorkspaceDatabase): CustomerAgentDao = db.customerAgentDao()

        // product
        @Provides
        fun provideProductDao(db: AmpairsWorkspaceDatabase): ProductDao = db.productDao()

        @Provides
        fun provideProductTaxCodeDao(db: AmpairsWorkspaceDatabase): ProductTaxCodeDao = db.productTaxCodeDao()

        @Provides
        fun provideTaxInfoDao(db: AmpairsWorkspaceDatabase): TaxInfoDao = db.taxInfoDao()

        @Provides
        fun provideGroupDao(db: AmpairsWorkspaceDatabase): GroupDao = db.groupDao()

        @Provides
        fun provideCategoryDao(db: AmpairsWorkspaceDatabase): CategoryDao = db.categoryDao()

        @Provides
        fun provideSubCategoryDao(db: AmpairsWorkspaceDatabase): SubCategoryDao = db.subCategoryDao()

        @Provides
        fun provideBrandDao(db: AmpairsWorkspaceDatabase): BrandDao = db.brandDao()

        @Provides
        fun provideProductVariantDao(db: AmpairsWorkspaceDatabase): ProductVariantDao = db.productVariantDao()

        @Provides
        fun provideVariantAttributeDao(db: AmpairsWorkspaceDatabase): VariantAttributeDao = db.variantAttributeDao()

        @Provides
        fun provideProductStandardCostDao(db: AmpairsWorkspaceDatabase): ProductStandardCostDao =
            db.productStandardCostDao()

        @Provides
        fun provideProductAgentDao(db: AmpairsWorkspaceDatabase): ProductAgentDao = db.productAgentDao()

        // tax
        @Provides
        fun provideTaxCodeDao(db: AmpairsWorkspaceDatabase): TaxCodeDao = db.taxCodeDao()

        @Provides
        fun provideTaxComponentTypeDao(db: AmpairsWorkspaceDatabase): TaxComponentTypeDao = db.taxComponentTypeDao()

        @Provides
        fun provideTaxComponentDao(db: AmpairsWorkspaceDatabase): TaxComponentDao = db.taxComponentDao()

        @Provides
        fun provideTaxRuleDao(db: AmpairsWorkspaceDatabase): TaxRuleDao = db.taxRuleDao()

        @Provides
        fun provideTaxConfigurationDao(db: AmpairsWorkspaceDatabase): TaxConfigurationDao = db.taxConfigurationDao()

        @Provides
        fun provideTaxAgentDao(db: AmpairsWorkspaceDatabase): TaxAgentDao = db.taxAgentDao()

        // order
        @Provides
        fun provideOrderDao(db: AmpairsWorkspaceDatabase): OrderDao = db.orderDao()

        @Provides
        fun provideOrderItemDao(db: AmpairsWorkspaceDatabase): OrderItemDao = db.orderItemDao()

        @Provides
        fun provideOrderAgentDao(db: AmpairsWorkspaceDatabase): OrderAgentDao = db.orderAgentDao()

        // invoice
        @Provides
        fun provideInvoiceDao(db: AmpairsWorkspaceDatabase): InvoiceDao = db.invoiceDao()

        @Provides
        fun provideInvoiceItemDao(db: AmpairsWorkspaceDatabase): InvoiceItemDao = db.invoiceItemDao()

        @Provides
        fun provideInvoiceAgentDao(db: AmpairsWorkspaceDatabase): InvoiceAgentDao = db.invoiceAgentDao()

        // purchase
        @Provides
        fun providePurchaseDao(db: AmpairsWorkspaceDatabase): PurchaseDao = db.purchaseDao()

        // payment
        @Provides
        fun providePartyBalanceDao(db: AmpairsWorkspaceDatabase): PartyBalanceDao = db.partyBalanceDao()

        @Provides
        fun provideLedgerEntryDao(db: AmpairsWorkspaceDatabase): LedgerEntryDao = db.ledgerEntryDao()

        @Provides
        fun providePaymentVoucherDao(db: AmpairsWorkspaceDatabase): PaymentVoucherDao = db.paymentVoucherDao()

        @Provides
        fun providePaymentAllocationDao(db: AmpairsWorkspaceDatabase): PaymentAllocationDao =
            db.paymentAllocationDao()

        @Provides
        fun provideAdjustmentVoucherDao(db: AmpairsWorkspaceDatabase): AdjustmentVoucherDao =
            db.adjustmentVoucherDao()

        @Provides
        fun providePaymentAgentDao(db: AmpairsWorkspaceDatabase): PaymentAgentDao = db.paymentAgentDao()

        // inventory
        @Provides
        fun provideInventoryItemDao(db: AmpairsWorkspaceDatabase): InventoryItemDao = db.inventoryItemDao()

        @Provides
        fun provideInventoryTransactionDao(db: AmpairsWorkspaceDatabase): InventoryTransactionDao =
            db.inventoryTransactionDao()

        @Provides
        fun provideInventoryAgentDao(db: AmpairsWorkspaceDatabase): InventoryAgentDao = db.inventoryAgentDao()

        // unit
        @Provides
        fun provideUnitDao(db: AmpairsWorkspaceDatabase): UnitDao = db.unitDao()

        @Provides
        fun provideUnitConversionDao(db: AmpairsWorkspaceDatabase): UnitConversionDao = db.unitConversionDao()

        @Provides
        fun provideUnitAgentDao(db: AmpairsWorkspaceDatabase): UnitAgentDao = db.unitAgentDao()

        // form
        @Provides
        fun provideFormSchemaDao(db: AmpairsWorkspaceDatabase): FormSchemaDao = db.formSchemaDao()

        @Provides
        fun provideFormSectionDao(db: AmpairsWorkspaceDatabase): FormSectionDao = db.formSectionDao()

        @Provides
        fun provideFormFieldDao(db: AmpairsWorkspaceDatabase): FormFieldDao = db.formFieldDao()

        @Provides
        fun provideFormAgentDao(db: AmpairsWorkspaceDatabase): FormAgentDao = db.formAgentDao()

        // business
        @Provides
        fun provideBusinessDao(db: AmpairsWorkspaceDatabase): BusinessDao = db.businessDao()

        @Provides
        fun provideBusinessAgentDao(db: AmpairsWorkspaceDatabase): BusinessAgentDao = db.businessAgentDao()

        // subscription
        @Provides
        fun provideSubscriptionDao(db: AmpairsWorkspaceDatabase): SubscriptionDao = db.subscriptionDao()

        // supplier
        @Provides
        fun provideSupplierDao(db: AmpairsWorkspaceDatabase): SupplierDao = db.supplierDao()

        // sequence
        @Provides
        fun provideSequenceDefinitionDao(db: AmpairsWorkspaceDatabase): SequenceDefinitionDao =
            db.sequenceDefinitionDao()

        @Provides
        fun provideSequenceAllocationDao(db: AmpairsWorkspaceDatabase): SequenceAllocationDao =
            db.sequenceAllocationDao()

        // pricing + offers
        @Provides
        fun providePriceListDao(db: AmpairsWorkspaceDatabase): PriceListDao = db.priceListDao()

        @Provides
        fun providePriceListItemDao(db: AmpairsWorkspaceDatabase): PriceListItemDao = db.priceListItemDao()

        @Provides
        fun provideGeoZoneDao(db: AmpairsWorkspaceDatabase): GeoZoneDao = db.geoZoneDao()

        @Provides
        fun provideOfferDao(db: AmpairsWorkspaceDatabase): OfferDao = db.offerDao()

        // printing
        @Provides
        fun providePrinterDao(db: AmpairsWorkspaceDatabase): PrinterDao = db.printerDao()

        @Provides
        fun providePrintRoutingDao(db: AmpairsWorkspaceDatabase): PrintRoutingDao = db.printRoutingDao()

        @Provides
        fun providePrintJobDao(db: AmpairsWorkspaceDatabase): PrintJobDao = db.printJobDao()

        @Provides
        fun providePrintTemplateDao(db: AmpairsWorkspaceDatabase): PrintTemplateDao = db.printTemplateDao()

        // notification
        @Provides
        fun provideNotificationDao(db: AmpairsWorkspaceDatabase): NotificationDao = db.notificationDao()

        // agent chat
        @Provides
        fun provideChatMessageDao(db: AmpairsWorkspaceDatabase): ChatMessageDao = db.chatMessageDao()

        // analytics (feature 022)
        @Provides
        fun provideDemandForecastDao(db: AmpairsWorkspaceDatabase): DemandForecastDao = db.demandForecastDao()

        // cb_* (California Burrito maintenance build)
        @Provides
        fun provideCbEmployeeDao(db: AmpairsWorkspaceDatabase): com.ampairs.cbemployee.data.db.dao.EmployeeDao =
            db.cbEmployeeDao()

        @Provides
        fun provideCbZonalOfficeDao(db: AmpairsWorkspaceDatabase): com.ampairs.cbstore.data.db.dao.ZonalOfficeDao =
            db.cbZonalOfficeDao()

        @Provides
        fun provideCbStoreDao(db: AmpairsWorkspaceDatabase): com.ampairs.cbstore.data.db.dao.StoreDao =
            db.cbStoreDao()

        @Provides
        fun provideCbPmScheduleDao(db: AmpairsWorkspaceDatabase): com.ampairs.cbmaintenance.data.db.dao.PmScheduleDao =
            db.cbPmScheduleDao()

        @Provides
        fun provideCbPmEntryDao(db: AmpairsWorkspaceDatabase): com.ampairs.cbmaintenance.data.db.dao.PmEntryDao =
            db.cbPmEntryDao()

        @Provides
        fun provideCbTicketDao(db: AmpairsWorkspaceDatabase): com.ampairs.cbmaintenance.data.db.dao.TicketDao =
            db.cbTicketDao()

        @Provides
        fun provideCbAssetCategoryAliasDao(
            db: AmpairsWorkspaceDatabase,
        ): com.ampairs.cbmaintenance.data.db.dao.AssetCategoryAliasDao = db.cbAssetCategoryAliasDao()
    }
}
