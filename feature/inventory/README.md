# feature:inventory

Stock and inventory management. Tracks stock levels per product, surfaces low-stock alerts, and provides inventory search and reporting.

## Responsibilities

- Display and filter the inventory list with pagination
- Show stock level for individual products
- Alert on low-stock items below a configurable threshold
- Sync inventory data offline-first via Room + Store5
- Expose inventory actions to the AI agent

## Key Classes

| Class | Purpose |
|---|---|
| `InventoryApi` / `InventoryApiImpl` | REST endpoints |
| `InventoryRepository` | Data access; `getProductInventory(productId)` |
| `InventoryDao` | Room queries (search, low-stock, count, paginated list) |
| `InventoryListViewModel` | Paginated inventory list |
| `InventoryViewModel` | Single inventory item detail/edit |
| `InventoryActionHandler` | Agent actions: SEARCH, COUNT, LIST (low-stock), GET_INVENTORY |

## Domain Models

`Inventory`, `CustomField`

## Koin Module

```kotlin
inventoryModule  // in com.ampairs.inventory
```

## Agent Actions

| Action | Description | Required Params |
|---|---|---|
| `SEARCH` | Search inventory by product description | `query` |
| `COUNT` | Count total inventory items | `query` (optional) |
| `GET_INVENTORY` | Get stock level for a specific product | `productId` |
| `LIST` | List low-stock items | `threshold` (optional, default 10) |

## Database

`InventoryRoomDatabase` — workspace-scoped (`factory` scope).
