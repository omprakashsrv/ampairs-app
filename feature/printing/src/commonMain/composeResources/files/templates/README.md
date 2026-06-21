# Sample static HTML print templates

Polished, ready-to-use HTML designs for the **static template** feature
(`TemplateKind.STATIC`). Import one via **Templates → Import HTML template →
{Invoice|Order}**. The file is stored in the file module and synced; at print
time only the `{{placeholders}}` are substituted — the HTML file itself is never
modified.

| File | Document type |
|---|---|
| `invoice_sample.html` | INVOICE |
| `order_sample.html` | ORDER |

## Placeholders

Substitution is done by `StaticHtmlResolver`. Values are HTML-escaped; unknown
tokens resolve to an empty string.

**Document fields** — `{{key}}`:

| Key | Meaning |
|---|---|
| `seller_name` | Business / seller name |
| `invoice_number` / `order_number` | Document number |
| `invoice_date` / `order_date` | Document date |
| `base_price` | Sub total |
| `total_tax` | Tax total |
| `total_cost` | Grand total |

**Nested entity** — `{{customer.field}}` (e.g. `{{customer.customer_name}}`),
`{{product.field}}` inside a line loop.

**Line items** — repeat a block per line:

```html
{{#lines}}
  <tr><td>{{description}}</td><td>{{quantity}}</td><td>{{price}}</td><td>{{total_cost}}</td></tr>
{{/lines}}
```

> Money/number values render via the print formatter (plain numbers). Add your
> own currency symbol or layout in the HTML as needed.
