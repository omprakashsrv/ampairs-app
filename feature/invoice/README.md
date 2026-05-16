# feature:invoice

Invoice generation and management with full GST compliance. Supports invoice creation, status workflows, PDF generation, and email delivery.

## Responsibilities

- Create and manage invoices with line items, discounts, and tax breakdown
- GST-compliant tax calculation (CGST, SGST, IGST)
- Invoice status lifecycle (DRAFT → SENT → PAID → CANCELLED)
- PDF generation and email delivery
- Offline-first sync with backend via Room + Store5
- Expose invoice actions to the AI agent

## Key Classes

| Class | Purpose |
|---|---|
| `InvoiceApi` / `InvoiceApiImpl` | REST endpoints |
| `InvoiceRepository` | Data access with invoice + item + tax info |
| `InvoiceDao` | Room queries (search by number/customer, filter by status, count) |
| `InvoicesViewModel` | Paginated invoice list |
| `InvoiceViewModel` | Create/edit invoice with line items |
| `InvoiceViewViewModel` | Read-only invoice detail / PDF view |
| `InvoiceActionHandler` | Agent actions: SEARCH, READ, COUNT, LIST |

## Domain Models

`Invoice`, `InvoiceItem`, `TaxInfo`, `TaxSpec`, `InvoiceStatus`, `Address`, `Discount`

## Koin Module

```kotlin
invoiceModule  // in com.ampairs.invoice
```

## Navigation Routes

```kotlin
InvoiceRoute.Invoices          // list
InvoiceRoute.Root(from, to, id)  // create/edit
InvoiceRoute.InvoiceView(id)   // detail view
```

## Agent Actions

| Action | Description | Params |
|---|---|---|
| `SEARCH` | Search by invoice number or customer name | `query` |
| `READ` | Get invoice details + navigate | `invoiceId` or `searchName` |
| `COUNT` | Total invoice count | — |
| `LIST` | List invoices, optionally by status | `status` (optional) |

## Database

`InvoiceRoomDatabase` — workspace-scoped (`factory` scope).

Tables: `invoices`, `invoice_items`
