---
template: title
---

# Presenting with MD-Slides

## The complete feature reference

**MD-Slides v1.0.0**

<!-- Speaker notes: This tour covers every feature in MD-Slides: all six templates, every content type, keyboard navigation, speaker view, CLI commands, themes, configuration, and validation. Each slide either demonstrates the feature by using it, or shows the markdown source alongside the rendered result. Follow along in examples/feature-tour.md while viewing the presentation — you'll see exactly what markdown produced each slide. -->

---
template: content
---

## How this tour works

MD-Slides converts Markdown to self-contained HTML presentations. This deck demonstrates every feature by **using it**, across seven sections:

1. Templates — six slide layouts
2. Content — formatting, lists, code blocks, images
3. Speaker view — notes, next-slide preview, elapsed timer
4. Navigation — every keyboard shortcut
5. CLI — render, display, report, smart default
6. Themes and configuration
7. Validation — errors collected and shown together

Open `examples/feature-tour.md` alongside this presentation to see what markdown produces each slide.

<!-- Speaker notes: The best way to read this tour is side-by-side: open feature-tour.md in a text editor and the rendered presentation in a browser. You can see exactly what markdown produces each slide. The file is self-contained — all images are embedded as base64 data URLs, so no external files are needed. -->

---
template: section-title
---

## Templates

Six slide types — each with named slots and enforced constraints

<!-- Speaker notes: Every slide declares its template in frontmatter. The template determines which slots are available, what content is required, and what the rendered layout looks like. Think of templates as typed containers for your content. Slides are separated by --- lines. Each slide opens with a frontmatter block: template name and any optional keys. -->

---
template: content
---

## The title template

Every deck starts with one. Slides are separated by `---`. Inside each slide, a frontmatter block declares the template:

```markdown
template: title

# Main Title          ← required, max 2 lines

## Subtitle           ← optional, max 2 lines

**Author**            ← optional, max 80 chars
```

The slide you saw first in this deck uses the `title` template. `H1` is used only here — all other templates use `H2` for headings.

<!-- Speaker notes: The title template is the only template where H1 is used. All other templates use H2 for headings. The author slot expects bold text (**name**) rather than a heading. Constraints on the title template are looser than content — it's for big, readable text only. -->

---
template: content
---

## The content template

`content` is the workhorse — used for most slides. Two slots:

```markdown
template: content

## Heading            ← required, max 1 line, max 80 chars

Body goes here.       ← required, max 12 lines, max 150 words
```

Density limits exist because slides that try to say everything say nothing. The validator tells you when you exceed them.

<!-- Speaker notes: The 12-line and 150-word limits are design guardrails, not arbitrary restrictions. Slides that exceed them typically need to be split. The validator reports all violations at once so you can fix them in one pass. This slide is itself a content template slide — you're looking at one right now. -->

---
template: content
---

## section-title and closing

`section-title` introduces a new chapter. `closing` ends the deck.

```markdown
template: section-title

## Part Two: Content

Formatting, lists, code, and images
```

```markdown
template: closing

## Thanks for watching

Questions welcome
```

Both work identically to `content` — heading plus optional body. Use them for visual rhythm and to signal transitions.

<!-- Speaker notes: The section-title template renders with a distinct visual treatment — typically a full-bleed background color or image — that signals a major transition. The closing template does the same for the final slide. Both are good hooks for per-template background images in a custom theme. -->

---
template: content
---

## The two-column template

Split a slide into two independent columns with `---column---`:

```markdown
template: two-column

## Heading

Left column content goes here.

---column---

Right column content goes here.
```

Each column has its own density limits: max 10 lines, 75 words. Use for comparisons, before/after, pros/cons, or parallel steps.

<!-- Speaker notes: The ---column--- delimiter must appear on its own line. Everything before it is the left column; everything after is the right column. The next slide demonstrates the two-column template in action — this slide is a content template so the code block above can safely contain ---column--- as literal text. -->

---
template: two-column
---

## Two-column in action: before / after

**Single-threaded approach:**

```scala
// Process items one by one
def processAll(items: List[Item]) =
  items.foreach(process)
// Simple but slow for large lists
```

Each item is processed sequentially. Fine for small lists; becomes a bottleneck at scale.

---column---

**Parallel approach:**

```scala
// Process items concurrently
def processAll(items: List[Item]) =
  items.parTraverse(process)
// Cats Effect parallel execution
```

Each item is processed concurrently using the effect system's parallel execution primitive.

<!-- Speaker notes: This is an actual two-column slide — two columns side by side. The left shows sequential processing, the right shows the parallel equivalent using Cats Effect parTraverse. Two-column layout is ideal for before/after and comparison slides like this one. -->

---
template: content
---

## The diagram template

`diagram` renders a Mermaid diagram with an optional caption:

```markdown
template: diagram
caption: System overview

## Heading

` ` `mermaid
graph TD
    A[Client] --> B[Server]
    B --> C[(Database)]
` ` `
```

Mermaid supports flowcharts, sequence diagrams, class diagrams, Gantt charts, pie charts, ER diagrams, and state diagrams.

<!-- Speaker notes: The diagram template is optimized for Mermaid diagrams — it provides extra horizontal space and renders the diagram at full width. The caption appears below the diagram. Mermaid is loaded from CDN so diagrams render without any build step. -->

---
template: section-title
---

## Content

Formatting, lists, code blocks, and images

<!-- Speaker notes: This section demonstrates the content types that work inside any template body. All standard CommonMark inline elements are supported: bold, italic, inline code, links, and strikethrough. Block elements include lists (ordered, unordered, nested), code blocks with syntax highlighting, and images. -->

---
template: two-column
---

## Inline formatting: you write / you get

```markdown
**Bold** text
*Italic* text
`Inline code`
[Link](https://example.com)
~~Strikethrough~~
```

These are standard CommonMark.
All work inside body and columns.

---column---

**Bold** text
*Italic* text
`Inline code`
[Link](https://github.com/TJMSolns/MD-Slides)
~~Strikethrough~~

The left column shows the markdown.
The right column is that same markdown rendered.

<!-- Speaker notes: All standard CommonMark inline elements are supported. The same formatting works in title subtitles, content bodies, column content, and speaker notes (though notes are plain text in the speaker view panel). -->

---
template: two-column
---

## Lists: you write / you get

```markdown
- First item
- Second item
  - Nested level 2
    - Nested level 3

1. Ordered first
2. Ordered second
   - Mixed with unordered
```

---column---

- First item
- Second item
  - Nested level 2
    - Nested level 3

1. Ordered first
2. Ordered second
   - Mixed with unordered

<!-- Speaker notes: Lists support up to 3 levels of nesting. Ordered and unordered lists can be mixed at nested levels. List style (disc, circle, square) is controlled by the theme CSS. Ordered lists maintain their source numbering. -->

---
template: content
---

## Code blocks: Scala

Fenced code blocks with a language name get full syntax highlighting:

```scala
case class Slide(id: SlideId, template: Template, slots: Map[SlotName, SlotContent])

object SlideDeck:
  def validated(slides: List[Slide]): Either[NonEmptyList[ValidationError], SlideDeck] =
    Either.cond(
      slides.nonEmpty,
      SlideDeck(slides),
      NonEmptyList.one(ValidationError("Empty deck"))
    )
```

This is real domain code from MD-Slides — pure Scala 3, no I/O.

<!-- Speaker notes: Code blocks use highlight.js for client-side syntax highlighting. The language name after the opening triple backtick determines the highlighter. Supported languages include Scala, Java, Python, JavaScript, TypeScript, Bash, SQL, JSON, YAML, and 190+ others. Code blocks scale automatically to fit the slide width. -->

---
template: two-column
---

## Code blocks: Python and Bash

```python
def contrast_ratio(fg, bg):
    l1 = luminance(fg)
    l2 = luminance(bg)
    lighter = max(l1, l2)
    darker  = min(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)

# WCAG AA: 4.5:1 for normal text
assert contrast_ratio('#000', '#FFF') >= 4.5
```

---column---

```bash
# Download the JAR
curl -L https://github.com/TJMSolns/\
  MD-Slides/releases/latest/\
  download/md-slides.jar \
  -o md-slides.jar

# Render a presentation
java -jar md-slides.jar render \
  my-talk --theme dark
```

<!-- Speaker notes: Any language supported by highlight.js works — just use the correct language identifier after the opening triple backtick. The highlighting is fully theme-aware: the light theme uses a light code background and the dark theme uses a dark code background. -->

---
template: content
---

## Images

Images embed with standard markdown syntax:

```markdown
![MD-Slides logo](./images/logo.svg)
```

MD-Slides copies image files to the output directory automatically. Alt text is required — the validator flags missing alt text as an accessibility error.

For fully self-contained files (no image folder needed), embed images as base64 data URLs:

```markdown
![Logo](data:image/svg+xml;base64,PHN2ZyB4bWxucy...)
```

<!-- Speaker notes: Image files referenced by path are copied to the output directory during render. Base64 data URLs are embedded inline — useful for single-file distributions like this feature tour. Alt text validates against WCAG 2.1 accessibility requirements. Missing alt text is reported as a validation error and blocks rendering. -->

---
template: content
---

## Images in action

The MD-Slides logo, embedded as a base64 data URL — no separate image file needed:

![MD-Slides logo](data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMDAgODAiIHdpZHRoPSIyMDAiIGhlaWdodD0iODAiPgogIDxyZWN0IHdpZHRoPSIyMDAiIGhlaWdodD0iODAiIHJ4PSI4IiBmaWxsPSIjMWUxZTFlIi8+CiAgPHRleHQgeD0iMjAiIHk9IjMwIiBmb250LWZhbWlseT0ibW9ub3NwYWNlIiBmb250LXNpemU9IjEzIiBmaWxsPSIjNGVjOWIwIj5NRDwvdGV4dD4KICA8dGV4dCB4PSI0OCIgeT0iMzAiIGZvbnQtZmFtaWx5PSJtb25vc3BhY2UiIGZvbnQtc2l6ZT0iMTMiIGZpbGw9IiNkNGQ0ZDQiPi1TbGlkZXM8L3RleHQ+CiAgPHRleHQgeD0iMjAiIHk9IjUyIiBmb250LWZhbWlseT0ibW9ub3NwYWNlIiBmb250LXNpemU9IjEwIiBmaWxsPSIjNmE5OTU1Ii8vIG1hcmtkb3duIOKGkiBwcmVzZW50YXRpb25zPC90ZXh0PgogIDxyZWN0IHg9IjIwIiB5PSI2MCIgd2lkdGg9IjE2MCIgaGVpZ2h0PSIyIiByeD0iMSIgZmlsbD0iIzU2OWNkNiIvPgo8L3N2Zz4=)

The same technique makes `feature-tour.md` downloadable as a single file — no companion assets.

<!-- Speaker notes: This logo is the same SVG from examples/images/logo.svg, converted to a base64 data URL. The render pipeline supports both: file-referenced images are copied to the output directory, and data URLs are passed through unchanged. This slide demonstrates that images fully work in the rendered presentation. -->

---
template: section-title
---

## Speaker View

Notes, next-slide preview, and elapsed timer

<!-- Speaker notes: Speaker view opens in a separate window synchronized to the main presentation via BroadcastChannel. The speaker window shows: current slide (small), next slide heading, speaker notes for the current slide, and an elapsed timer. Navigation in either window keeps both in sync. -->

---
template: content
---

## Adding speaker notes

Add notes to any slide with an HTML comment:

```markdown
<!-- Speaker notes: The key point here is X. Don't forget to mention Y. -->
```

Notes appear only in speaker view — never in the main presentation. They can be as long as needed. The comment can appear anywhere in the slide body.

Press **S** during the presentation to open speaker view in a new window. Navigation in either window keeps both synchronized.

<!-- Speaker notes: This is a speaker note. You're reading it right now in speaker view. Notes support plain text — markdown formatting inside the comment is not rendered. The BroadcastChannel API keeps main and speaker windows synchronized without any server. -->

---
template: content
---

## Speaker view layout

The speaker window shows four panels:

1. **Current slide** — small preview of what the audience sees
2. **Next slide heading** — what's coming so you can bridge smoothly
3. **Speaker notes** — your notes for the current slide
4. **Elapsed timer** — tracks how long you've been presenting

The timer starts on first navigation and pauses during break mode. Press **T** to pause or resume manually; press **R** to reset to 00:00:00.

The timer value can be displayed in slide headers and footers via the `{{timer}}` placeholder.

<!-- Speaker notes: The speaker view timer is separate from any header/footer timer display. The header/footer shows the same value. Press T to pause the timer if you need to stop the clock without entering full break mode. Press R to reset if you're practicing and want to start the clock fresh. -->

---
template: section-title
---

## Navigation

Every keyboard shortcut

<!-- Speaker notes: MD-Slides provides a full keyboard-driven navigation model. Basic arrow-key navigation, direct-jump goto, browser-like history, break mode, speaker view, and timer controls — all available without touching the mouse. -->

---
template: content
---

## Keyboard shortcuts — complete reference

| Key | Action |
|-----|--------|
| `→` / `Space` | Next slide |
| `←` | Previous slide |
| `Home` / `End` | First / last slide |
| `S` | Open speaker view |
| `B` | Toggle break mode (hides slides) |
| `G` | Goto: type slide number, Enter |
| `P` / `N` | History back / forward |
| `T` / `R` | Pause / reset timer |

<!-- Speaker notes: All shortcuts work in both the main presentation window and the speaker view window. Break mode (B) hides the slides from the audience — useful for Q&A or breaks. The timer pauses automatically during break mode. P and N provide browser-like back/forward navigation across non-linear jumps. -->

---
template: content
---

## Break mode

Press **B** to hide slides from the audience while you take a break or answer questions.

During break mode:
- The audience sees a blank or custom break screen
- The timer pauses automatically
- Your speaker view still shows your current slide and notes
- Press **B** again to return to the presentation

Configure a custom break screen image in your theme:

```json
{
  "breakScreen": "images/break-screen.png"
}
```

<!-- Speaker notes: Break mode is essential for Q&A sessions. While the audience sees a neutral screen, you can still see your current slide and notes in speaker view. The timer pause means break time doesn't count against your presentation time — your per-slide pacing data stays accurate. -->

---
template: content
---

## Goto and navigation history

**Goto (G):** Press `G`, type a slide number, press Enter. Jumps directly to that slide. Useful when an audience member asks about a specific slide.

**History navigation (P/N):**

`P` — navigate to the previously visited slide (browser-like back). Supports non-linear presentations and Q&A jumps.

`N` — navigate forward through history (browser-like redo), or advances linearly if no forward history exists.

The history stack records your full navigation path through the session, including goto jumps.

<!-- Speaker notes: Goto uses 1-indexed slide numbers matching the slide counter visible in the corner. The history stack (P/N) is separate from linear navigation — P takes you back through your actual navigation path, not just to the previous numbered slide. This is valuable for non-linear presentations where you skip around or revisit earlier content. -->

---
template: section-title
---

## CLI Commands

render, display, report, and smart default

<!-- Speaker notes: MD-Slides has four CLI commands, though you'll use render the most. The display command starts a tracked session. Report analyzes past sessions. The smart default (no subcommand) picks render or display automatically based on context. -->

---
template: content
---

## render — convert and open

`render` converts a markdown file to HTML:

```bash
java -jar md-slides.jar render my-talk --theme dark
```

Output goes to `my-talk/` — a self-contained directory containing `index.html`, `speaker.html`, and any copied image assets.

The deck name argument is path-flexible:
- `my-talk` finds `my-talk.md` or `my-talk.markdown`
- `talks/my-talk` looks in a subdirectory
- Full paths work too

<!-- Speaker notes: The render command validates before rendering. If validation fails, all errors are printed together and no output is written. Fix all the errors and run again. The output directory is created if it doesn't exist. Existing output is overwritten without warning. -->

---
template: two-column
---

## display — tracked session

```bash
java -jar md-slides.jar display my-talk
```

Opens the presentation **and logs events** to `my-talk/deck.log`:

- Navigation events (with method)
- Timer start / pause / resume
- Break mode toggles
- Session start and end

---column---

```bash
# After presenting, analyze the log
java -jar md-slides.jar report my-talk
```

**Report shows:**
- Total presentation time
- Per-slide time spent
- Navigation path
- Break duration

Use `report` to understand pacing and improve future presentations.

<!-- Speaker notes: The display command is identical to render in every way, but additionally enables session logging to deck.log. The report command reads that log and generates analytics. Over multiple presentations you can track improvement in pacing and identify which slides consistently run long. -->

---
template: content
---

## Smart default — no subcommand needed

MD-Slides is smart about what you want:

```bash
# These all do what you expect:
java -jar md-slides.jar my-talk            # → render
java -jar md-slides.jar my-talk.md         # → render
java -jar md-slides.jar my-talk --display  # → display with logging
```

If the output directory already exists and contains a `deck.log`, the smart default switches to `display` automatically — it assumes you're presenting, not just rendering.

<!-- Speaker notes: The smart default reduces friction for the common case. Most of the time you just want to render and open. The --display flag explicitly opts into session logging. The auto-detection based on deck.log presence is a convenience for repeat presentations of the same deck. -->

---
template: section-title
---

## Themes and Configuration

Built-in themes, custom themes, and four-layer config

<!-- Speaker notes: MD-Slides ships with two themes — light and dark. Custom themes are JSON files that override any visual property. Configuration is layered: CLI flags override project config, which overrides global config, which overrides built-in defaults. -->

---
template: content
---

## Built-in themes

MD-Slides ships with two themes:

```bash
java -jar md-slides.jar render my-talk --theme light  # default
java -jar md-slides.jar render my-talk --theme dark
```

The **light** theme: white background, dark text, blue accents — clean and professional for most venues.

The **dark** theme: dark background, light text, teal accents — high contrast for dark rooms and screen sharing.

Both themes apply consistent syntax highlighting, slide counters, and speaker view styling.

<!-- Speaker notes: The light theme is the default when no theme is specified. The dark theme is recommended for conference rooms with poor lighting or for screen-sharing where a dark background reads better on compressed video. Both themes pass WCAG 2.1 AA contrast requirements. -->

---
template: two-column
---

## Custom themes: JSON structure

```json
{
  "name": "mytheme",
  "version": "1.0.0",
  "background": { "color": "#ffffff" },
  "colors": {
    "text": "#333333",
    "heading": "#000000",
    "accent": "#0066cc",
    "codeBackground": "#f5f5f5",
    "codeText": "#333333"
  },
  "fonts": {
    "body": "Arial, sans-serif",
    "heading": "Arial, sans-serif",
    "code": "monospace"
  }
}
```

---column---

```json
  "spacing": {
    "slideMargin": "2rem",
    "headingMargin": "1rem 0",
    "lineHeight": "1.6"
  },
  "syntax": {
    "keyword": "#0000ff",
    "string": "#00aa00",
    "comment": "#888888",
    "function": "#aa00aa"
  },
  "slideCounter": {
    "color": "#666666",
    "fontSize": "0.9rem"
  }
}
```

Save as `mytheme/theme.json`. Use with `--theme ./mytheme/theme.json`.

<!-- Speaker notes: Custom themes can override any visual property. You don't need to specify everything — unspecified properties fall back to the built-in defaults. The breakScreen key in the theme JSON sets the image displayed during break mode. Per-template background images are configured under templateConfigurations in the theme JSON. -->

---
template: content
---

## Per-template backgrounds and header/footer

Themes support per-template background images:

```json
{
  "templateConfigurations": [
    {
      "template": "section-title",
      "background": { "image": "images/section-bg.png" },
      "header": "My Talk — {{pageNumber}}/{{totalPages}}",
      "footer": "{{timer}}"
    }
  ]
}
```

Available header/footer tokens: `{{pageNumber}}`, `{{totalPages}}`, `{{timer}}`, `{{date}}`

Headers and footers can also be set per-slide in frontmatter.

<!-- Speaker notes: Per-template configuration lets section-title slides have a dramatic background image while content slides stay clean. The header/footer tokens are resolved at runtime — pageNumber and totalPages update as you navigate, and timer updates each second. Headers and footers configured in the theme apply to all slides of that template type. Per-slide frontmatter overrides the theme setting for individual slides. -->

---
template: content
---

## Four-layer configuration

MD-Slides applies configuration in priority order (highest wins):

1. **CLI flags** — `--theme dark`, `--output-dir dist`
2. **Project config** — `.mdslides/config.json` committed with your repo
3. **Global config** — `~/.mdslides/config.json` for personal preferences
4. **Built-in defaults** — always present as the base

```json
{
  "theme": "dark",
  "outputDir": "dist"
}
```

Commit the project config to version control so everyone on the team renders the same way.

<!-- Speaker notes: The four-layer hierarchy solves a real problem: you want team-wide defaults (project config), personal overrides (global config), and per-run overrides (CLI flags). A conference organizer can commit a project config with the house theme so every speaker gets it automatically. A developer can have dark theme in their global config without affecting the project default. -->

---
template: content
---

## Vertical alignment

Content position on a slide is configurable per-slide via frontmatter:

```markdown
template: content
vertical-align: top
```

Three options:
- `top` — content flush to the top of the slide
- `center` — content centered vertically (default)
- `bottom` — content flush to the bottom

Useful when mixing dense and sparse slides — `top` prevents centered text from looking visually unbalanced on a sparse slide.

<!-- Speaker notes: Vertical alignment affects the main content area. Headers and footers are positioned independently. The center default works well for most slides. Use top for slides with code blocks or tables that benefit from predictable position, and for dense slides where centering would push content off-screen. -->

---
template: section-title
---

## Validation

All errors collected and shown at once

<!-- Speaker notes: MD-Slides validates every slide before rendering anything. Rather than stopping at the first error, it collects all validation errors and reports them together. This means you see every problem in one pass and can fix them all before running again. -->

---
template: content
---

## What gets validated

MD-Slides checks every slide before rendering:

**Structure:** required slots present, template declared, frontmatter well-formed

**Density:** heading within 80 characters; body within 12 lines and 150 words; columns within 10 lines and 75 words per column

**Accessibility:** images have alt text; contrast ratios meet WCAG 2.1 AA

**Template-specific:** title H1 within 2 lines; subtitle within 2 lines; author within 80 characters

<!-- Speaker notes: Validation is not optional — every render runs all checks. The --skip-accessibility flag bypasses only the WCAG contrast checks, not structural or density validation. This means you always get feedback on content density before the audience sees your slides. -->

---
template: content
---

## Validation output: all errors at once

When validation fails, all errors are shown together:

```
✗ Validation failed:
  - Slide 3: body exceeds max 12 lines (has 17)
  - Slide 5: heading exceeds max 80 characters (has 94)
  - Slide 7: image is missing alt text
  - Slide 12: two-column left exceeds max 10 lines (has 13)
```

Fix all of them, then render again. No output is written until every slide passes.

<!-- Speaker notes: The error accumulation design — Either[NonEmptyList[ValidationError], SlideDeck] in Scala 3 — is a deliberate architectural choice. Stopping at the first error would mean a loop of: fix, render, find next error, fix, render. Showing everything at once respects your time. -->

---
template: closing
---

## Every feature, in one deck

MD-Slides: Markdown → self-contained HTML presentations

**All templates** · title, content, section-title, two-column, diagram, closing
**All content types** · formatting, lists, code (190+ languages), images
**Navigation** · arrows, goto (G), history (P/N), break (B), timer (T/R)
**Speaker view** · notes, next-slide preview, elapsed timer, synchronized
**Themes** · light, dark, custom JSON, per-template backgrounds, tokens
**Config** · 4-layer: CLI → project → global → defaults
**Validation** · all errors at once, renders only when clean

<!-- Speaker notes: This is the last slide. The feature tour exercises every template, every content type, every keyboard shortcut, and every configuration option. It's also self-contained — the file you downloaded is the same file that produced this presentation. Start your own deck with: java -jar md-slides.jar render my-talk --theme light -->
