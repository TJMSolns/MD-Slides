---
name: audit
description: Ad-hoc governance review of one or more artifacts against POLs and CONTEXT-KERNEL non-negotiables. Usage: /audit <path> [path2 ...]
user-invocable: true
argument-hint: "<file-path> [additional paths]"
tools: Read, Write, Agent
---

Run an ad-hoc governance review. Argument(s): `$ARGUMENTS` — one or more file paths to review.

## Steps

1. Parse paths from `$ARGUMENTS`. If none provided, ask the user which artifacts to audit.

2. Verify each path exists. If a path doesn't exist, warn and skip it.

3. Read `docs/agents/CONTEXT-KERNEL.md` and `docs/decisions/pol/` to understand the active non-negotiables and policies.

4. Review each artifact against:
   - All active POLs
   - CONTEXT-KERNEL non-negotiables
   - Standard methodology rules (methodology.md)

5. For each **violation** found:
   - Report: artifact path, policy violated, specific finding
   - Ask: "Fix now or log and continue?"
   - If fix now: open the artifact for editing
   - If log: append an LL-NNN entry to `docs/agents/LESSONS-LEARNED.md`

6. For each **warning** (advisory, not a hard violation):
   - Report the concern
   - Ask if they want to create a POL to formalize the constraint

7. If result is PASS:
   - Print "PASS — no violations found"
   - Suggest adding a note to HANDOFF-LEDGER if this was a significant review

## Output Format

```
Governance Audit — <date>
Artifacts reviewed: <list>

VIOLATIONS:
  ❌ <artifact> — <policy violated>: <finding>

WARNINGS:
  ⚠️  <artifact> — <advisory finding>

PASS:
  ✅ <artifact>

Overall: PASS | VIOLATIONS FOUND
```
