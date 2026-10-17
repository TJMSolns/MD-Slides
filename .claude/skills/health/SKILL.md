---
name: health
description: Audit methodology infrastructure and produce a traffic-light health report.
user-invocable: true
tools: Read, Glob, Grep, Bash
---

Audit methodology infrastructure and produce a traffic-light health report.

## Steps

1. Run the following checks using Bash or Read. Report ✅ PASS, ⚠️ WARN, or ❌ FAIL for each.

   **a. Core artifact files exist**
   ```bash
   for f in docs/agents/CONTEXT-KERNEL.md docs/agents/WORK-QUEUE.md \
             docs/agents/HANDOFF-LEDGER.md docs/agents/DECISION-REGISTER.md; do
     [ -f "$f" ] && echo "✅ $f" || echo "❌ $f missing"
   done
   ```

   **b. Hooks wired in settings.json**
   ```bash
   grep -q "SessionStart" .claude/settings.json 2>/dev/null \
     && echo "✅ SessionStart hook wired" \
     || echo "❌ SessionStart hook missing in .claude/settings.json"
   grep -q "Stop" .claude/settings.json 2>/dev/null \
     && echo "✅ Stop hook wired" \
     || echo "⚠️  Stop hook not wired (handoff reminder inactive)"
   ```

   **c. Hook scripts executable**
   ```bash
   for h in .claude/hooks/session-start.sh .claude/hooks/session-stop.sh; do
     [ -x "$h" ] && echo "✅ $h executable" || echo "❌ $h not executable or missing"
   done
   ```

   **d. Skills present**
   ```bash
   for s in next status handoff decide groom health retro audit; do
     [ -f ".claude/skills/$s/SKILL.md" ] && echo "✅ /$s" || echo "❌ /$s missing"
   done
   ```

   **e. Stale in_progress items**
   ```bash
   COUNT=$(grep -c "in_progress" docs/agents/WORK-QUEUE.md 2>/dev/null || echo 0)
   [ "$COUNT" -gt 0 ] \
     && echo "⚠️  $COUNT in_progress item(s) — review if stale" \
     || echo "✅ No in_progress items"
   ```

   **f. Retro cadence**
   ```bash
   HL_COUNT=$(grep -c "^## HL-" docs/agents/HANDOFF-LEDGER.md 2>/dev/null || echo 0)
   RETRO_COUNT=$(grep -c "retro\|Retrospective" docs/agents/HANDOFF-LEDGER.md 2>/dev/null || echo 0)
   echo "Handoff entries: $HL_COUNT | Retro mentions: $RETRO_COUNT"
   [ "$HL_COUNT" -gt 10 ] && [ "$RETRO_COUNT" -eq 0 ] \
     && echo "⚠️  /retro overdue" \
     || echo "✅ Retro cadence OK"
   ```

   **g. Governance coverage**
   ```bash
   POL=$(ls docs/decisions/pol/*.md 2>/dev/null | wc -l | tr -d ' ')
   ADR=$(ls docs/decisions/adr/*.md 2>/dev/null | wc -l | tr -d ' ')
   echo "ADRs: $ADR | POLs: $POL"
   [ "$POL" -lt 2 ] \
     && echo "⚠️  Few formal POLs — non-negotiables may be informal" \
     || echo "✅ Governance coverage adequate"
   ```

2. Produce the summary table:

```
Methodology Health — <date>
═══════════════════════════════════════════════════════════════

┌──────────────────────────────────┬──────────┐
│ Check                            │ Status   │
├──────────────────────────────────┼──────────┤
│ a. Core artifact files           │  ✅/❌   │
│ b. Hooks wired                   │  ✅/❌   │
│ c. Hook scripts executable       │  ✅/❌   │
│ d. Skills present                │  ✅/❌   │
│ e. Stale in_progress items       │  ✅/⚠️   │
│ f. Retro cadence                 │  ✅/⚠️   │
│ g. Governance coverage           │  ✅/⚠️   │
└──────────────────────────────────┴──────────┘

Overall: ✅ HEALTHY  |  ⚠️ NEEDS ATTENTION  |  ❌ ACTION REQUIRED
```

3. For any ❌ item: state the specific file to fix and what to change.
4. If overall is ❌ ACTION REQUIRED, recommend resolving before the next `/next` execution.
