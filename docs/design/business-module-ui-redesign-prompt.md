# Design Prompt — Ampairs Business Module UI Redesign

> Copy everything below the line into a fresh Claude session (claude.ai with artifacts
> enabled works best — ask for interactive HTML mockups first, Compose code second).

---

You are a senior product designer specializing in business/SaaS tools and Material 3.
Design a complete UI overhaul for the **Business module** of Ampairs, a business
management app (think: a lightweight ERP for small businesses — customers, products,
orders, invoices, inventory). Produce visual mockups first, then a build-ready spec.

## 1. Product context

- The Business module is where an owner sets up and maintains their **company identity
  and operating configuration**. It is visited rarely but matters a lot: it's the first
  thing a new user fills in (onboarding) and the data feeds invoices, orders, and tax
  documents (legal name, address, currency, timezone appear on printed documents).
- Users: small-business owners and their staff in India and similar markets. Not
  designers, not patient. They use mid-range Android phones, and increasingly the
  Desktop (Windows/Mac) app for back-office work. iPad/tablet is secondary.
- Two distinct moments to design for:
  1. **First-run setup** — no business profile exists yet; the user must create one.
     Today this is a sad empty-state card with a "Create Business Profile" button.
  2. **Occasional maintenance** — change the logo, fix the address, adjust business
     hours, add a custom field value. Quick in-and-out edits.

## 2. Current screens (what exists today — redesign all of them)

1. **Business Overview** — entry hub. An outlined card with the business name, type,
   email, phone, address, currency/timezone as plain `label: value` text lines, an
   80dp logo thumbnail on the right, then a "Quick Actions" heading with 2–4
   navigation cards (Profile & Registration, Operations, Logo & Gallery, Custom
   Attributes).
2. **Profile & Registration** — long single-column form: company name, business type
   (dropdown), description, owner name/email/phone, address (street, city, state,
   postal code, country), website. Saved via a floating action button.
3. **Operational Settings** — timezone (dropdown), currency (dropdown), language,
   date/time format, business hours (open/close per day), operating days. Single
   column, FAB to save.
4. **Custom Attributes** — server-configured dynamic fields (text/number/dropdown
   widgets rendered from a form schema). FAB to save.
5. **Logo & Gallery** — circular logo with upload/delete, divider, gallery grid of
   business photos with add/edit/set-primary/delete via dialogs.

Navigation: these are five separate full-screen destinations reached from the
overview's quick-action cards. There is a global navigation drawer; screens have no
back-arrow top bars of their own.

## 3. Honest critique of the current UI (fix these)

- **No visual hierarchy or personality.** Every screen is a `headlineMedium` title
  over a flat stack of outlined cards/text fields, 16dp spacing throughout. The
  overview's business card is `label: value` plain text — it reads like a debug dump,
  not a business identity. The logo is an afterthought.
- **The overview wastes its job.** It should make the business feel *represented*
  (identity header: logo, name, type chip, key contact actions) and surface *setup
  completeness* ("Business hours not set", "No logo uploaded") — instead it
  duplicates raw fields and offers undifferentiated navigation cards.
- **Forms are undifferentiated walls.** Profile mixes company / owner / address /
  contact with no section grouping, icons, or progressive structure. Save is a FAB
  that covers content and gives no dirty-state feedback (no "unsaved changes", no
  disabled-until-valid, no inline validation styling).
- **Desktop is phone-UI-stretched-less.** We capped content at 720–920dp and
  centered it; that's not desktop design. No two-pane layouts, no inline section
  navigation, no use of the extra width (e.g., form sections as a left rail,
  detail on the right).
- **Business hours UX is primitive** — per-day dropdowns; no "same every day"
  shortcut, no visual week strip, no closed-day toggle pattern.
- **Empty/error states are bare** (text in an error-container card).
- Hardcoded strings, default Material shapes everywhere, no imagery/illustration,
  no motion.

## 4. Design requirements

- **Material 3 only** (Compose Multiplatform implementation later — but design
  first, don't constrain to what's easy). Use M3 color roles/tokens, not hex values;
  must work in light and dark.
- **Adaptive, with real layout changes per size class:**
  - Compact (phone): single column, comfortable touch targets, bottom-aligned
    primary actions.
  - Medium (tablet/small window): two-column where it earns its keep.
  - Expanded (desktop): consider a section rail + content pane for settings
    (list-detail), persistent save bar instead of FAB, denser rows, hover states.
- **Redesign the information architecture if it helps.** You may merge the five
  screens into one settings hub with sections, or keep them separate — justify the
  choice. Consider M3 list-detail pane patterns for desktop.
- **Design the first-run flow** as a short guided setup (what's the minimum to get
  started? name, type, currency/timezone — defer the rest), not a giant empty form.
- **Forms:** grouped sections with section headers/icons, inline validation, dirty
  state with explicit Save/Discard affordance, field-level helper text. Show what a
  good business-hours editor looks like.
- **Overview:** identity-first header (logo, name, type, completeness), actionable
  setup checklist for missing data, then settings entry points with current-value
  previews (e.g., "Operations — INR · Asia/Kolkata · Mon–Sat 9:00–18:00").
- **Images:** modern media management — drag-drop on desktop, clear primary-image
  affordance, skeleton loading.
- Accessibility: 4.5:1 contrast, 48dp touch targets, visible focus for keyboard
  users on desktop.

## 5. Constraints (hard)

- Material 3 component vocabulary (custom compositions of M3 parts are fine; no
  foreign design systems).
- Offline-first: data may be local-only/syncing — design a subtle sync status
  affordance, never a blocking spinner over stale-but-usable data.
- No bottom navigation (module navigation lives in a global drawer/rail).
- All text must be resource-keyed (design with realistic copy; flag strings).
- The dynamic Custom Attributes screen renders unknown server-defined fields —
  design the *container and field patterns*, not specific fields.

## 6. Deliverables (in this order)

1. **IA decision** — one paragraph + diagram: keep five screens vs. settings hub
   vs. list-detail; per size class.
2. **Interactive HTML mockups** (one artifact per screen, mobile and desktop
   variants) for: Overview, Profile form (with one section in error + dirty state),
   Operations (including the business-hours editor), Images. Use M3 styling.
3. **First-run setup flow** mockup (2–3 steps).
4. **Build spec for Compose Multiplatform**: component tree per screen, which M3
   components map to each region, breakpoint behavior (compact/medium/expanded),
   spacing/typography scale used, and state list per screen (loading / empty /
   error / dirty / saving / synced).
5. **A reusable pattern sheet**: section header, settings row with value preview,
   form section card, save bar, sync chip — so other modules (customer, product)
   can adopt the same language.

Ask me at most 3 clarifying questions before starting if anything is genuinely
ambiguous; otherwise proceed with stated assumptions.
