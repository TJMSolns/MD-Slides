# MD-Slides

**Markdown to HTML presentations — structured, validated, and speaker-ready.**

[![CI](https://github.com/TJMSolns/MD-Slides/actions/workflows/ci.yml/badge.svg)](https://github.com/TJMSolns/MD-Slides/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Scala 3](https://img.shields.io/badge/Scala-3.3.1-red.svg)](https://scala-lang.org)

MD-Slides converts structured Markdown files into self-contained HTML presentations with keyboard navigation, speaker view, syntax-highlighted code blocks, and a flexible theme system. Write slides in plain text; present anywhere a browser runs.

---

## Quick start

**Requirements:** Java 11 or higher.

```bash
# Download the JAR
curl -L https://github.com/TJMSolns/MD-Slides/releases/latest/download/md-slides.jar -o md-slides.jar
```

Create `my-talk.md`:

```markdown
---
template: title
---

# My Talk

## A subtitle

**Your Name**

---
template: content
---

## First slide

Write your content here.

- One idea per bullet
- Short phrases work best

---
template: closing
---

## Thanks

Questions?
```

Render and open:

```bash
java -jar md-slides.jar render my-talk --theme light
open my-talk/index.html          # macOS
xdg-open my-talk/index.html     # Linux
start my-talk/index.html         # Windows
```

`my-talk/` is a self-contained output directory — `index.html` plus all copied assets.

### See all features in action

[`examples/feature-tour.md`](examples/feature-tour.md) is a 21-slide deck that exercises every template, content element, image, speaker notes, and theme option. To run it, clone the repo (which includes the image assets it references):

```bash
git clone https://github.com/TJMSolns/MD-Slides.git
java -jar md-slides.jar render MD-Slides/examples/feature-tour --theme dark
open MD-Slides/examples/feature-tour/index.html
```

Press **S** to open speaker view — the feature tour has notes on every slide.

---

## Writing slides

Slides are separated by `---`. Each slide opens with a frontmatter block declaring its template.

```markdown
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
```

Render it:

```bash
java -jar md-slides.jar render my-talk --theme dark
```

### Keyboard controls

| Key | Action |
|-----|--------|
| `→` or `Space` | Next slide |
| `←` | Previous slide |
| `Home` | First slide |
| `End` | Last slide |
| `S` | Open speaker view |

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

Press `S` during a presentation to open a synchronized speaker window showing your notes, the next slide heading, and an elapsed timer.

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

### Custom themes

Create a JSON file anywhere and pass its path as the theme:

```bash
java -jar md-slides.jar render my-talk --theme ./themes/mytheme/theme.json
```

Minimal theme schema:

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

Per-template background images are also supported — see [`docs/decisions/pdr/PDR-013-directory-based-theme-architecture.md`](docs/decisions/pdr/PDR-013-directory-based-theme-architecture.md).

---

## Configuration

MD-Slides applies configuration in priority order (highest wins):

1. CLI flags (`--theme dark`)
2. Project config (`.mdslides/config.json` committed with your repo)
3. Global config (`~/.mdslides/config.json` for personal preferences)
4. Built-in defaults

Example project config:

```json
{
  "theme": "dark",
  "outputDir": "dist"
}
```

---

## Validation

MD-Slides validates every slide before rendering. Structure errors, density violations, and accessibility problems are all collected and shown together:

```
✗ Validation failed:
  - Slide 3: body exceeds max 12 lines (has 17)
  - Slide 5: heading exceeds max 80 characters (has 94)
  - Slide 7: image is missing alt text
```

Fix them all at once rather than in a loop of one-error-at-a-time.

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

## Architecture

MD-Slides is a three-module Mill project. Dependencies flow in one direction only: `cli → infrastructure → domain`.

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
