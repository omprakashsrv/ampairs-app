# feature:product

Product catalog management. Handles products, variants, categories, brands, tax codes, and product images across all platforms.

## Responsibilities

- CRUD for products with categories, groups, brands, and tax info
- Product variant management (size, colour, etc.)
- Tax code and tax info assignment per product
- Product image upload, management, and caching
- Offline-first sync with backend via Room + Store5
- Expose product actions to the AI agent

## Key Classes

| Class | Purpose |
|---|---|
| `ProductApi` / `ProductApiImpl` | REST endpoints |
| `ProductRepository` | Offline-first data access |
| `ProductsListViewModel` | Paginated product list |
| `ProductFormViewModel` | Create/edit product form |
| `ProductDetailsViewModel` | Product detail with inventory summary |
| `VariantFormViewModel` | Variant create/edit |
| `ProductActionHandler` | Agent actions: SEARCH, READ, CREATE, UPDATE, COUNT |

## Domain Models

`Product`, `ProductVariant`, `ProductCategory`, `Group`, `Image`, `ProductImage`, `TaxCode`, `TaxInfo`, `TaxSpec`, `TaxType`, `ProductType`

## Koin Module

```kotlin
productModule  // in com.ampairs.product
```

## Navigation Routes

```kotlin
ProductRoute.Products               // list
ProductRoute.ProductForm(id?)       // create/edit
ProductRoute.ProductDetails(id)     // detail
ProductRoute.VariantManagement(productId, name)
ProductRoute.VariantForm(productId, variantId?)
ProductRoute.Group(type, edit)
ProductRoute.TaxInfo / ProductRoute.TaxCode
```

## Agent Actions

| Action | Description | Params |
|---|---|---|
| `SEARCH` | Search products by name | `query` |
| `READ` | Get product details + navigate | `productId` or `searchName` |
| `CREATE` | Create a new product | `name` |
| `UPDATE` | Update product field | `searchName`, field, value |
| `COUNT` | Total product count | — |

## Database

`ProductRoomDatabase` — workspace-scoped (`factory` scope).

Tables: products, variants, categories, groups, brands, images, product_images, tax_codes, tax_info, variant_attributes
