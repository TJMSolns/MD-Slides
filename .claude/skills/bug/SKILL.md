---
name: bug
description: Report a bug or UX issue — queue a WORK-QUEUE item and stop. Never diagnose or fix inline.
user-invocable: true
tools: Read, Edit
---

Queue a bug or UX issue. Stop immediately after. Do not diagnose. Do not fix. Do not suggest a fix.

## Usage

`/bug <description>` — state what is broken and what the symptom is.

## Steps

1. Read `docs/agents/WORK-QUEUE.md` — find the highest item ID number in the file
2. Assign next ID using this project's prefix convention
3. Determine priority: P0 crash/data-loss, P1 broken feature, P2 UX friction — default P1
4. Find the Active section. Append to an existing "Reported Bugs" subsection, or create one before the Backlog separator
5. Write one row: `| ID | **P1 BUG** — <description>. Root cause undiagnosed. Diagnose + fix. | Claude | Active | — |`
6. Print exactly:
   ```
   Queued: <ID> — <description>
   Priority: P1 BUG

   STOP. Run /groom to prioritize, /next to execute.
   Do not read source files. Do not diagnose. Session is over.
   ```
7. Stop. Do not open any source files. Do not offer to investigate.

## Anti-patterns (harness violations)

- "Let me take a quick look at the source..."
- "I'll queue it, but first let me check..."
- "The fix is probably in X — want me to look?"
- Any sentence starting with "Let me" after the queue item is written

