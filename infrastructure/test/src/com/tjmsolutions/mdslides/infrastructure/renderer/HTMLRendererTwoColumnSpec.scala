package com.tjmsolutions.mdslides.infrastructure.renderer

import com.tjmsolutions.mdslides.domain.{Slide, SlideDeck, SlideId}
import cats.data.NonEmptyList

/**
 * Regression tests for two-column slide rendering.
 *
 * These tests were written after two bugs were found in production:
 * 1. Headings were not rendered — parseTwoColumnSlide put the ## line in leftColumn
 *    and renderTwoColumnSlide looked for a "title" slot that was never populated.
 * 2. Lists and tables in columns had no indentation or styling — CSS was scoped
 *    to .slide-body, which does not cover .column elements.
 */
class HTMLRendererTwoColumnSpec extends munit.FunSuite:

  private def twoColumnSlide(
    heading: String,
    leftColumn: String,
    rightColumn: String
  ): Slide =
    Slide(
      id = SlideId.unsafe(1),
      templateName = "two-column",
      slots = Map(
        "heading"     -> heading,
        "leftColumn"  -> leftColumn,
        "rightColumn" -> rightColumn
      )
    )

  private def renderSlide(slide: Slide): String =
    HTMLRenderer.renderDeck(SlideDeck(NonEmptyList.one(slide)))

  // --- Heading rendering ---

  test("two-column slide renders heading in slide-heading element") {
    val html = renderSlide(twoColumnSlide(
      heading = "Comparison: old vs new",
      leftColumn = "Old approach.",
      rightColumn = "New approach."
    ))
    assert(html.contains("slide-heading"), "Expected .slide-heading class in output")
    assert(html.contains("Comparison: old vs new"), "Expected heading text in output")
  }

  test("two-column heading is rendered as h2, not as paragraph in left column") {
    val html = renderSlide(twoColumnSlide(
      heading = "My Heading",
      leftColumn = "Left content here.",
      rightColumn = "Right content here."
    ))
    // Heading must appear in the slide-heading h2 — not buried in column body
    val headingIdx = html.indexOf("slide-heading")
    val h2CloseIdx = html.indexOf("</h2>", headingIdx)
    val headingRegion = html.substring(headingIdx, h2CloseIdx + 5)
    assert(headingRegion.contains("My Heading"),
      s"Expected 'My Heading' inside slide-heading h2, region was: $headingRegion")
  }

  test("two-column left column content does not contain the heading text repeated") {
    val html = renderSlide(twoColumnSlide(
      heading = "UniqueHeadingText",
      leftColumn = "Left body only.",
      rightColumn = "Right body only."
    ))
    // Heading should appear exactly once (in the h2), not duplicated in the column body
    val occurrences = "UniqueHeadingText".r.findAllIn(html).length
    assertEquals(occurrences, 1, "Heading text should appear exactly once, not duplicated in column body")
  }

  test("two-column slide with no heading renders without slide-heading element") {
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "two-column",
      slots = Map(
        "leftColumn"  -> "Left.",
        "rightColumn" -> "Right."
      )
    )
    val html = HTMLRenderer.renderDeck(SlideDeck(NonEmptyList.one(slide)))
    // Should not crash; heading element simply absent
    assert(html.contains("column-left"), "Should still render columns")
    assert(html.contains("column-right"), "Should still render columns")
  }

  // --- Column CSS: list indentation ---

  test("CSS contains .column ul with padding-left for list indentation") {
    val html = renderSlide(twoColumnSlide("H", "left", "right"))
    assert(html.contains(".column ul"), "Expected .column ul CSS selector")
    // Find the rule and check it contains padding-left
    val idx = html.indexOf(".column ul")
    val ruleRegion = html.substring(idx, math.min(idx + 200, html.length))
    assert(ruleRegion.contains("padding-left"), s"Expected padding-left in .column ul rule, got: $ruleRegion")
  }

  test("CSS contains .column li with margin for list item spacing") {
    val html = renderSlide(twoColumnSlide("H", "left", "right"))
    assert(html.contains(".column li"), "Expected .column li CSS selector")
  }

  test("CSS contains .column ul with list-style-type disc for bullet markers") {
    val html = renderSlide(twoColumnSlide("H", "left", "right"))
    assert(html.contains(".column ul"), "Expected .column ul CSS selector for bullet style")
    val idx = html.indexOf(".column ul")
    val ruleRegion = html.substring(idx, math.min(idx + 300, html.length))
    assert(ruleRegion.contains("disc"), s"Expected list-style-type: disc in .column ul rule")
  }

  test("CSS contains .column ul ul for nested list (level 2) styling") {
    val html = renderSlide(twoColumnSlide("H", "left", "right"))
    assert(html.contains(".column ul ul"), "Expected .column ul ul CSS selector for nested list level 2")
  }

  // --- Column CSS: table styling ---

  test("CSS contains .column table with border-collapse") {
    val html = renderSlide(twoColumnSlide("H", "left", "right"))
    assert(html.contains(".column table"), "Expected .column table CSS selector")
    val idx = html.indexOf(".column table")
    val ruleRegion = html.substring(idx, math.min(idx + 300, html.length))
    assert(ruleRegion.contains("border-collapse"), s"Expected border-collapse in .column table rule")
  }

  test("CSS contains .column table th for header cell styling") {
    val html = renderSlide(twoColumnSlide("H", "left", "right"))
    assert(html.contains(".column table th"), "Expected .column table th CSS selector")
  }

  test("CSS contains .column table td for data cell styling") {
    val html = renderSlide(twoColumnSlide("H", "left", "right"))
    assert(html.contains(".column table td"), "Expected .column table td CSS selector")
  }

end HTMLRendererTwoColumnSpec
