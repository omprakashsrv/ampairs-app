# feature:order

Order management. Handles order creation, line items, tax calculation, status workflows (DRAFT → CONFIRMED → SHIPPED → DELIVERED), and customer-to-customer order routing.

## Responsibilities

- Create and edit orders with multiple line items
- Apply discounts and multi-component tax (GST/VAT/etc.)
- Manage order status lifecycle
- Support from-customer / to-customer routing (B2B transfers)
- Offline-first sync via Room + Store5
- Expose order actions to the AI agent

## Key Classes

| Class | Purpose |
|---|---|
| `OrderApi` / `OrderApiImpl` | REST endpoints |
| `OrderRepository` | Data access with order + item + tax info |
| `OrderDao` | Room queries (search by number/customer, filter by status, count) |
| `OrdersViewModel` | Paginated order list |
| `OrderViewModel` | Create/edit order with line items |
| `OrderViewViewModel` | Read-only order detail |
| `OrderActionHandler` | Agent actions: SEARCH, READ, COUNT, LIST |

## Domain Models

`Order`, `OrderItem`, `TaxInfo`, `TaxSpec`, `OrderStatus`, `Address`, `Discount`

## Koin Module

```kotlin
orderModule  // in com.ampairs.order
```

## Navigation Routes

```kotlin
OrderRoute.Orders                         // list
OrderRoute.Root(fromCustomer, toCustomer, id)  // create/edit
OrderRoute.OrderView(id)                  // detail view
```

## Agent Actions

| Action | Description | Params |
|---|---|---|
| `SEARCH` | Search by order number or customer name | `query` |
| `READ` | Get order details + navigate | `orderId` or `searchName` |
| `COUNT` | Total order count | — |
| `LIST` | List orders, optionally by status | `status` (optional) |

## Database

`OrderRoomDatabase` — workspace-scoped (`factory` scope).

Tables: `orders`, `order_items`
