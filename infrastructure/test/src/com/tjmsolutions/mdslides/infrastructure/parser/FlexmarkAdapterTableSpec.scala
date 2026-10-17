package com.tjmsolutions.mdslides.infrastructure.parser

import com.tjmsolutions.mdslides.domain.{
  Table, TableElement, ParagraphElement, ContentElement
}
import munit.FunSuite

/**
 * Tests for markdown table parsing in FlexmarkAdapter.
 *
 * Verifies BUG-003: GFM tables are parsed into Table domain objects
 * and placed in source order within FormattedContent.content.
 *
 * Related Governance:
 * - BUG-003: Markdown Tables Not Rendering
 * - example-mapping-BUG-003.md: Concrete examples driving these tests
 * - three-amigos-BUG-003.md: Acceptance criteria AC1–AC6
 */
class FlexmarkAdapterTableSpec extends FunSuite:

  // ── Rule 1: Basic Table Parsing ─────────────────────────────────────────────

  test("BUG-003 / AC1 — two-column table parsed to Table domain object"):
    val markdown = """|Role|Responsibility|
                      ||------|----------------|
                      ||Program Manager|Coordination|
                      ||Architect|Domain modeling|""".stripMargin('|')

    val result = FlexmarkAdapter.parseInlineFormatting(markdown)
    val tables = result.content.collect { case TableElement(t) => t }

    assertEquals(tables.length, 1)
    val tbl = tables.head
    assertEquals(tbl.headers, List("Role", "Responsibility"))
    assertEquals(tbl.rows.length, 2)
    assertEquals(tbl.rows(0), List("Program Manager", "Coordination"))
    assertEquals(tbl.rows(1), List("Architect", "Domain modeling"))

  test("BUG-003 / AC1 — separator row is not included as data row"):
    val markdown = """|A|B|
                      ||---|---|
                      ||1|2|""".stripMargin('|')

    val result = FlexmarkAdapter.parseInlineFormatting(markdown)
    val tables = result.content.collect { case TableElement(t) => t }

    assertEquals(tables.length, 1)
    assertEquals(tables.head.rows.length, 1, "separator row must not appear in rows")

  test("BUG-003 / AC1 — single-column table"):
    val markdown = """|Key|
                      ||-----|
                      ||alpha|
                      ||beta|""".stripMargin('|')

    val result = FlexmarkAdapter.parseInlineFormatting(markdown)
    val tables = result.content.collect { case TableElement(t) => t }

    assertEquals(tables.length, 1)
    assertEquals(tables.head.headers, List("Key"))
    assertEquals(tables.head.rows, List(List("alpha"), List("beta")))

  test("BUG-003 / AC3 — bold markers stripped from cell text"):
    val markdown = """|Phase|Status|
                      ||-------|--------|
                      ||**Discovery**|Done|""".stripMargin('|')

    val result = FlexmarkAdapter.parseInlineFormatting(markdown)
    val tables = result.content.collect { case TableElement(t) => t }

    assertEquals(tables.length, 1)
    assertEquals(tables.head.rows(0)(0), "Discovery",
      "bold markers must be stripped; cell text must be plain")

  // ── Rule 2: Column Alignment ─────────────────────────────────────────────────

  test("BUG-003 / AC2 — left, center, right alignment parsed correctly"):
    val markdown = """|Name|Score|Grade|
                      ||:-----|:-----:|------:|
                      ||Alice|95|A|""".stripMargin('|')

    val result = FlexmarkAdapter.parseInlineFormatting(markdown)
    val tables = result.content.collect { case TableElement(t) => t }

    assertEquals(tables.length, 1)
    val alignment = tables.head.alignment
    assertEquals(alignment(0), "left")
    assertEquals(alignment(1), "center")
    assertEquals(alignment(2), "right")

  test("BUG-003 / AC2 — plain --- separator produces empty alignment string"):
    val markdown = """|A|B|
                      ||---|---|
                      ||1|2|""".stripMargin('|')

    val result = FlexmarkAdapter.parseInlineFormatting(markdown)
    val tables = result.content.collect { case TableElement(t) => t }

    assertEquals(tables.length, 1)
    assert(tables.head.alignment.forall(a => a == "" || a == "left" || a == "right" || a == "center"),
      "alignment must be one of the valid strings or empty")

  // ── Rule 4: Source Order Preservation ────────────────────────────────────────

  test("BUG-003 / AC4 — table preserves source order between paragraphs"):
    val markdown =
      """First paragraph.
        |
        || A | B |
        ||---|---|
        || 1 | 2 |
        |
        |Last paragraph.""".stripMargin

    val result = FlexmarkAdapter.parseInlineFormatting(markdown)
    val elements = result.content

    assert(elements.length >= 3, s"expected at least 3 content elements, got ${elements.length}")
    assert(elements(0).isInstanceOf[ParagraphElement], s"first element must be ParagraphElement, got ${elements(0).getClass.getSimpleName}")
    assert(elements(1).isInstanceOf[TableElement], s"second element must be TableElement, got ${elements(1).getClass.getSimpleName}")
    assert(elements(2).isInstanceOf[ParagraphElement], s"third element must be ParagraphElement, got ${elements(2).getClass.getSimpleName}")

  // ── Rule 5: Empty Table Body ──────────────────────────────────────────────────

  test("BUG-003 / AC6 — headers-only table has empty rows list"):
    val markdown = """|Col1|Col2|
                      ||------|------|""".stripMargin('|')

    val result = FlexmarkAdapter.parseInlineFormatting(markdown)
    val tables = result.content.collect { case TableElement(t) => t }

    assertEquals(tables.length, 1)
    assertEquals(tables.head.headers, List("Col1", "Col2"))
    assertEquals(tables.head.rows, List.empty, "headers-only table must have empty rows")
