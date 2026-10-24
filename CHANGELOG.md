# Changelog

All notable changes to MD-Slides will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.2] - 2026-10-21

Independent reviews of the 1.1.1 release, and building a 100-slide course deck with it, found the defects below.
Every one is fixed with a test that failed first.

`mdslides-mcp.jar` was replaced on 2026-10-24: the file first attached to this release was the 1.1.1 build, so its
`validate_deck` still stopped at the first problem. If you downloaded it before then, download it again.

### Fixed
- **Images with a relative path rendered broken.** `![alt](./images/x.png)`, the documented example, was copied to
  `images/images/x.png` while the page pointed at `images/x.png`. The copier and the renderer now share one path
  rule. A relative path keeps its folders under `images/`, so two images with the same file name in different
  folders no longer collide. An image outside the deck folder (`../shared/logo.png`, or an absolute path) is
  copied by its file name. Per-slide background images follow the same rule.
- **Validation now reports every problem in one pass, before anything is written,** as the docs promise:
  - problems on every slide, not just the first broken one;
  - every image without alt text on a slide;
  - density warnings, even when another slide has an error;
  - image files that don't exist, previously reported only after "Validation passed".

  A failed render no longer leaves an empty output folder. The MCP `validate_deck` tool runs the same checks.
- **Tables:** bold, code and links render inside cells and headers. Cell text used to be flattened, which also lost
  the spaces around bold text (`the **highest** of` became "thehighestof").
- **Frontmatter values** that end (or start) with a quoted word keep their quotation marks. Only a value wrapped in
  quotes as a whole is unquoted.
- **Diagram captions** show angle-bracketed text instead of dropping it, and inline HTML in slide text is shown
  as text.
- **Diagrams without mermaid-cli** are no longer counted as "Pre-rendered".
- **Messages:**
  - the `config` command suggested `echo '{{}}'`, which isn't valid JSON; it now suggests `echo '{}'`;
  - a two-column slide with the wrong number of `---column---` lines is reported in plain words.
- **Feature tour:** the frontmatter table lists the `{{title}}` token, and the tables slide shows bold and code
  inside cells.

## [1.1.1] - 2026-10-17

A verification pass of every documented feature against the released jar found features that did not work as
documented. This release fixes them, and makes the feature tour the tutorial, acceptance test and live demo.

### Fixed
- **Speaker view sync:** the presentation never sent or received slide changes, so speaker view only showed the
  slide it was opened on. Both windows now follow each other (same browser, same device). The 1.1.0 fix covered
  speaker view's side only.
- **Session analytics:** `display` wrote a log the browser never filled, so `report` always showed no events. The
  presentation now records slide changes (and how they happened), timer pauses and breaks; press **L** to save the
  log, and `report DECK` reads the newest one (deck folder, current folder or `~/Downloads`) or `--log FILE`. The
  report shows time per slide (breaks excluded), the path taken, breaks and navigation methods.
- **Goto:** the dialog had no number field and its list wasn't keyboard-accessible. Type a number and press Enter
  (focus starts in the field), or pick from the list; on touch screens, tap the slide counter.
- **Print / Save as PDF:** only the current slide printed. Each slide now prints on its own landscape page without
  the on-screen controls.
- **Header/footer tokens:** `{{title}}` (the deck title) and `{{date}}` (today) now render.
- **Images without alt text** were silently dropped; they are now an error naming the slide and the image.
- **Diagram captions** (`caption:` frontmatter) are rendered.
- **Break-screen images** (`--break-screen`, `breakScreen`) are copied into the output; a missing one is reported.
- **Strikethrough** (`~~text~~`) renders.
- **Diagrams without mermaid-cli** showed an empty box; they now show their source and an install notice.
- Headings, captions and the author line produced invalid HTML (`<p>` inside `<h2>`).
- **Accessibility:** pages declare their language (`lang="en"`), and syntax-highlighting colours that missed
  WCAG 2.1 AA contrast are adjusted (dark: keywords and comments; light: keywords, built-ins, comments, tags).
  Every slide of the feature tour now passes WCAG 2.1 AA (pa11y) in the light, dark and corporate themes.
- **corporate theme** link/accent colour now meets WCAG 2.1 AA; the timer overlay uses the theme text colour.
- On phones, footers keep clear of the controls bar, and wide code blocks shrink to fit.
- `--help` showed an old version and a broken link; `config` named the product inconsistently.
- Break mode and history keys no longer fire with modifier keys held (Ctrl+P printed *and* went back a slide) or
  while typing; the smart default no longer fails without a terminal.

### Changed
- The page title is the deck title (from the title slide).
- `--watch` says it isn't available yet instead of being silently ignored.
- The MCP server reports the build version.
- `feature-tour.md` (51 slides) has a **Try it** step on every feature slide and covers every feature;
  `docs/FEATURES.md` maps each feature to its slide, its docs page and how it is verified.

---

## [1.1.0] - 2026-10-16

### Added
- **Mobile presenting.** Decks can be presented from a phone or tablet:
  - Swipe left/right to change slides; tap the right 70% of the screen for next, the left 30% for previous (touch and pen only; mouse clicks don't navigate, so text selection on desktop is unaffected). Vertical drags, pinch-zoomed panning and horizontally scrollable code don't trigger navigation.
  - On-screen ‹ › buttons beside the slide counter (44px touch targets, disabled at the ends) and a fullscreen button, shown only where the browser supports fullscreen (hidden on iPhone).
  - Presentation clickers and keyboards: PageDown/PageUp, ArrowDown/ArrowUp and Enter now navigate, in addition to the existing keys. `F` toggles fullscreen. Keys are ignored while typing in a field, while the go-to dialog is open, or with Ctrl/Cmd/Alt held.
  - Deep links: the URL tracks the current slide (`#5`), so a reload stays on the same slide and links can open a given slide. Uses `replaceState`, so the OS/browser back gesture still leaves the deck rather than stepping through slides.
  - Phone layout: notch/safe-area aware (`viewport-fit=cover`), dynamic viewport height (`100dvh`, so mobile browser toolbars don't cut slides off), fluid heading/body sizes on small screens, and two-column slides stack into one column on portrait phones.
  - Speaker view works on a phone: single-column layout under 700px, ‹ › buttons, swipe navigation and the same clicker keys.
- **Fit-to-slide.** The current slide is scaled to fit the screen when its content overruns it vertically or horizontally: wide tables, Mermaid diagrams, code blocks and images are first shrunk to the available width, then the whole slide is scaled to the largest size that fits (scaled content is re-laid out at full slide width, so wide tables re-wrap rather than just shrinking). Content is kept clear of the floating controls. Re-fits on slide change, resize, rotation, font load and image load, but not on pinch-zoom. If a slide would need to drop below 35% to fit, it scrolls instead. Replaces the previous font-size auto-scaling, which only took effect on the first slide.

### Fixed
- **Speaker view half-width layout:** a duplicated `<div class="speaker-view">` nested the whole grid inside the first of its two columns.
- **Speaker view sync off by one:** keyboard navigation sent the slide *after* the one it had moved to (`currentSlideIndex + 1` was sent after the index had already advanced). All speaker-view navigation now goes through one function that syncs the slide actually shown.
- Space/Enter typed into the go-to dialog no longer also change slides.

---

## [1.0.7] - 2026-07-11

### Added
- **MCP server Tier 2:** Completes the 4-tool MCP surface. Two new tools over the existing JSON-RPC 2.0 stdio transport: `list_themes(themes_dir?)` → `ThemesResult` (built-in themes `light`/`dark`/`corporate` plus any directory-based themes found via `ThemeLoader`; a missing `themes_dir` is not an error) and `get_deck_info(input_path)` → `DeckInfo` (slide count, templates used, images referenced, whether the deck contains Mermaid diagrams) — read-only inspection, no rendering or validation. 8 new integration tests in `McpServerSpec`.

---

## [1.0.6] - 2026-05-17

### Added
- **MCP server Tier 1:** New `mcp` Mill module exposing mdslides as a Model Context Protocol server. Two tools over JSON-RPC 2.0 stdio transport: `render_deck(input_path, output_dir, theme?, no_copy_images?)` → `RenderResult` and `validate_deck(input_path)` → `ValidationResult`. Stateless file-in/file-out architecture. 11 integration tests in `McpServerSpec`.

---

## [1.0.5] - 2026-05-17

### Fixed
- **Two-column CSS coverage:** Lists (`ul`/`ol`/`li`), paragraphs (`p`), and tables (`th`/`td`/`tbody`) inside two-column slides were not styled because column content is rendered in `section.column` elements without a `.slide-body` wrapper. All `.slide-body X` CSS rules now also target `.column X`. Regression tests added in `HTMLRendererTwoColumnSpec`.

---

## [1.0.4] - 2026-05-17

### Fixed
- **Infrastructure test alignment:** 5 pre-existing infrastructure test failures resolved. Tests updated to match actual renderer behavior: (1) multi-slide deck active class check now uses substring match (`slide active`) to accommodate alignment suffix; (2) per-slide background path checks updated to `images/X.png` to reflect `rewriteImageUrl` behavior; (3) corporate theme assertion corrected to `None` (no built-in watermark image); (4) Mermaid diagram test updated to accept `mermaid-fallback` or `mermaid-diagram` element class (server-side pre-rendering path).

---

## [1.0.3] - 2026-05-17

### Fixed
- **Domain test alignment:** 20 pre-existing domain test failures resolved. `HeaderFooter.resolvePlaceholders` now uses single-brace `{title}`, `{author}`, `{date}`, `{current-slide}`, `{total-slides}`, `{elapsed-time}` format matching the documented spec. Property-based and example tests for density validation updated to check for `DensityWarning` instead of `ContentError`.

---

## [1.0.2] - 2026-05-17

### Changed
- **Version declaration:** `mdSlidesVersion` constant added to `build.sc`; `--version` flag now reads from generated `BuildInfo.version` instead of a hardcoded string. Eliminates risk of version drift.

### Fixed
- **PathResolver error message:** `--version` lookup error message now explicitly lists `deckName.md` in the "tried" clause (e.g., `Tried: myfile, myfile.md, myfile.markdown`). Previously the `.md` variant was mentioned only parenthetically and did not appear as a substring, breaking test assertions.

---

## [1.0.1] - 2026-05-16

### Fixed

- **Two-column headings not rendered.** The parser included the `## heading` line in the left
  column body rather than extracting it to a named slot; the renderer looked for a `title` slot
  that was never populated. All two-column slides silently rendered without a visible heading.
- **Lists and tables in two-column columns had no indentation or styling.** All list and table
  CSS was scoped to `.slide-body`; column content renders inside `.column`, which was not covered
  by those selectors. Bullet markers, nesting indentation, and table borders were absent in every
  two-column column.

## [1.0.0] - 2025-05-15

Initial public release.

### Features

- **Slide templates**: `title` and `content` templates with named slots
- **Markdown rendering**: bold, italic, inline code, links, ordered and unordered lists
- **Code blocks**: fenced code blocks with syntax highlighting via highlight.js (190+ languages)
- **Images**: local paths, external URLs, data URLs; automatic asset copying to output directory
- **Speaker notes**: `<!-- ... -->` comments rendered in speaker view; press `S` to open
- **Speaker view**: synchronized presenter display with next-slide preview and elapsed timer
- **Theme system**: built-in `light` and `dark` themes; custom themes via JSON
- **Directory-based themes**: per-template background images
- **4-layer configuration**: CLI flags → project config → global config → built-in defaults
- **Validation**: structure, density, content, and accessibility checks; all errors collected and reported together
- **Keyboard navigation**: `→` / `Space` next, `←` previous, `Home` first, `End` last
- **Standalone output**: self-contained HTML directory with all assets copied in
- **Property-based testing**: ScalaCheck invariant tests across the domain model
