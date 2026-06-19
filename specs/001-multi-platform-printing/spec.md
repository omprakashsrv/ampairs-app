# Feature Specification: Multi-Platform Printing Module

**Feature Branch**: `001-multi-platform-printing`

**Created**: 2026-06-19

**Status**: Draft

**Input**: User description: "Multi-platform printing module for the Ampairs KMP app (Android, iOS, Desktop). Print invoices, orders, receipts, credit/debit notes and labels to thermal (ESC/POS), inkjet/laser (HTML/PDF via OS print), and label (TSPL/ZPL) printers over network, Bluetooth, USB, plus share/export. Reliability spine (print-once idempotency, printer status, spool with retry), reprint integrity, logo and UPI QR, GST compliance, visual template editor, hybrid scoping, golden-file tests, telemetry and RBAC, SLOs."

**Design reference**: A reviewed technical design exists at `docs/features/MULTI_PLATFORM_PRINTING_PLAN.md`. This spec captures the WHAT/WHY; the plan captures the HOW.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reliable receipt/invoice printing on a thermal printer (Priority: P1)

A retail cashier finalizes a sale and prints a GST receipt/invoice on a counter thermal printer. The
printed output is correctly formatted for the paper width, prints once and only once, and the cashier
is clearly told if it failed (e.g. out of paper) so they can recover — including when the network or
printer is briefly unavailable.

**Why this priority**: This is the core value of the module and the highest-frequency action in a
store. Without dependable, single-copy thermal printing, nothing else matters.

**Independent Test**: Configure one network thermal printer, complete a sale, tap Print, and confirm a
correctly formatted receipt prints exactly once; then simulate paper-out and printer-offline and
confirm the cashier sees an actionable status and no silent duplicate.

**Acceptance Scenarios**:

1. **Given** a configured 80mm thermal printer, **When** the cashier prints an invoice, **Then** a
   correctly aligned receipt prints once with the store's totals matching the on-screen amounts.
2. **Given** the printer is out of paper, **When** the cashier prints, **Then** the app reports
   "out of paper" before assuming success and does not mark the job printed.
3. **Given** the network drops mid-send, **When** the job cannot be confirmed, **Then** the app shows
   an "unconfirmed — reprint or mark printed" prompt and never auto-reprints silently.
4. **Given** the printer is offline when printing, **When** it comes back online, **Then** the queued
   job either prints (if safe) or remains visible and cancelable in a print queue.

---

### User Story 2 - Connect printers and route documents (Priority: P1)

A store owner connects printers over network/WiFi, Bluetooth, or USB on a given device, runs a test
print, and sets which printer handles which document type (e.g. receipts → counter thermal, invoices →
office A4). These printer settings belong to the device, not the whole workspace.

**Why this priority**: Printing cannot happen until a printer is connected and selected; routing is
required for stores with more than one printer/station.

**Independent Test**: On one device, discover and add a printer for each available connection type,
run a successful test print, set per-document defaults, and confirm a second device in the same
workspace does not inherit those device-specific printers.

**Acceptance Scenarios**:

1. **Given** an available network/Bluetooth/USB printer, **When** the owner runs discovery, **Then**
   the printer appears and can be added and test-printed.
2. **Given** multiple printers, **When** the owner sets a default per document type, **Then**
   subsequent prints of that document type use the chosen printer without re-asking.
3. **Given** printers configured on device A, **When** the owner opens the same workspace on device B,
   **Then** device B has its own independent printer list.

---

### User Story 3 - Customize the printed format with a visual template editor (Priority: P2)

An admin designs how documents look — adds the store logo, chooses which fields appear (including
custom fields), arranges columns, sets headers/footers and terms, adds a scan-to-pay UPI QR — using a
visual editor. The template applies consistently to every staff member's device because it is shared
across the workspace.

**Why this priority**: Branding and field control are important for a professional, compliant
document, but the app must already print (P1) before formatting is customizable.

**Independent Test**: Edit the invoice template (add logo + a custom field + UPI QR), save, and confirm
the change appears when any device in the workspace prints that document type.

**Acceptance Scenarios**:

1. **Given** the editor, **When** an admin adds the store logo and a custom field binding, **Then** a
   preview reflects the change and the saved template prints with both.
2. **Given** a shared template edit, **When** another device prints the same document type, **Then** it
   uses the updated template.
3. **Given** a non-admin staff member, **When** they open template settings, **Then** they cannot edit
   the workspace template (view/print only).

---

### User Story 4 - A4 invoice printing and share/export as PDF (Priority: P2)

A wholesaler prints a multi-page A4 tax invoice on an office inkjet/laser printer, with the logo,
column headers, and "Page X of Y" repeating on each page and the grand total only on the last page;
and can instead send the invoice as a PDF over WhatsApp/email or save it to a file.

**Why this priority**: Page printing and PDF sharing are essential for wholesale/B2B and for customers
who want a digital copy, but come after the core thermal path.

**Independent Test**: Print a long invoice on A4 and confirm header/column-header/footer repeat
correctly across pages; then share the same invoice as a PDF and confirm a faithful file is produced.

**Acceptance Scenarios**:

1. **Given** an invoice spanning multiple pages, **When** printed on A4, **Then** the page header,
   table column headers, and page numbers repeat on every page and the final total appears once.
2. **Given** an invoice, **When** the user chooses Share, **Then** a PDF is produced and can be sent via
   the device's share options or saved to a file.

---

### User Story 5 - Faithful reprints and compliant copies (Priority: P2)

A staff member reprints a past invoice; the reprint matches the original layout and is clearly marked
as a duplicate, with GST copy labels (Original/Duplicate/Triplicate) and the scan-to-pay/IRN QR where
applicable.

**Why this priority**: Compliance and audit integrity are required for tax documents; reprints that
silently change layout are a legal risk.

**Independent Test**: Edit the invoice template, then reprint an older invoice and confirm it renders
in its original layout with a DUPLICATE mark, not the new template.

**Acceptance Scenarios**:

1. **Given** a template was changed after an invoice was issued, **When** that invoice is reprinted,
   **Then** it renders faithfully to how it was originally issued.
2. **Given** a reprint, **When** it prints, **Then** it is marked as a duplicate and shows the correct
   copy label.

---

### User Story 6 - Additional documents and labels (Priority: P3)

A merchant prints other documents (quotation, delivery challan, payment receipt, account statement,
day-end report, gift receipt) and product/price/barcode labels, including batch label printing.

**Why this priority**: Broadens coverage to full retail/wholesale workflows; valuable but builds on the
established engine and templates.

**Independent Test**: Select a supported additional document type or a set of products, print, and
confirm correct output on the routed printer.

**Acceptance Scenarios**:

1. **Given** a supported additional document type, **When** the user prints it, **Then** it uses its
   own template and the routed printer.
2. **Given** a set of products, **When** the user batch-prints labels, **Then** one label per
   product/quantity is produced with barcode and price.

---

### Edge Cases

- **Out of paper / cover open / cutter jam**: detected and surfaced before the job is reported as
  printed; the job is recoverable.
- **Sent-but-unconfirmed** (fire-and-forget device with no acknowledgement): never auto-duplicated;
  user decides reprint vs mark-printed.
- **Printer offline / network down**: job queues and is visible, cancelable, and bounded by a time-to-
  live so stale jobs are not printed later.
- **Poison job** (malformed/never-printable): does not retry forever; moves to a failed/dead state.
- **Template/printer mismatch** (80mm template to a 58mm printer, or a page template to thermal):
  re-flowed or rejected with a clear message — never silently mangled.
- **Custom field removed/renamed** in form configuration: templates referencing it surface a broken
  binding rather than printing wrong data.
- **Same network printer used by multiple devices simultaneously**: jobs serialize; no interleaved
  output.
- **Non-Latin/regional-language content** on thermal: prints correctly (via image rendering when the
  printer lacks the font).
- **Workspace switch**: device printers persist; workspace templates reflect the newly active
  workspace.
- **Totals/rounding**: printed amounts always equal the document's stored amounts (no re-computation).

## Requirements *(mandatory)*

### Functional Requirements

**Documents & output**

- **FR-001**: System MUST print invoices, orders, receipts, and credit/debit notes; and MUST be
  extensible to additional document types (quotation, delivery challan, payment receipt, account
  statement, day-end report, gift receipt) and labels/barcodes without redesigning the engine.
- **FR-002**: System MUST support three printer classes: thermal receipt (line-by-line with cut),
  page printers (inkjet/laser, A-series and US Letter/Legal), and label/barcode printers.
- **FR-003**: System MUST support printing over network/WiFi/Ethernet, Bluetooth, and USB (where the
  platform allows), and MUST support producing a PDF for share/export (messaging/email/file).
- **FR-004**: System MUST run on Android, iOS, and Desktop, using each platform's native print service
  for page printers.
- **FR-005**: Printed monetary and date values MUST use the active workspace's business locale, and
  printed totals MUST equal the document's stored totals (never recomputed).

**Printers & routing**

- **FR-006**: Users MUST be able to discover, add, test-print, and remove printers per connection type.
- **FR-007**: Users MUST be able to set a default printer per document type (routing).
- **FR-008**: Printer configuration and routing MUST be device-local (not shared across devices), while
  templates MUST be shared across the workspace.

**Templates & fields**

- **FR-009**: Admins MUST be able to design templates in a visual editor supporting a line-based mode
  for thermal/label and a 2-D page mode for inkjet/laser.
- **FR-010**: Templates MUST allow defining headers, footers, and content that repeats across pages,
  including repeating table column headers, "Page X of Y", and carry-forward subtotals (page mode).
- **FR-011**: Template fields MUST be selectable from the existing per-entity field catalog (standard
  and custom fields), plus computed/print-only fields (e.g. amount-in-words, tax summary, page
  numbers, UPI QR).
- **FR-012**: System MUST support placing the store logo and barcodes/QR codes on any printer class.
- **FR-013**: System MUST support a dynamic scan-to-pay (UPI) QR carrying the payee identity and the
  document's amount and reference, on thermal, inkjet, and laser output.
- **FR-014**: System MUST ship default templates so documents print correctly before any customization.
- **FR-015**: System MUST validate template-to-printer compatibility and either re-flow or reject with
  a clear message.

**Reliability**

- **FR-016**: System MUST guarantee print-once semantics: a job is tracked through queued → sending →
  sent → confirmed/failed/unconfirmed, and unconfirmed jobs MUST NOT be auto-reprinted.
- **FR-017**: System MUST detect and surface printer health (out of paper, near-end, cover open, cutter
  error, offline) before reporting success, where the connection supports a status channel.
- **FR-018**: System MUST queue print jobs when a printer/network is unavailable and retry on recovery,
  with bounded retries, a dead-letter state for un-printable jobs, and a time-to-live for stale jobs.
- **FR-019**: System MUST serialize concurrent jobs to the same printer (no interleaved output).
- **FR-020**: Users MUST be able to view, cancel, and reprint jobs from a print queue/history.

**Reprint & compliance**

- **FR-021**: Reprints MUST be faithful to the document's originally issued layout and MUST be marked as
  duplicates.
- **FR-022**: System MUST support GST requirements: labeled Original/Duplicate/Triplicate copies, HSN
  summary, tax breakup, round-off, Bill of Supply vs Tax Invoice, and rendering of an e-invoice IRN/QR
  when that data is available.

**Operability & governance**

- **FR-023**: Template editing MUST be restricted by role (admin/owner); printing MUST be available to
  all staff.
- **FR-024**: System MUST record print outcomes (success/failure, error reason, timing, printer, and
  template used) for diagnostics, and keep a local print history.
- **FR-025**: Printing (and individual transports) MUST be controllable by a per-workspace feature flag
  so it can be disabled without an app update.
- **FR-026**: The new module MUST coexist with the existing invoice print path during rollout so
  printing is never interrupted.
- **FR-027**: System MUST support operational actions: test print, open cash drawer (no-sale), reprint,
  and selectable number of copies.
- **FR-028**: System MUST print regional/non-Latin-script content correctly on thermal printers.

### Key Entities *(include if feature involves data)*

- **Printer**: a configured physical printer on a device — its class, connection details, paper
  capability, and capability flags (cut, codepage, code support, status protocol). Device-local.
- **Print Routing**: the mapping of document type → default printer on a device. Device-local.
- **Template**: a reusable, versioned definition of how a document type renders for a printer class and
  paper size, including bands, blocks, field bindings, logo, codes, and copy settings. Workspace-shared.
- **Field Binding**: a reference from a template element to a data field (standard, custom, or computed)
  resolved against a document at print time.
- **Print Document (rendered)**: the printer-agnostic representation of a specific document produced
  from a template + data, used for preview and rendering, and snapshotted for faithful reprint.
- **Print Job**: a queued unit of work with status, idempotency key, target printer, copies, and retry
  state. Device-local.
- **Printer Status**: the current health of a printer (paper, cover, cutter, online/offline).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: At least 99% of print attempts on supported hardware result in a correct printout.
- **SC-002**: Duplicate-print rate is under 1% of jobs.
- **SC-003**: Tap-to-print completes in under 3 seconds on a thermal printer over local network, and
  under 8 seconds over Bluetooth.
- **SC-004**: A new merchant can go from zero configuration to a first successful print (discover → set
  default → test print) in under 5 minutes.
- **SC-005**: When a printer is offline, queued jobs print successfully after reconnection without user
  re-entry, and stale jobs beyond their time-to-live are never printed.
- **SC-006**: Printed totals match the on-screen document totals in 100% of cases.
- **SC-007**: A template change never alters the appearance of previously issued documents on reprint.
- **SC-008**: Print success/failure outcomes are observable for at least 95% of jobs (for support and
  monitoring).
- **SC-009**: The same document can be printed correctly across all three supported platforms and all
  supported printer classes.

## Assumptions

- The active workspace's business locale (currency, timezone, date format) and the store logo and
  UPI/payment identity are available from the existing business profile.
- The existing per-entity field configuration (form schema) is the source of truth for which standard
  and custom fields exist; printing consumes it and does not define its own field catalog.
- Custom-field values are available on the documents being printed; documents that do not yet carry
  custom values (e.g. orders/invoices) will have that capability added as a prerequisite, mirroring
  entities that already do.
- Backend support is required and will be provided for: workspace template synchronization, and (for
  e-invoice IRN/QR) the IRN data itself; the module renders IRN/QR but does not generate them.
- Cloud-based printing is a future extension and is out of scope for the initial release.
- iOS USB printing is out of scope (not permitted by the platform); iOS Bluetooth support is limited to
  printers compatible with platform requirements, with network/BLE as the primary iOS path.
- The full visual template editor is in scope, sequenced after the core printing and connectivity paths
  are proven on real hardware.
