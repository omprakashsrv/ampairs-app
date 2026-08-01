# feature/analytics — Analytics & Forecasting Dashboard (mobile)

Offline-first analytics dashboard for the Ampairs KMP app (spec `022`; backend module `analytics/`).
Android + iOS + Desktop, Compose Multiplatform, Metro DI (`WorkspaceScope`).

## What it does
- **Offline KPI dashboard** — sales / net / tax / invoices / avg invoice / collections / stock value /
  low-stock / outstanding / inventory-turns for a selected period, computed **on-device** from the
  per-module read-only *agent DAOs* (invoice/inventory/payment/product) composed by
  `DashboardReadFacade` (no cross-DB join; each figure from its own module). Business-timezone
  bucketing via the zone the screen pushes into the ViewModel.
- **Charts** (`ui/charts/Charts.kt`) — the sales-trend **line** and receivables-aging **columns** are
  rendered with **Vico** (`com.patrykandpatrick.vico:multiplatform`, Compose Multiplatform). The GST
  intra/inter **donut** and the top-customers / top-products **ranked horizontal bars** stay on a KMP
  `Canvas` (`MaterialTheme` colours): Vico is Cartesian-only (no pie/donut), and a horizontal ranked
  list reads better for long named categories than Vico's vertical columns.
- **Top customers / top products** — ranked-by-sales breakdowns for the period, composed in
  `DashboardReadFacade` from the invoice agent DAO (`topCustomersBetween` / `topProductsBetween`);
  surfaces the customer & product module signals with no new sync plumbing.
- **Orders** — active-order total + a status breakdown (snapshot, not period-scoped) from the order
  agent DAO (`countActive` / `statusCounts`).
- **Home widgets** (`ui/home/`) — embedded in the app home screen (in `shared/`): `AnalyticsHomeSummary`
  (KPI strip + 7-day sparkline, taps to the dashboard) and `RecentActivityCard` (latest invoices +
  orders interleaved newest-first, from the invoice/order agent DAOs' `recentActivity`).
- **Demand forecast** — `DemandForecastSyncDelegate` (PULL-ONLY) mirrors the server
  `/forecasts/sync` feed into Room; the dashboard shows a per-product sparkline + reorder flag, and
  falls back to an on-device EWMA (`domain/DemandForecasting`) when the mirror is empty.
- **Ask a question** — `NlQueryMatcher` maps common questions to KPI tiles offline (deterministic, no
  LLM); free-form is handled by the separate agent assistant.
- **Deep-history + coverage** — when a period predates the local sync window, `DashboardViewModel`
  fetches the earlier slice via `AnalyticsApi` and merges it (`DashboardData.mergePriorSlice`), or
  shows a reduced-coverage badge when offline (`DashboardCoverage`).
- **Configurable layout** — add/remove/reorder KPI tiles, persisted per-workspace as a `StoreSetting`
  (`AnalyticsDashboardSettings`, `DashboardTile`) riding `SyncEntity.STORE`.
- **CSV export** — clipboard share via `buildDashboardCsv` (currency symbol passed in from the composable).
- **Agent-queryable** — `agent/AnalyticsQueryExecutor` + schema expose the `demand_forecast` table to
  the assistant's SafeQuery path.

## Layout
```
domain/      DashboardModels, DashboardTile, DemandForecasting (EWMA), NlQueryMatcher, AgingBuckets, coverage/merge
data/api/    AnalyticsApi (+Impl): forecast sync + deep-history dashboard reads
data/query/  DashboardReadFacade — composes the agent DAOs into DashboardData
data/settings/ AnalyticsDashboardSettings — dashboard_layout read/write
sync/        DemandForecastSyncDelegate (pull-only)
agent/       AnalyticsQueryExecutor + AnalyticsQuerySchemaModule
ui/dashboard DashboardScreen + DashboardViewModel; ui/charts Charts.kt
```
`DemandForecastEntity`/`DemandForecastDao` and the invoice/inventory/payment/product **agent DAOs**
live in `:data:database` (consolidated `AmpairsWorkspaceDatabase`), reached via
`WorkspaceDatabaseProvider` — the feature does not depend on `:data:database` for its own tables.

## Build / test
KMP build needs the JetBrains-vendor JDK toolchain, which is egress-blocked in the dev sandbox — rely
on **CI** (`pr.yml`: Android + Desktop + iOS compile + Kover). Pure-logic unit tests
(`DemandForecastingTest`, `DashboardMergeTest`, `NlQueryMatcherTest`, `AgingBucketsTest`,
`AnalyticsQuerySchemaTest`, `DemandForecastSyncDelegateTest`) run in `:feature:analytics:check`.
