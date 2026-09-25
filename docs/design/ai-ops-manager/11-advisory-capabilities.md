# 11 — Advisory / completeness capabilities (a second capability kind)

**Status:** implemented (first advisory: `customer.contact`). **Purpose:** everything so far has been a
*normalizer* — it detects a wrong value and proposes a safe, reversible rewrite. Many real data-quality
issues have **no deterministic fix**: a customer with neither email nor phone, an invoice with no line
items, a product with no price. For these the assistant should *flag for review*, never change data. This
adds that second capability kind — a **detect-only advisory** — to the engine, reusing the existing gate
and review/audit machinery with **no engine changes**.

## How it works (no engine change)

The `ConfidenceRiskGate` only marks a candidate auto-eligible when it is `HIGH ∧ LOW-risk ∧ reversible ∧
level ≥ AUTO_CORRECT`, where *reversible* means `candidate.before != null`. An advisory exploits this: it
proposes a **non-reversible NO_OP** candidate (`before = null`, `action = NO_OP`), so the gate can never
auto-fix it — it always routes to **SUGGEST** (a review item), or **OBSERVE** at the Observe autonomy
level. The finding lands in `aiops_finding` as `PENDING_REVIEW` and shows up in the AI Activity feed's
Suggestions section, exactly like a normalizer suggestion.

`data/common/.../aiops/AdvisoryCapability.kt` is the shared base (sibling of `FieldNormalizationCapability`).
It owns the fixed `propose` (the non-reversible NO_OP), `validate` (accepts that shape), and `score`
(a deterministic flag is HIGH-confidence), plus an `advisory(entityId, summary, …)` helper that builds a
stable-id finding. A concrete advisory supplies only its metadata and the DAO-bound `detect`/`gather`.

**No executor.** Advisories are never applied, so they have no `AiOpsExecutor`. In the review UI that
makes **Accept a safe no-op** — `AiOpsReviewService.accept` returns `false` when no executor is keyed to
the capability (it already did this), so nothing happens and the finding stays pending; the user acts on
the flag manually and **Dismiss**es it. (Follow-up: the Suggestions row could hide "Accept" for
non-fixable findings once the suggestion item carries a "fixable" flag — not needed for correctness.)

## First advisory — `customer.contact`

`feature/customer/.../aiops/CustomerContactCapability.kt` (`@CapabilityKey("customer.contact")`, on the
advisory base) flags active customers with **neither email nor phone** — a customer you can't reach.
`detect` reads active customers via `CustomerDao` and maps each unreachable one through `advisory(...)`.
It's live both ways already (Scan + the customer-save hook), with zero engine/DI/nav edits — the Metro
multibindings pick up the new `@CapabilityKey` contribution automatically.

## Tests

`CustomerContactCapabilityTest` (stage test on the in-memory `FakeCustomerDao`) covers detection (flags a
customer with neither email nor phone, incl. blank strings; skips ones with either, and inactive rows) and
pins the advisory contract from the base: the proposed candidate is a **non-reversible NO_OP** (which is
what forces the gate to review, never auto-fix), `validate` accepts that shape and rejects a field-write
candidate, and `score` is HIGH. The gate routing itself (non-reversible ⇒ SUGGEST) is covered generically
by `ConfidenceRiskGateTest`.
