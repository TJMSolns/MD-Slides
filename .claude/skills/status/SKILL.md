---
name: status
description: Generate a one-screen status digest — current phase, active work, recent decisions, open blockers.
user-invocable: true
tools: Read, Glob
---

Generate a concise status digest. Read the following files and produce a single structured summary:

1. `docs/agents/CONTEXT-KERNEL.md` — current phase and open decisions
2. `docs/agents/WORK-QUEUE.md` — active, in-progress, and blocked items
3. `docs/agents/HANDOFF-LEDGER.md` — last 3 entries
4. `docs/agents/DECISION-REGISTER.md` — last 3 decisions

## Output Format

```
Status — <project name> — <date>
═══════════════════════════════════════
Phase:    <current phase>

Active Work:
  <WQ-ID> <title> (<owner>) [<status>]
  ...

Blocked:
  <WQ-ID> <title> — <blocker>
  ...

Recent Decisions:
  <DR-ID> <title>
  ...

Open Decisions Needed:
  <description>
  ...

Next recommended action: <single action>
```

Keep it scannable. No prose. One screen maximum.
