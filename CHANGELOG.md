# Changelog

All notable changes to MD-Slides will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
