# printing (mobile)

Cross-platform printing subsystem: visual template editor, a domain-agnostic render engine, thermal
(ESC/POS) + page (HTML) + label output, multi-transport delivery (network / Bluetooth / USB / OS
print), an idempotent spool, and offline-sync of templates to the backend.

## Module layout (4 Gradle modules)

| Module | Source sets | Role |
|---|---|---|
| `:printing:core` | commonMain | **Pure domain**: `Template`/`TemplateBlock`, `PrintDocument`/`PrintElement`, `PrintEngine`, `StaticHtmlResolver`, `PaperSpec`, `PrinterProfile`, and the `Renderer` / `PrinterTransport` / `PrintValueProvider` interfaces. No platform, no DI, no IO. |
| `:printing:render` | commonMain | Pure `Renderer` implementations: `HtmlRenderer` (page), `EscPosRenderer` (thermal), `LabelRenderer`, `ThermalLineComposer`. Must stay IO-free (golden-file tested). |
| `:printing:transport` | android/desktop/ios + common | `PrinterTransport` actuals: `NetworkTransport`, `BluetoothThermalTransport` (Android), `UsbThermalTransport` (Android), `OsPrintTransport` (expect/actual per platform), `MockTransport`. |
| `:feature:printing` | all | Wires it together: Room DB (templates/printers/jobs), repositories, `TemplateSyncDelegate`, `PrintCoordinator`/`PrintService`, Compose UI (printer list, queue, template list/editor), `HtmlPreview` (expect/actual), Metro DI. |

`core`/`render` know nothing about invoices/orders — document knowledge is inverted into a
`PrintValueProvider` implemented by each owning feature and contributed via Metro.

## The print pipeline

```
Template (DYNAMIC blocks | STATIC html file)
   └─ PrintEngine.build(template, docId, provider)         → PrintDocument (blocks → PrintElements)
      or PrintEngine.buildStatic(template, docId, provider, html)  → PrintDocument[RawHtml]
         ↓
   Renderer (by PrinterClass)  HtmlRenderer / EscPosRenderer / LabelRenderer
         ↓  RenderedOutput  (Bytes | Markup(html) | Pdf)
   PrinterTransport (by ConnectionType)  network / bluetooth / usb / os-print
```

- `PrintEngine` resolves `FieldBinding`s against the provider into a printer-agnostic `PrintDocument`.
  Bindings are stored by stable `fieldKey` (never positions) so templates survive sync.
- `PrintCoordinator` (`@SingleIn(WorkspaceScope)`) is the entry point: resolve printer → resolve
  template → build → spool a `PrintJob` → send via `PrintService`. Idempotent on `idempotencyKey`
  (a retry can never double-print). `SpoolPolicy` drives job state transitions.
- `ValueFormatter` is locale-aware and supplied by the caller; renderers never recompute amounts.
  Currency/date follow the workspace business locale (see root CLAUDE §12 / `/cmp-practices` §12).

## Templates: DYNAMIC vs STATIC

`Template.kind`:
- **DYNAMIC** — authored in the block editor (`TemplateEditScreen`). `blocks: List<TemplateBlock>`
  (BoundText, StaticText, KeyValue, **LineTable** (per-line columns), **InfoGrid** (fixed labelled
  grid → HTML `<table>` on page printers), Divider, Spacer, Logo, Barcode, Qr, CutMark, CashDrawer).
- **STATIC** — a real `.html` file with `{{variables}}`. The file is stored in the **file module**
  (`entityType = PRINT_TEMPLATE`, `entityUid = template.id`), not inline; `Template.htmlFileUid`
  references it. At print/preview, `PrintCoordinator` reads the bytes via `FileRepository` and
  `PrintEngine.buildStatic` substitutes only the placeholders. Imported via FileKit picker in
  `TemplateListViewModel.importStaticTemplate`. Sample templates:
  `commonMain/composeResources/files/templates/{invoice_sample,order_sample}.html`.

`StaticHtmlResolver` supports `{{field}}`, `{{ref.field}}` (nested entity, e.g. `customer.name`),
and `{{#lines}}…{{/lines}}` repeat sections; values are HTML-escaped.

## Default template selection

Multiple templates can exist for one `(documentType, printerClass)`. `Template.isDefault` picks the
one used at print: `TemplateRepository.firstTemplate(type, class)` honors `isDefault` via
`listByTypeAndClass`; `setDefault(id)` (one default per pair). The list UI shows a **Default** chip /
"Set as default". `isDefault` is carried inside `template_json` (so no Room migration was needed) and
is also first-class on the backend (`is_default` column) and synced.

> **COLLATE NOCASE gotcha**: templates are stored with `documentType.name` ("INVOICE") but queried
> with `documentType.key` ("invoice"). The template DAO lookups use `COLLATE NOCASE` — keep that when
> adding queries on `document_type` / `printer_class`.

## Offline sync

`TemplateSyncDelegate` (`@ContributesIntoMap(WorkspaceScope)`, `@SyncEntityKey(SyncEntity.PRINT_TEMPLATE)`)
owns all template ↔ server traffic on the canonical `/printing/v1/templates/sync` contract
(`TemplateApi`). Repository is local-only and flags `markPendingPush`; the delegate bulk-pushes
unsynced rows (soft-deletes ride in-band, `active = false`) and pulls the workspace set (full pull —
templates are few). Local unsynced edits win; server-inactive rows are hard-deleted. See `/offline-sync`.
Backend counterpart: `ampairs` → `workspace/.../printing` (see its CLAUDE.md).

## HTML preview (platform-specific — read before touching)

`HtmlPreview` is `expect @Composable` with per-platform actuals — there is real history here:
- **Desktop**: JavaFX `WebView` inside a `JFXPanel` (`HtmlPreview.desktop.kt`). It is **displayed**,
  not snapshotted (off-screen snapshots render blank). `Platform.setImplicitExit(false)`, context
  menu disabled. Needs the OpenJFX deps (host classifier) in `build.gradle.kts`.
- **Android**: `WebView` with `useWideViewPort = false`, zoom enabled.
- **iOS**: WKWebView; inject a viewport meta tag.
- `HtmlRenderer` emits **legacy HTML attributes** (`<p align>`, `<b>`, `<font size>`, `<td width=% align=>`),
  not CSS classes — JEditorPane (used in some preview paths) only understands HTML 3.2/CSS1. `@page`
  is scoped to `@media print` only; do **not** add a `width=device-width` viewport meta to the page
  HTML (it forces a wide layout in the preview).

## Navigation routes (`PrintingRoutes.kt`)
`PrinterListRoute`, `PrintQueueRoute` (spool/history + retry + mark-printed + **delete**),
`TemplateListRoute`, `TemplateEditRoute(templateId)`.

## DI / data
- Room: `PrintingDatabase` (templates, printers, print jobs) — workspace-scoped
  (`@SingleIn(WorkspaceScope)`, registered with `WorkspaceClosableRegistry`). Platform `@ContributesTo`
  modules in `PrintingModule.{android,ios,desktop}.kt`; DAOs in `PrintingDaoModule`.
- ViewModels are `@ContributesIntoMap(WorkspaceScope)` (assisted for the editor). Repos are unscoped
  `@Inject`. See `/metro-di`.
- **No `fallbackToDestructiveMigration`** on the printing DB — add explicit Room migrations.

## Conventions specific to this subsystem
- `core` and `render` must stay **pure** (no platform imports, no IO) — they are unit/golden tested
  (`RenderPipelineTest`, `StaticHtmlResolverTest`, `ThermalLineComposerTest`, `SpoolPolicyTest`).
- Adding a `TemplateBlock` ⇒ update: `PrintEngine.renderBlock` + `bindingsOf`, `PrintElement`, every
  renderer (`HtmlRenderer`/`EscPosRenderer`/`LabelRenderer`), and `ReceiptPreview`.
- Money/dates in printed output go through a `ValueFormatter` / a `currencySymbol: String` param —
  never hardcode `₹`/`$` (non-composable code can't read `LocalAppLocale`).
- Validate all targets after `core`/`render` changes:
  `./gradlew :printing:core:compileKotlinIosSimulatorArm64 :printing:render:compileKotlinIosSimulatorArm64`.
