---
description: "Task list for Multi-Platform Printing Module"
---

# Tasks: Multi-Platform Printing Module

**Input**: Design documents from `/specs/001-multi-platform-printing/` and the authoritative design
`docs/features/MULTI_PLATFORM_PRINTING_PLAN.md`.

**Prerequisites**: spec.md (user stories), plan.md (module structure).

**Tests**: INCLUDED — the spec mandates golden-file renderer tests and a `MockTransport` (FR-016..020,
SC-001/002). Test tasks are first-class here.

**Organization**: Tasks are grouped by user story (US1–US6) so each is independently buildable,
testable, and demoable. KMP paths use the new modules from plan.md.

## Format: `[ID] [P?] [Story] Description`

- **[P]** = can run in parallel (different files, no dependency)
- **[Story]** = US1..US6, or SETUP/FOUND/POLISH

## Path conventions

- `printing/core/src/commonMain/kotlin/com/ampairs/printing/core/...`
- `printing/render/src/{common,android,ios,desktop}Main/...`
- `printing/transport/src/{common,android,ios,desktop}Main/...`
- `feature/printing/src/{common,android,ios,desktop}Main/...`
- per-feature adapters under e.g. `feature/invoice/.../print/`

---

## Implementation progress (2026-06-20)

**End-to-end thermal-over-network printing is implemented and CI-green on Android, Desktop, and iOS**
(PR #82). Full vertical: add a network printer in the UI → tap Print on an invoice → engine renders
ESC/POS → idempotent spool → network transport; plus workspace template sync.

**Done & CI-verified (all 3 platforms):**
- **Modules**: `printing/core`, `printing/render`, `printing/transport`, `feature/printing` created,
  wired into `shared`; `ktor-network` added; `SyncEntity.PRINT_TEMPLATE` (T001–T005).
- **Core** (T007–T010): `PrintDocument` IR, `Template`/`FieldBinding` model, `PaperSpec`
  (thermal/page incl. Letter-Legal/label) + `PrinterProfile`, interfaces, `BindingResolver`,
  `PrintEngine`, `SpoolPolicy`.
- **Render** (T017, T045, T056): `EscPosRenderer` + `ThermalLineComposer`, `HtmlRenderer`,
  `LabelRenderer`; golden/pipeline tests (T014–T016).
- **Transport** (T018, T011): `NetworkTransport` (commonMain ktor-network :9100), `MockTransport`.
- **feature/printing** (T019, T020, T023, T027, T036): Room DB (printers/routing/spool/templates),
  repositories, `PrintService` + `PrintCoordinator` (per-printer mutex, idempotent), seeded
  `DefaultTemplates`, `TemplateApi` + `TemplateSyncDelegate`, Metro DI.
- **UI & nav** (T013, T033): `PrinterListScreen` + VM (add/delete/set-default), `Route.Printing`,
  entry provider, Nav3 registration.
- **Invoice path** (T022, T024): `InvoicePrintValueProvider` + `InvoiceViewViewModel.printThermal()`
  wired to a Print action.

**Not yet done (well-defined follow-ups):**
- Platform transports: Bluetooth (T028), USB (T029), OS-print/inkjet + Share/PDF (T047–T049),
  discovery (T030), printer status reads (T021).
- Visual template editor (T038, T039) + binding picker from `FormSchema` (T037); page bands (T046).
- More document providers + labels/batch (T050, T055–T057); reprint/GST compliance (T051–T054).
- Print preview/queue UI, test print, ModuleRegistry menu entry; telemetry/RBAC/flags (T058–T068).
- Order/Invoice `attributes` carrier (custom-field printing prerequisite); localize print-status strings.

> Note: this sandbox cannot build KMP modules (the Android KMP plugin needs a JetBrains-vendor JDK 21,
> not provisionable here — foojay 403). All verification was done via the PR's CI (Android+Desktop+iOS
> compile + tests), which is green through commit `8bb1f34`.

---

## Phase 1: Setup (shared infrastructure)

- [x] T001 [SETUP] Create the four modules and register them in `settings.gradle.kts`
      (`:printing:core`, `:printing:render`, `:printing:transport`, `:feature:printing`).
- [x] T002 [P] [SETUP] Add `build.gradle.kts` per module (copy `feature/form-api` for core,
      `feature/unit` for the feature); add `composeResources` only to `feature/printing` and
      `printing/render` (seeded templates).
- [x] T003 [P] [SETUP] Add `ktor-network` (raw sockets) and any QR/barcode-gen + image-raster libs to
      `gradle/libs.versions.toml`; wire into `printing/transport` and `printing/render`.
- [x] T004 [SETUP] Add `api(projects.printing.*)` wiring: `shared/build.gradle.kts` →
      `api(projects.feature.printing)`; document features add `api(projects.printing.core)`.
- [x] T005 [P] [SETUP] Add `SyncEntity.PRINT_TEMPLATE` to `data/sync` and a `DocumentType` enum to
      `printing/core`.
- [ ] T006 [SETUP] Compile-gate: `./gradlew shared:compileKotlinIosSimulatorArm64 androidApp:compileDebugKotlinAndroid desktopApp:compileKotlin`.

---

## Phase 2: Foundational (blocking prerequisites)

**⚠️ No user-story work begins until this phase is complete.**

- [x] T007 [P] [FOUND] Define `PrintDocument` IR + `PrintElement` (TextLine, KeyValueRow, Table,
      Divider, Spacer, Image, Barcode, Qr, Feed, Cut, CashDrawerKick) in `printing/core/.../model/`.
- [x] T008 [P] [FOUND] Define `Template`, `ThermalLayout`/`PageLayout`, `TemplateBlock`, `FieldBinding`,
      `PrinterProfile`, `PaperSpec` (thermal 58/80mm; page A4–A7 **and US Letter/Legal**; label W×H),
      `ConnectionType`, `PrinterClass` in `printing/core/.../model/`.
- [x] T009 [P] [FOUND] Define interfaces `Renderer`, `PrinterTransport`, `PrintValueProvider`,
      `DocumentMapper`, `ComputedFieldCatalog` in `printing/core/.../`.
- [x] T010 [FOUND] Implement the generic template-walk engine (resolve bindings → build
      `PrintDocument`; iterate line-scope bindings for tables) in `printing/core/.../engine/`
      (depends T007–T009).
- [x] T011 [P] [FOUND] Implement `MockTransport` (captures bytes to file/preview) and the golden-file
      test harness in `printing/render/src/commonTest/` and `printing/transport/src/commonTest/`.
- [ ] T012 [P] [FOUND] Metro DI skeleton: contribute `Map<DocumentType, PrintValueProvider>` and
      `Map<PrinterClass, Renderer>`; `feature/printing` platform `@ContributesTo(WorkspaceScope)` stubs.
- [x] T013 [FOUND] Add `Route.Printing` + feature routes + `printingEntryProvider` +
      `mainRouteEntryProvider` mapping + `ModuleRegistry` entry (navigation wiring).

**Checkpoint**: IR, engine, DI map, navigation, and test harness exist — stories can begin.

---

## Phase 3: User Story 1 — Reliable thermal printing over network (P1) 🎯 MVP

**Goal**: print a correctly formatted, single-copy invoice/receipt to a network thermal printer with
status feedback and offline queueing.

**Independent test**: configure one network thermal printer, print an invoice, confirm exactly one
correct receipt; simulate paper-out and offline and confirm actionable status + no duplicate.

### Tests for US1

- [x] T014 [P] [US1] Golden-file tests for `EscPosRenderer` (58mm + 80mm, alignment/wrap/cut) in
      `printing/render/src/commonTest/`.
- [x] T015 [P] [US1] Unit tests for the spool state machine (queued→sending→sent→confirmed/failed/
      unconfirmed; dead-letter; TTL) in `feature/printing/src/commonTest/`.
- [x] T016 [P] [US1] Integration test: engine + `EscPosRenderer` + `MockTransport` prints a seeded
      invoice; assert totals equal stored amounts (SC-006).

### Implementation for US1

- [x] T017 [P] [US1] `EscPosRenderer`: char-grid line composition (capability-driven `columnsPerLine`,
      weighted columns, word-wrap, right-align, cut) in `printing/render/.../escpos/`.
- [x] T018 [P] [US1] `NetworkTransport` (raw socket :9100) in `printing/transport/src/commonMain/`
      (ktor-network) with timeout/cancellation.
- [x] T019 [US1] Idempotent `PrintJobEntity` + DAO + `PrintSpooler` (retry/backoff, dead-letter, TTL,
      FIFO per printer, process-death recovery) — device-local DB in `feature/printing/.../spool/`.
- [x] T020 [US1] `PrintService` orchestration: per-printer single-writer `Mutex`, pre-flight status,
      render→send→cut→status-poll, emits status via `SharedFlow` in `feature/printing/.../service/`.
- [ ] T021 [US1] `PrinterStatus` read via ESC/POS `DLE EOT`/ASB (paper/cover/cutter/offline) in
      `printing/transport/.../status/`.
- [x] T022 [P] [US1] `InvoicePrintValueProvider` + `ReceiptPrintValueProvider` (standard + computed
      fields; locale formatting) in `feature/invoice/.../print/`.
- [x] T023 [P] [US1] Seed default 80mm/58mm invoice + receipt templates in
      `printing/render/.../composeResources/`.
- [ ] T024 [US1] Print preview + Print action on the invoice detail screen (incl. a **copies**
      selector, FR-027); print queue UI (view/cancel/reprint); "unconfirmed" + "out of paper" prompts.
- [ ] T025 [US1] Capability negotiation: validate template `paperSpec`/`printerClass` vs the selected
      `PrinterProfile`; reflow or reject (FR-015).

**Checkpoint**: US1 fully functional — MVP demoable on all three platforms via network printer.

---

## Phase 4: User Story 2 — Connect printers & route documents (P1)

**Goal**: discover/add/test printers over network/Bluetooth/USB on a device and set per-document
defaults; printers are device-local.

**Independent test**: add a printer per connection type on one device, test-print, set defaults, and
confirm a second device does not inherit them.

### Tests for US2

- [ ] T026 [P] [US2] Tests for routing resolution (document type → default printer) and device-local
      persistence in `feature/printing/src/commonTest/`.

### Implementation for US2

- [x] T027 [US2] Device-local `PrinterEntity` + `PrintRoutingEntity` + DAOs + repository (NOT synced),
      scoped **per device + per workspace** (keyed by workspace; new instance per workspace graph) in
      `feature/printing/.../data/` (FR-008).
- [ ] T028 [P] [US2] `BluetoothTransport` actuals: Android `BluetoothSocket` SPP/BLE; iOS
      ExternalAccessory/CoreBluetooth; Desktop limited — in `printing/transport/src/{android,ios,desktop}Main/`.
- [ ] T029 [P] [US2] `UsbTransport` actuals: Android `UsbManager`; Desktop usb4java/serial; iOS n/a.
- [ ] T030 [P] [US2] Discovery per channel (mDNS/Bonjour + manual IP; paired BT scan; USB enumerate)
      with caching → common `DiscoveredPrinter` list.
- [ ] T031 [US2] Permission flows: Android BT manifest + runtime (Accompanist pattern); iOS Info.plist
      (`NSLocalNetworkUsageDescription`, Bonjour, BT); reuse `LocationPermissionHandler` pattern.
- [ ] T032 [US2] Connection keep-alive + reconnect/backoff + health checks (extends `PrintService`).
- [ ] T033 [US2] Printer list/add/edit/test-print screens + per-document routing settings; open
      cash-drawer (no-sale) + test-print actions (FR-027).

**Checkpoint**: US1 + US2 work — full thermal printing across all connection types.

---

## Phase 5: User Story 3 — Visual template editor + sync + bindings (P2)

**Goal**: admins design templates (logo, fields incl. custom, UPI QR, headers/footers); templates are
workspace-synced; editing is admin-gated.

**Independent test**: edit invoice template (logo + custom field + UPI QR), save, confirm it appears on
another device's print.

### Tests for US3

- [x] T034 [P] [US3] Tests for `TemplateSyncDelegate` (push/pull, version conflict re-pull/retry) and
      broken-binding detection in `feature/printing/src/commonTest/`.

### Implementation for US3

- [ ] T035 [US3] Workspace-synced `TemplateEntity` (+ blocks) + DAO + local-only repository
      (`markPendingPush`) — `@SingleIn(WorkspaceScope)` in `feature/printing/.../data/`.
- [x] T036 [US3] `TemplateSyncDelegate` (`@ContributesIntoMap(WorkspaceScope)`,
      `@SyncEntityKey(PRINT_TEMPLATE)`); backend `GET/POST /printing/v1/templates/sync` (coordinate).
- [ ] T037 [US3] Field-binding picker sourced from `ConfigLookup.observeSchema(documentType)`
      (standard + custom) + computed-field catalog; broken-binding surfacing.
- [ ] T038 [US3] Visual editor — **thermal/label mode** (vertical block list + live `EscPosRenderer`
      bitmap preview) in `feature/printing/.../editor/`. (Page-mode editor moved to US4 → T049a.)
- [ ] T040 [P] [US3] Logo support: source from business profile; thermal 1-bit raster (`GS v 0`/NV
      logo) in `EscPosRenderer`; `<img>` for page (`printing/render`).
- [ ] T041 [P] [US3] Barcode/QR + dynamic UPI scan-to-pay QR computed field (`upi_qr`: VPA + stored
      amount + ref); native ESC/POS code + raster fallback.
- [ ] T042 [US3] RBAC: gate the template editor to admin/owner roles (FR-023).
- [ ] T043 [P] [US3] Non-Latin script raster fallback in `EscPosRenderer` (FR-028).

**Checkpoint**: US1–US3 — customizable, branded, scan-to-pay thermal documents shared per workspace.

---

## Phase 6: User Story 4 — Page printing (inkjet/laser) + PDF share/export (P2)

**Goal**: A4–A7 (and Letter/Legal) page printing via OS print service with bands/pagination, plus
share/export as PDF.

**Independent test**: print a multi-page A4 invoice (repeating header/column-header/page numbers,
final-page total); share the same invoice as a PDF.

### Tests for US4

- [ ] T044 [P] [US4] Golden-file/snapshot tests for `HtmlRenderer` output and the band-pagination
      engine in `printing/render/src/commonTest/`.

### Implementation for US4

- [x] T045 [US4] `HtmlRenderer` (generalize `buildInvoiceHtml`): CSS `@page`, running header/footer,
      `<thead>` repeat, break control in `printing/render/.../html/`.
- [ ] T046 [US4] Page band model + pagination computed fields (`page_number`/`page_count`/`continued`/
      carry-forward subtotals) in `printing/core` + engine.
- [ ] T049a [US4] Visual editor — **page mode** (region-aware 2-D canvas: header/column-header/body/
      footer) with live `HtmlRenderer` preview (moved from US3; depends on T045/T046) in
      `feature/printing/.../editor/`.
- [ ] T047 [P] [US4] `PdfRenderer` expect/actual (Android `PdfDocument`; iOS `UIGraphicsPDFRenderer`;
      Desktop HTML→PDF lib) + explicit band-pagination for PDF.
- [ ] T048 [US4] `OsPrintTransport` expect/actual (move/generalize existing `InvoicePrinter` actuals:
      Android `PrintManager`, iOS `UIPrintInteractionController`, Desktop `PrinterJob`/browser).
- [ ] T049 [P] [US4] `ShareTransport`: PDF over share sheet (WhatsApp/email) + save-to-file (FileKit).
- [ ] T050 [US4] Order/credit-note/debit-note `PrintValueProvider`s + seeded A4 templates.

**Checkpoint**: US1–US4 — thermal + page + share across all platforms.

---

## Phase 7: User Story 5 — Reprint integrity + GST compliance (P2)

**Goal**: faithful, duplicate-marked reprints; GST copies, IRN/QR, HSN summary, Bill of Supply.

**Independent test**: change a template, reprint an older invoice, confirm original layout + DUPLICATE
mark and correct copy label.

### Tests for US5

- [ ] T051 [P] [US5] Tests: reprint renders the snapshot/pinned template version, not the current one
      (SC-007); duplicate marking.

### Implementation for US5

- [ ] T052 [US5] Pin template version on the document (or snapshot the rendered `PrintDocument`) at
      issue time; reprint resolves the pinned version in `printing/core` + `feature/printing`.
- [ ] T053 [P] [US5] DUPLICATE/REPRINT mark + Original/Duplicate/Triplicate copy-label computed fields.
- [ ] T054 [P] [US5] GST computed fields: HSN summary, tax breakup, round-off; Bill-of-Supply template
      variant; e-invoice IRN/QR rendering when IRN data is present (backend coordinate).

**Checkpoint**: US1–US5 — audit-safe, compliant documents.

---

## Phase 8: User Story 6 — Additional documents + labels (P3)

**Goal**: more document types and label/barcode printing (incl. batch).

**Independent test**: print a supported additional document type; batch-print labels for a product set.

### Implementation for US6

- [ ] T055 [P] [US6] `PrintValueProvider`s + seeded templates for quotation/estimate/proforma,
      delivery challan/packing slip, payment receipt/voucher, account statement, day-end X/Z report,
      gift receipt (each in its owning feature's `.print` package).
- [x] T056 [P] [US6] `LabelRenderer` (TSPL/ZPL) + label `PaperSpec`s in `printing/render/.../label/`.
- [ ] T057 [US6] Batch label printing (N labels per product/quantity) + price/shelf tag templates.

**Checkpoint**: All stories independently functional.

---

## Phase 9: Polish & cross-cutting

- [ ] T058 [P] [POLISH] Telemetry: structured print events (success/fail, reason, timing, model,
      template) via Sentry/Firebase + on-device print history (FR-024, SC-008).
- [ ] T059 [P] [POLISH] Feature-flag/kill switch via `store` toggles (printing + per-transport)
      (FR-025).
- [ ] T060 [POLISH] Strangler migration: keep existing `InvoicePrinter` path behind a flag until parity;
      switch over (FR-026).
- [ ] T061 [P] [POLISH] Built-in printer-profile database (vendor/model → capabilities) + auto-detect.
- [ ] T062 [P] [POLISH] Hardware compatibility matrix + beta checklist in `docs/features/`.
- [ ] T063 [POLISH] SLO instrumentation + dashboards for SC-001/002/003/005/008.
- [ ] T064 [P] [POLISH] WASM stance (no-op transports / OS-print-only) documented and enforced.
- [ ] T065 [POLISH] Full 3-target compile + golden-test CI gate; update plan/docs.
- [ ] T066 [P] [POLISH] Performance validation for tap-to-print: instrument and assert SC-003
      (< 3s thermal/LAN, < 8s Bluetooth) on the hardware matrix.
- [ ] T067 [P] [POLISH] Zero-config onboarding flow (auto-discover → set default → test print) and
      measurement of time-to-first-successful-print against SC-004 (< 5 min).
- [ ] T068 [P] [POLISH] Security/privacy: LAN-only printer-address validation, spool/history + exported
      PDF retention limits and cleanup (FR-029).

---

## Dependencies & execution order

- **Setup (P1)** → **Foundational (P2)** blocks everything.
- **US1, US2 (P1)** can proceed in parallel after Foundational (US2 transports reuse US1 `PrintService`).
- **US3 (P2)** needs Foundational + US1 renderer for preview; delivers the **thermal/label-mode**
  editor only. The **page-mode editor** moved to US4 (T049a) since it needs the page renderer.
- **US4 (P2)** needs Foundational; independent of US3 except shared seeded templates.
- **US5 (P2)** needs US1 (issue/print) and US3 (templates) for version pinning.
- **US6 (P3)** needs Foundational + the relevant renderer (EscPos/Label/Html).
- **Polish** after the desired stories.

### Parallel opportunities

- All `[P]` Setup/Foundational tasks; renderer vs transport vs DB work; per-feature `PrintValueProvider`s
  (different files); golden tests vs implementation once interfaces (T009) are frozen.

## Implementation strategy

1. Setup + Foundational → foundation ready.
2. US1 → validate on a real network thermal printer → **MVP**.
3. US2 → Bluetooth/USB → broaden hardware.
4. US3 → editor + sync + branding.
5. US4 → page + PDF + share.
6. US5 → compliance/reprint.
7. US6 → more documents + labels.
8. Polish → telemetry, flags, migration, hardware QA, SLOs.

## Notes

- Tests-first for renderers (golden files) and the spool state machine — they are the reliability and
  regression backbone (SC-001/002).
- Repositories stay local-only (`markPendingPush`); only `TemplateSyncDelegate` holds the API.
- Compile all three targets after each commonMain change; no `java.*`/`android.*` in commonMain.
