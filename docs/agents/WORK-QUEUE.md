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
