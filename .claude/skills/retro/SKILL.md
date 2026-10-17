---
name: retro
description: Run a structured retrospective. Reviews recent HANDOFF-LEDGER entries, prompts for findings, and produces POLs, LESSONS-LEARNED entries, or CONTEXT-KERNEL updates as appropriate.
user-invocable: true
tools: Read, Write, Edit, Glob, Agent
---

Run a structured retrospective.

## Steps

### 1. Load context

Read:
- `docs/agents/HANDOFF-LEDGER.md` — all entries since the last retro
- `docs/agents/LESSONS-LEARNED.md` — existing entries (to avoid duplication; create if missing)
- `docs/agents/CONTEXT-KERNEL.md` — current non-negotiables (to check if any need updating)
- `docs/decisions/pol/` — existing policies (to check for gaps)

### 2. Summarize the period

Present to the user:
- Number of WQ items completed since last retro
- Any open decisions that are still unresolved
- Any recurring patterns or themes in recent handoffs

### 3. Ask structured questions (one at a time, wait for response)

**Q1 — Process:** Were there any friction points in how we worked? Anything that slowed things down or caused rework?

**Q2 — Architecture:** Any design decisions from this period you'd make differently now? Any non-negotiable that turned out to be wrong or too restrictive?

**Q3 — Governance gaps:** Were there situations where we lacked a POL and had to make ad-hoc calls?

**Q4 — Tooling:** Any improvements needed to agents, skills, or hooks?

### 4. For each finding, produce the right artifact

| Finding type | Action | WQ item required? |
|---|---|---|
| New standing rule | Use `/decide pol <title>` pattern | Yes — queue item to enforce/implement the rule |
| ADR that needs updating | Flag for review — do not auto-edit | Yes — queue item for Tony to review and update |
| CONTEXT-KERNEL non-negotiable change | Propose the edit; do not write until confirmed | Yes — queue item once confirmed |
| Process improvement | Write to LESSONS-LEARNED | Yes — queue item to implement the improvement |
| Tooling improvement | Write to LESSONS-LEARNED | Yes — queue item to make the change |
| Informational | Write to LESSONS-LEARNED | No |

### 4b. Close the loop — queue WQ items (mandatory)

For every finding that requires a WQ item (see table above):
1. Open `docs/agents/WORK-QUEUE.md`
2. Add a concrete WQ item — one item per finding, specific enough to be executable
3. Record the WQ item ID in the LL entry (see step 5 format)

**A retro that produces actionable findings without queued WQ items has not closed the loop.** An LL entry without a WQ item ID is a dead end — the finding will never be executed.

Exception: If the action was completed during the retro itself (e.g., a POL was written inline, a CONTEXT-KERNEL edit was applied), no separate WQ item is needed — note "executed inline" in the LL entry.

### 5. Append to LESSONS-LEARNED

For each finding worth preserving, append an LL-NNN entry to `docs/agents/LESSONS-LEARNED.md`:
```markdown
## LL-<NNN> — <date> — <short title>
**Finding:** <what was learned>
**Action taken:** <POL created / WQ item <ID> queued / executed inline / no action (informational)>
```

### 6. Append retro summary to HANDOFF-LEDGER

```markdown
## HL-NNN — Retrospective <date>
**Items reviewed:** <WQ IDs or "org-level">
**Findings:** <N> (<N with action>, <N informational>)
**WQ items queued:** <N> (list IDs)
**Artifacts produced:** <list>
**Next retro:** <suggested date>
```

### 7. Report

Print a brief summary: period covered, finding count, WQ items queued, artifacts produced, any open decisions that remain unresolved.
