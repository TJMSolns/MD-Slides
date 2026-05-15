package com.tjmsolutions.mdslides.domain

import cats.data.NonEmptyList

/**
 * Example-based tests for SlideDeck aggregate.
 *
 * Scenarios tested:
 * 1. Create valid deck with single slide
 * 2. Create valid deck with multiple slides
 * 3. Deck exceeds max 200 slides
 * 4. Deck with duplicate slide IDs
 * 5. Deck with invalid slide IDs (out of range)
 * 6. Deck with mix of valid and invalid slides
 * 7. Deck at boundary (exactly 200 slides)
 * 8. Get slide by index
 * 9. Slide count
 *
 * Related Governance:
 * - PDR-003: Slide Deck Size Limits
 * - ADR-002: Validation Pipeline Architecture
 */
class SlideDeckSpec extends munit.FunSuite:

  // Test 1: Create valid deck with single slide
  test("create valid deck with single slide") {
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "title",
      slots = Map("title" -> "My Presentation")
    )

    val deck = SlideDeck.unsafe(slide)
    val result = SlideDeck.validated(deck.slides)

    result match
      case Right(validDeck) =>
        assertEquals(validDeck.slideCount, 1)
        assertEquals(validDeck.getSlide(0), Some(slide))

      case Left(errors) =>
        fail(s"Expected validation to succeed, but got errors: ${errors.toList.map(_.displayMessage).mkString(", ")}")
  }

  // Test 2: Create valid deck with multiple slides
  test("create valid deck with multiple slides") {
    val slides = List(
      Slide(SlideId.unsafe(1), "title", Map("title" -> "Introduction")),
      Slide(SlideId.unsafe(2), "content", Map("heading" -> "Overview", "body" -> "This is the content.")),
      Slide(SlideId.unsafe(3), "content", Map("heading" -> "Conclusion", "body" -> "Thank you."))
    )

    val deck = SlideDeck.fromList(slides).toOption.get
    val result = SlideDeck.validated(deck.slides)

    result match
      case Right(validDeck) =>
        assertEquals(validDeck.slideCount, 3)
        assertEquals(validDeck.getSlide(0).map(_.templateName), Some("title"))
        assertEquals(validDeck.getSlide(1).map(_.templateName), Some("content"))
        assertEquals(validDeck.getSlide(2).map(_.templateName), Some("content"))

      case Left(errors) =>
        fail(s"Expected validation to succeed, but got errors: ${errors.toList.map(_.displayMessage).mkString(", ")}")
  }

  // Test 3: Deck exceeds max 200 slides
  test("deck exceeds max 200 slides") {
    val slides = (1 to 201).map { i =>
      Slide(SlideId.unsafe(i), "content", Map("heading" -> s"Slide $i", "body" -> "Content"))
    }.toList

    val deck = SlideDeck.fromList(slides).toOption.get
    val result = SlideDeck.validated(deck.slides)

    result match
      case Left(errors) =>
        val errorMessages = errors.toList.map(_.displayMessage)
        assert(errorMessages.exists(_.contains("maximum 200 allowed")))

      case Right(_) =>
        fail("Expected validation to fail for deck with 201 slides")
  }

  // Test 4: Deck with duplicate slide IDs
  test("deck with duplicate slide IDs") {
    val slides = List(
      Slide(SlideId.unsafe(1), "title", Map("title" -> "Introduction")),
      Slide(SlideId.unsafe(1), "content", Map("heading" -> "Overview", "body" -> "Content")),  // Duplicate ID
      Slide(SlideId.unsafe(2), "content", Map("heading" -> "Conclusion", "body" -> "End"))
    )

    val deck = SlideDeck.fromList(slides).toOption.get
    val result = SlideDeck.validated(deck.slides)

    result match
      case Left(errors) =>
        val errorMessages = errors.toList.map(_.displayMessage)
        assert(errorMessages.exists(_.contains("Duplicate slide IDs")))

      case Right(_) =>
        fail("Expected validation to fail for duplicate slide IDs")
  }

  // Test 5: Deck with invalid slide IDs (out of range)
  test("deck with invalid slide ID out of range") {
    // SlideId.unsafe allows invalid values for testing
    val slides = List(
      Slide(SlideId.unsafe(0), "title", Map("title" -> "Introduction")),  // ID 0 is invalid
      Slide(SlideId.unsafe(1), "content", Map("heading" -> "Overview", "body" -> "Content"))
    )

    val deck = SlideDeck.fromList(slides).toOption.get
    val result = SlideDeck.validated(deck.slides)

    result match
      case Left(errors) =>
        val errorMessages = errors.toList.map(_.displayMessage)
        assert(errorMessages.exists(_.contains("Invalid slide IDs")))

      case Right(_) =>
        fail("Expected validation to fail for invalid slide ID (0)")
  }

  // Test 6: Deck with mix of valid and invalid slides
  test("deck with mix of valid and invalid slides") {
    val slides = List(
      Slide(SlideId.unsafe(1), "title", Map("title" -> "Introduction")),
      Slide(SlideId.unsafe(2), "content", Map("heading" -> "x" * 100)),  // Missing body (structure error)
      Slide(SlideId.unsafe(3), "content", Map("heading" -> "Conclusion", "body" -> "End"))
    )

    val deck = SlideDeck.fromList(slides).toOption.get
    val result = SlideDeck.validated(deck.slides)

    result match
      case Left(errors) =>
        // Should have errors for both missing body and heading too long
        assert(errors.length >= 1)

      case Right(_) =>
        fail("Expected validation to fail for slide with missing body")
  }

  // Test 7: Deck at boundary (exactly 200 slides)
  test("deck with exactly 200 slides passes validation") {
    val slides = (1 to 200).map { i =>
      Slide(SlideId.unsafe(i), "content", Map("heading" -> s"Slide $i", "body" -> "Content"))
    }.toList

    val deck = SlideDeck.fromList(slides).toOption.get
    val result = SlideDeck.validated(deck.slides)

    result match
      case Right(validDeck) =>
        assertEquals(validDeck.slideCount, 200)

      case Left(errors) =>
        fail(s"Expected validation to succeed for 200 slides, but got errors: ${errors.toList.map(_.displayMessage).mkString(", ")}")
  }

  // Test 8: Get slide by index
  test("get slide by index") {
    val slides = List(
      Slide(SlideId.unsafe(1), "title", Map("title" -> "Introduction")),
      Slide(SlideId.unsafe(2), "content", Map("heading" -> "Overview", "body" -> "Content")),
      Slide(SlideId.unsafe(3), "content", Map("heading" -> "Conclusion", "body" -> "End"))
    )

    val deck = SlideDeck.fromList(slides).toOption.get

    assertEquals(deck.getSlide(0).map(_.id.toInt), Some(1))
    assertEquals(deck.getSlide(1).map(_.id.toInt), Some(2))
    assertEquals(deck.getSlide(2).map(_.id.toInt), Some(3))
    assertEquals(deck.getSlide(3), None)  // Out of bounds
    assertEquals(deck.getSlide(-1), None)  // Negative index
  }

  // Test 9: Slide count
  test("slide count returns correct value") {
    val slides1 = List(Slide(SlideId.unsafe(1), "title", Map("title" -> "Test")))
    val deck1 = SlideDeck.fromList(slides1).toOption.get
    assertEquals(deck1.slideCount, 1)

    val slides5 = (1 to 5).map(i => Slide(SlideId.unsafe(i), "content", Map("heading" -> s"$i", "body" -> "x"))).toList
    val deck5 = SlideDeck.fromList(slides5).toOption.get
    assertEquals(deck5.slideCount, 5)
  }

  // Test 10: fromList with empty list fails
  test("fromList with empty list returns error") {
    val result = SlideDeck.fromList(Nil)

    result match
      case Left(error) =>
        assert(error.contains("zero slides"))
        assert(error.contains("minimum 1 required"))

      case Right(_) =>
        fail("Expected fromList to fail with empty list")
  }

  // Test 11: Collect all slide errors
  test("validation collects all slide errors") {
    val slides = List(
      Slide(SlideId.unsafe(1), "title", Map("title" -> "x" * 200)),  // Title too long (not enforced in current impl, but shows error collection)
      Slide(SlideId.unsafe(2), "content", Map("heading" -> "Test")),  // Missing body
      Slide(SlideId.unsafe(3), "content", Map("body" -> "Content"))  // Missing heading
    )

    val deck = SlideDeck.fromList(slides).toOption.get
    val result = SlideDeck.validated(deck.slides)

    result match
      case Left(errors) =>
        // Should have multiple errors (missing body, missing heading)
        assert(errors.length >= 2)

      case Right(_) =>
        fail("Expected validation to fail with multiple slide errors")
  }

end SlideDeckSpec
