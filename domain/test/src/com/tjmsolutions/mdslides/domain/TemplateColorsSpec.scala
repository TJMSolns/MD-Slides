package com.tjmsolutions.mdslides.domain

/**
 * Test suite for TemplateColors value object
 *
 * Domain Model: doc/domain-models/aggregates/template-configuration-aggregate.md
 * Acceptance Criteria: doc/acceptance-criteria/template-configuration-acceptance-criteria.md (AC-4)
 */
class TemplateColorsSpec extends munit.FunSuite:

  test("isValidCSSColor should accept hex colors") {
    assert(TemplateColors.isValidCSSColor("#FFFFFF"))
    assert(TemplateColors.isValidCSSColor("#fff"))
    assert(TemplateColors.isValidCSSColor("#002C74"))
  }

  test("isValidCSSColor should accept RGB/RGBA colors") {
    assert(TemplateColors.isValidCSSColor("rgb(255, 255, 255)"))
    assert(TemplateColors.isValidCSSColor("rgba(0, 44, 116, 0.5)"))
  }

  test("isValidCSSColor should accept HSL/HSLA colors") {
    assert(TemplateColors.isValidCSSColor("hsl(0, 100%, 50%)"))
    assert(TemplateColors.isValidCSSColor("hsla(0, 100%, 50%, 0.5)"))
  }

  test("isValidCSSColor should accept named colors") {
    assert(TemplateColors.isValidCSSColor("red"))
    assert(TemplateColors.isValidCSSColor("blue"))
    assert(TemplateColors.isValidCSSColor("transparent"))
  }

  test("isValidCSSColor should reject invalid colors") {
    assert(!TemplateColors.isValidCSSColor("not-a-color"))
    assert(!TemplateColors.isValidCSSColor("#GGGGGG"))
    assert(!TemplateColors.isValidCSSColor("invalid"))
  }

  test("validate should accept valid colors") {
    val colors = TemplateColors(
      heading = Some("#FFFFFF"),
      subtitle = Some("#CCCCCC"),
      body = Some("#333333"),
      author = Some("#002C74")
    )
    assertEquals(colors.validate(), Right(colors))
  }

  test("validate should reject invalid colors") {
    val colors = TemplateColors(
      heading = Some("#FFFFFF"),
      subtitle = Some("not-a-color"),
      body = None,
      author = None
    )
    assert(colors.validate().isLeft)
  }

  test("mergeWithDefaults should override specified colors") {
    val themeDefaults = Map(
      "heading" -> "#002C74",
      "subtitle" -> "#666666",
      "body" -> "#333333",
      "author" -> "#000000"
    )

    val templateColors = TemplateColors(
      heading = Some("#FFFFFF"),  // Override
      subtitle = None,            // Use default
      body = None,                // Use default
      author = Some("#CCCCCC")    // Override
    )

    val merged = templateColors.mergeWithDefaults(themeDefaults)

    assertEquals(merged("heading"), "#FFFFFF")
    assertEquals(merged("subtitle"), "#666666")
    assertEquals(merged("body"), "#333333")
    assertEquals(merged("author"), "#CCCCCC")
  }

  test("empty should have all None values") {
    val empty = TemplateColors.empty
    assertEquals(empty.heading, None)
    assertEquals(empty.subtitle, None)
    assertEquals(empty.body, None)
    assertEquals(empty.author, None)
  }
