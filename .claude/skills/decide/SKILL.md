---
name: decide
description: Record a durable decision. Writes to docs/decisions/ (adr/pdr/pol) and appends to DECISION-REGISTER.md. Usage: /decide adr|pol|pdr <short-title>
user-invocable: true
argument-hint: "adr|pol|pdr <short-title>"
tools: Read, Write, Edit, Glob
---

Record a durable decision. Arguments: `$ARGUMENTS` (format: `<type> <title>`)

## Steps

1. Parse type (`adr`, `pol`, or `pdr`) and title from `$ARGUMENTS`.
   - If missing, ask the user for type and title before continuing.

2. Find the next sequential ID by reading `docs/decisions/<type>/ (lowercase: adr, pdr, pol)`:
   - ADR-NNN, POL-NNN, or PDR-NNN (zero-padded to 3 digits)

3. Ask the user (or infer from context) for:
   - **Decision:** What was decided
   - **Context/Problem:** Why this decision was needed
   - **Rationale:** Why this option over alternatives
   - **Consequences:** What this enables or constrains
   - **Status:** Active / Superseded / Draft

4. Write the governance document to `docs/decisions/<type>/ (lowercase: adr, pdr, pol)<TYPE>-<NNN>-<kebab-title>.md`:
   ```markdown
   # <TYPE>-<NNN>: <Title>
   **Status:** Active
   **Date:** <today>

   ## Context
   <why this decision was needed>

   ## Decision
   <what was decided>

   ## Rationale
   <why this option over alternatives>

   ## Consequences
   <what this enables or constrains>

   ## Alternatives Rejected
   <alternatives considered and why rejected>
   ```

5. Append to `docs/agents/DECISION-REGISTER.md` (new entry at TOP):
   ```
   ## DR-<next-id> — <date> — <title>
   **Decision:** <one line>
   **Rationale:** <one line>
   **Type:** <ADR|POL|PDR>
   **Ref:** docs/decisions/<type>/ (lowercase: adr, pdr, pol)<filename>
   **Status:** Active
   ```

6. Confirm what was written.
