package com.tjmsolutions.mdslides.infrastructure.renderer

import munit.FunSuite
import com.tjmsolutions.mdslides.domain._
import cats.data.NonEmptyList

/**
 * Regression tests for two-column layout CSS coverage (MS-014).
 *
 * Verifies that `.column` elements receive the same CSS styling as `.slide-body`,
 * covering lists (ul/ol/li), paragraphs (p), and tables (th/td/tbody).
 *
 * Root cause: column content is rendered directly inside `section.column` without
 * a `.slide-body` wrapper, so `.slide-body`-only selectors don't apply.
 * Fix: all `.slide-body X` rules also target `.column X`.
 */
class HTMLRendererTwoColumnSpec extends FunSuite:

  private def twoColumnSlide(leftContent: String, rightContent: String): Slide =
    Slide(
      id = SlideId.unsafe(1),
      templateName = "two-column",
      slots = Map(
        SlotName.LeftColumn -> leftContent,
        SlotName.RightColumn -> rightContent
      )
    )

  private def renderSlide(leftContent: String, rightContent: String): String =
    val deck = SlideDeck(NonEmptyList.one(twoColumnSlide(leftContent, rightContent)))
    HTMLRenderer.renderDeck(deck, Theme.light)

  // ===== CSS Selector Coverage Tests =====

  test("CSS includes .column ul selector for list margin and padding"):
    val html = renderSlide("- item", "- item")
    assert(html.contains(".column ul"), "CSS must include .column ul selector")

  test("CSS includes .column ol selector"):
    val html = renderSlide("1. item", "1. item")
    assert(html.contains(".column ol"), "CSS must include .column ol selector")

  test("CSS includes .column li selector"):
    val html = renderSlide("- item", "- item")
    assert(html.contains(".column li"), "CSS must include .column li selector")

  test("CSS includes .column p selector for paragraph margins"):
    val html = renderSlide("some text", "some text")
    assert(html.contains(".column p"), "CSS must include .column p selector")

  test("CSS includes .column table selector"):
    val html = renderSlide("| a | b |\n|---|---|\n| 1 | 2 |", "text")
    assert(html.contains(".column table"), "CSS must include .column table selector")

  test("CSS includes .column table th selector"):
    val html = renderSlide("| a | b |\n|---|---|\n| 1 | 2 |", "text")
    assert(html.contains(".column table th"), "CSS must include .column table th selector")

  test("CSS includes .column table td selector"):
    val html = renderSlide("| a | b |\n|---|---|\n| 1 | 2 |", "text")
    assert(html.contains(".column table td"), "CSS must include .column table td selector")

  // ===== Content Rendering Tests =====

  test("unordered list in left column renders ul element"):
    val html = renderSlide("- First\n- Second\n- Third", "text")
    assert(html.contains("<ul>"), "Should render ul element for bullet list")
    assert(html.contains("First"), "Should render first item")
    assert(html.contains("Second"), "Should render second item")

  test("ordered list in right column renders ol element"):
    val html = renderSlide("text", "1. Step one\n2. Step two")
    assert(html.contains("<ol>"), "Should render ol element for numbered list")
    assert(html.contains("Step one"), "Should render first item")

  test("paragraph text in column renders p element"):
    val html = renderSlide("This is a paragraph.", "Another paragraph.")
    assert(html.contains("<p>"), "Should render p element")
    assert(html.contains("This is a paragraph."), "Left column text present")
    assert(html.contains("Another paragraph."), "Right column text present")

  test("table in column renders table element with th and td"):
    val tableMarkdown = "| Name | Value |\n|------|-------|\n| foo  | bar   |"
    val html = renderSlide(tableMarkdown, "text")
    assert(html.contains("<table"), "Should render table element")
    assert(html.contains("<thead"), "Should render thead element")
    assert(html.contains("<th "), "Should render th element for headers")
    assert(html.contains("<td "), "Should render td element for data")
    assert(html.contains("Name"), "Should render header text")
    assert(html.contains("foo"), "Should render data text")

  test("nested list in column renders nested ul"):
    val nested = "- Parent\n  - Child\n  - Child 2"
    val html = renderSlide(nested, "text")
    assert(html.contains("<ul>"), "Should render outer ul")
    assert(html.contains("Parent"), "Should render parent item")
    assert(html.contains("Child"), "Should render child item")

end HTMLRendererTwoColumnSpec
