package com.ampairs.inventory.agent

import com.ampairs.common.agent.ColumnSchema
import com.ampairs.common.agent.ModuleQuerySchema
import com.ampairs.common.agent.QuerySchemaKey
import com.ampairs.common.agent.TableSchema
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides

/**
 * Per-module SAFE_QUERY schema ownership (FR-016, T037): the inventory module contributes its own
 * curated read-only [ModuleQuerySchema]. Internal/sync/audit columns are deliberately omitted.
 */
@ContributesTo(WorkspaceScope::class)
interface InventoryQuerySchemaModule {
    companion object {
        @Provides
        @IntoMap
        @QuerySchemaKey("inventory")
        fun provideInventoryQuerySchema(): ModuleQuerySchema = ModuleQuerySchema(
            moduleName = "inventory",
            tables = listOf(
                TableSchema(
                    "inventory_items",
                    listOf(
                        ColumnSchema("id", "TEXT", "Inventory item UID (= inventory_transactions.inventoryItemId)"),
                        ColumnSchema("name", "TEXT", "Item name"),
                        ColumnSchema("sku", "TEXT", "Stock-keeping unit / code (nullable)"),
                        ColumnSchema("productId", "TEXT", "Linked product UID (nullable)"),
                        ColumnSchema("productVariantId", "TEXT", "Linked product variant UID (nullable)"),
                        ColumnSchema("unitId", "TEXT", "Unit UID (nullable)"),
                        ColumnSchema("warehouseId", "TEXT", "Warehouse UID (nullable)"),
                        ColumnSchema("currentStock", "REAL", "Total on-hand stock quantity"),
                        ColumnSchema("reservedStock", "REAL", "Stock reserved against orders"),
                        ColumnSchema("availableStock", "REAL", "Stock available to sell (currentStock - reservedStock)"),
                        ColumnSchema("reorderLevel", "REAL", "Reorder threshold; low stock when availableStock <= reorderLevel"),
                        ColumnSchema("costPrice", "REAL", "Cost / buying price"),
                        ColumnSchema("sellingPrice", "REAL", "Selling price"),
                        ColumnSchema("mrp", "REAL", "Maximum retail price"),
                        ColumnSchema("createdAt", "TEXT", "ISO-8601 creation timestamp"),
                    ),
                ),
                TableSchema(
                    "inventory_transactions",
                    listOf(
                        ColumnSchema("id", "TEXT", "Transaction UID"),
                        ColumnSchema("transactionNumber", "TEXT", "Human-facing transaction number"),
                        ColumnSchema("transactionType", "TEXT", "Movement type, e.g. IN / OUT / ADJUSTMENT"),
                        ColumnSchema("transactionReason", "TEXT", "Reason / category for the movement"),
                        ColumnSchema("inventoryItemId", "TEXT", "Affected inventory item UID (= inventory_items.id)"),
                        ColumnSchema("warehouseId", "TEXT", "Warehouse UID (nullable)"),
                        ColumnSchema("quantity", "REAL", "Quantity moved (signed by transactionType)"),
                        ColumnSchema("balanceAfter", "REAL", "On-hand balance after this movement"),
                        ColumnSchema("unitCost", "REAL", "Unit cost for the movement"),
                        ColumnSchema("totalCost", "REAL", "Total cost for the movement"),
                        ColumnSchema("sourceType", "TEXT", "Originating document type, e.g. ORDER / INVOICE (nullable)"),
                        ColumnSchema("sourceId", "TEXT", "Originating document UID (nullable)"),
                        ColumnSchema("referenceNumber", "TEXT", "External reference number (nullable)"),
                        ColumnSchema("transactionDate", "TEXT", "ISO-8601 movement date; filter by string comparison"),
                    ),
                ),
            ),
        )
    }
}
