package com.tjmsolutions.mdslides.domain

/**
 * TDD Specification for HeaderFooter Aggregate
 *
 * Feature: Header/Footer Enhancements (v3.0.0 - Feature 7 of 10)
 * Acceptance Criteria: doc/acceptance-criteria/header-footer-acceptance-criteria.md
 * Example Mapping: doc/scenarios/example-maps/header-footer-examples.md
 */
class HeaderFooterSpec extends munit.ScalaCheckSuite:

  val metadata = Map(
    "title" -> "My Presentation",
    "author" -> "John Doe",
    "date" -> "2025-12-29"
  )

  test("resolvePlaceholders() should replace {title} with metadata title") {
    val template = "Title: {title}"
    val result = HeaderFooter.resolvePlaceholders(template, metadata, currentSlide = 5, totalSlides = 20, elapsedTime = "00:15:30")
    assertEquals(result, "Title: My Presentation")
  }

  test("resolvePlaceholders() should replace {author} with metadata author") {
    val template = "By {author}"
    val result = HeaderFooter.resolvePlaceholders(template, metadata, currentSlide = 5, totalSlides = 20, elapsedTime = "00:15:30")
    assertEquals(result, "By John Doe")
  }

  test("resolvePlaceholders() should replace {date} with metadata date") {
    val template = "Date: {date}"
    val result = HeaderFooter.resolvePlaceholders(template, metadata, currentSlide = 5, totalSlides = 20, elapsedTime = "00:15:30")
    assertEquals(result, "Date: 2025-12-29")
  }

  test("resolvePlaceholders() should replace {current-slide} with 1-indexed slide number") {
    val template = "Slide {current-slide}"
    val result = HeaderFooter.resolvePlaceholders(template, metadata, currentSlide = 5, totalSlides = 20, elapsedTime = "00:15:30")
    assertEquals(result, "Slide 6", "0-indexed slide 5 → 1-indexed slide 6")
  }

  test("resolvePlaceholders() should replace {total-slides} with total slides") {
    val template = "of {total-slides}"
    val result = HeaderFooter.resolvePlaceholders(template, metadata, currentSlide = 5, totalSlides = 20, elapsedTime = "00:15:30")
    assertEquals(result, "of 20")
  }

  test("resolvePlaceholders() should replace {elapsed-time} with formatted time") {
    val template = "Time: {elapsed-time}"
    val result = HeaderFooter.resolvePlaceholders(template, metadata, currentSlide = 5, totalSlides = 20, elapsedTime = "00:15:30")
    assertEquals(result, "Time: 00:15:30")
  }

  test("resolvePlaceholders() should replace multiple placeholders") {
    val template = "{title} | {author} | Slide {current-slide}/{total-slides}"
    val result = HeaderFooter.resolvePlaceholders(template, metadata, currentSlide = 5, totalSlides = 20, elapsedTime = "00:15:30")
    assertEquals(result, "My Presentation | John Doe | Slide 6/20")
  }

  test("resolvePlaceholders() should preserve text without placeholders") {
    val template = "No placeholders here"
    val result = HeaderFooter.resolvePlaceholders(template, metadata, currentSlide = 5, totalSlides = 20, elapsedTime = "00:15:30")
    assertEquals(result, "No placeholders here")
  }

  test("resolvePlaceholders() should handle missing metadata gracefully") {
    val emptyMetadata = Map.empty[String, String]
    val template = "{title} | {author}"
    val result = HeaderFooter.resolvePlaceholders(template, emptyMetadata, currentSlide = 5, totalSlides = 20, elapsedTime = "00:15:30")
    assertEquals(result, " | ", "missing metadata → empty string")
  }

  test("resolvePlaceholders() should handle slide 0 as slide 1 (1-indexed)") {
    val template = "Slide {current-slide}"
    val result = HeaderFooter.resolvePlaceholders(template, metadata, currentSlide = 0, totalSlides = 20, elapsedTime = "00:15:30")
    assertEquals(result, "Slide 1")
  }

  test("resolvePlaceholders() should handle last slide correctly") {
    val template = "Slide {current-slide}/{total-slides}"
    val result = HeaderFooter.resolvePlaceholders(template, metadata, currentSlide = 19, totalSlides = 20, elapsedTime = "00:15:30")
    assertEquals(result, "Slide 20/20")
  }

  test("resolvePlaceholders() should preserve unknown placeholders") {
    val template = "{title} | {unknown-placeholder}"
    val result = HeaderFooter.resolvePlaceholders(template, metadata, currentSlide = 5, totalSlides = 20, elapsedTime = "00:15:30")
    assertEquals(result, "My Presentation | {unknown-placeholder}")
  }

  test("create() should create HeaderFooter with header template") {
    val config = Map(
      "header" -> "Header: {title}",
      "footer" -> "Footer: {author}"
    )
    val result = HeaderFooter.create(config, metadata)
    assert(result.isRight)
    val headerFooter = result.getOrElse(fail("Expected Right"))
    assertEquals(headerFooter.headerTemplate, Some("Header: {title}"))
    assertEquals(headerFooter.footerTemplate, Some("Footer: {author}"))
  }

  test("create() with missing header should default to None") {
    val config = Map("footer" -> "Footer: {author}")
    val result = HeaderFooter.create(config, metadata)
    assert(result.isRight)
    val headerFooter = result.getOrElse(fail("Expected Right"))
    assertEquals(headerFooter.headerTemplate, None)
    assertEquals(headerFooter.footerTemplate, Some("Footer: {author}"))
  }

  test("create() with missing footer should default to None") {
    val config = Map("header" -> "Header: {title}")
    val result = HeaderFooter.create(config, metadata)
    assert(result.isRight)
    val headerFooter = result.getOrElse(fail("Expected Right"))
    assertEquals(headerFooter.headerTemplate, Some("Header: {title}"))
    assertEquals(headerFooter.footerTemplate, None)
  }

  test("create() with empty config should have both None") {
    val config = Map.empty[String, String]
    val result = HeaderFooter.create(config, metadata)
    assert(result.isRight)
    val headerFooter = result.getOrElse(fail("Expected Right"))
    assertEquals(headerFooter.headerTemplate, None)
    assertEquals(headerFooter.footerTemplate, None)
  }

  test("renderHeader() should resolve placeholders for header") {
    val config = Map("header" -> "{title} | Slide {current-slide}")
    val headerFooter = HeaderFooter.create(config, metadata).getOrElse(fail("Expected Right"))
    val rendered = headerFooter.renderHeader(currentSlide = 5, totalSlides = 20, elapsedTime = "00:15:30")
    assertEquals(rendered, Some("My Presentation | Slide 6"))
  }

  test("renderFooter() should resolve placeholders for footer") {
    val config = Map("footer" -> "{author} | {date}")
    val headerFooter = HeaderFooter.create(config, metadata).getOrElse(fail("Expected Right"))
    val rendered = headerFooter.renderFooter(currentSlide = 5, totalSlides = 20, elapsedTime = "00:15:30")
    assertEquals(rendered, Some("John Doe | 2025-12-29"))
  }

  test("renderHeader() should return None if no header template") {
    val config = Map.empty[String, String]
    val headerFooter = HeaderFooter.create(config, metadata).getOrElse(fail("Expected Right"))
    val rendered = headerFooter.renderHeader(currentSlide = 5, totalSlides = 20, elapsedTime = "00:15:30")
    assertEquals(rendered, None)
  }

  test("renderFooter() should return None if no footer template") {
    val config = Map.empty[String, String]
    val headerFooter = HeaderFooter.create(config, metadata).getOrElse(fail("Expected Right"))
    val rendered = headerFooter.renderFooter(currentSlide = 5, totalSlides = 20, elapsedTime = "00:15:30")
    assertEquals(rendered, None)
  }
