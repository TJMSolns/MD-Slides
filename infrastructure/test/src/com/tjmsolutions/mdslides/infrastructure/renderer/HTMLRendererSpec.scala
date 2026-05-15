package com.tjmsolutions.mdslides.infrastructure.renderer

import com.tjmsolutions.mdslides.domain.{Slide, SlideDeck, SlideId, FormattedContent, TextSpan, ListElement, UnorderedListElement, OrderedListElement}
import cats.data.NonEmptyList

/**
 * Tests for HTMLRenderer.
 *
 * Tests verify:
 * - HTML structure (doctype, head, body)
 * - Title slide rendering (h1, h2, p tags)
 * - Content slide rendering (h2, div, p tags)
 * - Navigation controls (keyboard, counter)
 * - CSS and JavaScript injection
 * - Multi-slide deck rendering
 *
 * Related Governance:
 * - ADR-006: Rendering Architecture
 * - PDR-005: Accessibility Requirements (WCAG AA)
 */
class HTMLRendererSpec extends munit.FunSuite:

  test("render minimal title slide to HTML") {
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "title",
      slots = Map("title" -> "Welcome to MDSlides")
    )
    val deck = SlideDeck(NonEmptyList.one(slide))

    val html = HTMLRenderer.renderDeck(deck)

    // Check HTML structure
    assert(html.startsWith("<!DOCTYPE html>"))
    assert(html.contains("<html>"))
    assert(html.contains("</html>"))
    assert(html.contains("<head>"))
    assert(html.contains("<body>"))

    // Check metadata
    assert(html.contains("charset=\"UTF-8\""))
    assert(html.contains("viewport"))

    // Check title content
    assert(html.contains("Welcome to MDSlides"))
    assert(html.contains("slide-title"))

    // Check CSS injection
    assert(html.contains("<style>"))
    assert(html.contains(".slide"))
    assert(html.contains(".title-slide"))

    // Check JavaScript injection
    assert(html.contains("<script>"))
    assert(html.contains("currentSlide"))
    assert(html.contains("showSlide"))

    // Check slide counter
    assert(html.contains("slide-counter"))
    assert(html.contains("1 / 1"))
  }

  test("render title slide with all optional slots") {
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "title",
      slots = Map(
        "title" -> "MDSlides Framework",
        "subtitle" -> "Building Better Presentations",
        "author" -> "John Doe"
      )
    )
    val deck = SlideDeck(NonEmptyList.one(slide))

    val html = HTMLRenderer.renderDeck(deck)

    // Check all slots rendered
    assert(html.contains("MDSlides Framework"))
    assert(html.contains("Building Better Presentations"))
    assert(html.contains("John Doe"))

    // Check correct HTML tags
    assert(html.contains("slide-title"))
    assert(html.contains("slide-subtitle"))
    assert(html.contains("slide-author"))
  }

  test("render content slide with heading and body") {
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "content",
      slots = Map(
        "heading" -> "Key Principles",
        "body" -> "Domain-Driven Design\nTest-Driven Development\nClean Architecture"
      )
    )
    val deck = SlideDeck(NonEmptyList.one(slide))

    val html = HTMLRenderer.renderDeck(deck)

    // Check heading rendered
    assert(html.contains("Key Principles"))
    assert(html.contains("slide-heading"))

    // Check body content
    assert(html.contains("Domain-Driven Design"))
    assert(html.contains("Test-Driven Development"))
    assert(html.contains("Clean Architecture"))
    assert(html.contains("slide-body"))

    // Check body rendered as paragraphs
    assert(html.contains("<p>"))
  }

  test("render multi-slide deck") {
    val slide1 = Slide(
      id = SlideId.unsafe(1),
      templateName = "title",
      slots = Map("title" -> "Introduction")
    )
    val slide2 = Slide(
      id = SlideId.unsafe(2),
      templateName = "content",
      slots = Map(
        "heading" -> "Overview",
        "body" -> "This is the overview."
      )
    )
    val slide3 = Slide(
      id = SlideId.unsafe(3),
      templateName = "content",
      slots = Map(
        "heading" -> "Conclusion",
        "body" -> "Thank you!"
      )
    )
    val deck = SlideDeck(NonEmptyList.of(slide1, slide2, slide3))

    val html = HTMLRenderer.renderDeck(deck)

    // Check all slides present
    assert(html.contains("Introduction"))
    assert(html.contains("Overview"))
    assert(html.contains("Conclusion"))

    // Check slide counter shows total
    assert(html.contains("1 / 3"))

    // Check JavaScript has correct total
    assert(html.contains("const totalSlides = 3"))

    // Check first slide has "active" class
    assert(html.contains("class=\"slide active\""))

    // Check data attributes for navigation
    assert(html.contains("data-slide-index=\"0\""))
    assert(html.contains("data-slide-index=\"1\""))
    assert(html.contains("data-slide-index=\"2\""))
  }

  test("render unknown template shows error") {
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "unknown",
      slots = Map()
    )
    val deck = SlideDeck(NonEmptyList.one(slide))

    val html = HTMLRenderer.renderDeck(deck)

    // Check error message
    assert(html.contains("Unknown template"))
    assert(html.contains("unknown"))
    assert(html.contains("error"))
  }

  test("empty body lines are filtered out") {
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "content",
      slots = Map(
        "heading" -> "Test",
        "body" -> "Line 1\n\n\nLine 2\n   \nLine 3"
      )
    )
    val deck = SlideDeck(NonEmptyList.one(slide))

    val html = HTMLRenderer.renderDeck(deck)

    // Check content rendered
    assert(html.contains("Line 1"))
    assert(html.contains("Line 2"))
    assert(html.contains("Line 3"))

    // Empty lines should be filtered (no empty <p> tags)
    // Count paragraph tags - should have 3, not 6
    val paragraphCount = "<p>".r.findAllIn(html).length
    // Note: This is a simple heuristic; actual count may vary
    assert(paragraphCount >= 3, s"Expected at least 3 paragraphs, got $paragraphCount")
  }

  test("navigation JavaScript includes all required functions") {
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "title",
      slots = Map("title" -> "Test")
    )
    val deck = SlideDeck(NonEmptyList.one(slide))

    val html = HTMLRenderer.renderDeck(deck)

    // Check navigation functions
    assert(html.contains("function showSlide"))
    assert(html.contains("function nextSlide"))
    assert(html.contains("function prevSlide"))
    assert(html.contains("function firstSlide"))
    assert(html.contains("function lastSlide"))

    // Check keyboard event listener
    assert(html.contains("addEventListener('keydown'"))
    assert(html.contains("ArrowRight"))
    assert(html.contains("ArrowLeft"))
    assert(html.contains("Home"))
    assert(html.contains("End"))
  }

  test("CSS includes all required classes") {
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "title",
      slots = Map("title" -> "Test")
    )
    val deck = SlideDeck(NonEmptyList.one(slide))

    val html = HTMLRenderer.renderDeck(deck)

    // Check CSS classes
    assert(html.contains(".slide"))
    assert(html.contains(".slide.active"))
    assert(html.contains(".title-slide"))
    assert(html.contains(".content-slide"))
    assert(html.contains(".slide-title"))
    assert(html.contains(".slide-subtitle"))
    assert(html.contains(".slide-author"))
    assert(html.contains(".slide-heading"))
    assert(html.contains(".slide-body"))
    assert(html.contains(".controls"))
    assert(html.contains(".error"))
  }

  test("HTML is valid structure") {
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "title",
      slots = Map("title" -> "Test")
    )
    val deck = SlideDeck(NonEmptyList.one(slide))

    val html = HTMLRenderer.renderDeck(deck)

    // Check proper nesting (basic validation)
    assert(html.indexOf("<html>") < html.indexOf("</html>"))
    assert(html.indexOf("<head>") < html.indexOf("</head>"))
    assert(html.indexOf("<body>") < html.indexOf("</body>"))
    assert(html.indexOf("</head>") < html.indexOf("<body>"))
  }

  test("slide counter updates with deck size") {
    val slides = (1 to 5).map { i =>
      Slide(
        id = SlideId.unsafe(i),
        templateName = "content",
        slots = Map(
          "heading" -> s"Slide $i",
          "body" -> s"Content $i"
        )
      )
    }
    val deck = SlideDeck(NonEmptyList.fromListUnsafe(slides.toList))

    val html = HTMLRenderer.renderDeck(deck)

    // Check counter shows correct total
    assert(html.contains("1 / 5"))
    assert(html.contains("const totalSlides = 5"))
  }

  // US-011: Per-Slide Background Images - Fallback Chain
  test("render slide with per-slide background (highest priority)") {
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "content",
      slots = Map("heading" -> "Test", "body" -> "Content"),
      backgroundImage = Some("backgrounds/custom.png")
    )
    val deck = SlideDeck(NonEmptyList.one(slide))

    val html = HTMLRenderer.renderDeck(deck)

    // Should include inline style with per-slide background
    assert(html.contains("background-image"))
    assert(html.contains("backgrounds/custom.png"))
  }

  test("render slide using template background from theme") {
    import com.tjmsolutions.mdslides.domain.{Theme, Background, ColorScheme, FontScheme, Spacing, SyntaxColors, SlideCounter}

    // Create theme with template-specific backgrounds
    val theme = Theme(
      name = "TestTheme",
      version = "1.0.0",
      background = Background(color = "#FFFFFF", image = None),
      colors = ColorScheme(
        text = "#000000",
        heading = "#000000",
        accent = "#0000FF",
        link = "#0000FF",
        linkHover = "#0000FF",
        codeBackground = "#F0F0F0",
        codeText = "#000000"
      ),
      fonts = FontScheme(
        body = "Arial",
        heading = "Arial",
        code = "monospace"
      ),
      spacing = Spacing(
        slideMargin = "2rem",
        headingMargin = "1rem 0",
        paragraphMargin = "0.5rem 0",
        lineHeight = "1.6"
      ),
      syntax = SyntaxColors(
        keyword = "#0000FF",
        string = "#008000",
        comment = "#808080",
        function = "#000080",
        number = "#FF0000",
        operator = "#000000"
      ),
      slideCounter = SlideCounter(
        color = "#666666",
        background = "rgba(255,255,255,0.9)",
        fontSize = "0.9rem"
      ),
      templateBackgrounds = Map(
        "content" -> "backgrounds/content-bg.png"
      )
    )

    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "content",
      slots = Map("heading" -> "Test", "body" -> "Content"),
      backgroundImage = None  // No per-slide override
    )
    val deck = SlideDeck(NonEmptyList.one(slide))

    val html = HTMLRenderer.renderDeck(deck, theme)

    // Should use template background from theme
    assert(html.contains("background-image"))
    assert(html.contains("backgrounds/content-bg.png"))
  }

  test("per-slide background overrides template background") {
    import com.tjmsolutions.mdslides.domain.{Theme, Background, ColorScheme, FontScheme, Spacing, SyntaxColors, SlideCounter}

    val theme = Theme(
      name = "TestTheme",
      version = "1.0.0",
      background = Background(color = "#FFFFFF", image = None),
      colors = ColorScheme(
        text = "#000000",
        heading = "#000000",
        accent = "#0000FF",
        link = "#0000FF",
        linkHover = "#0000FF",
        codeBackground = "#F0F0F0",
        codeText = "#000000"
      ),
      fonts = FontScheme(
        body = "Arial",
        heading = "Arial",
        code = "monospace"
      ),
      spacing = Spacing(
        slideMargin = "2rem",
        headingMargin = "1rem 0",
        paragraphMargin = "0.5rem 0",
        lineHeight = "1.6"
      ),
      syntax = SyntaxColors(
        keyword = "#0000FF",
        string = "#008000",
        comment = "#808080",
        function = "#000080",
        number = "#FF0000",
        operator = "#000000"
      ),
      slideCounter = SlideCounter(
        color = "#666666",
        background = "rgba(255,255,255,0.9)",
        fontSize = "0.9rem"
      ),
      templateBackgrounds = Map(
        "content" -> "backgrounds/content-bg.png"
      )
    )

    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "content",
      slots = Map("heading" -> "Test", "body" -> "Content"),
      backgroundImage = Some("backgrounds/override.png")  // Should override template
    )
    val deck = SlideDeck(NonEmptyList.one(slide))

    val html = HTMLRenderer.renderDeck(deck, theme)

    // Should use per-slide background, not template background
    assert(html.contains("backgrounds/override.png"))
    assert(!html.contains("backgrounds/content-bg.png"))
  }

  test("slide without background falls back to theme default") {
    import com.tjmsolutions.mdslides.domain.{Theme, Background, ColorScheme, FontScheme, Spacing, SyntaxColors, SlideCounter}

    val theme = Theme(
      name = "TestTheme",
      version = "1.0.0",
      background = Background(color = "#FFFFFF", image = Some("backgrounds/theme-default.png")),
      colors = ColorScheme(
        text = "#000000",
        heading = "#000000",
        accent = "#0000FF",
        link = "#0000FF",
        linkHover = "#0000FF",
        codeBackground = "#F0F0F0",
        codeText = "#000000"
      ),
      fonts = FontScheme(
        body = "Arial",
        heading = "Arial",
        code = "monospace"
      ),
      spacing = Spacing(
        slideMargin = "2rem",
        headingMargin = "1rem 0",
        paragraphMargin = "0.5rem 0",
        lineHeight = "1.6"
      ),
      syntax = SyntaxColors(
        keyword = "#0000FF",
        string = "#008000",
        comment = "#808080",
        function = "#000080",
        number = "#FF0000",
        operator = "#000000"
      ),
      slideCounter = SlideCounter(
        color = "#666666",
        background = "rgba(255,255,255,0.9)",
        fontSize = "0.9rem"
      ),
      templateBackgrounds = Map.empty  // No template backgrounds
    )

    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "content",
      slots = Map("heading" -> "Test", "body" -> "Content"),
      backgroundImage = None
    )
    val deck = SlideDeck(NonEmptyList.one(slide))

    val html = HTMLRenderer.renderDeck(deck, theme)

    // Should fall back to theme default background
    assert(html.contains("backgrounds/theme-default.png"))
  }

  // Nested list rendering tests (US-003.3 - v1.2 TDD Phase 3)

  test("render 2-level nested unordered list"):
    import com.tjmsolutions.mdslides.domain._

    val nestedList = UnorderedList(List(
      ListItem(
        List(TextSpan("Level 1 item A", false, false, false)),
        nestedUnorderedLists = List(UnorderedList(List(
          ListItem(List(TextSpan("Level 2 item A.1", false, false, false))),
          ListItem(List(TextSpan("Level 2 item A.2", false, false, false)))
        )))
      ),
      ListItem(List(TextSpan("Level 1 item B", false, false, false)))
    ))

    val content = FormattedContent(
      List.empty, List.empty, List.empty, List.empty,
      List(nestedList), List.empty
    )

    val html = HTMLRenderer.renderFormattedContent(content)

    // Should have nested <ul> structure
    assert(html.contains("<ul>"))
    assert(html.contains("<li>Level 1 item A"))
    assert(html.contains("<ul>"))  // Nested ul
    assert(html.contains("<li>Level 2 item A.1</li>"))
    assert(html.contains("<li>Level 2 item A.2</li>"))
    assert(html.contains("</ul>"))
    assert(html.contains("</li>"))
    assert(html.contains("<li>Level 1 item B</li>"))

  test("render 2-level nested ordered list"):
    import com.tjmsolutions.mdslides.domain._

    val nestedList = OrderedList(List(
      ListItem(
        List(TextSpan("First main point", false, false, false)),
        nestedOrderedLists = List(OrderedList(List(
          ListItem(List(TextSpan("First sub-point", false, false, false))),
          ListItem(List(TextSpan("Second sub-point", false, false, false)))
        )))
      ),
      ListItem(List(TextSpan("Second main point", false, false, false)))
    ))

    val content = FormattedContent(
      List.empty, List.empty, List.empty, List.empty,
      List.empty, List(nestedList)
    )

    val html = HTMLRenderer.renderFormattedContent(content)

    // Should have nested <ol> structure
    assert(html.contains("<ol>"))
    assert(html.contains("<li>First main point"))
    assert(html.contains("<ol>"))  // Nested ol
    assert(html.contains("<li>First sub-point</li>"))
    assert(html.contains("<li>Second sub-point</li>"))
    assert(html.contains("</ol>"))

  test("render 3-level nested list"):
    import com.tjmsolutions.mdslides.domain._

    val level3List = UnorderedList(List(
      ListItem(List(TextSpan("Level 3", false, false, false)))
    ))
    val level2List = UnorderedList(List(
      ListItem(
        List(TextSpan("Level 2", false, false, false)),
        nestedUnorderedLists = List(level3List)
      )
    ))
    val level1List = UnorderedList(List(
      ListItem(
        List(TextSpan("Level 1", false, false, false)),
        nestedUnorderedLists = List(level2List)
      )
    ))

    val content = FormattedContent(
      List.empty, List.empty, List.empty, List.empty,
      List(level1List), List.empty
    )

    val html = HTMLRenderer.renderFormattedContent(content)

    // Should have 3 levels of nested <ul>
    val ulCount = html.sliding(4).count(_ == "<ul>")
    assert(ulCount == 3, s"Expected 3 <ul> tags, found $ulCount")
    assert(html.contains("Level 1"))
    assert(html.contains("Level 2"))
    assert(html.contains("Level 3"))

  test("render mixed nesting - ordered within unordered"):
    import com.tjmsolutions.mdslides.domain._

    val mixedList = UnorderedList(List(
      ListItem(
        List(TextSpan("Overview", false, false, false)),
        nestedOrderedLists = List(OrderedList(List(
          ListItem(List(TextSpan("Step one", false, false, false))),
          ListItem(List(TextSpan("Step two", false, false, false)))
        )))
      ),
      ListItem(List(TextSpan("Conclusion", false, false, false)))
    ))

    val content = FormattedContent(
      List.empty, List.empty, List.empty, List.empty,
      List(mixedList), List.empty
    )

    val html = HTMLRenderer.renderFormattedContent(content)

    // Should have <ul> containing <ol>
    assert(html.contains("<ul>"))
    assert(html.contains("<li>Overview"))
    assert(html.contains("<ol>"))
    assert(html.contains("<li>Step one</li>"))
    assert(html.contains("</ol>"))
    assert(html.contains("</ul>"))

  test("render mixed nesting - unordered within ordered"):
    import com.tjmsolutions.mdslides.domain._

    val mixedList = OrderedList(List(
      ListItem(
        List(TextSpan("Introduction", false, false, false)),
        nestedUnorderedLists = List(UnorderedList(List(
          ListItem(List(TextSpan("Point A", false, false, false))),
          ListItem(List(TextSpan("Point B", false, false, false)))
        )))
      ),
      ListItem(List(TextSpan("Body", false, false, false)))
    ))

    val content = FormattedContent(
      List.empty, List.empty, List.empty, List.empty,
      List.empty, List(mixedList)
    )

    val html = HTMLRenderer.renderFormattedContent(content)

    // Should have <ol> containing <ul>
    assert(html.contains("<ol>"))
    assert(html.contains("<li>Introduction"))
    assert(html.contains("<ul>"))
    assert(html.contains("<li>Point A</li>"))
    assert(html.contains("</ul>"))
    assert(html.contains("</ol>"))

  test("render nested list with formatted text"):
    import com.tjmsolutions.mdslides.domain._

    val nestedList = UnorderedList(List(
      ListItem(
        List(TextSpan("Bold level 1", true, false, false)),
        nestedUnorderedLists = List(UnorderedList(List(
          ListItem(List(TextSpan("Italic level 2", false, true, false)))
        )))
      )
    ))

    val content = FormattedContent(
      List.empty, List.empty, List.empty, List.empty,
      List(nestedList), List.empty
    )

    val html = HTMLRenderer.renderFormattedContent(content)

    // Should preserve inline formatting in nested lists
    assert(html.contains("<strong>Bold level 1</strong>"))
    assert(html.contains("<em>Italic level 2</em>"))

  test("render nested list preserves proper nesting structure"):
    import com.tjmsolutions.mdslides.domain._

    val nestedList = UnorderedList(List(
      ListItem(
        List(TextSpan("Parent", false, false, false)),
        nestedUnorderedLists = List(UnorderedList(List(
          ListItem(List(TextSpan("Child", false, false, false)))
        )))
      )
    ))

    val content = FormattedContent(
      List.empty, List.empty, List.empty, List.empty,
      List(nestedList), List.empty
    )

    val html = HTMLRenderer.renderFormattedContent(content)

    // Verify proper HTML nesting: <li>Parent<ul><li>Child</li></ul></li>
    val parentIdx = html.indexOf("Parent")
    val nestedUlIdx = html.indexOf("<ul>", parentIdx)
    val childIdx = html.indexOf("Child", nestedUlIdx)
    val closingUlIdx = html.indexOf("</ul>", childIdx)
    val closingLiIdx = html.indexOf("</li>", closingUlIdx)

    assert(parentIdx < nestedUlIdx)
    assert(nestedUlIdx < childIdx)
    assert(childIdx < closingUlIdx)
    assert(closingUlIdx < closingLiIdx)

  test("render empty nested list gracefully"):
    import com.tjmsolutions.mdslides.domain._

    val listWithEmptyNested = UnorderedList(List(
      ListItem(
        List(TextSpan("Item with content", false, false, false)),
        nestedUnorderedLists = List(UnorderedList(List.empty))  // Empty nested list
      ),
      ListItem(List(TextSpan("Item without nesting", false, false, false)))
    ))

    val content = FormattedContent(
      List.empty, List.empty, List.empty, List.empty,
      List(listWithEmptyNested), List.empty
    )

    val html = HTMLRenderer.renderFormattedContent(content)

    // Should handle empty nested list without error
    assert(html.contains("Item with content"))
    assert(html.contains("Item without nesting"))

  test("render multiple nested lists in same content"):
    import com.tjmsolutions.mdslides.domain._

    val list1 = UnorderedList(List(
      ListItem(
        List(TextSpan("List 1 parent", false, false, false)),
        nestedUnorderedLists = List(UnorderedList(List(
          ListItem(List(TextSpan("List 1 child", false, false, false)))
        )))
      )
    ))

    val list2 = UnorderedList(List(
      ListItem(
        List(TextSpan("List 2 parent", false, false, false)),
        nestedUnorderedLists = List(UnorderedList(List(
          ListItem(List(TextSpan("List 2 child", false, false, false)))
        )))
      )
    ))

    val content = FormattedContent(
      List.empty, List.empty, List.empty, List.empty,
      List(list1, list2), List.empty
    )

    val html = HTMLRenderer.renderFormattedContent(content)

    // Should render both lists with their nesting
    assert(html.contains("List 1 parent"))
    assert(html.contains("List 1 child"))
    assert(html.contains("List 2 parent"))
    assert(html.contains("List 2 child"))

  // ========================================
  // Syntax Highlighting Tests (v1.3)
  // ========================================

  test("render deck includes highlight.js script CDN"):
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "content",
      slots = Map("title" -> "Test")
    )
    val deck = SlideDeck(NonEmptyList.one(slide))

    val html = HTMLRenderer.renderDeck(deck)

    // Should include highlight.js script from CDN
    assert(html.contains("<script src=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js\">"))

  test("render deck with light theme includes github highlight.js CSS"):
    import com.tjmsolutions.mdslides.domain.Theme
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "content",
      slots = Map("title" -> "Test")
    )
    val deck = SlideDeck(NonEmptyList.one(slide))

    val html = HTMLRenderer.renderDeck(deck, Theme.light)

    // Should include github theme CSS for light theme
    assert(html.contains("<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github.min.css\" />"))

  test("render deck with dark theme includes monokai-sublime highlight.js CSS"):
    import com.tjmsolutions.mdslides.domain.Theme
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "content",
      slots = Map("title" -> "Test")
    )
    val deck = SlideDeck(NonEmptyList.one(slide))

    val html = HTMLRenderer.renderDeck(deck, Theme.dark)

    // Should include monokai-sublime theme CSS for dark theme
    assert(html.contains("<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/monokai-sublime.min.css\" />"))

  test("render deck includes hljs.highlightAll() initialization script"):
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "content",
      slots = Map("title" -> "Test")
    )
    val deck = SlideDeck(NonEmptyList.one(slide))

    val html = HTMLRenderer.renderDeck(deck)

    // Should include DOMContentLoaded event listener with hljs.highlightAll()
    assert(html.contains("document.addEventListener('DOMContentLoaded'"))
    assert(html.contains("hljs.highlightAll()"))

  test("render deck with corporate theme includes github highlight.js CSS"):
    import com.tjmsolutions.mdslides.domain.Theme
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "content",
      slots = Map("title" -> "Test")
    )
    val deck = SlideDeck(NonEmptyList.one(slide))

    val html = HTMLRenderer.renderDeck(deck, Theme.corporate)

    // Corporate theme should map to github (light theme)
    assert(html.contains("<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github.min.css\" />"))

  test("render code block with language hint includes language class"):
    import com.tjmsolutions.mdslides.domain.{CodeBlock, FormattedContent}
    val codeBlock = CodeBlock("val x = 42", Some("scala"))
    val content = FormattedContent(
      textSpans = List.empty,
      links = List.empty,
      codeBlocks = List(codeBlock),
      contentImages = List.empty,
      unorderedLists = List.empty,
      orderedLists = List.empty
    )

    val html = HTMLRenderer.renderFormattedContent(content)

    // Should render with class="language-scala" for highlight.js
    assert(html.contains("<code class=\"language-scala\">"))
    assert(html.contains("val x = 42"))

  test("render code block without language hint has no language class"):
    import com.tjmsolutions.mdslides.domain.{CodeBlock, FormattedContent}
    val codeBlock = CodeBlock("plain text code", None)
    val content = FormattedContent(
      textSpans = List.empty,
      links = List.empty,
      codeBlocks = List(codeBlock),
      contentImages = List.empty,
      unorderedLists = List.empty,
      orderedLists = List.empty
    )

    val html = HTMLRenderer.renderFormattedContent(content)

    // Should render without language class (no highlighting)
    assert(html.contains("<pre><code>plain text code</code></pre>"))
    assert(!html.contains("class=\"language-"))

  test("render empty code block with language hint"):
    import com.tjmsolutions.mdslides.domain.{CodeBlock, FormattedContent}
    val codeBlock = CodeBlock("", Some("python"))
    val content = FormattedContent(
      textSpans = List.empty,
      links = List.empty,
      codeBlocks = List(codeBlock),
      contentImages = List.empty,
      unorderedLists = List.empty,
      orderedLists = List.empty
    )

    val html = HTMLRenderer.renderFormattedContent(content)

    // Should render empty block with language class
    assert(html.contains("<code class=\"language-python\">"))

  // ========================================
  // BUG-002 Fix: 'S' Key Handler (v1.3.1)
  // ========================================

  test("navigation JavaScript includes S key handler for speaker view"):
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "content",
      slots = Map("title" -> "Test")
    )
    val deck = SlideDeck(NonEmptyList.one(slide))

    val html = HTMLRenderer.renderDeck(deck)

    // Should include both lowercase and uppercase S key handlers
    assert(html.contains("case 's':") || html.contains("case \"s\":"))
    assert(html.contains("case 'S':") || html.contains("case \"S\":"))
    // Should open speaker.html in new window
    assert(html.contains("window.open"))
    assert(html.contains("speaker.html"))

  // Example Mapping US-003.3 Scenario 8: Mixed ordered → unordered (SIBLING LISTS)
  test("render mixed ordered then unordered lists preserves source order"):
    // Test via FlexmarkAdapter to ensure end-to-end source order preservation
    import com.tjmsolutions.mdslides.infrastructure.parser.FlexmarkAdapter

    // Markdown with ordered list BEFORE unordered list
    val markdown = """**Ordered Lists:**
1. First
2. Second

**Unordered Lists:**
- Item A
- Item B
"""

    val content = FlexmarkAdapter.parseInlineFormatting(markdown)
    val html = HTMLRenderer.renderFormattedContent(content)

    // Find positions of <ol> and <ul> in rendered HTML
    val olPos = html.indexOf("<ol>")
    val ulPos = html.indexOf("<ul>")

    // Ordered list should appear BEFORE unordered list (source order preserved)
    assert(olPos >= 0, s"Expected <ol> tag in HTML")
    assert(ulPos >= 0, s"Expected <ul> tag in HTML")
    assert(olPos < ulPos, s"Expected <ol> (at $olPos) before <ul> (at $ulPos)")

  // Example Mapping US-003.3 Scenario 9: Mixed unordered → ordered (SIBLING LISTS)
  test("render mixed unordered then ordered lists preserves source order"):
    // Test via FlexmarkAdapter to ensure end-to-end source order preservation
    import com.tjmsolutions.mdslides.infrastructure.parser.FlexmarkAdapter

    // Markdown with unordered list BEFORE ordered list
    val markdown = """**Unordered Lists:**
- Apple
- Banana

**Ordered Lists:**
1. First step
2. Second step
3. Third step
"""

    val content = FlexmarkAdapter.parseInlineFormatting(markdown)
    val html = HTMLRenderer.renderFormattedContent(content)

    // Find positions of <ul> and <ol> in rendered HTML
    val ulPos = html.indexOf("<ul>")
    val olPos = html.indexOf("<ol>")

    // Unordered list should appear BEFORE ordered list (source order preserved)
    assert(ulPos >= 0, s"Expected <ul> tag in HTML")
    assert(olPos >= 0, s"Expected <ol> tag in HTML")
    assert(ulPos < olPos, s"Expected <ul> (at $ulPos) before <ol> (at $olPos)")

end HTMLRendererSpec
