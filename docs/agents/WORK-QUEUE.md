# MD-Slides — Work Queue

Work items are driven by feature requests, bugs, and maintenance needs. IDs continue the `MS-NNN` series.

Last groomed: 2026-10-17 (GL-033, org `/groom-all`). Order: the two verifier-pending items first (run their verifier passes in a session opened at this repo's root, where the `verifier` agent is defined), then MS-027. MS-029 [PROPOSED] added for the known verification gaps. Priority: P4 (org PDR-013).

> **PROPAGATION-STALE:** `.claude/hooks/stop-git-durability-gate.py` differs from harness-evolution's current copy — review (copy over, or merge if this project has real local customization — see HE-024/CNX and DN-024/open-agentic-platform precedent) as the first action next time this project is touched. Flagged GL-033, 2026-10-17.
> **PROPAGATION-STALE:** `.claude/skills/next/SKILL.md` differs from harness-evolution's current copy — review as above. Flagged GL-033, 2026-10-17.
> **PROPAGATION-STALE:** `.claude/skills/handoff/SKILL.md` differs from harness-evolution's current copy — review as above. Flagged GL-033, 2026-10-17.
> **PROPAGATION-STALE:** `.claude/settings.json` differs from harness-evolution's current copy — review as above. Flagged GL-033, 2026-10-17.

---

## Active

| ID | Item | Owner | Status | Depends On |
|----|------|-------|--------|-----------|
| MS-028 | v1.1.1: full-surface verification of v1.1.0 against the released jar found a dozen features not working as documented; fixed, tour made tutorial + acceptance test + live demo, docs corrected. | Claude | **Released v1.1.1; needs a verifier** — evidence `docs/agents/evidence/MS-028.md`; phone test passed (Tony, 2026-10-17) | — |
| MS-026 | One public repo: this repo becomes the only MD-Slides repo (harness moved in, history restarted clean, earlier private repo archived). | Claude | **Implementation complete, needs a verifier** — evidence `docs/agents/evidence/MS-026.md` | — |
| MS-034 | **v1.1.3 release** (2026-10-24, completing the v1.1.2 fixes after verifier review): the MS-032 VETO (unquote only when one pair of quotes wraps the whole value) and site WQ-077's VETO (each two-column column warns above 10 lines or 75 words, as the docs and the tour say; tour slide 38 trimmed to fit). Tests 779 → 782. | Claude | Done 2026-10-24 (verifier PASS; release https://github.com/TJMSolns/MD-Slides/releases/tag/v1.1.3; live demo 23/23 on v1.1.3; evidence `docs/agents/evidence/MS-034.md`) | — |
| MS-035 | **P3 BUG [found 2026-10-24 by the MS-030 verifier]** Images from outside the deck folder are copied by file name, so `../o1/logo.png` and `../o2/logo.png` both become `images/logo.png` and the second overwrites the first: one slide shows the wrong picture, silently. Not a regression (v1.1.1 also copied by name). Queued, not diagnosed. | Claude | Queued | — |
| MS-036 | **P3 BUG [found 2026-10-24 by the MS-031 verifier]** Two caption edge cases lose text: a caption made only of block-level HTML (`<div>x</div>`, `<script>…</script> & more`) renders empty, and literal entities (`&amp;amp; literal entity`, `x &amp;lt;y&amp;gt; z`) lose their entity characters. Not checked against v1.1.1. Queued, not diagnosed. | Claude | Queued | — |
| MS-033 | **v1.1.2 release** (Tony, 2026-10-21: "go ahead with the MD-Slides v1.1.2 fixes"): MS-030, MS-031, MS-032, plus validation reporting every problem in one pass before anything is written (`DeckCheck`, site WQ-077), including the MCP `validate_deck` tool. Tests 754 → 779. Evidence: commit `99c119e`, tag and release v1.1.2 (https://github.com/TJMSolns/MD-Slides/releases/tag/v1.1.2), CI green, release asset sha256 equal to the local build, downloaded jar reports v1.1.2 and passes every fixed scenario in a clean home directory. | Claude | Done 2026-10-24 (verifier PASS; evidence `docs/agents/evidence/MS-033.md`; MCP jar re-uploaded 2026-10-24, see evidence) | — |
| MS-030 | **P2 BUG [found 2026-10-21 by the MS-028 verifier]** A relative image path breaks the image. `![alt](./images/x.png)` (and `images/x.png`) is copied to `DECK/images/images/x.png`, but the HTML references `images/x.png`, so the picture is missing. A bare filename works. The same happens in v1.1.0. This is the exact example in the site docs (`writing-slides.md`) and in the feature tour's "Images" slide. The tour's live image is a data URL, and FEATURES.md lists copied images as unit-tested only, so no real render ever exercised it. Blocks MS-028's Done. Queued, not diagnosed. | Claude | Done 2026-10-24 (verifier PASS; evidence `docs/agents/evidence/MS-030.md`) | — |
| MS-031 | **P3 BUG [found 2026-10-21 by the MS-028 verifier]** Four small v1.1.1 defects:
- the `config` command's hint prints `echo '{{}}'` (should be `{}`);
- the feature tour's frontmatter token table omits `{{title}}`, which this release added;
- a diagram caption with `&` and `<special>` renders as `&amp;amp;  chars`, losing the `<special>` text;
- without mermaid-cli, render warns the diagram failed and then prints "Pre-rendered 1 diagram(s)".

Queued, not diagnosed. | Claude | Done 2026-10-24 (verifier PASS; evidence `docs/agents/evidence/MS-031.md`) | — |
| MS-032 | **P3 BUG [found 2026-10-21 building the short-course deck with v1.1.1]** Two rendering defects:
- **Bold inside a table cell.** `| the **highest** of the inputs |` renders as "thehighestof the inputs", losing the spaces around the bold text. A cell that is only `**Buy**` renders as plain text, without bold.
- **Quotation marks in a diagram caption.** `caption: Part of a typed concept graph for the word "chocolate"` renders without the closing quotation mark.

The deck currently avoids both. Queued, not diagnosed. | Claude | Done 2026-10-24 (first verifier VETO on v1.1.2, fixed in v1.1.3; second verifier PASS; evidence `docs/agents/evidence/MS-032.md`) | — |
| MS-027 | `--watch` live reload: re-render on save and reload the browser on the current slide (parsed today but not implemented; the CLI now says so). Needs design: polling vs file watcher, and how the page reloads without losing its place (the `#N` deep link helps). | Claude | Queued | — |
| MS-029 [PROPOSED] | Close the verification gaps MS-028 left open: run the live demo's Try it steps in Firefox, Safari (macOS or iOS) and Edge, and run the released jar on a Java 11 runtime (the docs say "Java 11 or higher"; the bytecode targets 11, but no Java 11 run was made). Record results in FEATURES.md; fix or correct the docs for anything that fails. Rationale: these are public claims not yet checked against the release (CLAUDE.md § Releasing). | Claude (Tony for Safari, if no Mac) | Queued | — |

---

## Done

| ID | Item | Evidence |
|----|------|---------|
| MS-025 | v1.1.0 — mobile presenting and fit-to-slide | CHANGELOG [1.1.0]; release v1.1.0 (`md-slides.jar`, `mdslides-mcp.jar`, `feature-tour.md`); 706/706 tests |

Earlier items (MS-001–MS-024) are recorded in the archived planning repo; their outcomes are in `CHANGELOG.md`.

---

## Notes

User docs: https://www.tjm.solutions/mdslides/docs — update them with any user-visible change.
