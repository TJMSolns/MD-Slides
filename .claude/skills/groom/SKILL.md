---
name: groom
description: Review and groom the WORK-QUEUE — reorder by priority and dependencies, split oversized items, flag stale or blocked items, suggest new items from open questions.
user-invocable: true
tools: Read, Write, Edit, Glob, Grep
---

Groom the WORK-QUEUE. Read the following and produce a grooming report + updated queue:

1. `docs/agents/WORK-QUEUE.md` — current state
2. `docs/agents/CONTEXT-KERNEL.md` — priorities, open decisions, release targets
3. `docs/agents/HANDOFF-LEDGER.md` — last 5 entries for context on recent direction

## Grooming Actions

- **Reorder:** Move items to reflect current priorities and unblock dependencies
- **Split:** Break oversized items (>1 sprint of work) into smaller slices
- **Promote:** Move Queued items to Active if their dependencies are met
- **Flag:** Mark stale or no-longer-relevant items for removal (ask before deleting)
- **Add:** Propose new items from context or recent decisions that imply work
- **Assign:** Suggest owners based on the work type

## Output

1. A brief grooming summary (what changed and why)
2. Apply the changes directly to `docs/agents/WORK-QUEUE.md`
3. If new items are added, update `docs/agents/CONTEXT-KERNEL.md` active priorities if relevant

Do not add speculative future work. Only groom what is real and near-term.
