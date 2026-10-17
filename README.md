# MD-Slides

**Markdown to HTML presentations — structured, validated, and speaker-ready.**

[![CI](https://github.com/TJMSolns/MD-Slides/actions/workflows/ci.yml/badge.svg)](https://github.com/TJMSolns/MD-Slides/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Scala 3](https://img.shields.io/badge/Scala-3.3.1-red.svg)](https://scala-lang.org)

MD-Slides converts structured Markdown files into self-contained HTML presentations with keyboard, clicker, and touch navigation, speaker view, syntax-highlighted code blocks, and a flexible theme system. Write slides in plain text; present anywhere a browser runs, including a phone or tablet.

**[Live demo](https://www.tjm.solutions/mdslides/demo/)** (try it on your phone) · **[Documentation](https://www.tjm.solutions/mdslides/docs/intro)** · **[Download](https://github.com/TJMSolns/MD-Slides/releases/latest)**

---

## Quick start

**Requirements:** Java 11 or higher.

```bash
# Download the JAR and the feature tour (a self-contained 51-slide tutorial)
curl -L https://github.com/TJMSolns/MD-Slides/releases/latest/download/md-slides.jar -o md-slides.jar
curl -L https://github.com/TJMSolns/MD-Slides/releases/latest/download/feature-tour.md -o feature-tour.md

# Render and open
java -jar md-slides.jar render feature-tour --theme dark
open feature-tour/index.html          # macOS
xdg-open feature-tour/index.html     # Linux
start feature-tour/index.html         # Windows
```

Press **S** to open speaker view — the feature tour has notes on every slide. Every feature slide has a **Try it** step; the tour is also MD-Slides' acceptance test, and the same deck is the [live demo](https://www.tjm.solutions/mdslides/demo/).

The feature tour exercises every template, content type, image, speaker notes, code block, two-column layout, theme, and validation rule. It's the fastest way to see what MD-Slides can do.

### Writing your own slides

Once you've seen the tour, create your own deck — `my-talk.md` anywhere next to the JAR:

```bash
java -jar md-slides.jar render my-talk --theme light
```

`my-talk/` is a self-contained output directory — `index.html` plus all copied assets. See [Writing slides](#writing-slides) below for the full syntax.

---

## Writing slides

Slides are separated by `---`. Each slide opens with a frontmatter block declaring its template.

````markdown
---
template: title
---

# My Talk Title

## A concise subtitle

**Author Name**

---
template: content
---

## First Section

What you want to say, kept to the point.

Key ideas:
- One idea per bullet
- Short phrases, not sentences
- Three to five bullets is plenty

---
template: content
---

## Code Example

```scala
case class Slide(id: SlideId, template: Template, slots: Map[SlotName, SlotContent])
```

MD-Slides validates slide density so your audience can actually read what you write.

<!-- Speaker notes: These appear only in speaker view — press S during the presentation. -->
````

Render it:

```bash
java -jar md-slides.jar render my-talk --theme dark
```

### Controls

| Key | Action |
|-----|--------|
| `→` / `↓` / `Space` / `Enter` / `Page Down` | Next slide |
| `←` / `↑` / `Page Up` | Previous slide |
| `Home` / `End` | First / last slide |
| `F` | Toggle fullscreen |
| `S` | Open speaker view |
| `B` | Break mode |
| `G` | Go to a slide (type its number, or tap the slide counter) |
| `P` / `N` | History back / forward |
| `T` / `R` | Pause / reset timer |
| `L` | Save the session log (read with `report`) |

Presentation clickers work out of the box: they send `Page Up`/`Page Down` or arrow keys. Full reference: [Navigation](https://www.tjm.solutions/mdslides/docs/navigation).

### Presenting from a phone or tablet

Swipe left or right, tap the right or left side of the screen, or use the on-screen `‹` `›` buttons. Each slide is scaled to fit the screen, and wide tables re-wrap rather than overflow. The address bar tracks the slide (`#5`), so a reload or a shared link opens the same slide.

Host the rendered directory where the device can reach it (GitHub Pages or any web server) and open its URL. Speaker view also works on a phone, in a single-column layout.

---

## Templates

### `title`

```markdown
---
template: title
---

# Main Title

## Optional subtitle

**Optional author**
```

Constraints: title max 2 lines; subtitle max 2 lines; author max 80 characters.

### `content`

```markdown
---
template: content
---

## Slide Heading

Body content: markdown, lists, code blocks, images.
```

Constraints: heading max 80 characters; body max 12 lines, max 150 words.

MD-Slides reports all constraint violations together — you see every problem in one pass, not one at a time.

---

## Speaker view

Press `S` during a presentation to open a speaker window showing your notes, the next slide heading, and an elapsed timer. Change slides in either window and the other follows (both windows in the same browser).

Add notes to any slide with an HTML comment:

```markdown
<!-- Speaker notes: The key point here is X. Don't forget to mention Y. -->
```

Output: `my-talk/index.html` (audience), `my-talk/speaker.html` (presenter).

---

## Themes

```bash
java -jar md-slides.jar render my-talk --theme light   # default
java -jar md-slides.jar render my-talk --theme dark
```

`corporate` is also built in. All three pass WCAG 2.1 AA contrast.

### Custom themes

A theme is a folder with a `theme.json`, found by name in `./themes` (or in `paths.themesDir` from your global config):

```
themes/mytheme/theme.json
```

```bash
java -jar md-slides.jar render my-talk --theme mytheme
```

Every section below is required; copy it and change what you need:

```json
{
  "name": "mytheme",
  "version": "1.0.0",
  "background": { "color": "#ffffff" },
  "colors": {
    "text": "#333333",
    "heading": "#000000",
    "accent": "#0066cc",
    "link": "#0066cc",
    "linkHover": "#0044aa",
    "codeBackground": "#f5f5f5",
    "codeText": "#333333"
  },
  "fonts": {
    "body": "Arial, sans-serif",
    "heading": "Arial, sans-serif",
    "code": "monospace"
  },
  "spacing": {
    "slideMargin": "2rem",
    "headingMargin": "1rem 0",
    "paragraphMargin": "0.5rem 0",
    "lineHeight": "1.6"
  },
  "syntax": {
    "keyword": "#0000ff",
    "string": "#00aa00",
    "comment": "#888888",
    "function": "#aa00aa",
    "number": "#aa5500",
    "operator": "#333333"
  },
  "slideCounter": {
    "color": "#666666",
    "background": "rgba(255,255,255,0.9)",
    "fontSize": "0.9rem"
  }
}
```

Optional: `"background": { "image": "images/bg.png" }`, `"templateBackgrounds": { "section-title": "images/section.png" }`, and `"templateConfig": { "closing": { "footer": "{{title}} · {{date}}" } }`. Image paths are relative to the theme folder. See [Themes](https://www.tjm.solutions/mdslides/docs/themes).

---

## Configuration

MD-Slides applies configuration in priority order (highest wins):

1. CLI flags (`--theme dark`)
2. Project config (`.mdslides/config.json` committed with your repo)
3. Global config (`~/.mdslides/config.json` for personal preferences)
4. Built-in defaults

Project config (`.mdslides/config.json`):

```json
{
  "theme": "dark",
  "outputDir": "dist"
}
```

Global config (`~/.mdslides/config.json`) puts settings under `defaults`:

```json
{
  "defaults": { "theme": "light" }
}
```

`java -jar md-slides.jar config` shows what is in effect and where it came from.

---

## Validation

MD-Slides validates every slide before rendering and reports everything in one pass.

**Errors** stop the render: missing required slots, unknown templates, images without alt text, image files that don't exist.

```
✗ Validation failed:
  - Slide 3: Structure Error - Required slot 'heading' is missing
  - Slide 3: Structure Error - Required slot 'body' is missing
```

**Warnings** don't: slides over the density guidance (headings over 80 characters, bodies over 12 lines or 150 words), and theme colours below WCAG 2.1 AA contrast.

---

## Building from source

MD-Slides is built with [Mill](https://mill-build.org). See [CONTRIBUTING.md](CONTRIBUTING.md) for full setup instructions.

```bash
git clone https://github.com/TJMSolns/MD-Slides.git
cd MD-Slides

# Install Mill (macOS/Linux)
curl -L https://github.com/com-lihaoyi/mill/releases/download/0.11.6/0.11.6 > mill
chmod +x mill && sudo mv mill /usr/local/bin/mill

# Compile and test
mill __.compile
mill __.test

# Build a standalone JAR
mill cli.assembly
# → out/cli/assembly.dest/out.jar
```

---

## MCP server

`mdslides-mcp.jar` (on the [releases page](https://github.com/TJMSolns/MD-Slides/releases/latest)) runs MD-Slides as a [Model Context Protocol](https://modelcontextprotocol.io) server over stdio, so an AI assistant can render and inspect decks. Tools: `render_deck`, `validate_deck`, `list_themes`, `get_deck_info`. Build it from source with `mill mcp.assembly`.

---

## Architecture

MD-Slides is a three-module Mill project, plus an `mcp` module that wraps `infrastructure` as an MCP server. Dependencies flow in one direction only: `cli → infrastructure → domain`.

```
┌─────────────────────────────────────────────────────┐
│  cli                                                │
│  Cats Effect IOApp · Decline argument parsing       │
│  Wires modules. Contains no business logic.         │
└───────────────────────────┬─────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────┐
│  infrastructure                                     │
│  Markdown parser (Flexmark anticorruption layer)    │
│  HTML renderer (Scalatags)                          │
│  Theme loader (Circe)  · Asset copier (os-lib)      │
└───────────────────────────┬─────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────┐
│  domain                                             │
│  Pure functions — no I/O, no side effects           │
│  Slide · SlideDeck · Template · Theme               │
│  Validation pipeline · Error accumulation           │
└─────────────────────────────────────────────────────┘
```

**Domain layer** is pure Scala 3 — opaque types, smart constructors, `Either[NonEmptyList[ValidationError], A]` for error accumulation, no `cats-effect`. It has no knowledge of files, HTML, or markdown.

**Infrastructure layer** is where all I/O lives. Third-party libraries (Flexmark, Circe) are wrapped behind domain-facing interfaces so they never leak into domain types.

**CLI layer** is thin: parse arguments, call infrastructure, handle errors, exit with the right code.

See [CONTRIBUTING.md](CONTRIBUTING.md) and [docs/decisions/](docs/decisions/) for a deeper walkthrough.

---

## Technology stack

| Dependency | Purpose |
|-----------|---------|
| [Scala 3.3.1 LTS](https://scala-lang.org) | Language |
| [Mill 0.11.6](https://mill-build.org) | Build tool |
| [Cats Core 2.10.0](https://typelevel.org/cats/) | Functional primitives (`Either`, `NonEmptyList`) |
| [Cats Effect 3.5.4](https://typelevel.org/cats-effect/) | Effect system for I/O |
| [Decline 2.4.1](https://ben.kirw.in/decline/) | CLI argument parsing |
| [Flexmark 0.64.8](https://github.com/vsch/flexmark-java) | Markdown parsing |
| [Scalatags 0.12.0](https://com-lihaoyi.github.io/scalatags/) | Type-safe HTML generation |
| [Circe 0.14.6](https://circe.github.io/circe/) | JSON (theme files) |
| [os-lib 0.9.3](https://github.com/com-lihaoyi/os-lib) | File I/O |
| [MUnit 0.7.29](https://scalameta.org/munit/) | Test framework |
| [ScalaCheck 1.17.0](https://scalacheck.org) | Property-based testing |

---

## Contributing

Contributions are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for the full guide, including how to add templates, themes, and features while keeping the domain layer clean.

Please read the [Code of Conduct](CODE_OF_CONDUCT.md) before participating.

---

## License

[MIT](LICENSE) — Copyright © 2025 TJM Solutions LLC
