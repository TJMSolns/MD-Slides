# mdslides — Escalations Log (E4 / DN-004, propagated from harness-evolution HE-005)

Append-only, mirrors HANDOFF-LEDGER's convention. Each entry records a `verifier` agent's `ESCALATE`
verdict — genuinely context-dependent uncertainty the verifier could not resolve on the artifact's
merits alone. New entries at the top, headed `## ESC-<NNN> — <date> — <item ID>`.

**Every entry MUST have a `**Status:**` field with value `open` or `resolved` — this is the ONLY field
`.claude/hooks/count-escalations.py` counts on.** Do not rely on the presence/absence of a `Resolved:`
note for counting — free-text mentions of resolution status in an entry's own prose are NOT parsed and
will not affect the open count (a real bug the second independent-verifier VETO on HE-004 caught: a
naive whole-file grep both matched this file's own template example below and could be fooled by a
`**Resolved:**`-looking line that wasn't the real status field). `Status:` is the single, unambiguous
source of truth. Update it in place when Tony answers — do not just add a `Resolved:` note.

No auto-triage: every escalation is surfaced, none are pre-filtered by assumed urgency (DN-004 scope
guard — auto-triage is explicitly deferred to a later research parcel, not built speculatively here).

Entry template (this block is fenced and stripped by the counter before parsing — it will never be
mistaken for a real entry, even though it contains example `## ESC-`/`**Status:**` lines):
```markdown
## ESC-<NNN> — <date> — <item ID>

**Status:** open
**Verifier tier:** <tier>
**Question for Tony:** <the exact ESCALATE reason text>
**Evidence artifact:** docs/agents/evidence/<ID>.md
**Resolved:** <date> — <how, once answered — free text, NOT parsed for counting; also flip Status to `resolved`>

---
```

---

_No escalations yet._
