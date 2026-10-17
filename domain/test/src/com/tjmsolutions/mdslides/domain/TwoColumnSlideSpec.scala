package com.tjmsolutions.mdslides.domain

/**
 * TDD Specification for TwoColumnSlide Aggregate
 *
 * Feature: Two-Column Layout Template (v3.0.0 - Feature 6 of 10)
 * Acceptance Criteria: doc/acceptance-criteria/two-column-layout-acceptance-criteria.md
 * Example Mapping: doc/scenarios/example-maps/two-column-layout-examples.md
 */
class TwoColumnSlideSpec extends munit.ScalaCheckSuite:

  val validLeftContent = "# Left Column\n\nSome content"
  val validRightContent = "# Right Column\n\nMore content"
  val columnDelimiter = "---column---"

  test("parseColumns() with exactly one delimiter should split content") {
    val content = s"$validLeftContent\n$columnDelimiter\n$validRightContent"
    val result = TwoColumnSlide.parseColumns(content)
    assert(result.isRight)
    val (left, right) = result.getOrElse(fail("Expected Right"))
    assertEquals(left, validLeftContent)
    assertEquals(right, validRightContent)
  }

  test("parseColumns() should trim whitespace from columns") {
    val content = s"  $validLeftContent  \n$columnDelimiter\n  $validRightContent  "
    val result = TwoColumnSlide.parseColumns(content)
    assert(result.isRight)
    val (left, right) = result.getOrElse(fail("Expected Right"))
    assertEquals(left, validLeftContent)
    assertEquals(right, validRightContent)
  }

  test("parseColumns() with zero delimiters should return MissingDelimiter") {
    val content = "Just single column content"
    val result = TwoColumnSlide.parseColumns(content)
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      TwoColumnError.MissingDelimiter
    )
  }

  test("parseColumns() with two delimiters should return MultipleDelimiters") {
    val content = s"Left\n$columnDelimiter\nMiddle\n$columnDelimiter\nRight"
    val result = TwoColumnSlide.parseColumns(content)
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      TwoColumnError.MultipleDelimiters(2)
    )
  }

  test("parseColumns() with three delimiters should return MultipleDelimiters") {
    val content = s"A\n$columnDelimiter\nB\n$columnDelimiter\nC\n$columnDelimiter\nD"
    val result = TwoColumnSlide.parseColumns(content)
    assert(result.isLeft)
    result.swap.toOption.get match
      case TwoColumnError.MultipleDelimiters(count) => assertEquals(count, 3)
      case _ => fail("Expected MultipleDelimiters")
  }

  test("parseColumns() with empty left column should return EmptyColumn") {
    val content = s"\n$columnDelimiter\n$validRightContent"
    val result = TwoColumnSlide.parseColumns(content)
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      TwoColumnError.EmptyColumn("left")
    )
  }

  test("parseColumns() with empty right column should return EmptyColumn") {
    val content = s"$validLeftContent\n$columnDelimiter\n  "
    val result = TwoColumnSlide.parseColumns(content)
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      TwoColumnError.EmptyColumn("right")
    )
  }

  test("parseColumns() with whitespace-only left column should return EmptyColumn") {
    val content = s"   \n$columnDelimiter\n$validRightContent"
    val result = TwoColumnSlide.parseColumns(content)
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      TwoColumnError.EmptyColumn("left")
    )
  }

  test("parseColumns() with both columns empty should return EmptyColumn for left") {
    val content = s"\n$columnDelimiter\n"
    val result = TwoColumnSlide.parseColumns(content)
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      TwoColumnError.EmptyColumn("left")
    )
  }

  test("create() with valid content should create TwoColumnSlide") {
    val content = s"$validLeftContent\n$columnDelimiter\n$validRightContent"
    val frontmatter = Map("template" -> "two-column")
    val result = TwoColumnSlide.create(content, frontmatter)
    assert(result.isRight)
    val slide = result.getOrElse(fail("Expected Right"))
    assertEquals(slide.leftColumn, validLeftContent)
    assertEquals(slide.rightColumn, validRightContent)
    assertEquals(slide.template, "two-column")
  }

  test("create() with invalid delimiter count should return error") {
    val content = "No delimiter here"
    val frontmatter = Map("template" -> "two-column")
    val result = TwoColumnSlide.create(content, frontmatter)
    assert(result.isLeft)
  }

  test("create() with empty column should return error") {
    val content = s"\n$columnDelimiter\n$validRightContent"
    val frontmatter = Map("template" -> "two-column")
    val result = TwoColumnSlide.create(content, frontmatter)
    assert(result.isLeft)
  }

  test("delimiter should be case-sensitive") {
    val content = s"$validLeftContent\n---COLUMN---\n$validRightContent"
    val result = TwoColumnSlide.parseColumns(content)
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      TwoColumnError.MissingDelimiter
    )
  }

  test("delimiter with extra whitespace should not match") {
    val content = s"$validLeftContent\n--- column ---\n$validRightContent"
    val result = TwoColumnSlide.parseColumns(content)
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      TwoColumnError.MissingDelimiter
    )
  }

  test("parseColumns() should preserve markdown formatting in columns") {
    val leftMarkdown = "# Title\n\n- Item 1\n- Item 2"
    val rightMarkdown = "```scala\nval x = 42\n```"
    val content = s"$leftMarkdown\n$columnDelimiter\n$rightMarkdown"
    val result = TwoColumnSlide.parseColumns(content)
    assert(result.isRight)
    val (left, right) = result.getOrElse(fail("Expected Right"))
    assertEquals(left, leftMarkdown)
    assertEquals(right, rightMarkdown)
  }

  test("parseColumns() with delimiter on first line should have empty left column") {
    val content = s"$columnDelimiter\n$validRightContent"
    val result = TwoColumnSlide.parseColumns(content)
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      TwoColumnError.EmptyColumn("left")
    )
  }

  test("parseColumns() with delimiter on last line should have empty right column") {
    val content = s"$validLeftContent\n$columnDelimiter"
    val result = TwoColumnSlide.parseColumns(content)
    assert(result.isLeft)
    assertEquals(
      result.swap.getOrElse(fail("Expected Left")),
      TwoColumnError.EmptyColumn("right")
    )
  }
