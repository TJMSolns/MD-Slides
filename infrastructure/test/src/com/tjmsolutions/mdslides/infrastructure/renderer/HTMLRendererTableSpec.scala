package com.tjmsolutions.mdslides.infrastructure.renderer

import com.tjmsolutions.mdslides.domain.{Slide, SlideDeck, SlideId}
import cats.data.NonEmptyList
import munit.FunSuite

/**
 * Tests for HTML rendering of markdown tables (BUG-003).
 *
 * Verifies that a GFM table in a slide body slot is rendered as a proper
 * HTML <table> with correct structure, alignment, and CSS hooks.
 *
 * Slots contain raw markdown strings; the renderer parses them via
 * FlexmarkAdapter before building HTML, so these tests cover the full
 * parse → render round-trip within the infrastructure layer.
 *
 * Related Governance:
 * - BUG-003: Markdown Tables Not Rendering
 * - three-amigos-BUG-003.md: AC1 (HTML structure), AC2 (alignment), AC6 (empty body)
 */
class HTMLRendererTableSpec extends munit.FunSuite:

  private def slideWithBody(bodyMarkdown: String): SlideDeck =
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "content",
      slots = Map(
        "heading" -> "Test Slide",
        "body"    -> bodyMarkdown
      )
    )
    SlideDeck(NonEmptyList.one(slide))

  // ── AC1: HTML Structure ───────────────────────────────────────────────────────

  test("BUG-003 / AC1 — table renders as <table> element"):
    val body = """|Tool|Version|
                  ||-----|-------|
                  ||Mill|0.11.6|
                  ||Scala|3.3.1|""".stripMargin('|')
    val html = HTMLRenderer.renderDeck(slideWithBody(body))
    assert(html.contains("<table"), s"Expected <table> in output")

  test("BUG-003 / AC1 — headers appear in <th> inside <thead>"):
    val body = """|Role|Responsibility|
                  ||------|----------------|
                  ||Architect|Domain modeling|""".stripMargin('|')
    val html = HTMLRenderer.renderDeck(slideWithBody(body))

    assert(html.contains("<thead"), "Expected <thead>")
    assert(html.contains("<th"), "Expected <th> for header cells")
    assert(html.contains("Role"), "Expected header 'Role'")
    assert(html.contains("Responsibility"), "Expected header 'Responsibility'")

  test("BUG-003 / AC1 — body rows appear in <td> inside <tbody>"):
    val body = """|A|B|
                  ||---|---|
                  ||alpha|beta|
                  ||gamma|delta|""".stripMargin('|')
    val html = HTMLRenderer.renderDeck(slideWithBody(body))

    assert(html.contains("<tbody"), "Expected <tbody>")
    assert(html.contains("<td"), "Expected <td> for body cells")
    assert(html.contains("alpha"), "Expected cell text 'alpha'")
    assert(html.contains("delta"), "Expected cell text 'delta'")

  test("BUG-003 / AC1 — separator row does not appear as data row"):
    val body = """|A|B|
                  ||---|---|
                  ||1|2|""".stripMargin('|')
    val html = HTMLRenderer.renderDeck(slideWithBody(body))

    // The separator "---|---" should not appear as text content
    assert(!html.contains("---|---"), "Separator row must not appear in rendered HTML")

  // ── AC2: Alignment ────────────────────────────────────────────────────────────

  test("BUG-003 / AC2 — left alignment applies text-align: left"):
    val body = """|Name|
                  ||:---|
                  ||Alice|""".stripMargin('|')
    val html = HTMLRenderer.renderDeck(slideWithBody(body))

    assert(html.contains("text-align: left"), "Expected 'text-align: left'")

  test("BUG-003 / AC2 — right alignment applies text-align: right"):
    val body = """|Score|
                  ||---:|
                  ||95|""".stripMargin('|')
    val html = HTMLRenderer.renderDeck(slideWithBody(body))

    assert(html.contains("text-align: right"), "Expected 'text-align: right'")

  test("BUG-003 / AC2 — center alignment applies text-align: center"):
    val body = """|Grade|
                  ||:---:|
                  ||A|""".stripMargin('|')
    val html = HTMLRenderer.renderDeck(slideWithBody(body))

    assert(html.contains("text-align: center"), "Expected 'text-align: center'")

  test("BUG-003 / AC2 — plain separator falls back to text-align: left"):
    val body = """|Col|
                  ||---|
                  ||val|""".stripMargin('|')
    val html = HTMLRenderer.renderDeck(slideWithBody(body))

    assert(html.contains("text-align: left"), "Plain separator must fall back to left")

  // ── AC4: Source Order ─────────────────────────────────────────────────────────

  test("BUG-003 / AC4 — table appears between surrounding paragraphs in rendered HTML"):
    val body =
      """First paragraph.
        |
        || A | B |
        ||---|---|
        || 1 | 2 |
        |
        |Last paragraph.""".stripMargin

    val html = HTMLRenderer.renderDeck(slideWithBody(body))

    val pFirst = html.indexOf("First paragraph")
    val tableIdx = html.indexOf("<table")
    val pLast = html.indexOf("Last paragraph")

    assert(pFirst >= 0, "Expected 'First paragraph' in output")
    assert(tableIdx >= 0, "Expected <table> in output")
    assert(pLast >= 0, "Expected 'Last paragraph' in output")
    assert(pFirst < tableIdx, "First paragraph must precede table")
    assert(tableIdx < pLast, "Table must precede last paragraph")

  // ── AC6: Empty Table Body ─────────────────────────────────────────────────────

  test("BUG-003 / AC6 — headers-only table renders without crashing"):
    val body = """|Col1|Col2|
                  ||------|------|""".stripMargin('|')
    val html = HTMLRenderer.renderDeck(slideWithBody(body))

    assert(html.contains("<table"), "Expected <table>")
    assert(html.contains("<thead"), "Expected <thead>")
    assert(html.contains("Col1"), "Expected header 'Col1'")
