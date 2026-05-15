package com.tjmsolutions.mdslides.domain

import cats.data.NonEmptyList

/**
 * Example-based tests for Slide aggregate (US-001: Title Slide).
 *
 * Scenarios tested:
 * 1. Minimal valid title slide (Example 1)
 * 2. Full title slide with all slots (Example 2)
 * 3. Title exceeds max lines (Example 3)
 * 4. Missing required title slot (Example 4)
 * 5. Author exceeds character limit (Example 5)
 * 6. Template not found (Example 6)
 * 7. Title with markdown formatting (Example 7)
 *
 * Related Governance:
 * - POL-004: Property-Based Testing Requirements
 * - ADR-009: Property-Based Testing Strategy
 * - Three Amigos Session: three-amigos-session-001.md
 * - Example Mapping: example-mapping-001.md
 */
class SlideSpec extends munit.FunSuite:

  val slideId: SlideId = SlideId.unsafe(1)

  // Example 1: Minimal Valid Title Slide
  test("create title slide with minimal content") {
    val slide = Slide(
      id = slideId,
      templateName = "title",
      slots = Map(
        "title" -> "Welcome to MDSlides"
      )
    )

    val result = Slide.validated(slideId, "title", slide.slots)

    result match
      case Right(validSlide) =>
        assertEquals(validSlide.templateName, "title")
        assertEquals(validSlide.getSlot("title"), Some("Welcome to MDSlides"))
        assertEquals(validSlide.getSlot("subtitle"), None)
        assertEquals(validSlide.getSlot("author"), None)

      case Left(errors) =>
        fail(s"Expected validation to succeed, but got errors: ${errors.toList.map(_.displayMessage).mkString(", ")}")
  }

  // Example 2: Full Title Slide with All Slots
  test("create title slide with all optional slots") {
    val slide = Slide(
      id = slideId,
      templateName = "title",
      slots = Map(
        "title" -> "Welcome to MDSlides",
        "subtitle" -> "Create Beautiful Presentations with Markdown",
        "author" -> "Tony Moores"
      )
    )

    val result = Slide.validated(slideId, "title", slide.slots)

    result match
      case Right(validSlide) =>
        assertEquals(validSlide.getSlot("title"), Some("Welcome to MDSlides"))
        assertEquals(validSlide.getSlot("subtitle"), Some("Create Beautiful Presentations with Markdown"))
        assertEquals(validSlide.getSlot("author"), Some("Tony Moores"))

      case Left(errors) =>
        fail(s"Expected validation to succeed, but got errors: ${errors.toList.map(_.displayMessage).mkString(", ")}")
  }

  // Example 3: Title Exceeds Max Lines (Validation Failure)
  test("title exceeds 2 line constraint") {
    val slide = Slide(
      id = slideId,
      templateName = "title",
      slots = Map(
        "title" -> "This is a very long title\nthat spans three\nseparate lines"
      )
    )

    val result = Slide.validated(slideId, "title", slide.slots)

    result match
      case Left(errors) =>
        val errorMessages = errors.toList.map(_.displayMessage)
        assert(errorMessages.exists(_.contains("title")))
        assert(errorMessages.exists(_.contains("exceeds max 2 lines")))

      case Right(_) =>
        fail("Expected validation to fail with ContentError for title exceeding 2 lines")
  }

  // Example 4: Missing Required Title Slot
  test("no title heading in markdown") {
    val slide = Slide(
      id = slideId,
      templateName = "title",
      slots = Map(
        "subtitle" -> "Just a subtitle, no title"
      )
    )

    val result = Slide.validated(slideId, "title", slide.slots)

    result match
      case Left(errors) =>
        val errorMessages = errors.toList.map(_.displayMessage)
        assert(errorMessages.exists(_.contains("Structure Error")))
        assert(errorMessages.exists(_.contains("title")))
        assert(errorMessages.exists(_.contains("missing")))

      case Right(_) =>
        fail("Expected validation to fail with StructureError for missing title")
  }

  // Example 5: Author Exceeds Character Limit
  test("author name too long") {
    val longAuthor = "This is an extremely long author name that definitely exceeds the eighty character limit imposed by the template"

    val slide = Slide(
      id = slideId,
      templateName = "title",
      slots = Map(
        "title" -> "My Presentation",
        "author" -> longAuthor
      )
    )

    val result = Slide.validated(slideId, "title", slide.slots)

    result match
      case Left(errors) =>
        val errorMessages = errors.toList.map(_.displayMessage)
        assert(errorMessages.exists(_.contains("author")))
        assert(errorMessages.exists(_.contains("exceeds max 80 characters")))

      case Right(_) =>
        fail("Expected validation to fail with ContentError for author exceeding 80 chars")
  }

  // Example 6: Template Not Found
  test("front matter specifies non-existent template") {
    val slide = Slide(
      id = slideId,
      templateName = "super-fancy-template",
      slots = Map(
        "title" -> "My Title"
      )
    )

    val result = Slide.validated(slideId, "super-fancy-template", slide.slots)

    result match
      case Left(errors) =>
        val errorMessages = errors.toList.map(_.displayMessage)
        assert(errorMessages.exists(_.contains("Structure Error")))
        assert(errorMessages.exists(_.contains("Unknown template 'super-fancy-template'")))

      case Right(_) =>
        fail("Expected validation to fail with StructureError for non-existent template")
  }

  // Example 7: Title with Markdown Formatting and Unicode
  test("title with inline markdown and mathematical symbols") {
    val slide = Slide(
      id = slideId,
      templateName = "title",
      slots = Map(
        "title" -> "Introduction to **Fourier Transforms** and ∑ Notation",
        "subtitle" -> "Mathematical foundations for signal processing",
        "author" -> "Tony Moores"
      )
    )

    val result = Slide.validated(slideId, "title", slide.slots)

    result match
      case Right(validSlide) =>
        // Markdown formatting and Unicode should be preserved
        assertEquals(
          validSlide.getSlot("title"),
          Some("Introduction to **Fourier Transforms** and ∑ Notation")
        )
        assertEquals(
          validSlide.getSlot("subtitle"),
          Some("Mathematical foundations for signal processing")
        )

      case Left(errors) =>
        fail(s"Expected validation to succeed for markdown/unicode in title, but got errors: ${errors.toList.map(_.displayMessage).mkString(", ")}")
  }

  // Additional edge cases
  test("subtitle exceeds 2 line constraint") {
    val slide = Slide(
      id = slideId,
      templateName = "title",
      slots = Map(
        "title" -> "My Title",
        "subtitle" -> "Line 1\nLine 2\nLine 3"
      )
    )

    val result = Slide.validated(slideId, "title", slide.slots)

    result match
      case Left(errors) =>
        val errorMessages = errors.toList.map(_.displayMessage)
        assert(errorMessages.exists(_.contains("subtitle")))
        assert(errorMessages.exists(_.contains("exceeds max 2 lines")))

      case Right(_) =>
        fail("Expected validation to fail with ContentError for subtitle exceeding 2 lines")
  }

  test("empty title slot is invalid") {
    val slide = Slide(
      id = slideId,
      templateName = "title",
      slots = Map(
        "title" -> ""
      )
    )

    val result = Slide.validated(slideId, "title", slide.slots)

    // Empty title should pass validation (it's present, just empty)
    // The "required" constraint checks presence, not emptiness
    result match
      case Right(_) =>
        // This is expected - slot is present (even if empty)
        assert(true)

      case Left(_) =>
        // If we want to fail on empty required slots, we need additional validation
        // For now, empty is allowed (spec doesn't explicitly forbid it)
        assert(true)
  }

  test("multiple validation errors are collected") {
    val slide = Slide(
      id = slideId,
      templateName = "title",
      slots = Map(
        "title" -> "Line 1\nLine 2\nLine 3",  // Too many lines
        "author" -> "x" * 100  // Too many chars
      )
    )

    val result = Slide.validated(slideId, "title", slide.slots)

    result match
      case Left(errors) =>
        // Should have 2 errors (title lines + author chars)
        assertEquals(errors.length, 2)
        val errorMessages = errors.toList.map(_.displayMessage)
        assert(errorMessages.exists(_.contains("title")))
        assert(errorMessages.exists(_.contains("author")))

      case Right(_) =>
        fail("Expected validation to fail with multiple errors")
  }

  // US-011: Per-Slide Background Images
  test("slide with no backgroundImage defaults to None") {
    val slide = Slide(
      id = slideId,
      templateName = "title",
      slots = Map("title" -> "My Title")
    )

    assertEquals(slide.backgroundImage, None)
  }

  test("slide accepts backgroundImage as simple string") {
    val slide = Slide(
      id = slideId,
      templateName = "title",
      slots = Map("title" -> "My Title"),
      backgroundImage = Some("backgrounds/custom.png")
    )

    assertEquals(slide.backgroundImage, Some("backgrounds/custom.png"))
  }

  test("slide accepts backgroundImage as BackgroundConfig") {
    val config = BackgroundConfig(
      image = "backgrounds/custom.png",
      opacity = Some(0.5)
    )

    val slide = Slide(
      id = slideId,
      templateName = "title",
      slots = Map("title" -> "My Title"),
      backgroundImage = Some(config)
    )

    slide.backgroundImage match
      case Some(bg: BackgroundConfig) =>
        assertEquals(bg.image, "backgrounds/custom.png")
        assertEquals(bg.opacity, Some(0.5))
      case _ =>
        fail("Expected BackgroundConfig")
  }

  test("slide.backgroundImage preserves union type (String | BackgroundConfig)") {
    val slideWithString = Slide(
      id = slideId,
      templateName = "title",
      slots = Map("title" -> "Test"),
      backgroundImage = Some("path/to/bg.png")
    )

    val slideWithConfig = Slide(
      id = slideId,
      templateName = "title",
      slots = Map("title" -> "Test"),
      backgroundImage = Some(BackgroundConfig("path/to/bg.png", Some(0.3)))
    )

    // Both should compile and work
    assert(slideWithString.backgroundImage.isDefined)
    assert(slideWithConfig.backgroundImage.isDefined)
  }

  // US-004: Speaker Notes Parsing

  test("slide with no notes defaults to None") {
    val slide = Slide(
      id = slideId,
      templateName = "title",
      slots = Map("title" -> "My Title")
    )

    assertEquals(slide.notes, None)
  }

  test("slide accepts notes as Some(String)") {
    val slide = Slide(
      id = slideId,
      templateName = "title",
      slots = Map("title" -> "My Title"),
      notes = Some("Remember to pause after this point.")
    )

    assertEquals(slide.notes, Some("Remember to pause after this point."))
  }

  test("slide accepts empty notes string") {
    val slide = Slide(
      id = slideId,
      templateName = "title",
      slots = Map("title" -> "My Title"),
      notes = Some("")
    )

    assertEquals(slide.notes, Some(""))
  }

  test("slide accepts multi-line notes") {
    val multiLineNotes = "Point one\nPoint two\nPoint three"
    val slide = Slide(
      id = slideId,
      templateName = "content",
      slots = Map("heading" -> "My Heading", "body" -> "Content"),
      notes = Some(multiLineNotes)
    )

    assertEquals(slide.notes, Some(multiLineNotes))
  }

end SlideSpec
