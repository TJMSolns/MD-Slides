# ADR-006: Rendering Architecture

**Status**: Accepted
**Date**: 2024-12-20
**Deciders**: Tony Moores (Architect, Bench Developer)
**Related Ceremony**: US-016 (HTML Rendering)

---

## Context

After validation, SlideDeck must be rendered to HTML for browser viewing. Requirements:
- **Type-Safe HTML**: Prevent invalid HTML generation
- **Markdown Rendering**: Body content contains markdown (bold, italics, lists, code blocks)
- **Theme Application**: Apply theme colors, fonts, layout as CSS
- **Standalone Output**: Single HTML file, no external dependencies (CSS/JS inlined)
- **Keyboard Navigation**: Arrow keys, Space, Home, End to navigate slides
- **Slide Counter**: Display "current / total" (e.g., "3 / 10")

**Technical Challenges**:
1. How to generate type-safe HTML? (avoid string concatenation)
2. How to render markdown within slides?
3. How to inline CSS/JS for standalone output?
4. How to implement navigation without external libraries?

---

## Decision

**Use Scalatags + Flexmark** with standalone HTML architecture:

### Scalatags for HTML Generation
```scala
import scalatags.Text.all._

def renderSlideDeck(deck: SlideDeck, theme: Theme): String = {
  val slideHtml = deck.slides.map(slide => renderSlide(slide, theme))
  val themeCSS = generateThemeCSS(theme)
  val navigationJS = generateNavigationJS(deck.slides.length)

  html(
    head(
      meta(charset := "UTF-8"),
      meta(name := "viewport", content := "width=device-width, initial-scale=1.0"),
      tag("title")(deck.metadata.title.getOrElse("Slide Deck")),
      tag("style")(raw(themeCSS))  // Inline CSS
    ),
    body(
      div(cls := "slide-container")(slideHtml),
      div(cls := "slide-counter", id := "counter")("1 / " + deck.slides.length),
      tag("script")(raw(navigationJS))  // Inline JS
    )
  ).render
}
```

### Flexmark for Markdown Rendering
```scala
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser

def renderMarkdown(markdown: String): String = {
  val parser = Parser.builder().build()
  val renderer = HtmlRenderer.builder().build()
  val document = parser.parse(markdown)
  renderer.render(document)
}

def renderSlide(slide: Slide, theme: Theme): Tag = {
  slide.template match {
    case "title" =>
      div(cls := "slide title-slide")(
        h1(slide.getSlot("title").getOrElse("")),
        slide.getSlot("subtitle").map(s => h2(s)),
        p(cls := "author")(slide.getSlot("author").getOrElse(""))
      )

    case "content" =>
      div(cls := "slide content-slide")(
        h2(slide.getSlot("heading").getOrElse("")),
        div(cls := "body")(
          raw(renderMarkdown(slide.getSlot("body").getOrElse("")))
        )
      )
  }
}
```

### Standalone HTML (CSS/JS Inlined)
- **CSS**: Generated from theme, inlined into `<style>` tag
- **JavaScript**: Navigation logic inlined into `<script>` tag
- **No External Files**: HTML works offline, no CDN dependencies

---

## Consequences

### Positive

1. **Type Safety**: Scalatags prevents invalid HTML (e.g., `div` inside `span` caught at compile time)
2. **Composability**: HTML fragments compose like regular Scala code
3. **Markdown Support**: Flexmark handles markdown → HTML conversion
4. **Standalone Output**: Single HTML file, no external dependencies
5. **Offline Capable**: Works without internet (presentations on planes, etc.)
6. **Simple Deployment**: Email HTML file, works in any browser
7. **Version Control Friendly**: Single file, easy to diff changes
8. **Security**: No external scripts (no XSS from CDN compromise)

### Negative

1. **File Size**: Inlined CSS/JS increases HTML size (~10-20 KB overhead)
2. **Cache Inefficiency**: Browser can't cache CSS/JS across decks
3. **No Hot Reloading**: Changes require full regeneration
4. **Limited Interactivity**: JavaScript must be inline (no npm packages)

### Risks

1. **Risk**: Scalatags HTML escaping breaks markdown rendering
   - **Mitigation**: Use `raw()` for Flexmark output (already HTML-escaped)
2. **Risk**: Large decks (200 slides) → large HTML files
   - **Mitigation**: Target < 100 KB/slide (50-slide deck = 5 MB, acceptable)
3. **Risk**: Browser compatibility issues
   - **Mitigation**: Use vanilla JS (ES5), test on Chrome, Firefox, Safari, Edge

---

## Alternatives Considered

### Alternative A: Template Engine (Mustache, Handlebars)
```scala
val template = """
  <div class="slide">
    <h1>{{title}}</h1>
    <p>{{body}}</p>
  </div>
"""
```
**Why Rejected**:
- No type safety (typos caught at runtime)
- String-based templates hard to compose
- Additional dependency
- Doesn't prevent invalid HTML

### Alternative B: React/Vue (Generate Static HTML)
```scala
// Use React SSR to generate HTML
```
**Why Rejected**:
- Massive dependency (React runtime)
- Overkill for static HTML generation
- No compile-time type checking
- Harder to inline CSS/JS

### Alternative C: Plain String Concatenation
```scala
def renderSlide(slide: Slide): String = {
  s"""
    <div class="slide">
      <h1>${slide.title}</h1>
      <p>${slide.body}</p>
    </div>
  """
}
```
**Why Rejected**:
- No type safety
- Prone to XSS (forgetting to escape)
- No HTML structure validation
- Hard to maintain complex HTML

### Alternative D: External CSS/JS Files
```html
<link rel="stylesheet" href="theme.css">
<script src="navigation.js"></script>
```
**Why Rejected**:
- Requires distributing multiple files (not standalone)
- Doesn't work offline without all files
- Harder to deploy (email, USB stick)
- Breaks if files missing

### Alternative E: Custom HTML DSL
```scala
def renderSlide(slide: Slide): Html = {
  Html.div(className = "slide")(
    Html.h1(slide.title),
    Html.p(slide.body)
  )
}
```
**Why Rejected**:
- Reinventing the wheel (Scalatags already exists)
- More code to maintain
- Fewer features than Scalatags

---

## Implementation Notes

### Theme CSS Generation

```scala
def generateThemeCSS(theme: Theme): String = {
  s"""
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }

    body {
      font-family: ${theme.typography.bodyFont};
      font-size: ${theme.typography.bodySize};
      color: ${theme.colors.text};
      background: ${theme.colors.background};
      overflow: hidden;
    }

    .slide-container {
      width: ${theme.layout.slideWidth};
      height: ${theme.layout.slideHeight};
      position: relative;
    }

    .slide {
      width: 100%;
      height: 100%;
      padding: ${theme.layout.padding};
      display: none;
      flex-direction: column;
      justify-content: center;
    }

    .slide.active {
      display: flex;
    }

    .title-slide h1 {
      font-family: ${theme.typography.titleFont};
      font-size: ${theme.typography.titleSize};
      color: ${theme.colors.heading};
      text-align: center;
    }

    .content-slide h2 {
      font-size: calc(${theme.typography.titleSize} * 0.75);
      color: ${theme.colors.heading};
      margin-bottom: 20px;
    }

    .slide-counter {
      position: fixed;
      bottom: 20px;
      right: 20px;
      font-size: 18px;
      color: ${theme.colors.text};
    }

    code {
      font-family: ${theme.typography.codeFont};
      font-size: ${theme.typography.codeSize};
      background: ${theme.colors.code};
      padding: 2px 6px;
      border-radius: 3px;
    }

    pre {
      background: ${theme.colors.code};
      padding: 20px;
      border-radius: 5px;
      overflow: auto;
    }
  """
}
```

### Navigation JavaScript

```scala
def generateNavigationJS(slideCount: Int): String = {
  s"""
    let currentSlide = 0;
    const slides = document.querySelectorAll('.slide');
    const counter = document.getElementById('counter');

    function showSlide(index) {
      slides.forEach((slide, i) => {
        slide.classList.toggle('active', i === index);
      });
      counter.textContent = (index + 1) + ' / ' + ${slideCount};
    }

    function nextSlide() {
      if (currentSlide < slides.length - 1) {
        currentSlide++;
        showSlide(currentSlide);
      }
    }

    function prevSlide() {
      if (currentSlide > 0) {
        currentSlide--;
        showSlide(currentSlide);
      }
    }

    function firstSlide() {
      currentSlide = 0;
      showSlide(currentSlide);
    }

    function lastSlide() {
      currentSlide = slides.length - 1;
      showSlide(currentSlide);
    }

    document.addEventListener('keydown', (e) => {
      switch(e.key) {
        case 'ArrowRight':
        case ' ':
          nextSlide();
          break;
        case 'ArrowLeft':
          prevSlide();
          break;
        case 'Home':
          firstSlide();
          break;
        case 'End':
          lastSlide();
          break;
      }
    });

    // Show first slide on load
    showSlide(0);
  """
}
```

### HTML Escaping Strategy

```scala
// Scalatags auto-escapes text
h1("Title with <html> & \"quotes\"")
// Renders: <h1>Title with &lt;html&gt; &amp; &quot;quotes&quot;</h1>

// Flexmark output is already HTML-escaped, use raw()
div(cls := "body")(
  raw(renderMarkdown("**Bold** and `code`"))
)
// Renders: <div class="body"><strong>Bold</strong> and <code>code</code></div>
```

### Performance Optimization

```scala
// Pre-compile theme CSS (not per-slide)
val themeCSS = generateThemeCSS(theme)  // Once

// Render slides in parallel (for large decks)
import cats.effect.IO
import cats.syntax.all._

def renderSlidesParallel(slides: List[Slide], theme: Theme): IO[List[Tag]] = {
  slides.parTraverse { slide =>
    IO(renderSlide(slide, theme))
  }
}
```

### Testing Strategy

```scala
class RenderingSpec extends munit.FunSuite {
  test("title slide renders correctly") {
    val slide = Slide(
      template = "title",
      slots = Map(
        "title" -> "My Presentation",
        "subtitle" -> "A Great Talk",
        "author" -> "John Doe"
      )
    )

    val html = renderSlide(slide, Theme.default).render
    assert(html.contains("<h1>My Presentation</h1>"))
    assert(html.contains("<h2>A Great Talk</h2>"))
    assert(html.contains("<p class=\"author\">John Doe</p>"))
  }

  test("markdown rendered in body") {
    val slide = Slide(
      template = "content",
      slots = Map(
        "heading" -> "Heading",
        "body" -> "**Bold** and *italics*"
      )
    )

    val html = renderSlide(slide, Theme.default).render
    assert(html.contains("<strong>Bold</strong>"))
    assert(html.contains("<em>italics</em>"))
  }

  test("HTML special characters escaped") {
    val slide = Slide(
      template = "content",
      slots = Map(
        "heading" -> "<script>alert('xss')</script>",
        "body" -> "Test & \"quotes\""
      )
    )

    val html = renderSlide(slide, Theme.default).render
    assert(!html.contains("<script>"))
    assert(html.contains("&lt;script&gt;"))
    assert(html.contains("&amp;"))
    assert(html.contains("&quot;"))
  }
}
```

### W3C HTML5 Validation

```scala
// Integration test (requires network)
test("generated HTML passes W3C validator") {
  val deck = SlideDeck(/* ... */)
  val html = renderSlideDeck(deck, Theme.default)

  // POST to https://validator.w3.org/nu/
  val response = validateHTML(html)
  assertEquals(response.errors.length, 0)
}
```

---

**ADR Type**: Architectural Pattern
**Impact**: Infrastructure layer (rendering module)
**Reversibility**: Medium (refactoring possible but affects all rendering code)
**Validation**: Validated in ceremony US-016
**Key Decision Rationale**: Scalatags provides type-safe HTML generation, Flexmark handles markdown, standalone output enables offline use
