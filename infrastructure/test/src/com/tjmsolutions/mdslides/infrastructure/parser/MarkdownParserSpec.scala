package com.tjmsolutions.mdslides.infrastructure.parser

import com.tjmsolutions.mdslides.domain.{Slide, SlideDeck}

/**
 * Tests for MarkdownParser.
 *
 * Tests verify:
 * - Parsing title slides (with/without optional slots)
 * - Parsing content slides
 * - Multi-slide decks
 * - Error handling (empty input, unknown templates)
 * - Frontmatter extraction
 * - Markdown structure preservation (not rendering yet)
 *
 * Related Governance:
 * - ADR-006: Rendering Architecture
 * - ADR-007: Anticorruption Layer for Flexmark
 */
class MarkdownParserSpec extends munit.FunSuite:

  test("parse minimal title slide") {
    val markdown = """---
                     |template: title
                     |---
                     |# Welcome to MDSlides""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Right(deck) =>
        assertEquals(deck.slideCount, 1)
        val slide = deck.getSlide(0).get
        assertEquals(slide.templateName, "title")
        assertEquals(slide.getSlot("title"), Some("Welcome to MDSlides"))
        assertEquals(slide.getSlot("subtitle"), None)
        assertEquals(slide.getSlot("author"), None)

      case Left(error) =>
        fail(s"Expected successful parse, got error: $error")
  }

  test("parse title slide with all optional slots") {
    val markdown = """---
                     |template: title
                     |---
                     |# MDSlides Framework
                     |## Building Better Presentations
                     |John Doe""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Right(deck) =>
        val slide = deck.getSlide(0).get
        assertEquals(slide.getSlot("title"), Some("MDSlides Framework"))
        assertEquals(slide.getSlot("subtitle"), Some("Building Better Presentations"))
        assertEquals(slide.getSlot("author"), Some("John Doe"))

      case Left(error) =>
        fail(s"Expected successful parse, got error: $error")
  }

  test("parse content slide") {
    val markdown = """---
                     |template: content
                     |---
                     |## Key Principles
                     |Domain-Driven Design helps us build better software.
                     |Test-Driven Development ensures quality.""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Right(deck) =>
        val slide = deck.getSlide(0).get
        assertEquals(slide.templateName, "content")
        assertEquals(slide.getSlot("heading"), Some("Key Principles"))
        assert(slide.getSlot("body").get.contains("Domain-Driven Design"))
        assert(slide.getSlot("body").get.contains("Test-Driven Development"))

      case Left(error) =>
        fail(s"Expected successful parse, got error: $error")
  }

  test("parse multi-slide deck") {
    val markdown = """---
                     |template: title
                     |---
                     |# Introduction
                     |
                     |---
                     |template: content
                     |---
                     |## Overview
                     |This is the overview.
                     |
                     |---
                     |template: content
                     |---
                     |## Conclusion
                     |Thank you!""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Right(deck) =>
        assertEquals(deck.slideCount, 3)

        // Check first slide (title)
        val slide1 = deck.getSlide(0).get
        assertEquals(slide1.templateName, "title")
        assertEquals(slide1.getSlot("title"), Some("Introduction"))

        // Check second slide (content)
        val slide2 = deck.getSlide(1).get
        assertEquals(slide2.templateName, "content")
        assertEquals(slide2.getSlot("heading"), Some("Overview"))
        assertEquals(slide2.getSlot("body"), Some("This is the overview."))

        // Check third slide (content)
        val slide3 = deck.getSlide(2).get
        assertEquals(slide3.templateName, "content")
        assertEquals(slide3.getSlot("heading"), Some("Conclusion"))
        assertEquals(slide3.getSlot("body"), Some("Thank you!"))

      case Left(error) =>
        fail(s"Expected successful parse, got error: $error")
  }

  test("default template is content when not specified") {
    val markdown = """---
                     |---
                     |## Default Content
                     |This uses default template.""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Right(deck) =>
        val slide = deck.getSlide(0).get
        assertEquals(slide.templateName, "content")
        assertEquals(slide.getSlot("heading"), Some("Default Content"))

      case Left(error) =>
        fail(s"Expected successful parse, got error: $error")
  }

  test("parse empty markdown fails") {
    val result = MarkdownParser.parse("")

    result match
      case Left(error) =>
        assert(error.contains("No slides found"))

      case Right(_) =>
        fail("Expected parsing to fail for empty markdown")
  }

  test("parse unknown template fails") {
    val markdown = """---
                     |template: unknown
                     |---
                     |Some content""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Left(error) =>
        assert(error.contains("Unknown template"))
        assert(error.contains("unknown"))

      case Right(_) =>
        fail("Expected parsing to fail for unknown template")
  }

  test("markdown formatting is preserved (not rendered)") {
    val markdown = """---
                     |template: content
                     |---
                     |## Formatted Content
                     |This has **bold** and *italic* text.
                     |Also a [link](https://example.com).""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Right(deck) =>
        val slide = deck.getSlide(0).get
        val body = slide.getSlot("body").get

        // Markdown should be preserved as-is (not rendered to HTML)
        assert(body.contains("**bold**"))
        assert(body.contains("*italic*"))
        assert(body.contains("[link](https://example.com)"))

      case Left(error) =>
        fail(s"Expected successful parse, got error: $error")
  }

  test("extra whitespace is handled") {
    val markdown = """---
                     |template: title
                     |---
                     |
                     |
                     |# Title with Extra Whitespace
                     |
                     |
                     |## Subtitle
                     |
                     |""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Right(deck) =>
        val slide = deck.getSlide(0).get
        assertEquals(slide.getSlot("title"), Some("Title with Extra Whitespace"))
        assertEquals(slide.getSlot("subtitle"), Some("Subtitle"))

      case Left(error) =>
        fail(s"Expected successful parse, got error: $error")
  }

  test("content slide body preserves newlines") {
    val markdown = """---
                     |template: content
                     |---
                     |## Multi-line Body
                     |Line 1
                     |Line 2
                     |Line 3""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Right(deck) =>
        val slide = deck.getSlide(0).get
        val body = slide.getSlot("body").get

        // Newlines should be preserved
        assert(body.contains("\n"))
        assert(body.contains("Line 1"))
        assert(body.contains("Line 2"))
        assert(body.contains("Line 3"))

      case Left(error) =>
        fail(s"Expected successful parse, got error: $error")
  }

  // US-011: Per-Slide Background Images
  test("parse slide with background field (simple string)") {
    val markdown = """---
                     |template: content
                     |background: backgrounds/custom.png
                     |---
                     |## Slide with Background
                     |This slide has a custom background.""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Right(deck) =>
        val slide = deck.getSlide(0).get
        assertEquals(slide.templateName, "content")

        // Background should be extracted as string
        slide.backgroundImage match
          case Some(bg: String) =>
            assertEquals(bg, "backgrounds/custom.png")
          case _ =>
            fail("Expected backgroundImage to be Some(String)")

      case Left(error) =>
        fail(s"Expected successful parse, got error: $error")
  }

  test("parse slide without background field defaults to None") {
    val markdown = """---
                     |template: content
                     |---
                     |## Slide Without Background
                     |Default behavior.""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Right(deck) =>
        val slide = deck.getSlide(0).get
        assertEquals(slide.backgroundImage, None)

      case Left(error) =>
        fail(s"Expected successful parse, got error: $error")
  }

  test("parse title slide with background") {
    val markdown = """---
                     |template: title
                     |background: backgrounds/title-bg.png
                     |---
                     |# Title with Background""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Right(deck) =>
        val slide = deck.getSlide(0).get
        assertEquals(slide.templateName, "title")

        slide.backgroundImage match
          case Some(bg: String) =>
            assertEquals(bg, "backgrounds/title-bg.png")
          case _ =>
            fail("Expected backgroundImage to be Some(String)")

      case Left(error) =>
        fail(s"Expected successful parse, got error: $error")
  }

  test("parse multi-slide deck with mixed backgrounds") {
    val markdown = """---
                     |template: title
                     |background: backgrounds/title.png
                     |---
                     |# Slide 1
                     |
                     |---
                     |template: content
                     |---
                     |## Slide 2
                     |No custom background
                     |
                     |---
                     |template: content
                     |background: backgrounds/special.png
                     |---
                     |## Slide 3
                     |Special background""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Right(deck) =>
        assertEquals(deck.slideCount, 3)

        // Slide 1 has background
        val slide1 = deck.getSlide(0).get
        slide1.backgroundImage match
          case Some(bg: String) => assertEquals(bg, "backgrounds/title.png")
          case _ => fail("Slide 1 should have background")

        // Slide 2 has no background
        val slide2 = deck.getSlide(1).get
        assertEquals(slide2.backgroundImage, None)

        // Slide 3 has background
        val slide3 = deck.getSlide(2).get
        slide3.backgroundImage match
          case Some(bg: String) => assertEquals(bg, "backgrounds/special.png")
          case _ => fail("Slide 3 should have background")

      case Left(error) =>
        fail(s"Expected successful parse, got error: $error")
  }

  // US-004: Speaker Notes Parsing

  test("parse slide with notes field (simple string)") {
    val markdown = """---
                     |template: content
                     |notes: "Remember to pause after this point."
                     |---
                     |## Slide with Notes
                     |This slide has speaker notes.""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Right(deck) =>
        val slide = deck.getSlide(0).get
        assertEquals(slide.notes, Some("Remember to pause after this point."))

      case Left(error) =>
        fail(s"Expected successful parse, got error: $error")
  }

  test("parse slide without notes field defaults to None") {
    val markdown = """---
                     |template: content
                     |---
                     |## Slide Without Notes
                     |Default behavior.""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Right(deck) =>
        val slide = deck.getSlide(0).get
        assertEquals(slide.notes, None)

      case Left(error) =>
        fail(s"Expected successful parse, got error: $error")
  }

  test("parse slide with notes array (multi-line)") {
    val markdown = """---
                     |template: content
                     |notes:
                     |  - "Point one"
                     |  - "Point two"
                     |  - "Point three"
                     |---
                     |## Slide with Array Notes
                     |Multi-line speaker notes.""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Right(deck) =>
        val slide = deck.getSlide(0).get
        // Array should be joined with newlines
        assertEquals(slide.notes, Some("Point one\nPoint two\nPoint three"))

      case Left(error) =>
        fail(s"Expected successful parse, got error: $error")
  }

  test("parse slide with empty notes string") {
    val markdown = """---
                     |template: content
                     |notes: ""
                     |---
                     |## Slide with Empty Notes
                     |Empty notes.""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Right(deck) =>
        val slide = deck.getSlide(0).get
        assertEquals(slide.notes, Some(""))

      case Left(error) =>
        fail(s"Expected successful parse, got error: $error")
  }

  test("parse multi-slide deck with mixed notes") {
    val markdown = """---
                     |template: title
                     |notes: "Title slide note"
                     |---
                     |# Slide 1
                     |
                     |---
                     |template: content
                     |---
                     |## Slide 2
                     |No notes
                     |
                     |---
                     |template: content
                     |notes:
                     |  - "First point"
                     |  - "Second point"
                     |---
                     |## Slide 3
                     |Array notes""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Right(deck) =>
        assertEquals(deck.slideCount, 3)

        // Slide 1 has notes
        val slide1 = deck.getSlide(0).get
        assertEquals(slide1.notes, Some("Title slide note"))

        // Slide 2 has no notes
        val slide2 = deck.getSlide(1).get
        assertEquals(slide2.notes, None)

        // Slide 3 has array notes
        val slide3 = deck.getSlide(2).get
        assertEquals(slide3.notes, Some("First point\nSecond point"))

      case Left(error) =>
        fail(s"Expected successful parse, got error: $error")
  }

  test("parse slide with both background and notes") {
    val markdown = """---
                     |template: content
                     |background: backgrounds/special.png
                     |notes: "This is a special slide"
                     |---
                     |## Combined Metadata
                     |Both background and notes.""".stripMargin

    val result = MarkdownParser.parse(markdown)

    result match
      case Right(deck) =>
        val slide = deck.getSlide(0).get

        slide.backgroundImage match
          case Some(bg: String) => assertEquals(bg, "backgrounds/special.png")
          case _ => fail("Expected backgroundImage")

        assertEquals(slide.notes, Some("This is a special slide"))

      case Left(error) =>
        fail(s"Expected successful parse, got error: $error")
  }

end MarkdownParserSpec
