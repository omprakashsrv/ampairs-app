# Implementation Plan: Multi-Platform Printing Module

**Branch**: `001-multi-platform-printing` | **Date**: 2026-06-19 | **Spec**: [spec.md](./spec.md)

**Detailed design**: `docs/features/MULTI_PLATFORM_PRINTING_PLAN.md` (authoritative HOW — modules, IR,
renderers, transports, layout engines, reliability spine, bindings, pagination). This file is the
condensed planning view that `tasks.md` is generated from.

## Summary

A reusable printing subsystem for the Ampairs KMP app printing invoices/orders/receipts/credit-debit
notes (and later labels) to thermal (ESC/POS), inkjet/laser (HTML/PDF via OS print), and label
(TSPL/ZPL) printers over network/Bluetooth/USB, plus share/export as PDF. Built around a
printer-agnostic `PrintDocument` IR with pluggable renderers and transports, form-schema-driven field
bindings, two layout engines (thermal char-grid + page bands/pagination), a full visual template
editor, hybrid scoping (device-local printers, workspace-synced templates), and a reliability spine
(print-once idempotency, printer status, offline spool with retry).

## Technical Context

**Language/Runtime**: Kotlin Multiplatform 2.4.0; Compose Multiplatform 1.11.1; targets Android, iOS,
Desktop (JVM). **DI**: Metro. **DB**: Room KMP. **Networking**: Ktor 3.5.0 (+ ktor-network for raw
sockets). **Storage**: existing DataStore for device-local prefs. **Testing**: Kotlin test + golden
files + a `MockTransport`. **Observability**: existing Sentry + Firebase.

## Module structure (new)

```
printing/core        ── PrintDocument IR, Template model, FieldBinding, PrinterProfile, PaperSpec,
                        enums, interfaces (Renderer, PrinterTransport, PrintValueProvider,
                        DocumentMapper), generic template-walk engine. commonMain only.
printing/render      ── EscPosRenderer, LabelRenderer, HtmlRenderer, PdfRenderer (expect/actual).
printing/transport   ── NetworkTransport (commonMain), BluetoothTransport, UsbTransport,
                        OsPrintTransport, ShareTransport, discovery, permissions. expect/actual.
feature/printing     ── device-local printer/routing/spool DBs, workspace-synced template DB +
                        TemplateSyncDelegate, ViewModels, visual editor, screens, navigation, DI.
per-feature adapters ── PrintValueProvider + DocumentMapper inside invoice/order/customer/product
                        (.print package), contributed via Metro; engine never imports them.
```

## Constitution / project-rule check

- Workspace-aware DBs (templates) use `@SingleIn(WorkspaceScope::class)` + `WorkspaceClosableRegistry`;
  device-local DBs (printers, routing, spool) are not synced.
- Offline-first: template repo is local-only + `markPendingPush`; `TemplateSyncDelegate` owns the API.
- No `java.*`/`android.*` in commonMain; platform code via expect/actual. Money/date via `formatMoney`/
  `formatDate` + business locale. Strings via Compose resources. ViewModels via Metro.
- Compile all three targets after commonMain changes.

## Phasing (maps to spec user stories)

1. Setup + Foundational (core IR, interfaces, engine, test harness) — blocks all stories.
2. US1 (P1) thermal-over-network + reliability spine + idempotent spool.
3. US2 (P1) discovery/config/routing + Bluetooth/USB transports.
4. US3 (P2) template DB + sync + visual editor + bindings + logo + UPI QR + RBAC.
5. US4 (P2) page renderer + PDF + OS print + pagination + share/export.
6. US5 (P2) reprint integrity + GST compliance.
7. US6 (P3) additional document types + labels.
8. Polish — telemetry, feature flag, strangler migration, hardware QA, SLO instrumentation.
