---
name: next
description: Pick the next unblocked item from WORK-QUEUE and drive it to completion. Continues until the queue is empty or all items are blocked.
user-invocable: true
tools: Read, Write, Edit, Glob, Grep, Agent
---

Autonomously drive the WORK-QUEUE forward.

## Steps

1. Read `docs/agents/CONTEXT-KERNEL.md` — verify current phase and non-negotiables
2. Read `docs/agents/WORK-QUEUE.md` — identify the next unblocked item:
   - Priority is embedded in each item's description as **P1**/**P2**/**P3**. Scan P1 → P2 → P3.
   - Within each priority, pick the first item in Active, else Queued, whose `Depends On` are all in Done
   - Report which item was selected and why (e.g. "HE-001 selected: P1, depends on HE-000 (Done)")
   - If nothing is unblocked, report the specific blockers and stop
3. Read `docs/agents/HANDOFF-LEDGER.md` (last 3 entries) for recent context

4. **Pre-Implementation Gate** (E1–E4 enrichment build items only — skip for docs/governance/tooling
   items that are self-evidently a single artifact):
   - Check if a design note exists describing the mechanism and its hook/skill/gate integration point
   - If missing: write the design note and pause for review before proceeding
   - If present: proceed normally

5. Execute the work item:
   - For design/rails work: produce the artifact (ADR, note, hook, skill, gate) in the appropriate location
   - For governance: use the `/decide` pattern to record decisions
   - **E2 is built (DN-002):** never self-certify a build item as done — draw a verifier tier by
     running `python3 .claude/hooks/draw-verifier-tier.py <P1|P2|P3> <doer-tier>` (HE-009/RL-001: this
     is the ONLY correct way to draw — do not hand-derive the offset/clamp/stake-weighting yourself),
     spawn the `verifier` agent (fresh context, `model` = the drawn tier) and require its `PASS` in the
     evidence artifact's `Verifier-verdict:` field before the Done edit; a `VETO` blocks it structurally
     via `.claude/hooks/pretooluse-done-gate.py`
   - **Ordering (RL-002/HE-008 — a real VETO was caused by getting this backwards):** write
     `docs/agents/evidence/<ID>.md` FIRST, with every field filled in except `Verifier-verdict:`
     (leave an honest placeholder like `PENDING` there — never write `PASS` before it's real) — THEN
     dispatch the verifier against that artifact + the real work. A verifier dispatched before the
     artifact exists has nothing valid to certify and will (correctly) VETO on that basis alone. Commit
     the PENDING checkpoint if a Stop hook fires mid-wait rather than leaving it uncommitted.
   - **Field formatting (HE-016/HE-018 — the same mistake blocked the Done edit twice this session):**
     every required field (`Invariance-recheck:` especially, since its content is often long) needs
     non-empty text ON ITS OWN LINE, immediately after the colon — a bare `Invariance-recheck:` with
     the real content starting on the NEXT line (e.g. a numbered list) reads as blank to
     `pretooluse-done-gate.py`'s field-value regex and blocks. Put a lead-in clause on the same line,
     then continue with detail below if needed.
   - **Gap 1 closed (HE-011/DN-006):** a written `PASS` is now cross-checked against the session
     transcript's actual `Agent` tool-call record (`subagent_type: "verifier"`, matching `model`, a
     completed result ending in `PASS`) — spawning the verifier for real is what makes the check pass,
     not just writing the field
   - **Model unavailable (HE-012/DN-007):** if the `Agent` call fails because the drawn tier can't be
     invoked in this environment (not a content/logic failure), redraw once via
     `draw-verifier-tier.py <P1|P2|P3> <doer-tier> <excluded-tiers-comma-separated>` — it falls back to
     your own tier (guaranteed callable) if both others are excluded. If THAT redraw itself outputs
     `tier=ESCALATE`, do not retry again — emit `ESCALATE` yourself instead of a verifier PASS and stop
   - **Agent-type unavailable (HE-016/DN-010) — a DIFFERENT axis from model-tier-unavailable above:**
     if `subagent_type: "verifier"` itself fails (e.g. `Agent type 'verifier' not found...`), or a
     spawned verifier reports missing a tool/capability its own current `.claude/agents/verifier.md`
     says it should have — do NOT retry the same `subagent_type` call, and this is NOT a content/logic
     VETO. Instead: `Read` `.claude/agents/verifier.md`'s live content in full, then spawn a
     `general-purpose` `Agent` call whose prompt is that file's full role content pasted inline
     (verbatim), preceded by the exact line `VERIFIER-FALLBACK (DN-010):` as its very first line — this
     marker is REQUIRED, not decoration: `pretooluse-done-gate.py`'s corroboration check only
     recognizes a fallback call as equivalent to a real verifier call when the prompt starts with it.
     Pass the same `model` = the drawn tier, same inputs (item ID, evidence path, commit) verifier.md's
     own "Inputs you receive" section describes. This is still a genuine, independent check — only the
     routing changed, not the independence guarantee.
   - **E4 is built (DN-004):** if the verifier returns `ESCALATE` instead of `PASS`/`VETO`, do NOT
     fix-and-retry — append an entry to `docs/agents/ESCALATIONS.md`, set the item's status to
     `escalated — awaiting Tony`, and STOP the loop entirely (do not proceed to the next queue item;
     do not spawn a second verifier to break the tie)
   - **Async notification (DN-009/HE-015, HE-018):** the moment that `ESCALATE` entry is logged,
     also draft an email via `mcp__claude_ai_Gmail__create_draft` to `tony@tjm.solutions` — subject
     `[harness-evolution] ESCALATE: <short summary>`, body pointing at the exact `ESC-NNN` entry and
     why it needs him specifically (not something a fix could resolve). Draft only, never send — same
     hard rule as the `/handoff` checkpoint.

6. When the item is complete:
   - Verify evidence exists (file path, commit SHA, or decision ID)
   - **Evidence artifact required (DN-001 / GATE-004, HE-001):** create `docs/agents/evidence/<ID>.md`
     with `Commit:`, `Run-count:`, `Invariance-recheck:`, and `Verified-by:` filled in before moving the
     item to Done. A `PreToolUse` hook (`.claude/hooks/pretooluse-done-gate.py`) blocks (exit 2) the
     WORK-QUEUE edit if it's missing or incomplete — this is the enforcement, not this line.
   - Update `docs/agents/WORK-QUEUE.md`: move to Done with evidence
   - Write a HANDOFF-LEDGER entry (use the `/handoff` pattern)

7. Loop to the next unblocked item (go back to step 2).

8. Stop only when:
   - The queue is empty
   - All remaining items are blocked (report which and why)
   - The user says to stop

Report between items: one line — item ID, what was done, what's next.
