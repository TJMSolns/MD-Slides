# MD-Slides — Handoff Ledger

Append-only. New entries at the top. Numbering continues from the archived planning repo (HL-001–HL-068).

---

## HL-072 — 2026-10-24 — Verifier pass on v1.1.2; v1.1.3 released (MS-030–036)

**Session:** Tony + Claude (org root). Tony: "go, run the verifier checks and fix the GS1 links".

**What happened:**
- Pre-verifier recheck found the v1.1.2 release carried the v1.1.1 MCP jar (same sha256 `0f45bf12…`, no `DeckCheck`).
  Cause: the documented step `mill cli.assembly mcp.assembly` builds only the CLI jar in Mill 0.11 and exits 0.
  Rebuilt from the tag, re-uploaded; CHANGELOG and release notes say so; CLAUDE.md § Releasing now uses `+` and
  requires both jars' digests to change (`c0fcf6b`).
- Independent verifiers (DN-010 fallback): MS-030 PASS, MS-031 PASS, MS-033 PASS, MS-032 VETO (a caption starting
  and ending with a quoted word lost its outer quotes). Site WQ-077's verifier vetoed the two-column column limits
  the docs promise but the jar never checked.
- v1.1.3 (`0164759`, https://github.com/TJMSolns/MD-Slides/releases/tag/v1.1.3): quote rule fixed; each two-column
  column warns above 10 lines / 75 words; tour slide 38 trimmed. 782 tests, CI green, digests verified, downloaded
  jars exercised through CLI and MCP. Live demo rebuilt from v1.1.3, 23/23 against the live site.
- MS-034 (v1.1.3) PASS; MS-032 second verifier PASS. MS-030, 031, 032, 033, 034 Done.
- Queued from verifier side findings: MS-035 (outside-folder images with the same name overwrite each other), MS-036
  (captions of only block HTML render empty; literal entities lose characters).

**Decisions made:** none
**Open items carried forward:**
- MS-035, MS-036 (P3 bugs, queued); MS-027 (`--watch`), MS-029 (other browsers, Java 11).
- MS-026: deleting the 14 old public Actions runs is Tony's call.
- Phone walk of the v1.1.3 tour (release step 8) is Tony's.
**Next owner:** Tony — phone test of v1.1.3; MS-026 decision.

---

## HL-071 — 2026-10-21 — v1.1.2: image paths, all-at-once validation, table formatting (MS-030–033)

**Session:** Tony + Claude (org root)

**What happened:**
- v1.1.2 released (`99c119e`, tag v1.1.2, CI green, asset digests equal to the local build). Fixes: relative image
  paths keep their folders (MS-030); validation reports every problem on every slide in one pass and writes nothing
  until the deck is clean, in the CLI and the MCP `validate_deck` tool (`DeckCheck`); table cells keep bold, code and
  links; diagram captions keep their quotation marks; frontmatter unquoting only strips wrapping quotes; clearer
  two-column, config and no-mmdc messages (MS-031, MS-032). Tests 754 → 779.
- Every fixed scenario checked with the downloaded jar in a clean home directory. CHANGELOG, FEATURES.md, README and
  the feature tour updated; the live demo was rebuilt from v1.1.2 (browser check 23/23 on a local build).
- Real-world use: a 102-slide course deck renders with no warnings and WCAG 2.1 AA passing.

**Decisions made:** none.
**Open items:** verifier passes for MS-030–033 (and the still-open MS-026, MS-028 findings); live-site browser check
of the rebuilt demo; Tony's phone test; MS-027 (`--watch`).
**Next owner:** Claude (verifier passes, in a session at this repo's root); Tony (phone test).

---

## HL-070 — 2026-10-17 — v1.1.1: every documented feature verified against the release (MS-028)

**Session:** Tony + Claude (org root)

**What happened:**
- Tony asked whether the docs had been updated diligently after v1.1.0. A check of every documented claim against
  the released jar found a dozen features that didn't work as documented (headline: speaker-view sync never worked,
  session analytics was dead code, goto had no number entry, print gave one page). All fixed in v1.1.1; see
  `docs/agents/evidence/MS-028.md` and CHANGELOG [1.1.1].
- The feature tour is now the tutorial, the acceptance test and the live demo (https://www.tjm.solutions/mdslides/demo/):
  51 slides, a **Try it** step per feature. `docs/FEATURES.md` maps every feature to its slide, docs page and check.
- Release procedure in CLAUDE.md rewritten: verify every public claim against the released artifacts, run the
  site's browser check, then a real phone.
- v1.1.1 released (`7c5363a`, three assets, digests verified, CI green). Live demo built from it; browser check 23/23.

**Decisions made:** none recorded as ADR/PDR (Tony's directives: the tour is tutorial + test; done = full surface).
**Open items:** MS-028 verifier pass; Tony's phone test; MS-027 (`--watch`).
**Next owner:** Tony (phone test).

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
