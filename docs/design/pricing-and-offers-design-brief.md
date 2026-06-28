# Design Brief & Prompt — Pricing & Offers Module (Ampairs Mobile)

> Paste this whole document into Claude (design/artifact mode) or hand it to a product designer.
> It defines the product context, the domain mental model, the information architecture, and every
> screen + flow to design. Treat the **Domain Mental Model** as ground truth — it mirrors the real
> backend engine.

---

## 0. Your role (read first)

You are designing the **Pricing & Offers** experience for **Ampairs**, a business-management app for
small and mid-size merchants (think: distributors, wholesalers, retailers in India). You are
producing a complete, build-ready UI/UX design: screen layouts, component specs, interaction flows,
empty/loading/error/offline states, and a clickable flow narrative for each journey.

Design for a **merchant admin** (the shop owner or their ops person) who is NOT a pricing engineer.
They think in plain language: "Distributors in Maharashtra pay ₹240 a box, and ₹220 if they buy 50+."
Your job is to make a genuinely powerful, multi-dimensional pricing engine feel that simple.

**Deliver:** high-fidelity screens + flows for every section below, plus a short rationale per screen.

---

## 1. Platform & system constraints (must respect)

- **App:** Kotlin Multiplatform + **Compose Multiplatform**, one shared UI across **Android, iOS,
  Desktop (JVM)** — phone, tablet, and desktop window sizes. Design **adaptive** layouts
  (single-pane on phone → list/detail two-pane on tablet/desktop).
- **Design language:** **Material 3** (Material You). Use M3 components and color tokens
  (`colorScheme.primary`, `surface`, `surfaceVariant`, `onSurface`, etc.). Dynamic color via Material
  Kolor. Support **light and dark** themes. No hardcoded hex; no third-party design language.
- **Currency/locale:** Money renders in the **workspace's business currency** (default ₹ INR, Indian
  digit grouping like ₹9,20,710.50). Never hardcode a currency symbol in a mockup label — show it as a
  themeable token. Dates render in the business timezone/format.
- **Offline-first:** Everything works offline. Every list screen has a **sync state** (synced /
  pending / syncing / failed). Edits save locally and sync in the background — design for "saved, will
  sync" not "saving… spinner blocks me."
- **Navigation:** Global nav drawer / adaptive nav already exists at the app shell. Pricing is one
  top-level module. Form screens do **not** show a back arrow when the global drawer is present —
  rely on the app shell. Use a consistent screen header pattern.
- **Input reality:** Mostly touch, often one-handed, sometimes on a small phone in a noisy shop.
  Big tap targets, minimal typing, smart defaults, dropdowns sourced from real data (never free-text
  where an ID is needed).

---

## 2. Domain mental model (GROUND TRUTH — internalize before designing)

Ampairs prices a product by **resolving** the best matching rule for a given context. There are two
layers: **Price Lists** (what the base price is) and **Offers/Promotions** (discounts on top — new).

### 2.1 Price List
A **named set of prices** that applies to a slice of the market. A price list has:
- **Channel** (required): `RETAIL` (B2C) or `WHOLESALE` (B2B). Every list belongs to one channel.
- **Targeting dimensions** (all optional — the more you set, the narrower the audience):
  - Customer: a specific **Customer**, a **Customer Group** (e.g. "Distributors"), or a **Customer Type**
  - Product taxonomy: a **Brand**, **Category**, or **Product Group**
  - Geography: a **Geo Zone** (a reusable set of pincodes/states)
  - Advanced: **Attribute rules** (e.g. `customer.tier = GOLD`) — lowest priority, power-user feature
- **Currency**, **Priority** (a tiebreaker number, higher wins), **Status** (`DRAFT` / `ACTIVE` /
  `INACTIVE` — only ACTIVE participates), and an optional **active time window** (`startsAt`/`endsAt`).
- A list with **no dimensions set** = the default price for everyone in that channel.

### 2.2 Price List Item (the actual prices)
Inside a price list, each **item** prices one product (or a specific **variant SKU**):
- **Unit price** (base).
- **MOQ** (minimum order quantity — units, optional). Below MOQ the price still shows but is flagged.
- **Quantity tiers** (optional volume breaks): an ordered ladder like
  `1+ → ₹240`, `50+ → ₹220`, `100+ → ₹200`. Tiers must be **strictly ascending, no gaps/overlaps**.

### 2.3 Geo Zone (reusable geography)
A **named geography** referenced by price lists (and later by offers). Membership is any mix of:
- exact **pincodes** (`110001`),
- **pincode ranges** (`400000–400099`),
- **states** (`MAHARASHTRA`).
A delivery pincode/state maps to the first zone that contains it.

### 2.4 How a price is resolved (the engine — design a "Test a Price" tool around this)
Given a context (channel, product, quantity, customer, location, attributes), the engine:
1. Takes all **ACTIVE** lists in that channel, within their time window.
2. Keeps only lists whose **every pinned dimension matches** the context.
3. Ranks survivors by **specificity** (most specific wins):
   `specific Customer > Customer Group > Brand/Category/Product Group > Geo Zone / Customer Type >
   Attribute rules > channel-only default`
   …then by **Priority** (higher), then **most recently updated**.
4. In the winning list, picks the item (variant beats base product), then the **highest tier** whose
   `minQty ≤ quantity`.
5. Flags **below MOQ** if quantity is short.
6. If nothing matches → **Catalog Fallback** (the product's normal selling price).
The result tells you: **effective unit price**, **source** (which list, or "catalog fallback"),
**which tier fired**, and **below-MOQ** yes/no. This explainability is a key UX asset — surface it.

### 2.5 Offers / Promotions (NEW — design from scratch, mirror the pricing model)
Today discounts are only manual per-order percentages. Design a proper **Offers** sub-module that sits
*on top of* resolved prices. Model an offer as:
- **Name, status (DRAFT/ACTIVE/INACTIVE/SCHEDULED/EXPIRED), channel, time window, priority.**
- **Who/where/what it targets** — reuse the same dimensions (customer group/type, brand/category,
  geo zone, attributes) so it feels consistent with price lists.
- **Trigger / conditions:** e.g. "cart ≥ ₹5,000", "buy 10+ of Brand X", "first order", "coupon code".
- **Reward type:** % off, flat amount off, buy-X-get-Y (BOGO), free shipping, free gift, tiered/slab
  discount.
- **Stacking rules:** can it combine with other offers? exclusivity? usage limits (per customer /
  total)? min/max discount caps.
- **Coupon** (optional): a code, redemption count, expiry.
Offers resolve with the same specificity+priority spirit, and the result must be **explainable** ("Why
did this customer get ₹50 off? → Offer 'Diwali 10%' matched, capped at ₹50").

---

## 3. Information architecture & navigation

Pricing & Offers is one module with these sections (design the section nav as M3 tabs on
tablet/desktop, and a segmented control or top tabs on phone):

```
Pricing & Offers
├── Overview            (dashboard: active lists, active offers, quick "test a price")
├── Price Lists         (list → detail → items + tiers)
├── Offers              (list → builder → conditions/rewards → preview)
├── Geo Zones           (list → zone builder)
└── Price Tester        (standalone resolution preview / simulator)
```

Advanced concepts (Attribute rules, Priority, time windows) are **progressive disclosure** — hidden
behind "Advanced" expanders so the 80% case stays one-tap simple.

---

## 4. Screen-by-screen brief (design each of these)

For every screen below, design: the layout, the M3 components used, the primary action, empty state,
loading (skeleton) state, error state, and offline/sync state. Phone first, then the tablet/desktop
two-pane adaptation.

### 4.1 Overview / Dashboard
**Goal:** at-a-glance health of pricing + fastest path to common tasks.
- Summary cards: **# Active Price Lists**, **# Active Offers**, **# Geo Zones**, **Channel split**
  (Retail vs Wholesale).
- A prominent **"Test a Price"** entry (pick product + customer → see resolved price). This is the hero.
- "Needs attention" strip: lists in DRAFT, offers expiring soon, items below MOQ frequently hit,
  tiers with validation issues.
- Recent activity feed (price changes, offers launched).
- Sync status chip in the header.

### 4.2 Price Lists — list screen
**Goal:** scan and manage all price lists.
- Each row: **name**, channel chip (Retail/Wholesale), **status** chip (Draft/Active/Inactive — color
  coded), a compact "targets" summary ("Distributors · Maharashtra · Brand: Acme"), item count,
  priority, and updated time.
- **Filter bar:** by channel, status, dimension. **Search** by name. **Sort** by priority/updated/name.
- FAB / primary button: **New Price List**.
- Row swipe or overflow: Duplicate, Activate/Deactivate, Delete (soft delete + "will sync").
- Empty state: friendly illustration + "Create your first price list" + a one-line explainer of what a
  price list is.
- Tablet/desktop: left list pane + right detail pane.

### 4.3 Price List — detail screen
**Goal:** understand one list and edit its prices.
- Header: name, channel, status toggle, priority, time window (if set).
- **Targets section:** chips showing every dimension this list applies to; tap to edit.
- **Items table:** product (+ variant), base unit price, MOQ, tier badge ("3 tiers"), updated.
  - Tapping a row opens the **Item editor** (4.5).
  - Search/filter within items. Add Item button.
- A **"Test this list"** shortcut that pre-fills the Price Tester with this list's context.
- States: empty (no items yet → "Add the first product"), syncing, conflict/failed-sync banner.

### 4.4 Create / Edit Price List — guided form (the core flow)
**Goal:** make multi-dimensional targeting feel like answering plain questions. Design as a **stepped
form** (wizard on phone, single scrollable form with section cards on desktop).

Step 1 — **Basics:** Name, Channel (segmented Retail/Wholesale), Currency (defaulted), Status
(Draft default).
Step 2 — **Who does this apply to?** (all optional, "Everyone in this channel" if blank):
  - Customer scope: pick one of *All / Customer Group / Customer Type / Specific Customer* (dropdowns
    sourced from real customer data). Show a plain-language preview sentence that updates live:
    "Applies to **Distributors** buying on **Wholesale**."
Step 3 — **Which products?** Brand / Category / Product Group pickers (optional; "All products" if blank).
Step 4 — **Where?** Geo Zone picker (optional) with a "Create new zone" inline shortcut.
Step 5 — **Advanced (collapsed):** Attribute rules builder (4.7), Priority (with helper text "used only
  when two lists match"), Active time window (date/time range, business timezone).
Step 6 — **Review:** the live plain-language summary + "Save as Draft" / "Save & Activate".

Design principles: **live preview sentence** always visible; **smart defaults**; never show raw IDs —
show names. Validation inline. "Saved locally, will sync" confirmation.

### 4.5 Price List Item editor + Tier editor
**Goal:** set the actual numbers, including volume tiers, with zero ambiguity.
- Fields: **Product** (searchable picker), optional **Variant SKU**, **Unit price** (money input with
  currency token), **MOQ** (optional, with helper "minimum units per order").
- **Tier editor** (the showpiece): an ordered list of rows `min qty → unit price`. Design:
  - "Add tier" appends a row; rows auto-sort by min qty.
  - **Live validation:** strictly ascending min qty, no gaps/overlaps, qty > 0. Show inline errors and
    a small **visual ladder/preview** ("1–49 → ₹240, 50–99 → ₹220, 100+ → ₹200").
  - Make it obvious that tier price should generally decrease (volume discount) but don't hard-block it.
- Show a tiny inline "what the customer pays at qty N" tester.

### 4.6 Geo Zones — list + builder
**Goal:** build reusable geographies without GIS knowledge.
- List: zone name, a summary ("12 pincodes · 1 range · 2 states"), used-by count (how many lists/offers
  reference it), updated.
- **Zone builder:**
  - Three input modes in tabs/sections: **Pincodes** (chip input, paste many), **Pincode ranges**
    (from–to pairs), **States** (multi-select from a state list).
  - Live **member count** and a **"test a pincode"** field ("Is 400052 in this zone?" → yes/no).
  - Optional: a simple map/region visualization is a nice-to-have, not required.
- Deletion guard: warn if the zone is referenced by active lists/offers.

### 4.7 Attribute rule builder (advanced, progressive disclosure)
**Goal:** power users add conditions like `customer.tier = GOLD`, `order.attr.region IN [N,S]`.
- Each rule row: **Field** (key, with suggestions), **Operator** (= ≠ in > < ≥ ≤), **Value**.
- Multiple rules = all must pass (AND). Keep it visually lightweight, clearly labeled "Advanced —
  most merchants don't need this."
- Plain-language echo: "Only when customer's **tier** is **GOLD**."

### 4.8 Price Tester / Resolution simulator (the trust-builder)
**Goal:** let the merchant ask "what price would X customer pay for Y product?" and SEE why.
- Inputs: Channel, Product (+variant), Quantity, optional Customer (auto-fills group/type), optional
  Pincode/State, optional attributes.
- Output card:
  - **Effective unit price** (big), line total for the quantity.
  - **Source badge:** which Price List won (tappable → opens it) OR "Catalog fallback."
  - **Tier applied:** "50+ tier" or "base price."
  - **Below MOQ** warning if applicable.
  - **Offer applied** (once offers exist): which offer, discount amount, caps.
  - An **"Explain"** expander showing the ranked candidates and why the winner won (specificity →
    priority → recency). This is the killer feature — design it as a clear, readable trace, not a log.

### 4.9 Offers — list screen (NEW)
- Rows: offer name, **reward summary** ("10% off, max ₹500"), channel, **status** (Draft/Scheduled/
  Active/Expired/Inactive), time window, coupon badge if any, usage ("142/500 used"), priority.
- Filter by status/channel/reward type; search; sort.
- Primary: **New Offer**. Empty state explains offers in one sentence.

### 4.10 Offer builder (NEW — guided, mirror the price-list wizard)
Steps:
1. **Basics:** name, channel, status, priority, time window.
2. **Who/where/what it targets:** reuse the same dimension pickers (customer group/type, brand/
   category, geo zone, attribute rules) for consistency.
3. **Trigger / conditions:** choose condition type(s): cart total ≥ X, quantity of item/brand ≥ N,
   first order, specific products in cart, **coupon code** (with code + redemption limit + expiry).
4. **Reward:** pick reward type — % off / flat off / **buy-X-get-Y** / free shipping / free gift /
   tiered slab discount — with the relevant inputs, plus **caps** (max discount) and **min spend**.
5. **Stacking & limits:** combine with other offers? exclusive? usage limit per customer / total.
6. **Review & preview:** live plain-language sentence ("Spend ₹5,000+ on Wholesale → get 10% off, up
   to ₹500, one per customer, valid Diwali week") + a **mini simulator** showing a sample cart's
   before/after. Save as Draft / Schedule / Activate.

### 4.11 Offer preview / simulator (NEW)
Like the Price Tester but for offers: build a sample cart/context → see which offers fire, in what
order, the discount math, caps hit, and final price. Reuse the **"Explain"** trace pattern.

---

## 5. End-to-end flows to storyboard (show the click-path)

1. **Create a wholesale distributor price list with volume tiers** → from Price Lists → New → wizard →
   add item + 3 tiers → Save & Activate → confirmation → appears in list as Active.
2. **"Why is this customer paying this price?"** → Price Tester → fill context → read source + tier +
   Explain trace → tap the winning list to edit.
3. **Set up a Diwali coupon offer** → Offers → New → conditions (coupon + cart ≥ ₹5,000) → reward (10%
   off, max ₹500) → limits → preview a sample cart → Schedule for the festival window.
4. **Build and reuse a Geo Zone** → create "Mumbai Metro" zone (range + state) → reference it from a
   price list's "Where?" step without leaving the flow.
5. **Edit a price while offline** → change a tier on a phone with no network → "Saved · pending sync"
   → reconnect → syncs → status chip clears. Show the conflict/failed-sync edge case too.

---

## 6. Cross-cutting states & components to design

- **Sync state system:** chip + per-row indicator for Synced / Pending push / Syncing / Failed, plus a
  "pending changes" banner. Consistent across all list/detail screens.
- **Status chips:** Draft / Active / Inactive / Scheduled / Expired — color-coded, accessible contrast.
- **Channel chips:** Retail vs Wholesale, visually distinct.
- **Money input** with currency token + correct grouping; **quantity input**; **date-range** picker in
  business timezone.
- **Dimension picker** (reused everywhere): searchable, single-select, sourced from real data, shows
  name not ID, with "All / None" default.
- **Plain-language preview sentence** component (live-updating) — used in every builder.
- **Tier ladder** visual; **Explain trace** component; **empty / skeleton / error** templates.
- **Below-MOQ** and **validation-error** inline patterns.

## 7. Tone, accessibility, and quality bar

- Plain merchant language, not engineering jargon ("Who gets this price?" not "targeting predicates").
- Progressive disclosure: simple by default, powerful when expanded.
- Accessibility: content descriptions on all icon buttons, ≥ 4.5:1 text contrast, large tap targets,
  full keyboard/focus order on desktop, dynamic type friendly.
- Every destructive action confirms and explains sync impact.
- Light + dark, phone + tablet + desktop, for **every** screen.

## 8. What to hand back

For each screen: a high-fidelity mockup (light + dark), the adaptive variant, a one-paragraph
rationale, and the interaction notes (primary action, states, edge cases). For each of the 5 flows: an
ordered storyboard of frames. Plus a small component sheet for the cross-cutting components in §6.
