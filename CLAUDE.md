# MD-Slides

**Role:** Scala 3 / Mill CLI tool for rendering markdown presentation decks to HTML with speaker view.
**Org:** TJM Solutions LLC.

This is the only MD-Slides repository: code, docs, releases and the working harness all live here. It is
**public**. Everything committed here, including `docs/agents/`, is published.

---

## Mandatory Startup Read Order

Read these in order at the start of every session. Do not skip. If starting from a context summary, STOP — read these files now before doing anything else.

1. This file (`CLAUDE.md`)
2. `docs/agents/CONTEXT-KERNEL.md` — current phase and non-negotiables
3. `docs/agents/WORK-QUEUE.md` — what's in progress, what's next
4. `docs/agents/HANDOFF-LEDGER.md` — last 2–3 entries only

---

## Public-Repo Rule — Non-Negotiable

Nothing committed or published (files, CHANGELOG, commit messages, release notes, ledger entries) may name
former employers or clients — not even to say a reference was removed. Use neutral example names
(`acme`, `example.com`). Check before every push: `git ls-files | xargs grep -il <name>` is empty.

---

## Available Skills

| Skill | Purpose |
|-------|---------|
| `/next` | Pick next unblocked WORK-QUEUE item and drive to completion |
| `/handoff` | End-of-session: write HANDOFF-LEDGER entry, update WORK-QUEUE |
| `/status` | One-screen status digest |
| `/decide adr\|pol\|pdr <title>` | Record a durable decision (`docs/decisions/`) |
| `/health` | Audit methodology infrastructure |
| `/retro` | Structured retrospective |
| `/audit <path>` | Governance review of any artifact |
| `/groom` | Review and reorder WORK-QUEUE |

---

## Build System

Mill 0.11.6, Scala 3.3.1 LTS. All warnings are fatal (`-Xfatal-warnings`).

```bash
mill __.compile                                        # Compile all modules
mill __.test                                           # Run all tests
mill domain.test.testOnly com.tjmsolutions.mdslides.domain.SlideSpec  # Single suite
mill cli.run -- render my-preso                        # Run CLI
mill cli.assembly                                      # CLI fat JAR → out/cli/assembly.dest/out.jar
mill mcp.assembly                                      # MCP server JAR → out/mcp/assembly.dest/out.jar
```

## Architecture

Layered design with strict dependency flow: `domain` → `infrastructure` → `cli`, plus `mcp`, which wraps `infrastructure` as a Model Context Protocol server.

**domain** — Pure functional, zero I/O, zero side effects. Depends only on Cats Core. Contains aggregates (`Slide`, `SlideDeck`, `Template`, `Theme`), value objects (`SlideId`, `ValidationError`, `FormattedContent`), and all validation logic. The domain layer must never acquire Cats Effect or os-lib as dependencies.

**infrastructure** — I/O adapters only. Flexmark parses markdown into domain models; Scalatags + HTMLRenderer renders them back out; Circe loads theme JSON; os-lib handles file I/O; Cats Effect runs effects. Infrastructure adapts the domain — it does not extend it.

**cli** — `Main extends IOApp`. Wires modules together, parses CLI args with Decline, orchestrates the rendering pipeline. No business logic lives here.

**mcp** — JSON-RPC 2.0 over stdio; stateless file-in/file-out tools `render_deck`, `validate_deck`, `list_themes`, `get_deck_info` (ADR-013).

## Rendering Pipeline

`render` command flow:
1. Load and merge configuration (CLI args → project config → global config → defaults)
2. Parse markdown via `FlexmarkAdapter` → `SlideDeck`
3. Validate structure, content, density, and accessibility
4. Load theme (built-in: `light`, `dark`, `corporate`; or directory-based JSON under `themes/`)
5. Copy image assets (`ImageAssetCopier`)
6. Pre-render Mermaid diagrams (`MermaidRenderer`)
7. Emit `index.html` + `speaker.html` + JS support files via `HTMLRenderer` + `SpeakerViewRenderer`

## Ubiquitous Language

Terms used exactly in code, tests, and docs. Never substitute "DTO", "Manager", "Handler", "Helper", or "Service" in domain code (infrastructure adapters may use technical terms).

| Term | Meaning |
|---|---|
| **Slide** | Single presentation unit bound to a Template |
| **SlideDeck** | Complete presentation; enforced 1–200 slides (NonEmptyList) |
| **Template** | Structural definition — declares named slots and their constraints |
| **Slot** | Named content area within a Template (e.g., `title`, `body`, `heading`) |
| **Theme** | Visual styling — colors, fonts, backgrounds |
| **FormattedContent** | Parsed inline markdown (bold, italic, code, links, images, nested lists) |
| **ValidationError** | Blocking structural/content error; warnings are density violations |

## Testing

MUnit + ScalaCheck (property-based). 700+ tests across all modules.

- Example-based tests live in `*Spec.scala`; property-based generators live in `properties/` subdirectories
- Test names use domain language — match ubiquitous language exactly
- Domain tests are pure (no I/O); infrastructure tests may use temporary files via os-lib

## Key Conventions

- Scala 3 idioms: `enum` over sealed traits for ADTs with `label`/`value` fields; `given`/`using` over `implicit`
- Validation uses `Either[NonEmptyList[ValidationError], A]` — accumulate all errors, never short-circuit
- Configuration merges at three levels; `ConfigurationMerger` in domain handles the merge logic
- ADRs, PDRs and POLs (`docs/decisions/`) document all significant decisions — check them before changing architecture or adding features

## Definition of Done — Bug Fix

1. Write a **failing test** that reproduces the bug before touching the code
2. Fix makes the test pass; all previously-passing tests still pass
3. Update `CHANGELOG.md` under the next version heading
4. Bump PATCH version in `build.sc` (`val mdSlidesVersion`)
5. Release (below)

## Releasing

A release is done only when **every public claim reachable from the product** (site pages, docs, README, CLI help,
release notes, the feature tour) has been checked against the **released artifacts**. Never declare done from a
review of the changed parts only.

1. `mill __.test` green. `CHANGELOG.md`, `mdSlidesVersion` (build.sc), `docs/FEATURES.md` and the feature tour
   (`examples/feature-tour.md`: every feature has a slide with a **Try it** step) are up to date. Commit, push `main`,
   CI green.
2. `mill cli.assembly + mcp.assembly` (the `+` matters: without it, Mill 0.11 passes `mcp.assembly` to the first
   target as an argument, builds only the CLI jar and still exits 0)
3. Create the release on the pushed commit with **all three assets**: `md-slides.jar`, `mdslides-mcp.jar`,
   `examples/feature-tour.md`:
   ```bash
   cp out/cli/assembly.dest/out.jar /tmp/md-slides.jar
   cp out/mcp/assembly.dest/out.jar /tmp/mdslides-mcp.jar
   cp examples/feature-tour.md /tmp/feature-tour.md
   gh release create vX.Y.Z /tmp/md-slides.jar /tmp/mdslides-mcp.jar /tmp/feature-tour.md --target main --notes-file <changelog section>
   ```
4. **Verify the release:** asset digests match the local builds; both jars' digests differ from the previous release's;
   a *downloaded* `md-slides.jar --version` prints vX.Y.Z; the *downloaded* `mdslides-mcp.jar` is exercised on
   whatever this release changed in the MCP tools.
5. **Live demo and browser check** (in `~/Cloud/GitHub/tjm-solutions-site`):
   - `scripts/build-mdslides-demo.sh vX.Y.Z` renders the released tour into `static/mdslides/demo/`
   - serve the built site and run `node scripts/verify-mdslides-demo.mjs <url>/mdslides/demo <downloaded jar>`:
     every check must pass (navigation, speaker sync, goto, history, break, tokens, fit at phone and laptop
     sizes, session log + `report`, PDF pages, WCAG 2.1 AA on every slide)
6. **Docs:** every command and example in the site docs and README run against the downloaded jar in a clean
   environment (`java -Duser.home=<empty dir>` — Java ignores `$HOME`). Update the docs for anything user-visible:
   `mdslides-docs/`, `src/pages/mdslides.tsx`, `src/pages/projects.tsx`.
7. Deploy the site; rerun the browser check against https://www.tjm.solutions/mdslides/demo.
8. **Phone:** walk through the tour's Try it steps on a real phone before announcing the release.

## Harness Rules — Non-Negotiable

**Rule 1 — Bugs and issues:** Use `/bug <description>`. Queue it. Stop. `/groom` decides priority. `/next` executes. No inline diagnosis, no inline fixes, no self-sequencing.

**Rule 2 — Discussion is not instruction:** A design conversation or question produces words only — no file writes, no WORK-QUEUE additions, no artifacts — unless Tony explicitly asks for one. If something seems worth recording, ask "Should I queue this?" and wait.

**Rule 3 — Approved plan ≠ execute permission:** A plan record in HANDOFF-LEDGER is history, not a command. Execution requires explicit instruction in the current session.
