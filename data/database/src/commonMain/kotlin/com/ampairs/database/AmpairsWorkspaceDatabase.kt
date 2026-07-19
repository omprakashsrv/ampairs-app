package com.ampairs.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import com.ampairs.agent.data.db.dao.ChatMessageDao
import com.ampairs.agent.data.db.entity.ChatMessageEntity
import com.ampairs.business.agent.BusinessAgentDao
import com.ampairs.business.data.db.BusinessDao
import com.ampairs.business.data.db.BusinessEntity
import com.ampairs.customer.agent.CustomerAgentDao
import com.ampairs.customer.data.db.CustomerDao
import com.ampairs.customer.data.db.CustomerEntity
import com.ampairs.customer.data.db.CustomerGroupDao
import com.ampairs.customer.data.db.CustomerGroupEntity
import com.ampairs.customer.data.db.CustomerTypeDao
import com.ampairs.customer.data.db.CustomerTypeEntity
import com.ampairs.customer.data.db.StateDao
import com.ampairs.customer.data.db.StateEntity
import com.ampairs.ecom.data.db.dao.AddressDao
import com.ampairs.ecom.data.db.dao.CartDao
import com.ampairs.ecom.data.db.dao.EcomOrderDao
import com.ampairs.ecom.data.db.dao.ListedProductDao
import com.ampairs.ecom.data.db.dao.StorefrontDao
import com.ampairs.ecom.data.db.dao.SyncCursorDao
import com.ampairs.ecom.data.db.dao.TaxonomyImageDao
import com.ampairs.ecom.data.db.entity.CartEntity
import com.ampairs.ecom.data.db.entity.CartItemEntity
import com.ampairs.ecom.data.db.entity.CustomerAddressEntity
import com.ampairs.ecom.data.db.entity.EcomOrderEntity
import com.ampairs.ecom.data.db.entity.EcomOrderLineItemEntity
import com.ampairs.ecom.data.db.entity.ListedProductEntity
import com.ampairs.ecom.data.db.entity.StorefrontEntity
import com.ampairs.ecom.data.db.entity.SyncCursorEntity
import com.ampairs.ecom.data.db.entity.TaxonomyImageEntity
import com.ampairs.file.db.dao.FileDao
import com.ampairs.file.db.entity.FileEntity
import com.ampairs.form.agent.FormAgentDao
import com.ampairs.form.data.db.FormFieldDao
import com.ampairs.form.data.db.FormFieldEntity
import com.ampairs.form.data.db.FormSchemaDao
import com.ampairs.form.data.db.FormSchemaEntity
import com.ampairs.form.data.db.FormSectionDao
import com.ampairs.form.data.db.FormSectionEntity
import com.ampairs.inventory.agent.InventoryAgentDao
import com.ampairs.inventory.data.db.InventoryItemDao
import com.ampairs.inventory.data.db.InventoryItemEntity
import com.ampairs.inventory.data.db.InventoryTransactionDao
import com.ampairs.inventory.data.db.InventoryTransactionEntity
import com.ampairs.invoice.agent.InvoiceAgentDao
import com.ampairs.invoice.db.dao.InvoiceDao
import com.ampairs.invoice.db.dao.InvoiceItemDao
import com.ampairs.invoice.db.entity.InvoiceEntity
import com.ampairs.invoice.db.entity.InvoiceItemEntity
import com.ampairs.notification.data.db.dao.NotificationDao
import com.ampairs.notification.data.db.entity.NotificationLogEntity
import com.ampairs.order.agent.OrderAgentDao
import com.ampairs.order.db.dao.OrderDao
import com.ampairs.order.db.dao.OrderItemDao
import com.ampairs.order.db.entity.OrderEntity
import com.ampairs.order.db.entity.OrderItemEntity
import com.ampairs.payment.agent.PaymentAgentDao
import com.ampairs.payment.data.db.dao.AdjustmentVoucherDao
import com.ampairs.payment.data.db.dao.LedgerEntryDao
import com.ampairs.payment.data.db.dao.PartyBalanceDao
import com.ampairs.payment.data.db.dao.PaymentAllocationDao
import com.ampairs.payment.data.db.dao.PaymentVoucherDao
import com.ampairs.payment.data.db.entity.AdjustmentVoucherEntity
import com.ampairs.payment.data.db.entity.LedgerEntryEntity
import com.ampairs.payment.data.db.entity.PartyBalanceEntity
import com.ampairs.payment.data.db.entity.PaymentAllocationEntity
import com.ampairs.payment.data.db.entity.PaymentVoucherEntity
import com.ampairs.pricing.data.db.dao.GeoZoneDao
import com.ampairs.pricing.data.db.dao.OfferDao
import com.ampairs.pricing.data.db.dao.PriceListDao
import com.ampairs.pricing.data.db.dao.PriceListItemDao
import com.ampairs.pricing.data.db.entity.GeoZoneEntity
import com.ampairs.pricing.data.db.entity.OfferEntity
import com.ampairs.pricing.data.db.entity.PriceListEntity
import com.ampairs.pricing.data.db.entity.PriceListItemEntity
import com.ampairs.printing.data.db.PrintJobDao
import com.ampairs.printing.data.db.PrintJobEntity
import com.ampairs.printing.data.db.PrintRoutingDao
import com.ampairs.printing.data.db.PrintRoutingEntity
import com.ampairs.printing.data.db.PrintTemplateDao
import com.ampairs.printing.data.db.PrintTemplateEntity
import com.ampairs.printing.data.db.PrinterDao
import com.ampairs.printing.data.db.PrinterEntity
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
import com.ampairs.product.db.entity.BrandEntity
import com.ampairs.product.db.entity.CategoryEntity
import com.ampairs.product.db.entity.GroupEntity
import com.ampairs.product.db.entity.ProductEntity
import com.ampairs.product.db.entity.ProductStandardCostEntity
import com.ampairs.product.db.entity.ProductVariantEntity
import com.ampairs.product.db.entity.SubCategoryEntity
import com.ampairs.product.db.entity.TaxCodeEntity as ProductTaxCodeEntity
import com.ampairs.product.db.entity.TaxInfoEntity
import com.ampairs.product.db.entity.VariantAttributeEntity
import com.ampairs.purchase.db.dao.PurchaseDao
import com.ampairs.purchase.db.entity.PurchaseEntity
import com.ampairs.purchase.db.entity.PurchaseItemEntity
import com.ampairs.sequence.data.db.dao.SequenceAllocationDao
import com.ampairs.sequence.data.db.dao.SequenceDefinitionDao
import com.ampairs.sequence.data.db.entity.SequenceAllocationEntity
import com.ampairs.sequence.data.db.entity.SequenceDefinitionEntity
import com.ampairs.store.data.db.dao.StoreSettingDao
import com.ampairs.store.data.db.dao.StoreSettingDefinitionDao
import com.ampairs.store.data.db.entity.StoreSettingDefinitionEntity
import com.ampairs.store.data.db.entity.StoreSettingEntity
import com.ampairs.subscription.db.DeviceRegistrationEntity
import com.ampairs.subscription.db.SubscriptionDao
import com.ampairs.subscription.db.SubscriptionEntity
import com.ampairs.subscription.db.SubscriptionPlanEntity
import com.ampairs.subscription.db.UsageMetricsEntity
import com.ampairs.supplier.data.db.SupplierDao
import com.ampairs.supplier.data.db.SupplierEntity
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.sync.db.SyncStateEntity
import com.ampairs.tax.agent.TaxAgentDao
import com.ampairs.tax.data.db.TaxInstantConverters
import com.ampairs.tax.data.db.dao.TaxCodeDao
import com.ampairs.tax.data.db.dao.TaxComponentDao
import com.ampairs.tax.data.db.dao.TaxComponentTypeDao
import com.ampairs.tax.data.db.dao.TaxConfigurationDao
import com.ampairs.tax.data.db.dao.TaxRuleDao
import com.ampairs.tax.data.db.entity.TaxCodeEntity
import com.ampairs.tax.data.db.entity.TaxComponentEntity
import com.ampairs.tax.data.db.entity.TaxComponentTypeEntity
import com.ampairs.tax.data.db.entity.TaxConfigurationEntity
import com.ampairs.tax.data.db.entity.TaxRuleEntity
import com.ampairs.unit.agent.UnitAgentDao
import com.ampairs.unit.data.db.dao.UnitConversionDao
import com.ampairs.unit.data.db.dao.UnitDao
import com.ampairs.unit.data.db.entity.UnitConversionEntity
import com.ampairs.unit.data.db.entity.UnitEntity

/**
 * Consolidated WorkspaceScope database — one file per workspace
 * (`workspace_{slug}_main.db` on Android, `workspace_{slug}/main.db` on iOS/Desktop) holding every
 * feature schema that was previously spread over 23 per-module files. One connection pool + one
 * InvalidationTracker instead of 23 replaces the biggest native-memory multiplier in the app.
 *
 * The legacy per-feature database classes are gone (a DAO may only have ONE Room-generated impl per
 * app classpath). On upgrade the consolidated file is created fresh and server-authoritative data
 * re-syncs; the old per-module files are left in place.
 *
 * Schema changes: bump [Database.version] here and add a migration under `migrations/` —
 * append-only, one shared version line for all features.
 */
@Database(
    entities = [
        // customer (was workspace_{slug}_customer.db v10)
        CustomerEntity::class,
        StateEntity::class,
        CustomerTypeEntity::class,
        CustomerGroupEntity::class,
        // product (was product.db v11)
        ProductEntity::class,
        ProductTaxCodeEntity::class,
        TaxInfoEntity::class,
        GroupEntity::class,
        CategoryEntity::class,
        SubCategoryEntity::class,
        BrandEntity::class,
        ProductVariantEntity::class,
        VariantAttributeEntity::class,
        ProductStandardCostEntity::class,
        // tax (was tax.db v3)
        TaxCodeEntity::class,
        TaxComponentTypeEntity::class,
        TaxComponentEntity::class,
        TaxRuleEntity::class,
        TaxConfigurationEntity::class,
        // order (was order.db v6)
        OrderEntity::class,
        OrderItemEntity::class,
        // invoice (was invoice.db v6)
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        // purchase (was purchase.db v1)
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        // payment (was payment.db v1)
        PartyBalanceEntity::class,
        LedgerEntryEntity::class,
        PaymentVoucherEntity::class,
        PaymentAllocationEntity::class,
        AdjustmentVoucherEntity::class,
        // inventory (was inventory.db v1)
        InventoryItemEntity::class,
        InventoryTransactionEntity::class,
        // unit (was unit.db v2)
        UnitEntity::class,
        UnitConversionEntity::class,
        // form (was form_v2.db v2)
        FormSchemaEntity::class,
        FormSectionEntity::class,
        FormFieldEntity::class,
        // file (was file.db v1)
        FileEntity::class,
        // business (was business.db v4)
        BusinessEntity::class,
        // store (was store.db v2)
        StoreSettingEntity::class,
        StoreSettingDefinitionEntity::class,
        // subscription (was subscription.db v1)
        SubscriptionEntity::class,
        SubscriptionPlanEntity::class,
        DeviceRegistrationEntity::class,
        UsageMetricsEntity::class,
        // supplier (was supplier.db v1)
        SupplierEntity::class,
        // sequence (was sequence.db v1)
        SequenceDefinitionEntity::class,
        SequenceAllocationEntity::class,
        // pricing (was pricing.db v2) + offers (was offers.db v1)
        PriceListEntity::class,
        PriceListItemEntity::class,
        GeoZoneEntity::class,
        OfferEntity::class,
        // printing (was printing.db v1)
        PrinterEntity::class,
        PrintRoutingEntity::class,
        PrintJobEntity::class,
        PrintTemplateEntity::class,
        // notification (was notification.db v1)
        NotificationLogEntity::class,
        // ecom (was ecom.db v2)
        StorefrontEntity::class,
        TaxonomyImageEntity::class,
        ListedProductEntity::class,
        SyncCursorEntity::class,
        CartEntity::class,
        CartItemEntity::class,
        CustomerAddressEntity::class,
        EcomOrderEntity::class,
        EcomOrderLineItemEntity::class,
        // agent chat (was agent_chat.db v1)
        ChatMessageEntity::class,
        // sync state (was sync.db v2)
        SyncStateEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
@ColumnTypeConverters(TaxInstantConverters::class)
@ConstructedBy(AmpairsWorkspaceDatabaseConstructor::class)
abstract class AmpairsWorkspaceDatabase : RoomDatabase() {
    // customer
    abstract fun customerDao(): CustomerDao
    abstract fun stateDao(): StateDao
    abstract fun customerTypeDao(): CustomerTypeDao
    abstract fun customerGroupDao(): CustomerGroupDao
    abstract fun customerAgentDao(): CustomerAgentDao

    // product ("taxCodeDao" collides with the tax module's accessor, hence the product prefix)
    abstract fun productDao(): ProductDao
    abstract fun productTaxCodeDao(): ProductTaxCodeDao
    abstract fun taxInfoDao(): TaxInfoDao
    abstract fun groupDao(): GroupDao
    abstract fun categoryDao(): CategoryDao
    abstract fun subCategoryDao(): SubCategoryDao
    abstract fun brandDao(): BrandDao
    abstract fun productVariantDao(): ProductVariantDao
    abstract fun variantAttributeDao(): VariantAttributeDao
    abstract fun productStandardCostDao(): ProductStandardCostDao
    abstract fun productAgentDao(): ProductAgentDao

    // tax
    abstract fun taxCodeDao(): TaxCodeDao
    abstract fun taxComponentTypeDao(): TaxComponentTypeDao
    abstract fun taxComponentDao(): TaxComponentDao
    abstract fun taxRuleDao(): TaxRuleDao
    abstract fun taxConfigurationDao(): TaxConfigurationDao
    abstract fun taxAgentDao(): TaxAgentDao

    // order
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun orderAgentDao(): OrderAgentDao

    // invoice
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceItemDao(): InvoiceItemDao
    abstract fun invoiceAgentDao(): InvoiceAgentDao

    // purchase
    abstract fun purchaseDao(): PurchaseDao

    // payment
    abstract fun partyBalanceDao(): PartyBalanceDao
    abstract fun ledgerEntryDao(): LedgerEntryDao
    abstract fun paymentVoucherDao(): PaymentVoucherDao
    abstract fun paymentAllocationDao(): PaymentAllocationDao
    abstract fun adjustmentVoucherDao(): AdjustmentVoucherDao
    abstract fun paymentAgentDao(): PaymentAgentDao

    // inventory
    abstract fun inventoryItemDao(): InventoryItemDao
    abstract fun inventoryTransactionDao(): InventoryTransactionDao
    abstract fun inventoryAgentDao(): InventoryAgentDao

    // unit
    abstract fun unitDao(): UnitDao
    abstract fun unitConversionDao(): UnitConversionDao
    abstract fun unitAgentDao(): UnitAgentDao

    // form
    abstract fun formSchemaDao(): FormSchemaDao
    abstract fun formSectionDao(): FormSectionDao
    abstract fun formFieldDao(): FormFieldDao
    abstract fun formAgentDao(): FormAgentDao

    // file
    abstract fun fileDao(): FileDao

    // business
    abstract fun businessDao(): BusinessDao
    abstract fun businessAgentDao(): BusinessAgentDao

    // store
    abstract fun storeSettingDao(): StoreSettingDao
    abstract fun storeSettingDefinitionDao(): StoreSettingDefinitionDao

    // subscription
    abstract fun subscriptionDao(): SubscriptionDao

    // supplier
    abstract fun supplierDao(): SupplierDao

    // sequence
    abstract fun sequenceDefinitionDao(): SequenceDefinitionDao
    abstract fun sequenceAllocationDao(): SequenceAllocationDao

    // pricing + offers
    abstract fun priceListDao(): PriceListDao
    abstract fun priceListItemDao(): PriceListItemDao
    abstract fun geoZoneDao(): GeoZoneDao
    abstract fun offerDao(): OfferDao

    // printing
    abstract fun printerDao(): PrinterDao
    abstract fun printRoutingDao(): PrintRoutingDao
    abstract fun printJobDao(): PrintJobDao
    abstract fun printTemplateDao(): PrintTemplateDao

    // notification
    abstract fun notificationDao(): NotificationDao

    // ecom
    abstract fun storefrontDao(): StorefrontDao
    abstract fun taxonomyImageDao(): TaxonomyImageDao
    abstract fun listedProductDao(): ListedProductDao
    abstract fun syncCursorDao(): SyncCursorDao
    abstract fun cartDao(): CartDao
    abstract fun addressDao(): AddressDao
    abstract fun ecomOrderDao(): EcomOrderDao

    // agent chat
    abstract fun chatMessageDao(): ChatMessageDao

    // sync state
    abstract fun syncStateDao(): SyncStateDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AmpairsWorkspaceDatabaseConstructor : RoomDatabaseConstructor<AmpairsWorkspaceDatabase> {
    override fun initialize(): AmpairsWorkspaceDatabase
}
