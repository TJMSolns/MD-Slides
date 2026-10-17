# mdslides — Lessons Learned

---

## LL-004 — 2026-05-18 — Pre-scaffold gate with sequence diagrams prevents first-run regressions

**Finding:** MCP server implementation (MS-012, HL-006): sequence diagram + wire format reference written before implementation. Result: 11 integration tests all passed on first run — atypically clean. Contrast with earlier items that hit first-run failures requiring a second session. Pre-scaffold gate with diagrams is worth the upfront cost.
**Scope:** mdslides
**Action taken:** Informational — practice is now standard per POL-002. Evidence of gate value preserved here for justification.

---

## LL-003 — 2026-05-18 — Parser-renderer slot contract mismatch causes silent feature disappearance

**Finding:** MS-005 (HL-002): heading slot was extracted by the parser but the renderer looked for a different slot name. No error — the heading simply didn't render. The contract between parser and renderer was implicit (String-based slot names). A typed slot name enum/sealed trait would make this a compile error rather than a silent runtime omission. This is a structural design gap, not a one-off bug.
**Scope:** mdslides
**Action taken:** WQ item MS-017 queued: introduce typed slot name ADT (`sealed trait SlotName`) and enforce via the parser-renderer interface; eliminates the class of silent slot-name mismatch bugs.

---

## LL-002 — 2026-05-18 — CSS scoping for new layout containers requires explicit propagation

**Finding:** The same CSS scoping gap appeared in two separate sessions (HL-002 and HL-005/MS-014): adding a new HTML wrapper class (`column`, `slide-body`) without propagating existing CSS rules to it. Three separate items (initial fix, MS-010 audit, MS-014 complete fix) were needed to fully close one structural gap. Root cause: no "new wrapper class checklist" — when a new container class is added, all existing rules that apply to its siblings or parent must be evaluated for propagation.
**Scope:** mdslides
**Action taken:** MS-010 (CSS audit) executed 2026-05-17. Informational — the checklist is now informal practice. If CSS grows further, consider formalizing as a pre-close gate item for any PR that adds a new layout container class.

---

## LL-001 — 2026-05-18 — Pre-harness green baseline must be established before feature work begins

**Finding:** HL-002: 25 pre-existing test failures (20 domain + 5 infrastructure) entered the harness without a baseline audit. Two subsequent sessions (HL-003, HL-004) were consumed triaging and closing these. The failures were accumulated before the methodology was applied and had no WQ IDs. Pattern: when applying the harness to an existing codebase, the first act must be achieving green + assigning IDs to any red before starting new work. Without this, the queue is working against an unknown debt burden.
**Scope:** mdslides
**Action taken:** Informational — addressed through MS-013, MS-015, MS-016, MS-007 burn-down. Recommendation added to methodology: harness on-boarding gate requires `mill test` green before first WQ item is opened.

---
