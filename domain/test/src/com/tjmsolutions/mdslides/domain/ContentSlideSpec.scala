package com.tjmsolutions.mdslides.domain

import cats.data.NonEmptyList

/**
 * Example-based tests for Content Slide (US-002).
 *
 * Scenarios tested:
 * 1. Minimal valid content slide (Example 1)
 * 2. Content slide with bullet list (Example 2)
 * 3. Content slide with markdown formatting (Example 3)
 * 4. Body exceeds max lines (Example 4)
 * 5. Body exceeds max words (Example 5)
 * 6. Missing required heading (Example 6)
 * 7. Missing required body (Example 7)
 * 8. Heading exceeds 80 chars (additional edge case)
 *
 * Note: Code blocks and images (Examples 8-9) require infrastructure layer
 * to detect, so they're tested in integration tests.
 *
 * Related Governance:
 * - POL-004: Property-Based Testing Requirements
 * - ADR-009: Property-Based Testing Strategy
 * - Three Amigos Session: three-amigos-session-002.md
 * - Example Mapping: example-mapping-002.md
 */
class ContentSlideSpec extends munit.FunSuite:

  val slideId: SlideId = SlideId.unsafe(1)

  // Example 1: Minimal Valid Content Slide
  test("create content slide with minimal content") {
    val slide = Slide(
      id = slideId,
      templateName = "content",
      slots = Map(
        "heading" -> "Key Takeaways",
        "body" -> "Domain-Driven Design helps us build better software."
      )
    )

    val result = Slide.validated(slideId, "content", slide.slots)

    result match
      case Right(validSlide) =>
        assertEquals(validSlide.templateName, "content")
        assertEquals(validSlide.getSlot("heading"), Some("Key Takeaways"))
        assertEquals(validSlide.getSlot("body"), Some("Domain-Driven Design helps us build better software."))

      case Left(errors) =>
        fail(s"Expected validation to succeed, but got errors: ${errors.toList.map(_.displayMessage).mkString(", ")}")
  }

  // Example 2: Content Slide with Bullet List
  test("create content slide with bullet list") {
    val body = """- Ubiquitous Language
                 |- Bounded Contexts
                 |- Aggregates and Entities
                 |- Strategic Design""".stripMargin

    val slide = Slide(
      id = slideId,
      templateName = "content",
      slots = Map(
        "heading" -> "Key Principles",
        "body" -> body
      )
    )

    val result = Slide.validated(slideId, "content", slide.slots)

    result match
      case Right(validSlide) =>
        assertEquals(validSlide.getSlot("heading"), Some("Key Principles"))
        assertEquals(validSlide.getSlot("body"), Some(body))
        // Body has 4 lines
        assertEquals(SlotContent.fromPlainText(body).lineCount, 4)

      case Left(errors) =>
        fail(s"Expected validation to succeed, but got errors: ${errors.toList.map(_.displayMessage).mkString(", ")}")
  }

  // Example 3: Content Slide with Markdown Formatting
  test("content slide with rich markdown formatting") {
    val body = """Functional programming offers **immutability** by default, making code
                 |easier to reason about. Pure functions have *no side effects*, which
                 |simplifies testing and debugging.
                 |
                 |Learn more at [FP Resources](https://functional.org).""".stripMargin

    val slide = Slide(
      id = slideId,
      templateName = "content",
      slots = Map(
        "heading" -> "Why Functional Programming?",
        "body" -> body
      )
    )

    val result = Slide.validated(slideId, "content", slide.slots)

    result match
      case Right(validSlide) =>
        // Markdown formatting preserved
        assert(validSlide.getSlot("body").get.contains("**immutability**"))
        assert(validSlide.getSlot("body").get.contains("*no side effects*"))
        assert(validSlide.getSlot("body").get.contains("[FP Resources]"))
        // Body has 5 lines
        assertEquals(SlotContent.fromPlainText(body).lineCount, 5)

      case Left(errors) =>
        fail(s"Expected validation to succeed, but got errors: ${errors.toList.map(_.displayMessage).mkString(", ")}")
  }

  // Example 4: Body Exceeds Max Lines
  test("body exceeds 12 line constraint") {
    val body = (1 to 13).map(i => s"Line $i").mkString("\n")

    val slide = Slide(
      id = slideId,
      templateName = "content",
      slots = Map(
        "heading" -> "Long Content",
        "body" -> body
      )
    )

    val result = Slide.validated(slideId, "content", slide.slots)

    result match
      case Left(errors) =>
        val errorMessages = errors.toList.map(_.displayMessage)
        assert(errorMessages.exists(_.contains("body")))
        assert(errorMessages.exists(_.contains("exceeds max 12 lines")))

      case Right(_) =>
        fail("Expected validation to fail with ContentError for body exceeding 12 lines")
  }

  // Example 5: Body Exceeds Max Words
  test("body exceeds 150 word constraint") {
    // Create 160 words
    val body = (1 to 160).map(i => s"word$i").mkString(" ")

    val slide = Slide(
      id = slideId,
      templateName = "content",
      slots = Map(
        "heading" -> "Overly Verbose Content",
        "body" -> body
      )
    )

    val result = Slide.validated(slideId, "content", slide.slots)

    result match
      case Left(errors) =>
        val errorMessages = errors.toList.map(_.displayMessage)
        assert(errorMessages.exists(_.contains("body")))
        assert(errorMessages.exists(_.contains("exceeds max 150 words")))

      case Right(_) =>
        fail("Expected validation to fail with ContentError for body exceeding 150 words")
  }

  // Example 6: Missing Required Heading
  test("no heading in markdown") {
    val slide = Slide(
      id = slideId,
      templateName = "content",
      slots = Map(
        "body" -> "This is just body text with no heading."
      )
    )

    val result = Slide.validated(slideId, "content", slide.slots)

    result match
      case Left(errors) =>
        val errorMessages = errors.toList.map(_.displayMessage)
        assert(errorMessages.exists(_.contains("Structure Error")))
        assert(errorMessages.exists(_.contains("heading")))
        assert(errorMessages.exists(_.contains("missing")))

      case Right(_) =>
        fail("Expected validation to fail with StructureError for missing heading")
  }

  // Example 7: Missing Required Body
  test("heading but no body content") {
    val slide = Slide(
      id = slideId,
      templateName = "content",
      slots = Map(
        "heading" -> "Lonely Heading"
      )
    )

    val result = Slide.validated(slideId, "content", slide.slots)

    result match
      case Left(errors) =>
        val errorMessages = errors.toList.map(_.displayMessage)
        assert(errorMessages.exists(_.contains("Structure Error")))
        assert(errorMessages.exists(_.contains("body")))
        assert(errorMessages.exists(_.contains("missing")))

      case Right(_) =>
        fail("Expected validation to fail with StructureError for missing body")
  }

  // Additional edge case: Heading exceeds 80 chars
  test("heading exceeds 80 character limit") {
    val longHeading = "This is an extremely long heading that definitely exceeds the eighty character limit"

    val slide = Slide(
      id = slideId,
      templateName = "content",
      slots = Map(
        "heading" -> longHeading,
        "body" -> "Some body content."
      )
    )

    val result = Slide.validated(slideId, "content", slide.slots)

    result match
      case Left(errors) =>
        val errorMessages = errors.toList.map(_.displayMessage)
        assert(errorMessages.exists(_.contains("heading")))
        assert(errorMessages.exists(_.contains("exceeds max 80 characters")))

      case Right(_) =>
        fail("Expected validation to fail with ContentError for heading exceeding 80 chars")
  }

  // Edge case: Body at boundary (exactly 12 lines)
  test("body with exactly 12 lines passes validation") {
    val body = (1 to 12).map(i => s"Line $i").mkString("\n")

    val slide = Slide(
      id = slideId,
      templateName = "content",
      slots = Map(
        "heading" -> "Boundary Test",
        "body" -> body
      )
    )

    val result = Slide.validated(slideId, "content", slide.slots)

    result match
      case Right(validSlide) =>
        assertEquals(SlotContent.fromPlainText(body).lineCount, 12)

      case Left(errors) =>
        fail(s"Expected validation to succeed for 12 lines, but got errors: ${errors.toList.map(_.displayMessage).mkString(", ")}")
  }

  // Edge case: Body at boundary (exactly 150 words)
  test("body with exactly 150 words passes validation") {
    val body = (1 to 150).map(i => s"word$i").mkString(" ")

    val slide = Slide(
      id = slideId,
      templateName = "content",
      slots = Map(
        "heading" -> "Boundary Test",
        "body" -> body
      )
    )

    val result = Slide.validated(slideId, "content", slide.slots)

    result match
      case Right(validSlide) =>
        assertEquals(SlotContent.fromPlainText(body).wordCount, 150)

      case Left(errors) =>
        fail(s"Expected validation to succeed for 150 words, but got errors: ${errors.toList.map(_.displayMessage).mkString(", ")}")
  }

  // Edge case: Multiple validation errors collected
  test("multiple content errors are collected") {
    val body = (1 to 13).map(i => s"Line $i").mkString("\n")
    val longHeading = "x" * 100

    val slide = Slide(
      id = slideId,
      templateName = "content",
      slots = Map(
        "heading" -> longHeading,  // Too long
        "body" -> body              // Too many lines
      )
    )

    val result = Slide.validated(slideId, "content", slide.slots)

    result match
      case Left(errors) =>
        // Should have 2 errors (heading chars + body lines)
        assertEquals(errors.length, 2)
        val errorMessages = errors.toList.map(_.displayMessage)
        assert(errorMessages.exists(_.contains("heading")))
        assert(errorMessages.exists(_.contains("body")))

      case Right(_) =>
        fail("Expected validation to fail with multiple errors")
  }

end ContentSlideSpec
