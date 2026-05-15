package com.tjmsolutions.mdslides.domain

/**
 * TDD Specification for GotoCommand
 *
 * Feature: Goto Function (v3.0.0 - Feature 5 of 10)
 * Acceptance Criteria: doc/acceptance-criteria/goto-function-acceptance-criteria.md
 * Example Mapping: doc/scenarios/example-maps/goto-function-examples.md
 */
class GotoCommandSpec extends munit.ScalaCheckSuite:

  test("gotoSlide() with valid 1-indexed input should convert to 0-indexed") {
    val result = GotoCommand.gotoSlide(userInput = 5, totalSlides = 20)
    assert(result.isRight)
    assertEquals(result.getOrElse(fail("Expected Right")), 4, "slide 5 (1-indexed) → index 4 (0-indexed)")
  }

  test("gotoSlide() with input = 1 should go to slide 0") {
    val result = GotoCommand.gotoSlide(userInput = 1, totalSlides = 20)
    assert(result.isRight)
    assertEquals(result.getOrElse(fail("Expected Right")), 0)
  }

  test("gotoSlide() with input = totalSlides should go to last slide") {
    val result = GotoCommand.gotoSlide(userInput = 20, totalSlides = 20)
    assert(result.isRight)
    assertEquals(result.getOrElse(fail("Expected Right")), 19)
  }

  test("gotoSlide() with input = 0 should return InvalidSlideIndex") {
    val result = GotoCommand.gotoSlide(userInput = 0, totalSlides = 20)
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      GotoError.InvalidSlideIndex(userInput = 0, totalSlides = 20)
    )
  }

  test("gotoSlide() with negative input should return InvalidSlideIndex") {
    val result = GotoCommand.gotoSlide(userInput = -5, totalSlides = 20)
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      GotoError.InvalidSlideIndex(userInput = -5, totalSlides = 20)
    )
  }

  test("gotoSlide() with input > totalSlides should return InvalidSlideIndex") {
    val result = GotoCommand.gotoSlide(userInput = 21, totalSlides = 20)
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      GotoError.InvalidSlideIndex(userInput = 21, totalSlides = 20)
    )
  }

  test("gotoSlide() with input = totalSlides + 100 should return InvalidSlideIndex") {
    val result = GotoCommand.gotoSlide(userInput = 120, totalSlides = 20)
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      GotoError.InvalidSlideIndex(userInput = 120, totalSlides = 20)
    )
  }

  test("gotoSlide() boundary: input = 1 with totalSlides = 1 (single slide deck)") {
    val result = GotoCommand.gotoSlide(userInput = 1, totalSlides = 1)
    assert(result.isRight)
    assertEquals(result.getOrElse(fail("Expected Right")), 0)
  }

  test("gotoSlide() boundary: input = 2 with totalSlides = 1 should be invalid") {
    val result = GotoCommand.gotoSlide(userInput = 2, totalSlides = 1)
    assert(result.isLeft)
  }

  test("parseUserInput() should parse valid numeric string") {
    val result = GotoCommand.parseUserInput("42")
    assert(result.isRight)
    assertEquals(result.getOrElse(fail("Expected Right")), 42)
  }

  test("parseUserInput() should trim whitespace") {
    val result = GotoCommand.parseUserInput("  15  ")
    assert(result.isRight)
    assertEquals(result.getOrElse(fail("Expected Right")), 15)
  }

  test("parseUserInput() should reject empty string") {
    val result = GotoCommand.parseUserInput("")
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      GotoError.InvalidInput("")
    )
  }

  test("parseUserInput() should reject whitespace-only string") {
    val result = GotoCommand.parseUserInput("   ")
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      GotoError.InvalidInput("   ")
    )
  }

  test("parseUserInput() should reject non-numeric string") {
    val result = GotoCommand.parseUserInput("abc")
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      GotoError.InvalidInput("abc")
    )
  }

  test("parseUserInput() should reject decimal numbers") {
    val result = GotoCommand.parseUserInput("5.5")
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      GotoError.InvalidInput("5.5")
    )
  }

  test("parseUserInput() should reject alphanumeric strings") {
    val result = GotoCommand.parseUserInput("12abc")
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      GotoError.InvalidInput("12abc")
    )
  }

  test("parseAndGotoSlide() should parse and validate in one step") {
    val result = GotoCommand.parseAndGotoSlide("5", totalSlides = 20)
    assert(result.isRight)
    assertEquals(result.getOrElse(fail("Expected Right")), 4)
  }

  test("parseAndGotoSlide() should reject invalid input") {
    val result = GotoCommand.parseAndGotoSlide("abc", totalSlides = 20)
    assert(result.isLeft)
    assert(result.swap.getOrElse(fail("Expected Left")).isInstanceOf[GotoError.InvalidInput])
  }

  test("parseAndGotoSlide() should reject out-of-range input") {
    val result = GotoCommand.parseAndGotoSlide("25", totalSlides = 20)
    assert(result.isLeft)
    assert(result.swap.getOrElse(fail("Expected Left")).isInstanceOf[GotoError.InvalidSlideIndex])
  }
