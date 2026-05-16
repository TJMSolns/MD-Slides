# Examples

## feature-tour

A 20-slide deck that exercises every MD-Slides feature:

- All six templates: `title`, `content`, `section-title`, `two-column`, `closing`, `diagram`
- Text formatting: bold, italic, inline code, links, strikethrough
- Lists: ordered, unordered, nested (2 levels)
- Code blocks with syntax highlighting (Scala, Python, Bash)
- Images: local SVG files with alt text
- Speaker notes on every slide — press `S` to open speaker view
- Themes, configuration, and validation explained in context

**Render it:**

```bash
java -jar md-slides.jar render examples/feature-tour --theme dark
open examples/feature-tour/index.html
```

## hello-world

A minimal 3-slide deck showing the basic structure. Good starting point for your first presentation.

```bash
java -jar md-slides.jar render examples/hello-world --theme light
open examples/hello-world/index.html
```
