# Examples

## feature-tour

MD-Slides' tutorial, acceptance test and [live demo](https://www.tjm.solutions/mdslides/demo/): 51 slides that use every feature, each with a **Try it** step. `docs/FEATURES.md` maps every feature to its slide.

- All six templates: `title`, `content`, `section-title`, `two-column`, `closing`, `diagram`
- Text formatting, lists, tables, code (including a long listing), images, Mermaid diagrams
- Speaker notes on every slide — press `S` for speaker view
- Touch, clickers, fullscreen, fit-to-screen and `#N` links
- Goto, history, break mode, timer and the session log (`L`, then `report`)
- Themes, configuration and validation, explained in context

It is a single file (images are inline), so it renders anywhere.

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
