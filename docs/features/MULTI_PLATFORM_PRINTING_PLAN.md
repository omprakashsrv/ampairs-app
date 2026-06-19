# Multi-Platform Printing Module — Final Plan

Status: Proposed
Targets: Android, iOS, Desktop (JVM) — Compose Multiplatform
Owner: TBD

---

## 1. Purpose & Scope

A reusable printing subsystem for the Ampairs KMP app that prints **invoices, orders,
receipts, credit notes, and debit notes** (and, later, **product/barcode labels**) in a
retail/wholesale store, across **Android, iOS, and Desktop**.

It must support multiple printer classes and connection channels:

- **Thermal receipt printers** — ESC/POS, line-by-line, 58 mm / 80 mm, cut + cash-drawer kick.
- **Inkjet / laser printers** — page formats A4 / A5 / A6 / A7, via the OS print service.
- **Label / barcode printers** — TSPL / ZPL, fixed label sizes, batch labels (future phase).

Connectivity: **Network/WiFi/Ethernet, Bluetooth, USB**, plus the **OS print service** for
page printers. **Cloud printing** is a planned future extension.

### Out of scope (initial)
- Cloud print backend service (designed for, not built — Phase 6).
- iOS USB (not permitted by the platform).

---

## 2. Current State (baseline)

The app today has a minimal, invoice-only print path:

- `feature/invoice/.../print/InvoiceHtml.kt` → `buildInvoiceHtml(invoice, workspaceName, currencySymbol): String`.
- `feature/invoice/.../print/InvoicePrinter.kt` (expect) + actuals hand that HTML to the OS:
  - Android `WebView` → `PrintManager`; iOS `UIPrintInteractionController` + `UIMarkupTextPrintFormatter`; Desktop temp `.html` → browser.

There is **no** raw/thermal printing, **no** Bluetooth/USB/socket code, **no** PDF library, and
**no** FileKit export anywhere in the repo. The page-print path above is reused and generalized by
this plan (it becomes `OsPrintTransport`).

The codebase conventions this plan follows: feature-module layout, Metro DI with `WorkspaceScope`,
offline-sync delegates, expect/actual transports, DataStore for device-local prefs, the `store`
module for workspace-synced settings, `feature/form` for dynamic field schemas, and Navigation3
entry providers.

### Library landscape
No single third-party library covers Android + iOS + Desktop across USB + Bluetooth + network
(Printer-KMP = no iOS/BT; Blueline = BT-only; DantSu/Printer-ktx = Android-only). ESC/POS is just
byte-array command building, trivial in pure Kotlin `commonMain`. The hard part is **transport per
platform**, which we own regardless. Therefore: **build a thin ESC/POS / label command layer in
`commonMain` + an expect/actual transport layer**, borrowing command tables from DantSu /
escpos-coffee. Network transport (raw socket :9100) lives entirely in `commonMain` via
**ktor-network**.

---

## 3. Architecture Overview

A 4-layer pipeline so every document type and every printer type reuse the same core:

```
Business doc (Invoice/Order/Receipt/CreditNote/DebitNote/Label)
        │  DocumentMapper + PrintValueProvider (per feature, contributed via Metro)
        ▼
   PrintDocument  ── printer-agnostic intermediate representation (IR)
        │  Renderer (selected by printer class)
        ├─► EscPosRenderer  → ByteArray   (thermal 58/80 mm)
        ├─► LabelRenderer   → ByteArray   (TSPL/ZPL labels & barcodes)         [phase 5]
        └─► HtmlRenderer (→ PdfRenderer)  → HTML/PDF (inkjet A4–A7)
        │  Transport (selected by connection type / output target)
        ▼
   PrinterTransport (raw bytes)            │ OsPrintTransport (HTML/PDF) │ ShareTransport (PDF)
   ├─ NetworkTransport (commonMain :9100)  │ ├─ Android PrintManager     │ ├─ WhatsApp / email / SMS
   ├─ BluetoothTransport (platform)        │ ├─ iOS UIPrintInteraction…  │ ├─ Save to file (FileKit)
   └─ UsbTransport (platform)              │ └─ Desktop PrinterJob/brws  │ └─ (future) cloud upload
```

A **third output target — Share/Export** — is first-class for SMBs: the same `PdfRenderer` output is
sent over WhatsApp / email / SMS or saved to a file (FileKit, currently unused in the repo). For many
small merchants this is used more than physical printing.

**Single most important decision:** an **intermediate representation (`PrintDocument`)** decouples
*what to print* (business logic, workspace-consistent) from *how to print* (device/printer-specific).
Every new "document × printer" combination is then just a mapper or a renderer, never a rewrite.

### Two output families (resolves the "two meanings of network" ambiguity)

| | Thermal / Label | Inkjet / Laser |
|---|---|---|
| Render to | ESC/POS / TSPL **bytes** | **HTML → (PDF)** from a page template |
| Sent via | our `PrinterTransport` (socket :9100 / BT / USB) | **platform print service** (`OsPrintTransport`) |
| Who finds the printer | we do (IP/MAC/USB enumerate) | the **OS** (AirPrint / Mopria / CUPS) |
| "Network" means | our app opens a TCP socket to the printer | the OS stack reaches a network/AirPrint printer |

We never open a socket for inkjet; the OS owns its driver/discovery.

---

## 4. Module Decomposition

This is a multi-module subsystem. A tiny dependency-free **core** sits at the bottom;
infrastructure libraries in the middle; the app-facing **feature** on top. The document coupling is
**inverted** so the printing engine never depends on invoice/order/etc.

```
printing/core        ── contracts + generic engine        (commonMain only; no platform/Room/UI)
printing/render      ── EscPos / Label / Html / Pdf        (depends: core; platform only for Pdf)
printing/transport   ── Network / Bluetooth / USB / OsPrint (depends: core; heavy expect/actual)
feature/printing     ── DB + sync + ViewModels + visual editor + nav + DI
                        (depends: core, render, transport, form-api, data/common, data/sync)

per-feature adapters ── INSIDE invoice/order/customer/product (a `.print` package)
                        (depends: printing/core, form-api only)
```

| Module | Layer | Source sets | Contains | Must NOT depend on |
|---|---|---|---|---|
| **printing/core** | contracts + engine | commonMain | `PrintDocument` IR, `PrintElement`, `Template`, `ThermalLayout`/`PageLayout`, `FieldBinding`, `PrinterProfile`, `PaperSpec`, enums; interfaces `Renderer`, `PrinterTransport`, `PrintValueProvider`, `DocumentMapper`; the generic template-walk engine | any feature, Room, Compose, platform |
| **printing/render** | renderers | common + android/ios/desktop (Pdf) | `EscPosRenderer`, `LabelRenderer`, `HtmlRenderer` (generalizes `buildInvoiceHtml`), `PdfRenderer` (expect/actual) | transport, feature/*, UI |
| **printing/transport** | connectivity | common + all platform | `NetworkTransport` (commonMain ktor-network), `BluetoothTransport`, `UsbTransport`, `OsPrintTransport` (moved-in `InvoicePrinter` actuals), discovery, permission flows | render, feature/* |
| **feature/printing** | app feature | common + platform DI | device-local printer-config DB + routing, workspace-synced template DB + `TemplateSyncDelegate`, ViewModels, visual template editor, printer/preview screens, navigation, Metro wiring | — (top of graph) |

### Dependency inversion: providers are contributed, not imported

The printing engine resolves fields generically; only per-document knowledge lives next to each
feature, contributed into a Metro map (exactly like `SyncDelegate`):

```kotlin
// feature/order/.../print/OrderPrintModule.kt
@ContributesIntoMap(WorkspaceScope::class) @DocumentTypeKey(DocumentType.ORDER) @Inject
class OrderPrintValueProvider(...) : PrintValueProvider<Order> { ... }
```

`feature/printing` injects `Map<DocumentType, PrintValueProvider>` and never imports
invoice/order. A new document type = add a provider in its own feature, zero changes to the printing
modules.

### Acyclic dependency graph

```
printing/core ◄── printing/render
printing/core ◄── printing/transport
printing/core, form-api ◄── per-feature adapters
printing/core, printing/render, printing/transport, form-api, data/* ◄── feature/printing
```

`printing/core` depends only on `common`/`locale` (money/date formatting types), so renderers,
transports, and adapters all build and unit-test independently of Room/Compose/platform.

---

## 5. Intermediate Representation (`PrintDocument`)

Printer-agnostic. Elements carry semantics; layout is interpreted per renderer.

```
PrintDocument { meta, blocks: List<PrintElement> }
PrintElement =
   | TextLine(text, align, style)
   | KeyValueRow(label, value, style)
   | Table(columns: List<Column>, rows: List<List<Cell>>)
   | Divider | Spacer(n)
   | Image(logoRef) | Barcode(value, symbology) | Qr(value, size)
   | Feed(n) | Cut(partial|full) | CashDrawerKick
style { bold, align, fontScale, widthWeight, padding }
```

Renderers consume the IR:
- `EscPosRenderer` → byte stream (char-grid composition, see §8).
- `LabelRenderer` → TSPL/ZPL stream.
- `HtmlRenderer` → self-contained HTML (CSS `@page` sized to the page spec); `PdfRenderer` converts
  when exactness / silent print / sharing is needed.

---

## 6. Templates & the Visual Editor

Templates are **data, not code**. A `Template` targets one printer class and one paper spec.

```
Template {
  id, name, documentType, printerClass (THERMAL|PAGE|LABEL),
  paperSpec (T58/T80 | A4..A7 | LABEL WxH),
  layout: ThermalLayout | PageLayout,
  blocks: List<TemplateBlock>
}
```

A **full visual editor** is required, with **two modes** because the hardware differs fundamentally
(see §8):

- **Thermal / Label mode** — vertical block list (single-column WYSIWYG at 58/80 mm); live preview
  rendered through the **actual `EscPosRenderer` to a monospace bitmap** so wrapping/alignment/cut
  position are true, not approximated.
- **Page mode (A4–A7)** — free 2-D canvas (positioned boxes, header/footer regions, real tables);
  preview via `HtmlRenderer`.

Both modes edit the same `Template` model; the editor and runtime share it, so what you design is
what prints. Default seeded templates ship in `composeResources` (thermal-80mm-invoice,
thermal-58mm-receipt, A4-tax-invoice, label-product-barcode) so the app prints out-of-the-box before
anyone opens the editor.

---

## 7. Field Binding — mapping data objects to template fields

**The binding catalog IS the form schema.** `feature/form` is the single source of truth for "what
fields exist on an entity" (`FormSchema` per `entityType`, exposed via
`ConfigLookup.observeSchema(entityType): Flow<FormSchema?>`). Each `FormField` already carries what a
binding needs: `source (STANDARD|CUSTOM)`, `fieldKey`, `displayName`, `dataType`, `sectionUid`,
`displayOrder`, `enumValues`. The editor's field picker is populated from the same schema that drives
the create/edit forms — **one catalog, two consumers** (data entry + printing), always in sync.

### Binding stored in a template (by stable key, never by position)

```
FieldBinding {
  fieldKey: String,                       // == FormField.fieldKey
  scope: DOCUMENT | LINE,                 // header value vs per-item (in a repeating table/line block)
  entityRef: SELF | CUSTOMER | PRODUCT,   // which schema/entity to resolve against (nested)
  source: STANDARD | CUSTOM | COMPUTED,
  format: FormatOverride?,                // date pattern / decimals / uppercase
  fallback: String?                       // shown when value null/blank
}
```

`entityRef` makes the catalog **composable**: the document is `SELF`; its `customer` resolves against
schema `"customer"`; each line's `product` against `"product"`.

### Resolution at render time — mirror the existing form bridge

The forms already map typed entity ↔ `fieldKey`-keyed map
(`CustomerFormMapping.toStandardValueMap()` for standard; `attributes: Map<String,String>` for
custom). Printing reuses this via a per-document `PrintValueProvider`:

```
interface PrintValueProvider<D> {
  fun standardValues(doc: D): Map<String, FieldValue>   // only code that knows the entity shape
  fun customValues(doc: D): Map<String, String>         // = doc.attributes
  fun lineProviders(doc: D): List<LineValueProvider>    // per item: standard + item.attributes
}
resolve(fieldKey, source) = when (source) {
  CUSTOM   -> customValues[fieldKey]
  STANDARD -> standardValues[fieldKey]
  COMPUTED -> compute(fieldKey)
}
```

Resolution = two map lookups + formatting (money/date via `formatMoney` / `formatDate` with
`LocalAppLocale`, since money/date aren't form `dataType`s).

### Three field categories the editor exposes

| Category | Source | Value from | Example |
|---|---|---|---|
| Standard | `FormField.source=STANDARD` | typed property via `standardValues()` | `order_number`, `grand_total` |
| Custom | `FormField.source=CUSTOM` | `entity.attributes[fieldKey]` | `vehicle_no`, `po_reference` |
| Computed (print-only) | not in form schema | a renderer-side `ComputedField` catalog | amount-in-words, tax-summary, UPI QR, page totals, copy label |

The picker shows fields regardless of the form's `visible`/`enabled` flags — those govern the
data-entry form, not printing.

### `dataType` → default print element + format

| dataType | Default element | Format |
|---|---|---|
| TEXT / TEXTAREA | TextLine | wrap |
| NUMBER | TextLine (right-align) | decimals; money fields → `formatMoney` |
| DATE | TextLine | `formatDate(value, locale)` |
| BOOLEAN | TextLine | ✓/✗ |
| CHOICE / MULTI_CHOICE | TextLine / chips | label from `enumValues` |
| (binding marked Barcode/QR) | Barcode / Qr | symbology + value |

### Prerequisite gap

Custom-field **values** exist today on **Customer** and **Product** (`attributes` map; Product DB
`attributes_json`) but **NOT on Order or Invoice**. To print custom order/invoice fields, a small
prerequisite mirrors Product exactly:
- add `attributes: Map<String,String>` to `Order`/`Invoice` domain models,
- add `attributes_json: String?` to their entities + (de)serialize in `asDomainModel()`,
- (their `FormSchema` + `ConfigLookup` already work).

Until then, order/invoice templates bind **standard + computed** fully; **custom** order/invoice
fields light up once the carrier is added. Customer/product custom fields (incl. on line items) work
immediately.

---

## 8. Two Layout Engines (thermal vs page)

Thermal heads have **no X/Y addressing**: a fixed character grid printed one line at a time. So
"alignment" there means **composing each line in software** by distributing a character budget across
columns. Page printers are free 2-D. Each template targets one family; we never flatten a page design
onto a thermal head.

| | Thermal / Label (flow) | Inkjet / Laser (page) |
|---|---|---|
| Coordinate model | ordered **Rows**, top→bottom | positioned boxes / flex regions |
| Width unit | **characters** (grid) | mm / points |
| Column widths | char counts **or weights** of line budget | mm / % |
| Fonts | discrete scale (1×/2×), monospace | proportional, any point size |
| Alignment | software-composed per line | layout engine (real tables, header/footer) |
| Page size | 58/80 mm continuous + cut | A4–A7 paginated |
| Renderer | `EscPosRenderer` → bytes | `HtmlRenderer` → HTML/PDF |
| Editor mode | vertical block list, monospace preview | free 2-D canvas |

### Thermal layout model

```
ThermalTemplate { columnsLogical: Int, blocks: List<ThermalBlock> }
ThermalBlock =
   | Row(cells: List<Cell>)                 // multi-column primitive
   | FullLine(text, align, fontScale)       // titles, single values
   | Divider | Feed(n) | Cut(partial|full)
   | Barcode(binding, symbology, height) | Qr(binding, size) | Image(logo)
Cell { binding, width: Chars(n) | Weight(w), align: LEFT|CENTER|RIGHT, overflow: WRAP|ELLIPSIS, fontScale }
```

Author columns in **weights/percent** so one 80 mm template re-flows to a 58 mm device; `Chars(n)`
pins fixed widths (e.g. a 3-char qty).

### Alignment algorithm ("done properly")

`columnsPerLine N` comes from the **printer profile capability** (e.g. `charsPerLine58 ≈ 32`,
`charsPerLine80 ≈ 48`; Font-B narrower → more), not assumed. Per `Row`:

1. **Budget** = `N − gaps − fixedWidths`, distributed to weighted cells.
2. **Per cell**: resolve value → align within width → **word-wrap** if longer (or ellipsis); a 2× cell
   consumes 2 base cells (recompute budget).
3. **Compose in software**: a Row emits `max(cell line counts)` lines; line *k* concatenates each
   cell's padded segment; shorter cells pad with blanks.
4. **Emit each composed line as left-aligned raw text** — alignment already done, so it's identical on
   every vendor. Hardware align (`ESC a`) used only for safe single-column titles/logos.

Example, 80 mm (N=48), columns `[name w=24 L][qty 5 R][rate 9 R][amt 10 R]`:
```
Item name that is quite lo  2   120.00    240.00
ng
```
Numeric columns right-aligned → decimals line up. Totals = `Row([label weight R][value 12 R])`.

The thermal editor preview renders through the real `EscPosRenderer` to a monospace bitmap so output
is non-WYSIWYG-accurate.

### Multi-language / non-Latin scripts on thermal

Thermal heads print from a fixed **codepage** (CP437/CP1252/etc.) — they cannot natively render
Devanagari/Tamil/Bengali/Arabic and other complex scripts. The `EscPosRenderer` therefore supports a
**raster fallback**: when a line contains glyphs outside the printer's codepage capability (declared
on the `PrinterProfile`), that line is rendered to a monochrome bitmap (Compose/skia text → raster)
and sent as an ESC/POS image (`GS v 0`). Latin/numeric content stays in fast native text mode; only
complex-script lines go raster. This matters for regional-language receipts common in Indian retail.

---

## 9. Inkjet / Page Pipeline

```
PageLayout Template + document (bindings resolved)
        │ HtmlRenderer → self-contained HTML (CSS @page sized to A4/A5/A6/A7)
        │ (optional) PdfRenderer → PDF for silent print / share / archive
        ▼
   OsPrintTransport (expect/actual)
        ├─ Android : WebView → createPrintDocumentAdapter → PrintManager.print() (or PrintedPdfDocument)
        ├─ iOS     : UIPrintInteractionController + UIMarkupTextPrintFormatter (HTML) or PDF
        └─ Desktop : HTML→PDF then PrinterJob / Desktop.print() (browser fallback = today's path)
```

- **HTML** is the portable intermediate for the interactive OS print dialog (reuses today's path;
  zero new deps on Android/iOS). Page size/margins from CSS `@page` driven by `paperSpec`.
- **PDF** is generated when we need exactness / no dialog (sharing, archiving, future silent/direct
  network print via IPP :631 or cloud).
- **Desktop HTML fidelity is the one real gap**: the JVM has no built-in HTML engine, so faithful A4
  print needs an **HTML→PDF step** (e.g. openhtmltopdf / Flying Saucer) → `PrinterJob`. Desktop-only.
- `PdfRenderer` is **expect/actual** (Android `PdfDocument`; iOS `UIGraphicsPDFRenderer`; Desktop
  openhtmltopdf/PDFBox), all fed from the same `PageLayout` IR.

This **generalizes the existing `InvoicePrinter` actuals** — keep them, move into
`printing/transport` as `OsPrintTransport`, feed them template-driven HTML instead of the hardcoded
`buildInvoiceHtml`.

---

## 10. Transport Layer — one command core, many channels

The ESC/POS byte stream is identical regardless of channel; only the channel differs.

```
EscPosRenderer → ByteArray ─► PrinterTransport.send(bytes)  (expect interface)
   ├─ NetworkTransport   commonMain, ktor-network :9100             (WiFi/Ethernet)
   ├─ BluetoothTransport android: BluetoothSocket SPP / BLE GATT
   │                     ios: ExternalAccessory MFi / CoreBluetooth BLE
   │                     desktop: bluez/serial where available
   └─ UsbTransport       android: UsbManager bulkTransfer; desktop: usb4java/serial; ios: n/a
```

| Transport | Android | iOS | Desktop | Lives in |
|---|---|---|---|---|
| Network :9100 | ✓ | ✓ | ✓ | **commonMain** |
| Bluetooth (SPP/BLE) | `BluetoothSocket` | `ExternalAccessory`(MFi)/CoreBluetooth(BLE) | limited | platform |
| USB | `UsbManager` | ✗ | usb4java/serial | platform |
| OS print (HTML/PDF) | PrintManager | UIPrint | PrinterJob/browser | platform |

**Discovery** is per-channel/per-platform (network = mDNS/Bonjour + manual IP; Bluetooth = paired
scan; USB = enumerate) and returns a common `DiscoveredPrinter` list to the UI.

---

## 11. Configuration Scoping (multi-user / multi-device / workspace)

Hybrid model:

| Concern | Scope | Storage | Rationale |
|---|---|---|---|
| Physical printer config (IP/MAC/USB id, paper width, connection type) | **Device-local** | non-synced Room table (feature/printing) | A counter printer isn't reachable from a rep's phone; connections are per-device |
| Default printer routing (invoices→Counter-80mm, labels→Zebra) | **Device-local** | same device DB | references device-local printers |
| Templates & formatting (layout, logo, fields, copies, tax breakup) | **Workspace-scoped + synced** | `TemplateEntity` via `TemplateSyncDelegate` (`SyncEntity.PRINT_TEMPLATE`) | every staff device prints an identical, brand-consistent document |
| (optional, deferred) per-user routing override on a shared device | **User+device** | DataStore keyed by userId | only if multiple users share one device |

This mirrors the existing split (DataStore device-local prefs vs the `store` module's synced
settings).

---

## 12. Persistence, DI & Sync

- **Printer config DB** (device-local): `PrinterEntity`, `PrintRoutingEntity` + DAOs. NOT synced.
  May live in a workspace-scoped DB but is never pushed; or in DataStore if simple enough.
- **Template DB** (workspace-synced): `TemplateEntity` (+ blocks) + DAO, `@SingleIn(WorkspaceScope::class)`.
- **`TemplateSyncDelegate`**: `@ContributesIntoMap(WorkspaceScope::class)` + `@SyncEntityKey(SyncEntity.PRINT_TEMPLATE)`;
  injects `Api` + `Dao` (+ `SyncStateDao`). Repository is local-only and marks `PENDING_PUSH` per the
  offline-sync rules. Backend must expose `GET/POST /printing/v1/templates/sync` (canonical contract).
- **ViewModels**: `@ContributesIntoMap(WorkspaceScope::class)` + `@ViewModelKey` (+ `@AssistedInject`
  where an id is needed).
- **Print orchestration service** (`PrintService`): discover → connect → render → send → cut → close;
  emits status via `SharedFlow`; persistent connections implement `WorkspaceClosable` and register
  with `WorkspaceClosableRegistry`.
- **Print spool + retry** (device-local, offline-first): a `PrintJobEntity` queue (status
  QUEUED/PRINTING/DONE/FAILED, copies, target printer, retry count). When a printer/network is
  unavailable the job stays QUEUED and a `PrintSpooler` retries on reconnect/printer-online — the same
  offline-first philosophy the app applies to data writes, applied to printing. This is **device-local
  and NOT synced** (a job belongs to the device that issued it). Note: this is a separate concern from
  `CentralSyncService` (which syncs *data*); spool retry is local and event-driven (printer/network
  availability), not server push/pull.

All workspace-aware DBs use `@SingleIn(WorkspaceScope::class)` + `closableRegistry.register { it.close() }`
with explicit reified type params, per `/metro-di`.

---

## 13. Navigation

- Add `@Serializable data object Printing : Route` to `shared/.../Routes.kt`.
- Feature routes: `PrinterListRoute`, `PrinterEditRoute(id?)`, `TemplateListRoute`,
  `TemplateEditorRoute(id?)`, `PrintPreviewRoute(documentType, documentId)`.
- `printingEntryProvider` added to `CombinedEntryProvider`; `Route.Printing → PrinterListRoute` in
  `mainRouteEntryProvider`; register in `ModuleRegistry` so the workspace module grid can open it.
- Print entry points: a Print action on invoice/order/receipt detail screens opens `PrintPreviewRoute`.

---

## 14. Platform Setup Deltas (net-new)

- **Android manifest**: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` (API 31+), legacy `BLUETOOTH` /
  `BLUETOOTH_ADMIN`, USB host feature; runtime permission flow (reuse the existing
  `LocationPermissionHandler` pattern — repo uses Accompanist for runtime perms).
- **iOS Info.plist**: `NSLocalNetworkUsageDescription` + `NSBonjourServices` (network discovery),
  `NSBluetoothAlwaysUsageDescription`; `UISupportedExternalAccessoryProtocols` if supporting MFi
  classic-Bluetooth thermal printers.
- **Desktop**: USB via a JVM lib (usb4java/jSerialComm); Bluetooth is weak on JVM → prefer network;
  HTML→PDF lib for page fidelity.

---

## 15. Phased Roadmap

1. **Phase 1 — Core + network thermal.** `printing/core` IR + template model + engine;
   `EscPosRenderer`; `NetworkTransport` (commonMain); `feature/printing` skeleton with device-local
   printer config + routing; invoice/receipt `PrintValueProvider`s; basic print preview; the
   **print spool + retry** queue (§12); operational actions (test print, open cash drawer, reprint,
   copies). **Reliability spine from §19 ships here**: idempotent jobs, printer-status pre-flight,
   per-printer single-writer lock, capability validation. **Plus the test harness from §21**
   (pure renderers + golden-file tests + `MockTransport`). Works on all 3 platforms with zero
   per-platform code. *(Highest value, lowest risk.)*
2. **Phase 2 — Bluetooth + USB transports** with platform discovery + permission flows (manifest/plist
   deltas). Same ESC/POS bytes, new channels. **Connection keep-alive + reconnect/backoff (§19)** and
   **post-print status poll** on bidirectional transports land here.
3. **Phase 3 — Visual template editor + sync.** Template DB + `TemplateSyncDelegate`; thermal
   vertical-block editor + live bitmap preview; field-binding picker from `ConfigLookup`.
4. **Phase 4 — Page mode + inkjet + share/export.** Generalize `HtmlRenderer` + `OsPrintTransport`;
   orders/credit/debit notes through the pipeline; A4–A7; 2-D canvas editor mode; `PdfRenderer`;
   **`ShareTransport`** (WhatsApp/email/SMS + save-to-file); add Order/Invoice `attributes` carrier
   (prerequisite §7); **GST compliance computed fields** (Original/Duplicate/Triplicate copies,
   e-invoice IRN QR, round-off, MRP/savings, HSN summary, Bill of Supply variant).
5. **Phase 5 — Additional document types + label/barcode.** Seeded templates + providers for
   **quotation/estimate/proforma, delivery challan/packing slip, payment receipt/voucher,
   purchase order/GRN, returns, account statement, day-end X/Z report, gift receipt, price/shelf
   tags** (each = a `DocumentType` + provider + template, no engine changes); `LabelRenderer`
   (TSPL/ZPL) + batch label printing.
6. **Phase 6 — Cloud + advanced.** `CloudTransport` POSTs the render job to a backend print service;
   silent/direct page printing (IPP :631) for high-volume back office; reuses the same IR + renderers.

---

## 16. Risks & Open Decisions

- **iOS classic-Bluetooth thermal printers need MFi** certification; BLE-only and network printers
  avoid it. If target printers are SPP-only, iOS Bluetooth is limited to MFi-listed models —
  network/BLE is the safe iOS path.
- **ESC/POS dialect variance** across vendors (Epson/Xprinter/…) — mitigate with per-profile
  capability flags (cut, codepage, QR, chars-per-line).
- **Desktop page-print fidelity** depends on the HTML→PDF library choice (new Desktop-only dep).
- **Visual editor is the largest UI effort** — Phases 1–2 deliver real thermal printing with seeded
  templates before the editor lands in Phase 3.
- **Backend work** required for template sync (`/printing/v1/templates/sync`) and, later, cloud print.
- **E-invoice IRN + signed QR** depend on backend/GSP integration (IRP registration) — the printing
  module only renders the QR/IRN once the data exists; it does not generate them.
- **Non-Latin thermal printing** uses raster fallback (§8) — slower and lower-resolution than native
  text; acceptable for receipts, validate on target hardware.
- **Document-type breadth** (Phase 5) depends on the corresponding data existing in the app
  (e.g. quotations/challans/GRN may need their own feature modules first) — printing renders what the
  domain provides.

---

## 17. Build Wiring Checklist (per new module)

For `printing/core`, `printing/render`, `printing/transport`, `feature/printing`:
- [ ] `settings.gradle.kts` → `include(":printing:core", ":printing:render", ":printing:transport", ":feature:printing")`
- [ ] `build.gradle.kts` copied from a peer (`feature/form-api` for core; `feature/unit` for the feature)
- [ ] `composeResources/` only in `feature/printing` and (for seeded templates) `printing/render`
- [ ] `shared/build.gradle.kts` → `api(projects.feature.printing)`; each document feature adds
      `api(projects.printing.core)` for its adapter
- [ ] If any module gets `maven-publish` and has `composeResources`/`ampairsapp.*` imports → pin
      `compose.resources { packageOfResClass = "ampairsapp.{module.path}.generated.resources" }`
- [ ] Compile all targets after commonMain changes:
      `./gradlew shared:compileKotlinIosSimulatorArm64 androidApp:compileDebugKotlinAndroid desktopApp:compileKotlin`

---

## 18. Retail / Wholesale / SMB Use-Case Coverage

A deliberate enumeration so no merchant workflow is forgotten. The architecture (§3–§7) supports every
row below with **no engine changes** — each is a `DocumentType` + a `PrintValueProvider` (in the
relevant feature) + a seeded `Template`. Phase indicates when it is planned.

### Document types

| Document | Typical printer | Notes | Phase |
|---|---|---|---|
| Tax Invoice | thermal / A4 | GST; Original/Duplicate/Triplicate copies; IRN+QR (B2B) | 1 / 4 |
| Bill of Supply | thermal / A4 | composition dealers / exempt goods (not a Tax Invoice) | 4 |
| Sale receipt | thermal | round-off, MRP/"you saved", UPI QR footer | 1 |
| Order confirmation | thermal / A4 | from order detail | 1 |
| Quotation / Estimate / Proforma | A4 | pre-sale; wholesale | 5 |
| Delivery challan / Packing slip / Dispatch | thermal / A4 | goods movement; e-way bill ref | 5 |
| Purchase order / GRN | A4 | buying side (needs domain data) | 5 |
| Payment receipt / voucher | thermal | advance & partial payments | 5 |
| Credit / Debit note (sales/purchase return) | thermal / A4 | reason + original-doc ref | 4 |
| Customer account statement / ledger | A4 | outstanding for credit customers (wholesale) | 5 |
| Day-end X / Z sales report + cash-drawer/shift | thermal | retail POS shift close | 5 |
| Gift receipt | thermal | no prices | 5 |
| Product/barcode label & price/shelf tag | label / thermal | batch printing | 5 |

### Cross-cutting operational features

| Feature | Covered by | Phase |
|---|---|---|
| Offline print spool + auto-retry | `PrintSpooler` / `PrintJobEntity` (§12) | 1 |
| Multiple copies + labeled copies (Original/Duplicate/Triplicate) | template `copies` + computed copy-label | 1 / 4 |
| Reprint with DUPLICATE/REPRINT mark | computed field + spool history | 1 |
| Test print / printer diagnostics | `PrintService` action | 1 |
| Open cash drawer (no-sale) | `CashDrawerKick` element + standalone action | 1 |
| Batch / bulk print (e.g. all of today's invoices) | spool enqueues N jobs | 4 |
| Share/export as PDF (WhatsApp / email / SMS / file) | `ShareTransport` + `PdfRenderer` | 4 |
| Silent / direct page printing (high volume) | IPP / default-printer, no dialog | 6 |
| Branding header (logo, GSTIN/FSSAI/license, contact) | template header + business profile | 3 |
| Footer (T&C, return policy, UPI-pay QR, thank-you) | template footer blocks | 3 |
| Multi-language / non-Latin receipts | `EscPosRenderer` raster fallback (§8) | 2–3 |

### GST / compliance specifics (India-centric, SMB-critical)

- **Original/Duplicate/Triplicate** copy labels on tax invoices.
- **E-invoice IRN + signed QR** for B2B above turnover threshold (backend mints IRN; we render).
- **E-way bill** number on delivery challans for goods movement.
- **HSN summary** block (mandatory above threshold) and **tax breakup** (CGST/SGST/IGST by rate).
- **Round-off** line; **MRP vs selling price / amount saved** on retail receipts.
- **Bill of Supply** variant (composition / exempt) distinct from Tax Invoice.

All of the above are **computed fields or template options** — they do not change the core engine,
renderers, or transports.

---

## 19. Reliability & Idempotency (the spine)

Printing's hardest question is *"did it actually print?"* Most thermal printers are **fire-and-forget
over a raw socket — there is no application-level ack**, so *sent ≠ printed*. The module is built
around this, not in spite of it.

- **Print-once guarantee.** Every job carries an **idempotency key** and runs a state machine:
  `QUEUED → SENDING → SENT → CONFIRMED | FAILED | UNKNOWN`. `UNKNOWN` (sent but unconfirmed) **never
  auto-retries** — it asks the user ("Did it print? Reprint / Mark printed"). This prevents the
  classic duplicate GST invoice / double receipt.
- **Spool hardening** (extends §12): max retries + exponential backoff, **dead-letter for poison
  jobs** (a malformed job must not retry forever — same trap as the offline-sync "PENDING with null
  path" bug), **TTL/expiry** (don't print yesterday's queued receipt today), **FIFO per printer**,
  process-death recovery (queue persisted), and user-visible/cancelable jobs.
- **Printer status / health.** Read ESC/POS real-time status (`DLE EOT n`) / ASB (`GS a`) for
  **paper-out, paper-near-end, cover-open, cutter jam, over-temp, offline**. Do a **pre-flight status
  check** and a **post-print poll** on bidirectional transports (USB/BT/network). A `PrinterStatus`
  model is surfaced in the UI — tell the cashier "out of paper" *before* they assume the bill printed.
- **Connection lifecycle.** **Reuse/keep-alive** connections with health checks + reconnect with
  backoff (do not open a Bluetooth socket per receipt). A **per-printer single-writer `Mutex`**
  (mirror `CentralSyncService.pushMutexes`) serializes concurrent jobs; `:9100` is single-session, so
  this lock is mandatory. Per-transport **timeouts + cancellation**, all off-main-thread (iOS
  `Dispatchers.Default`).
- **Capability negotiation.** Validate `Template.paperSpec/printerClass` against the resolved
  `PrinterProfile` at routing/preview — **reflow or reject**, never silently mangle (80 mm template to
  a 58 mm head, page template to thermal).

---

## 20. Reprint Integrity & Snapshotting

Templates are editable and workspace-synced, so a later template edit must **not** silently re-lay-out
a past document — a reprinted GST invoice must match the original.

- **Pin the template version** on the document (or snapshot the resolved `PrintDocument`/PDF) so
  reprints are faithful and auditable. This mirrors the app already snapshotting
  `sellerName/address/GST` on the invoice.
- **Reprints carry a DUPLICATE/REPRINT mark**; e-invoice output is immutable once generated.
- **Never recompute totals in the renderer** — render the document's stored amounts so paper matches
  the on-screen invoice exactly (only locale formatting is applied).

---

## 21. Testing & Hardware QA

Printing cannot be validated by compilation — this is the highest-leverage robustness investment.

- **Renderers are pure functions** (`IR → bytes/HTML`); add **golden-file tests** per `PrinterProfile`
  (byte-exact) so any regression in command output is caught in CI.
- **`MockTransport`** captures bytes to a file/preview, making the whole pipeline testable with **zero
  hardware**.
- A **hardware compatibility matrix** (Epson, Star, Xprinter, Rongta, TVS, Zebra + common 58/80 mm
  clones) and a **beta program** — "support as many printers as possible" is only real if it's tested.
- A built-in **printer-profile database** (vendor/model → capabilities: chars-per-line, codepage, cut,
  QR, status protocol) with auto-detect where possible + manual override, so merchants aren't
  hand-entering capability flags.

---

## 22. Operability — Telemetry, RBAC, Flags, Migration

- **Telemetry** (reuse the app's Sentry + Firebase): structured **print events** (success/fail,
  time-to-print, error code, printer model, template id) + an **on-device print history** for support.
  Without this, field "it didn't print" tickets are undebuggable.
- **Authorization (RBAC).** The **template editor is gated to admin/owner roles** (workspace RBAC) —
  a cashier must not redesign the company invoice. Printing itself is open to all staff.
- **Feature flags / kill switch** via the existing `store` toggles — disable printing (or a specific
  transport/vendor) **per workspace without an app update** if a regression appears.
- **Migration / coexistence (strangler pattern).** The existing `InvoicePrinter` path stays live
  behind a flag until the new module reaches parity — no rip-out, no printing outage during build-out.

---

## 23. Success Metrics / SLOs (acceptance bar)

- **≥ 99% print success** on supported hardware; **< 1% duplicate-print rate**.
- **Tap-to-print < 3 s** on thermal/LAN, **< 8 s** on Bluetooth.
- **Zero-config onboarding**: auto-discover → one-tap set default → seeded template → test print;
  track time-to-first-successful-print for a new merchant.
- **Crash-free print sessions**; spool fully drains within a bounded time after reconnect.

---

## 24. Graphics: Store Logo, Barcodes & Payment (UPI) QR

Logos and codes use the same IR elements across every printer class (`Image`, `Barcode`, `Qr` — §5);
only the rasterization differs by class. This reuses the raster path from §8.

### Store logo
- **Source**: the **business-profile logo** already in the app, resolved once and cached per device.
- **Thermal**: convert to a **monochrome 1-bit raster** (scale to the head's dot width, Floyd–Steinberg
  dither / threshold) and emit as an ESC/POS raster image (`GS v 0`); optionally store it as an **NV
  logo** in printer memory (`FS q` / `FS p`) for fast repeated prints.
- **Inkjet / Laser (HTML/PDF)**: a normal full-resolution `<img>`.
- Placed as a header block in both layout engines — sized in dots (thermal) or mm/% (page).

### Barcodes & QR — native first, raster fallback
- Thermal printers with native code support use `GS ( k` (QR) / `GS k` (1-D: Code128/EAN/UPC).
  Printers that lack it (declared on `PrinterProfile`) get a **code rasterized in `commonMain`** and
  printed as an image. Inkjet/Laser always render codes as images in HTML/PDF. **One `Barcode`/`Qr`
  IR element → three output paths.**

### Payment (UPI) QR — scan-to-pay on the bill
- It is a **dynamic QR, not a 1-D barcode**, carrying the UPI deep link
  `upi://pay?pa={VPA}&pn={payee}&am={amount}&tn={ref}&cu={currency}`.
- **Dynamic per document**: payee VPA + name from the **business/payment profile**; `am` = the
  document's **stored grand total** (never recomputed — §20); `tn` = invoice/order number for
  reconciliation. Exposed as the computed field **`upi_qr`** (§7) so it drops into any template.
- Renders on **thermal, inkjet, and laser** via the native/raster QR path above — the printed
  receipt/invoice is scannable to pay on every printer class. A **static counter QR** (fixed VPA, no
  amount) is also supported.

---

## 25. Page Bands & Pagination (repeating headers/footers)

A page document (A4–A7) can span multiple pages, so a `PageLayout` template is organized into
**bands**, not a flat block list. Thermal/label stay continuous (one roll, no pagination) — this is a
page-mode concern (§8). All bands use the same elements + `FieldBinding`s as everywhere else, now
**page-aware**.

### Band model
- **Page header** — repeats across pages; variants: *first-page-only*, *every-page*, *except-first*.
- **Column header** — a table's header row, **repeated at the top of every page the table spans**.
- **Body** — the flowing content (line items / statement rows) that breaks across pages.
- **Page footer** — repeats across pages; variants: *every-page* or *last-page-only* (e.g. grand total
  + signature only on the final page).

### Repeating static & dynamic content
- **Static** (logo, seller address/GSTIN, terms, "computer-generated" note): placed once in a band,
  printed on every page it's assigned to.
- **Dynamic**: bound fields re-resolved per page — **`page_number`, `page_count`, "Page X of Y"**, a
  **`continued`** marker on non-final pages, and **carry-forward subtotals** (`brought_forward` /
  `carried_forward`) for long itemized invoices and account statements. These are computed fields
  (§7), so they drop into any band.

### How it renders
- **HtmlRenderer**: lean on **CSS print primitives** — `@page` margin-boxes for running header/footer,
  `position: fixed` running elements, `<thead>` auto-repeat per printed page, and
  `break-inside: avoid` / `break-after` to control row/section breaks. The browser/OS engine paginates.
- **PdfRenderer**: an explicit **band-pagination engine** measures the body, breaks it into pages, and
  draws the assigned header/column-header/footer bands on each page (native PDF has no CSS);
  carry-forward subtotals accumulate at each break.
- Both consume the same banded `Template`; the page-mode visual editor (§6/§8) edits the
  header / column-header / body / footer regions directly (the free 2-D canvas becomes region-aware).

This keeps one template + binding model while giving full control over **what repeats, where, and on
which pages**.
