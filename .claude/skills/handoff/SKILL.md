---
name: handoff
description: Write an end-of-session HANDOFF-LEDGER entry, update WORK-QUEUE statuses, and close the session cleanly. Run at the end of every working session.
user-invocable: true
tools: Read, Write, Edit, Glob, Grep
---

Close this session cleanly by writing a handoff entry.

## Steps

1. Read `docs/agents/WORK-QUEUE.md` — note what was in_progress or completed this session
2. Read `docs/agents/DECISION-REGISTER.md` — note any decisions made this session
3. Review recent conversation context to summarize what happened

4. **Harvest check:** Are there patterns, lessons, or decisions from this session worth generalizing to other projects or org methodology? If yes, note them as `**Harvest candidates:**` in the ledger entry. If no, omit.

4b. **Harvest gate (blocking — POL-010):** Before writing the HL entry, resolve every candidate named in step 4:

   - **Already has a WQ item:** Record the WQ item ID in the HL entry — no action needed.
   - **No WQ item:** Choose one before closing:
     - **(a) Queue inline:** Add a concrete WQ item to `docs/agents/WORK-QUEUE.md` now; add the new ID to the HL entry.
     - **(b) Defer explicitly:** Add to the HL entry body:
       ```
       **Harvest deferred:** <candidate name> — <reason> — revisit by <date, within 30 days>
       ```

   **Do not write the HL entry until every named harvest candidate has either a WQ item ID or an explicit deferral.** (POL-010: 7-day SLA from HL entry date)

   Separately: scan the last 3 HANDOFF-LEDGER entries for older `**Harvest candidates:**` lines without visible resolution. If any are found, print:
   ```
   ⚠  Unresolved harvest candidates in prior sessions — queue or defer per POL-010 (7-day SLA).
   ```

5. **Update WORK-QUEUE:**
   - Set completed items to `done` with evidence (file path or decision ID)
   - Leave in_progress items as-is if genuinely still in progress
   - Add any newly discovered items
   - **Evidence-artifact gate (propagated from harness-evolution HE-005, DN-001/DN-002):** before
     moving any item into `## Done`, create `docs/agents/evidence/<ID>.md` with `Commit:`,
     `Run-count:`, `Invariance-recheck:`, `Verified-by:`, `Verifier-tier:`, and `Verifier-verdict:`
     (PASS required) filled in. A `PreToolUse` hook (`.claude/hooks/pretooluse-done-gate.py`) blocks
     the WORK-QUEUE edit (exit 2) if it's missing or incomplete, or if the verifier verdict is a VETO
     or ESCALATE rather than PASS — spawn the `verifier` agent (fresh context, tier drawn per DN-002's
     ±1 clamped/stake-weighted coin) before marking done; never self-certify.

5a. **CONTEXT-KERNEL substantive-edit gate (BLOCKING — POL-013 / WQ-P4-109)**:
   ```bash
   git diff HEAD -- docs/agents/CONTEXT-KERNEL.md 2>/dev/null
   ```
   If the diff is non-empty:
   - **Substantive change** (Current Phase, Status, dormancy decision, non-negotiables, decisions, charter) → drafted HL MUST include `**CONTEXT-KERNEL change:** <section> — <reason or DR-ID>`.
   - **Cosmetic only** → include `**CONTEXT-KERNEL change:** cosmetic — <one-line>` to satisfy explicitly.
   Source: POL-013, RL-009 F3.

5b. **Git audit gate (BLOCKING — WQ-P4-103)** — applies to this project's repo.

   **(a) Working tree clean check:**
   ```bash
   git status --short
   ```
   If output is non-empty, do **not** write the HL entry. Resolve first:
     - **Commit** the file(s) and reference the new hash in the HL entry; or
     - **Stash** with `git stash push -m "pre-handoff stash"`; or
     - **Explicit defer** — add to the HL body:
       ```
       **Working-tree carry-over:** <file path> — <reason> — revisit by <date>
       ```

   **(b) Commit-hash existence check:** grep the drafted HL body for commit hashes (regex `[0-9a-f]{7,40}` appearing after "commit" / "Commit" / "@"). For each hash:
   ```bash
   git cat-file -e <hash> 2>/dev/null && echo OK || echo MISSING
   ```
   If any MISSING in this repo, the HL entry references a ghost commit. Correct or remove the hash before writing.

   Source: HL-093 ghost-commit pattern; org POL via TJMSolns WQ-P4-103.

   **Now also structurally enforced (propagated from harness-evolution HE-005, DN-003/DN-005):** a
   blocking `Stop` hook (`.claude/hooks/stop-git-durability-gate.py`) sweeps every repo touched this
   session (via the transcript, not just CWD) at session end and blocks the stop if any is dirty or
   unpushed — this step's manual check is context for why, not the sole enforcement. A pre-existing,
   deliberately-deferred backlog can be carved out via `docs/agents/GIT-DURABILITY-DEFER.md` (see that
   file for mdslides' current entry — the known 54-file backlog, expiring 2026-07-18) rather than
   silently blocking on debt this session isn't responsible for.

6. **Write HANDOFF-LEDGER entry** at the TOP of `docs/agents/HANDOFF-LEDGER.md`:
   ```markdown
   ## HL-<next-id> — <date> — <session summary in 5 words>

   **Session:** <who worked, what mode>
   **What happened:**
   - <bullet: artifact created or decision made, with file path>
   - ...

   **Decisions made:** <DR-IDs or "none">
   **Open items carried forward:**
   - <description>
   **Next owner:** <person or "any"> — <what they should do next>

   ---
   ```

7. Confirm the entry was written. Print the "Next owner" line.
