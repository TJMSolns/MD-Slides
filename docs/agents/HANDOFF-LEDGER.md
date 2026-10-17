# MD-Slides — Handoff Ledger

Append-only. New entries at the top. Numbering continues from the archived planning repo (HL-001–HL-068).

---

## HL-069 — 2026-10-17 — One public repo (MS-026)

**Session:** Tony + Claude (org root)

**What happened:**
- This repo became the only MD-Slides repo (Tony, 2026-10-17). Moved in the working harness: `CLAUDE.md`,
  `.claude/` (hooks, skills, verifier agent; decision skills now point at `docs/decisions/`), and a fresh
  `docs/agents/`. Also moved in the `tjm-solutions` theme and ADR-013 (MCP server) / ADR-014 (domain layer).
- History restarted as a single commit at v1.1.0. Earlier releases (v1.0.0–v1.0.7) and their tags were removed;
  `CHANGELOG.md` keeps their history. Release v1.1.0 is re-tagged on the new commit with all three assets.
- The previous private planning repo is archived read-only. Org rails now point here.

**Carry-over from the archived ledger (HL-001–HL-068), in brief:**
- v1.0.0 public release (2026-05-15); v1.0.1–v1.0.5 bug fixes (two-column rendering and CSS, test alignment);
  v1.0.6–v1.0.7 MCP server (4 tools); MS-017 typed `SlotName` ADT; MS-018 LMS investigation spike (advisory
  input to the org's open-agentic-platform).
- v1.1.0 (2026-10-17): mobile presenting and fit-to-slide (MS-025), released and documented on the site.
- Lessons kept in `LESSONS-LEARNED.md`. Open queue: only MS-026's verifier pass.

**Decisions made:** one repo, public, history restarted (org ADR; see DECISION-REGISTER)
**Next owner:** any session at this repo's root for the MS-026 verifier pass
