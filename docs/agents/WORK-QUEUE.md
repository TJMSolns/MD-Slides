# MD-Slides — Work Queue

Work items are driven by feature requests, bugs, and maintenance needs. IDs continue the `MS-NNN` series.

---

## Active

| ID | Item | Owner | Status | Depends On |
|----|------|-------|--------|-----------|
| MS-027 | `--watch` live reload: re-render on save and reload the browser on the current slide (parsed today but not implemented; the CLI now says so). Needs design: polling vs file watcher, and how the page reloads without losing its place (the `#N` deep link helps). | Claude | Queued | — |
| MS-028 | v1.1.1: full-surface verification of v1.1.0 against the released jar found a dozen features not working as documented; fixed, tour made tutorial + acceptance test + live demo, docs corrected. | Claude | **Released v1.1.1; needs a verifier** — evidence `docs/agents/evidence/MS-028.md`; phone test passed (Tony, 2026-10-17) | — |
| MS-026 | One public repo: this repo becomes the only MD-Slides repo (harness moved in, history restarted clean, earlier private repo archived). | Claude | **Implementation complete, needs a verifier** — evidence `docs/agents/evidence/MS-026.md` | — |

---

## Done

| ID | Item | Evidence |
|----|------|---------|
| MS-025 | v1.1.0 — mobile presenting and fit-to-slide | CHANGELOG [1.1.0]; release v1.1.0 (`md-slides.jar`, `mdslides-mcp.jar`, `feature-tour.md`); 706/706 tests |

Earlier items (MS-001–MS-024) are recorded in the archived planning repo; their outcomes are in `CHANGELOG.md`.

---

## Notes

User docs: https://www.tjm.solutions/mdslides/docs — update them with any user-visible change.
