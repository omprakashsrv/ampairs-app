# Feature Specification: Dynamic Pricing & Replenishment (Mobile)

**Feature Branch**: `003-dynamic-pricing-replenishment`

**Created**: 2026-06-28

**Status**: Draft

**Input**: User description: "specs/027-dynamic-pricing-replenishment" (mobile client of the backend Dynamic Pricing & Replenishment feature)

## Overview *(informative)*

This is the **mobile-app** side of the Dynamic Pricing & Replenishment capability (backend spec
`ampairs/specs/027-dynamic-pricing-replenishment`). Today the app prices every order/invoice line
from a flat catalog price plus an ad-hoc manual discount, and the inventory screens show stock but
never tell the user *when* or *how much* to reorder. This feature brings two capabilities to the
Android / iOS / Desktop app:

1. **Dynamic pricing at order time** — the right price (customer-group discount, quantity break,
   seasonal/promo price) is applied automatically as the user adds each order/invoice line, computed
   **on-device so it works with no network**, and frozen onto the saved line so later rule changes
   never re-price a committed document.
2. **Replenishment on the device** — reorder suggestions (reorder point, suggested quantity, and the
   demand/lead-time assumptions behind them) are synced down and shown in the app, and the user can
   accept a suggested reorder level or build a draft purchase from selected items.

The app is the *consumer and offline evaluator* of pricing rules and the *viewer/initiator* of
replenishment; the backend owns the authoritative rule set, the replenishment math, and re-validates
prices on sync.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Offline rule-based pricing while capturing an order (Priority: P1)

A field salesperson opens the app with no connectivity and builds an order for a customer who belongs
to the "Wholesale" group. As each product line is added — including one line of 60 units during an
active "Diwali" promotional window — the correct unit price appears immediately, the line shows which
rule produced it, and nothing waits on the network. The salesperson can still manually override a
line price when needed. When the order is saved and later syncs, the price the app showed equals the
price the server confirms, and editing the rules afterward never changes this saved order.

**Why this priority**: Order capture in the field is the app's core offline workflow; pricing must be
correct and instantaneous without connectivity. This is the minimum viable slice and delivers value
on its own.

**Independent Test**: With pricing rules synced to the device, put the device in airplane mode, build
a multi-line order for a grouped customer spanning a quantity break and an active promo, and confirm
(a) each line is priced by the most-specific/highest-priority rule, (b) the applied rule is shown,
(c) a manual override wins and is marked as manual, (d) after reconnect the saved line prices match
the server and are unchanged by later rule edits.

**Acceptance Scenarios**:

1. **Given** a synced 10%-off Wholesale-group rule, **When** the salesperson adds a line for a
   Wholesale customer offline, **Then** the line unit price is 10% below catalog and the line shows
   the Wholesale rule was applied.
2. **Given** both a quantity-break rule (50+ units) and a group rule match a line of 60 units,
   **When** the line is added, **Then** exactly one rule wins by the documented precedence (no
   compounding by default) and the result is shown.
3. **Given** a promo valid only 1–31 October in the workspace business timezone, **When** a line is
   priced on 30 September, **Then** the promo is not applied; on 5 October it is applied.
4. **Given** the device is offline with the current rule set, **When** the order syncs later, **Then**
   the server-confirmed price equals the price computed on-device for the same line inputs.
5. **Given** a saved order priced by a rule, **When** that rule is later edited or deactivated and the
   order is reopened/reprinted, **Then** the order keeps its originally recorded price.
6. **Given** a salesperson manually edits a line price, **When** the line is saved, **Then** the
   manual price overrides any rule and the line records that the price was set manually.

---

### User Story 2 - Reorder suggestions and draft purchases on the device (Priority: P2)

An inventory manager opens a "needs reordering" screen in the app. For each item it shows the
suggested reorder point, a suggested order quantity, and the demand and lead-time assumptions behind
them (synced from the backend). The manager selects several at-or-below-reorder-point items and
creates a draft purchase that carries a generated number and can be reviewed before commit. The
manager can accept a suggested reorder level onto an item; the inventory record stays the system of
record and is never overwritten without an explicit accept. The list is viewable offline from the
last sync.

**Why this priority**: High operational value (prevents stockouts/over-ordering) but depends on the
backend producing suggestions and on demand history, so it follows the pricing slice.

**Independent Test**: With suggestions synced, open the reorder screen offline and confirm each item
shows its reorder point, suggested quantity, and assumptions; select items and confirm a numbered,
editable draft purchase is created; accept a suggestion and confirm only that explicit action changes
the item's reorder level.

**Acceptance Scenarios**:

1. **Given** synced reorder suggestions, **When** the manager opens the reorder screen offline,
   **Then** items at or below their reorder point are listed with their assumptions visible.
2. **Given** several selected items, **When** the manager creates a draft purchase, **Then** one
   numbered draft is created with a line per item at its suggested quantity, editable before commit.
3. **Given** a suggestion, **When** the manager accepts the suggested reorder level, **Then** the
   item's reorder level updates; **When** the manager ignores it, **Then** the item's reorder level is
   unchanged.
4. **Given** an item flagged by the backend as having insufficient demand data, **When** it appears in
   the list, **Then** it is shown as "insufficient data" rather than with a fabricated reorder point.

---

### User Story 3 - Natural-language pricing & replenishment questions on-device (Priority: P3)

A user asks the in-app assistant "which items need reordering?" or "what's the Wholesale price for SKU
A-100?" and gets a correct answer drawn from the synced rules and suggestions, resolved through the
app's existing on-device query path.

**Why this priority**: A convenience layer over data the first two stories already sync locally; adds
reach but is not required for core value.

**Independent Test**: With rules and suggestions present locally and a chat model loaded, ask each
sample question and confirm the answer matches the pricing/reorder screens.

**Acceptance Scenarios**:

1. **Given** synced reorder suggestions, **When** the user asks which items need reordering, **Then**
   the assistant lists items at or below their reorder point.
2. **Given** a synced customer-group rule, **When** the user asks for that group's price on an item,
   **Then** the assistant returns the price the on-device pricing evaluation would apply.

---

### Edge Cases

- **No rule matches a line** → the catalog price is used and the line records a catalog fallback,
  never a blank or zero price.
- **Two rules tie on every precedence factor** → the device breaks the tie deterministically (same
  rule chosen as the server would) so offline and server prices agree.
- **Future-dated or expired rule** → must not affect today's on-device pricing; windows open/close at
  the correct moment in the workspace business timezone, not the device timezone.
- **Back-dated document** → prices using the rules effective on the document's business date.
- **Device holds a stale rule set** (offline a long time) → the order still prices and saves offline;
  on sync the server re-validates and the authoritative result is reflected back without blocking.
- **Currency rounding** → percentage/amount adjustments resolve to a clean rounded unit price with no
  fractional drift, identical on device and server, and amounts display in the workspace business
  currency.
- **Workspace switch** → pricing rules and reorder suggestions shown always belong to the active
  workspace only (no stale data from the previous workspace).
- **Reorder screen with no prior sync** → shows an empty/needs-sync state rather than fabricated data.

## Requirements *(mandatory)*

### Functional Requirements

**Pricing at order time (P1)**

- **FR-001**: The app MUST keep the current pricing rule set available locally and refresh it via the
  standard synchronization mechanism so order/invoice pricing works fully offline.
- **FR-002**: As each order/invoice line is added or its quantity/customer changes, the app MUST
  resolve the line's unit price on-device from the line's product, quantity, the order's customer
  group, and the document's business date — with no network call.
- **FR-003**: The app MUST select exactly one winning rule by the documented deterministic precedence
  (priority, then specificity, then recency) and MUST NOT stack rules by default; when the workspace
  has opted into bounded stacking, it MUST apply the additional promo in the same fixed, deterministic
  order the server uses.
- **FR-004**: The on-device resolved price MUST equal the server's resolved price for the same line
  inputs and rule set, including identical currency rounding.
- **FR-005**: The app MUST evaluate seasonal/effective windows in the workspace business timezone, not
  the device timezone.
- **FR-006**: When a line is saved, the app MUST snapshot the resolved unit price, the winning rule
  (if any), and the price source (rule / catalog fallback / manual) onto the line so later rule
  changes never re-price a saved document.
- **FR-007**: The app MUST let the user manually override a line price; a manual override takes
  precedence over any rule and is recorded as a manual price source.
- **FR-008**: When no rule matches, the app MUST fall back to the product's catalog price and record
  the fallback.
- **FR-009**: The app MUST display each line's applied price and an indication of which rule (or
  fallback/manual) produced it, with all amounts shown in the workspace business currency.
- **FR-010**: The app MUST treat the server as authoritative on the rule set: on sync, the server's
  re-validated price is reflected back without blocking the original offline save.

**Replenishment on the device (P2)**

- **FR-011**: The app MUST sync reorder suggestions down and present a reorder list showing, per item,
  the reorder point, suggested order quantity, and the demand/lead-time assumptions behind them,
  viewable offline from the last sync.
- **FR-012**: The app MUST clearly flag items the backend marks as having insufficient demand data,
  rather than showing a fabricated reorder point.
- **FR-013**: Users MUST be able to select one or more reorder suggestions and create a draft purchase
  that carries a generated number and is reviewable/editable before commit.
- **FR-014**: Users MUST be able to accept a suggested reorder level onto an item; the inventory
  record remains the system of record and MUST NOT change except through an explicit accept.
- **FR-015**: The reorder list and draft-purchase creation MUST follow the app's offline-first model
  (local-first writes, background sync) so actions taken offline are queued and synced on reconnect.

**Cross-cutting**

- **FR-016**: All pricing rules, reorder suggestions, and draft purchases MUST be isolated per
  workspace; switching workspace MUST never show the previous workspace's data.
- **FR-017**: Pricing rules and reorder suggestions MUST be answerable through the app's on-device
  natural-language assistant query path (P3).
- **FR-018**: All new user-visible text, money, and dates MUST be localized to the workspace business
  locale (currency, timezone, formats), consistent with the rest of the app.

### Key Entities *(include if feature involves data)*

- **Pricing Rule (local mirror)**: A workspace-scoped, effective-dated, prioritized rule synced from
  the backend — scope (product/category/all) + reference, optional customer group, optional quantity
  band, optional effective window, adjustment type (percent off / amount off / fixed price) and value,
  priority, active flag. Read on-device to resolve line prices.
- **Price Resolution (per line)**: The on-device result for a line — resolved unit price, currency,
  winning rule (if any), and price source (rule / catalog fallback / manual) — snapshotted onto the
  order/invoice line.
- **Reorder Suggestion (local mirror)**: Per-item recommendation synced from the backend — average
  daily demand, demand variability, lead time, safety stock, reorder point, suggested order quantity,
  status (incl. insufficient-data), and generated-at time.
- **Draft Purchase**: A numbered, editable purchase draft built on the device from selected reorder
  suggestions, one line per item at its suggested quantity.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: With the device offline, a salesperson can price and save a complete multi-line order
  using rule-based prices, with no step blocked on connectivity.
- **SC-002**: For every line, the price shown on the device equals the price the server confirms for
  the same inputs and rule set in 100% of cases, including at currency-rounding boundaries.
- **SC-003**: A saved order's recorded line prices remain unchanged after any later edit, deactivation,
  or deletion of the rules that originally applied — verified in 100% of reopened/reprinted documents.
- **SC-004**: On-device price resolution for a line is perceived as instant (no visible spinner or
  delay) while building an order.
- **SC-005**: A reorder list synced before going offline remains fully viewable offline, with each
  item's reorder point, suggested quantity, and assumptions visible.
- **SC-006**: A user can turn selected reorder suggestions into a numbered, editable draft purchase in
  under a minute.
- **SC-007**: Accepting or ignoring a reorder suggestion changes an item's reorder level only through
  an explicit accept action (no silent overwrites), verified in 100% of cases.
- **SC-008**: After a workspace switch, no pricing rule or reorder suggestion from the previous
  workspace is ever shown.
- **SC-009**: The in-app assistant answers "which items need reordering" and "what is the {group}
  price for {item}" with answers matching the pricing/reorder screens.

## Assumptions

- The backend feature (`ampairs/specs/027-dynamic-pricing-replenishment`) owns the authoritative rule
  set, the replenishment math (safety stock / reorder point / order quantity), and price
  re-validation; the app consumes rules and suggestions and evaluates prices on-device.
- The on-device price evaluation follows the exact same precedence, as-of-business-date, and rounding
  rules as the backend so results agree; money uses exact (non-floating) arithmetic with a single
  rounding step to the workspace currency's minor unit.
- Pricing rules and reorder suggestions ride the app's existing offline-first synchronization and
  workspace-isolation mechanisms; draft purchases use the app's standard local-first write + sync flow
  and the existing numbering scheme.
- The order/invoice line gains snapshot fields for the applied rule and price source; the existing
  manual discount becomes the "manual" price source and continues to win over rules.
- The natural-language path reuses the app's existing on-device assistant query mechanism and requires
  a chat model loaded on the device for free-form questions; it is a convenience layer (P3).
- A web/desktop admin UI for *authoring* rules is out of scope for the mobile app; the app targets
  pricing application and replenishment viewing/initiation.

## Dependencies

- **Backend Dynamic Pricing & Replenishment** (`ampairs/specs/027-dynamic-pricing-replenishment`) —
  authoritative rules, replenishment computation, price re-validation, and the sync endpoints.
- **Order / Invoice (app)** — host the priced line and its snapshot.
- **Inventory (app)** — owns the committed reorder level the suggestions inform.
- **Customer / Product (app)** — supply customer groups and catalog prices/categories used in
  resolution.
- **Workspace settings, sync, and the on-device assistant (app)** — reused infrastructure for policy,
  offline rule/suggestion availability, workspace isolation, and natural-language queries.
