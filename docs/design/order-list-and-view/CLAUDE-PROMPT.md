# Claude Design Prompt — Order List + Order View (spec 010 follow-on, "v2 ops surfaces")

Copy everything below the line into Claude Code, run from the `ampairs-app` repo root. This is the
design-driven UI layer for the two surfaces *around* the v2 fast-entry editor that already shipped:
the **order listing** (where staff find and start documents) and the **order view** (where every
save lands, and where order → invoice conversion happens). Visual language continues the v2 editor:
Material 3 via the app theme, money in mono with ₹ Indian grouping (`Double.toInr()`), `dd MMM yyyy`
dates, sync chips, density −2, no emoji, all strings via `stringResource`.

---

You are implementing two screens in `feature/order` of the `ampairs-app` Kotlin Multiplatform
codebase (Compose Multiplatform, offline-first, Metro DI, Navigation3). The data layer is already
built: Room paging (`OrderRepository.getOrders`), `CentralSyncService` with `SyncEntity.ORDER`,
client-side order → invoice conversion (`OrderViewViewModel.createInvoice()`, idempotent via
`invoiceRefId`), and the v2 editor (`OrderScreen` → `DocEditorScaffold`). Do NOT touch the editor
or the calc layer. Reuse the shared v2 components from `com.ampairs.invoice.editor` where noted
(`DocSyncUi` chips, `toInr`, the visual grammar).

## Screen A — Order list (`OrdersScreen` + `OrderPaneScreen`)

**Purpose:** browse orders, see status & sync at a glance, find one fast (number / customer),
start a new one. Order-desk speed, not analytics.

### Layout
- **Compact (phone, <840dp):** single pane. Pinned header (title + count + search + filter chips),
  scrolling list of rows, FAB `+ New order` bottom-right. Tap row → push `OrderView`.
- **Expanded (tablet/desktop):** existing `ListDetailPaneScaffold` (`OrderPaneScreen`) — list rail
  left, **order view as the detail pane** right. Selecting a row loads the detail pane in place
  (no navigation); the selected row gets a tonal `secondaryContainer` highlight.

### Header (pinned, surface tonal elevation 2)
1. Title "Orders" + live count caption ("%d orders").
2. **Search field** (`OutlinedTextField`, search icon, clear button): matches **order number OR
   buyer/seller customer name** (substring, case-insensitive) in ONE box. Search is DAO-side
   (paging query), debounce-free (local Room).
3. **Filter chip row** (`FilterChip`, single-select): `All · Draft · New · Ordered · Invoiced ·
   Offline`. Status chips filter by `OrderStatus`; `Invoiced` = `invoice_ref_id` set; `Offline` =
   `synced = 0` (what hasn't reached the server — the order-desk's end-of-day check). Selected chip
   has a leading check icon. Filters compose with search.

### List row anatomy (min 56dp, full-row ripple)
```
[icon]  ORD-0042            Rajesh Kumar Hardware        [Draft]   ₹9,207.10
        11 Jun 2026 · 3 items · → INV/0007                [cloud_off]
```
- **Leading:** `shopping_cart` icon in a 36dp tonal circle (tertiaryContainer).
- **Line 1:** order number — **mono, medium weight** ("—" while draft/unnumbered) · buyer name
  (`to_customer_name`, fallback seller name) ellipsized · **status chip** (Draft =
  secondaryContainer, New = tertiaryContainer, Ordered = primaryContainer) · **amount** ₹ mono,
  right-aligned, the visually heaviest cell.
- **Line 2 (caption row):** date `dd MMM yyyy` · "%d items" · a small `→ {linked}` hint when the
  order is already invoiced · trailing **sync glyph**: `cloud_off` (amber/secondary) when
  `synced = 0`; nothing when synced (synced is the quiet default, offline is the signal).
- Divider `outlineVariant` between rows. Stable `key = id`, `contentType` set.

### States
- **Empty (no orders at all):** centered icon + "No orders yet" + "Create your first order" +
  a tonal `+ New order` button (don't make the user find the FAB).
- **Empty (search/filter):** "No orders match" + the active query/filter named + "Clear filters"
  text button.
- **Syncing:** thin `LinearProgressIndicator` under the header (driven by
  `observeEntity(ORDER).status is Syncing`) — never a blocking spinner over the list.
- **Sync failed:** inline error strip under the header (errorContainer, message + Retry) — the
  cached list stays fully usable.

### Behavior
- Open with `TriggerPull(ORDER)` (already in the VM); manual refresh not required for v1.
- `+ New order` → editor with no id. Row tap → `OrderView(id)` (compact) / detail pane (expanded).
- Search + filter live in the ViewModel (`searchText`, `statusFilter`) and re-create the Pager.

## Screen B — Order view (`OrderViewScreen`) — the post-save landing

**Purpose:** read-only confirmation of what was just saved (or what was tapped in the list), and
the springboard for what happens next: edit, convert to invoice, follow the linked invoice.
Must read like a document, not a form.

### Header (`TopAppBar`)
- Title: order number **mono** ("Order · ORD-0042"; "Order · draft" before numbering).
- Back arrow; **Edit** action (pencil) → editor with this id.
- **Sync chip** in the app bar (`DocSyncUi` mapping: offline `cloud_off` amber / syncing / synced
  green / failed + Retry) — live via `observeEntity(ORDER)` and the row's `synced` flag.

### Body (LazyColumn over a "paper" Surface, max width 760dp centered on expanded)
1. **Status band:** status chip + date `dd MMM yyyy` + items/quantity caption; when converted, an
   `AssistChip` "Invoice INV-…" with a `receipt_long` icon that **opens the linked invoice**.
2. **Parties block** (two columns, surfaceVariant cards): "From (seller)" and "Bill to (buyer)"
   — name, GSTIN when present.
3. **Line table:** columns `# · Item · Qty · Rate · Amount`; the Item cell stacks description +
   per-line discount caption when present (`−10% · ₹1,080.00`) + unit name with the qty. Money mono.
   No vertical grid lines — horizontal `outlineVariant` dividers only (calmer than the legacy
   bordered table).
4. **Totals block** (right-aligned column, mono): Taxable → each GST component from the order's
   stored `taxInfos` (grouped, `CGST ₹303.55` …) → Discount (when present) → **Grand total**
   (largest text, `headlineSmall` mono).
5. Footer caption: "%d items · %s qty".

### Actions (bottom bar on compact, trailing in the header band on expanded)
- **Primary:** `Create invoice` — opens the existing confirmation `AlertDialog` (checklist +
  idempotency note, strings `ord_conv_*`). After conversion the button becomes **`View invoice`**
  (idempotent: `invoiceRefId` set ⇒ never create a second one — navigate instead).
- Secondary: **Edit**. ~~Save~~ only for legacy unnumbered drafts (keep the existing fallback).

### Flow map
```
List ── + New ──────────► Editor (fast entry) ── Save ──► Order view  (landing, sync chip live)
  │                                                          │  ▲
  ├─ tap row (compact) ──────────────────────────────────────┘  │ Edit → Editor(id) → Save ──┘
  ├─ select row (expanded) → detail pane = Order view
  └─ chip "Invoiced" filter ──► rows with → INV link
Order view ── Create invoice ── confirm dialog ──► converts (offline) ──► button flips to
  "View invoice" + status chip — tap ──► Invoice view (numbered, printable)
```

## Constraints
- M3 only, app theme, strings via `stringResource` in each module's `strings.xml`.
- `Double.toInr()` for ALL money; dates via kotlinx-datetime → `dd MMM yyyy`.
- MVI: list filter state in `OrdersViewModel`; DAO-side search/filter (new paging query: number OR
  buyer OR seller name, optional status / invoiced / unsynced predicate). No repository API calls.
- Touch targets ≥44dp; contentDescription on every icon-only control; row semantics read
  "Order ORD-0042, Rajesh Kumar Hardware, ₹9,207.10, draft, saved offline".

## Acceptance checks
1. Typing `raj` in search shows only orders whose buyer/seller name or number contains it; chips
   still apply on top.
2. `Offline` chip shows exactly the rows with `synced = 0`, each with the amber `cloud_off` glyph.
3. Saving from the editor lands on the order view: number (or "draft"), correct grand total in
   mono ₹ Indian grouping, sync chip starts at "Saved offline" and flips to "Synced" when the push
   completes.
4. `Create invoice` → confirm → the button becomes `View invoice` and tapping it opens the linked
   invoice; invoking convert again NEVER creates a duplicate.
5. The order view totals reconcile: taxable + Σ GST components − discount = grand total shown.
6. Expanded: selecting rows swaps the detail pane in place; the selected row stays highlighted.
7. Empty workspace shows the call-to-action empty state; a fruitless search shows "No orders match"
   with a working "Clear filters".

Work screen by screen (list first), committing per screen. Reuse before reinventing; visual polish
follows the app's existing M3 conventions.
