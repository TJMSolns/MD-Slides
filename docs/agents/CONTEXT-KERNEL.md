# MD-Slides — Context Kernel

*Single source of truth for this repo. Update at phase transitions only.*

**Last updated:** 2026-10-17 (one public repo, MS-026)
**Phase:** Active maintenance

---

## Status

Active. v1.1.0 (2026-10-17) added mobile presenting and fit-to-slide. Work is driven by feature requests
and bugs, not a milestone roadmap. Priority is set at org level (TJM Solutions portfolio).

---

## Charter

**Mission:** To support TJM Solutions' productivity and engineering showcase by providing a Scala 3 / Cats
Effect CLI tool for rendering markdown presentation decks to styled HTML with speaker view.

---

## What This Is

Mill project: `domain` → `infrastructure` → `cli`, plus `mcp`.

- Domain: pure FP, zero I/O. Slide/SlideDeck/Template/Theme aggregates, validation.
- Infrastructure: Flexmark (parse), Scalatags + HTMLRenderer (render), Circe (theme), os-lib (file I/O), Cats Effect.
- CLI: `Main extends IOApp`, wires modules, Decline for args, no business logic.
- MCP: JSON-RPC 2.0 stdio server over `infrastructure` (ADR-013).

Tech stack: Scala 3.3.1 LTS, Mill 0.11.6, Cats Effect (not ZIO — MD-Slides predates the org ZIO default),
MUnit + ScalaCheck (700+ tests).

Single repo since 2026-10-17: code, docs, releases and the working harness are all here, and all public.
User docs live on https://www.tjm.solutions/mdslides/docs (site repo `TJMSolns/tjm-solutions-site`).

---

## Non-Negotiables

- Public repo: no former employer or client names anywhere, not even to note their removal
- `-Xfatal-warnings` — all warnings are fatal
- Domain layer must never depend on Cats Effect or os-lib
- Validation uses `Either[NonEmptyList[ValidationError], A]` — accumulate all errors
- Test names use domain language exactly
- ADR and PDR changes require a governance doc (`docs/decisions/`) before implementation
- Every release carries `md-slides.jar`, `mdslides-mcp.jar` and `feature-tour.md`
