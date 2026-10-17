package com.tjmsolutions.mdslides.domain

import munit.FunSuite

/**
 * Tests for FormattedContent value object.
 *
 * FormattedContent represents parsed markdown with structure:
 * - Text spans with inline formatting (bold, italic, code)
 * - Hyperlinks
 * - Raw text extraction (for validation)
 *
 * Related Governance:
 * - US-003: Full Markdown Rendering
 * - PDR-001: Density Validation Limits
 * - POL-004: Property-Based Testing Requirements
 */
class FormattedContentSpec extends FunSuite:

  test("FormattedContent.plainText - single text span without formatting"):
    val span = TextSpan("Hello world", bold = false, italic = false, code = false)
    val content = FormattedContent(List(span), List.empty, List.empty)

    assertEquals(content.plainText, "Hello world")

  test("FormattedContent.plainText - text span with bold formatting"):
    val span = TextSpan("important", bold = true, italic = false, code = false)
    val content = FormattedContent(List(span), List.empty, List.empty)

    // Plain text strips formatting
    assertEquals(content.plainText, "important")

  test("FormattedContent.plainText - text span with italic formatting"):
    val span = TextSpan("emphasis", bold = false, italic = true, code = false)
    val content = FormattedContent(List(span), List.empty, List.empty)

    assertEquals(content.plainText, "emphasis")

  test("FormattedContent.plainText - text span with inline code"):
    val span = TextSpan("println", bold = false, italic = false, code = true)
    val content = FormattedContent(List(span), List.empty, List.empty)

    assertEquals(content.plainText, "println")

  test("FormattedContent.plainText - multiple text spans"):
    val spans = List(
      TextSpan("MDSlides is ", bold = false, italic = false, code = false),
      TextSpan("domain-driven", bold = true, italic = false, code = false),
      TextSpan(" and ", bold = false, italic = false, code = false),
      TextSpan("well-tested", bold = false, italic = true, code = false),
      TextSpan(".", bold = false, italic = false, code = false)
    )
    val content = FormattedContent(spans, List.empty, List.empty)

    assertEquals(content.plainText, "MDSlides is domain-driven and well-tested.")

  test("FormattedContent.plainText - with hyperlink"):
    val spans = List(
      TextSpan("Learn more at ", bold = false, italic = false, code = false)
    )
    val links = List(
      Link("our docs", "https://example.com")
    )
    val content = FormattedContent(spans, links, List.empty)

    // Plain text includes link text, not URL
    assert(content.plainText.contains("our docs"))
    assert(!content.plainText.contains("https://example.com"))

  test("FormattedContent.wordCount - counts plain text words only"):
    val spans = List(
      TextSpan("MDSlides is ", bold = false, italic = false, code = false),
      TextSpan("domain-driven", bold = true, italic = false, code = false),
      TextSpan(" and ", bold = false, italic = false, code = false),
      TextSpan("well-tested", bold = false, italic = true, code = false),
      TextSpan(".", bold = false, italic = false, code = false)
    )
    val content = FormattedContent(spans, List.empty, List.empty)

    // "MDSlides is domain-driven and well-tested." = 5 words
    assertEquals(content.wordCount, 5)

  test("FormattedContent.wordCount - includes link text"):
    val spans = List(
      TextSpan("Learn more at ", bold = false, italic = false, code = false)
    )
    val links = List(
      Link("our docs", "https://example.com")
    )
    val content = FormattedContent(spans, links, List.empty)

    // "Learn more at our docs" = 5 words
    assertEquals(content.wordCount, 5)

  test("FormattedContent.lineCount - counts newlines in plain text"):
    val spans = List(
      TextSpan("Line one\n", bold = false, italic = false, code = false),
      TextSpan("Line two", bold = false, italic = false, code = false)
    )
    val content = FormattedContent(spans, List.empty, List.empty)

    assertEquals(content.lineCount, 2)

  test("FormattedContent.lineCount - single line with no newline"):
    val spans = List(
      TextSpan("Single line", bold = false, italic = false, code = false)
    )
    val content = FormattedContent(spans, List.empty, List.empty)

    assertEquals(content.lineCount, 1)

  test("FormattedContent.empty - creates empty content"):
    val content = FormattedContent.empty

    assertEquals(content.plainText, "")
    assertEquals(content.wordCount, 0)
    assertEquals(content.lineCount, 0)

  // List support tests (US-003 enhancement)

  test("FormattedContent with unordered list - plainText includes list items"):
    val listItems = List(
      ListItem(List(TextSpan("First item", bold = false, italic = false, code = false))),
      ListItem(List(TextSpan("Second item", bold = false, italic = false, code = false)))
    )
    val list = UnorderedList(listItems)
    val content = FormattedContent(List.empty, List.empty, List.empty, List.empty, List(list), List.empty)

    val plain = content.plainText
    assert(plain.contains("First item"))
    assert(plain.contains("Second item"))

  test("FormattedContent with ordered list - plainText includes list items"):
    val listItems = List(
      ListItem(List(TextSpan("Step one", bold = false, italic = false, code = false))),
      ListItem(List(TextSpan("Step two", bold = false, italic = false, code = false)))
    )
    val list = OrderedList(listItems)
    val content = FormattedContent(List.empty, List.empty, List.empty, List.empty, List.empty, List(list))

    val plain = content.plainText
    assert(plain.contains("Step one"))
    assert(plain.contains("Step two"))

  test("FormattedContent with list - wordCount includes list item words"):
    val listItems = List(
      ListItem(List(TextSpan("Three word item", bold = false, italic = false, code = false))),
      ListItem(List(TextSpan("Two words", bold = false, italic = false, code = false)))
    )
    val list = UnorderedList(listItems)
    val content = FormattedContent(List.empty, List.empty, List.empty, List.empty, List(list), List.empty)

    // "Three word item" (3) + "Two words" (2) = 5 words
    assertEquals(content.wordCount, 5)

  test("ListItem with formatted text - supports bold and italic"):
    val spans = List(
      TextSpan("Normal ", bold = false, italic = false, code = false),
      TextSpan("bold", bold = true, italic = false, code = false),
      TextSpan(" and ", bold = false, italic = false, code = false),
      TextSpan("italic", bold = false, italic = true, code = false)
    )
    val item = ListItem(spans)

    assertEquals(item.textSpans.length, 4)
    assertEquals(item.plainText, "Normal bold and italic")

  // Nested list support tests (US-003.3 - v1.2)

  test("calculateNestingDepth - single level unordered list has depth 1"):
    val listItems = List(
      ListItem(List(TextSpan("Item 1", bold = false, italic = false, code = false))),
      ListItem(List(TextSpan("Item 2", bold = false, italic = false, code = false)))
    )
    val list = UnorderedList(listItems)
    val content = FormattedContent(List.empty, List.empty, List.empty, List.empty, List(list), List.empty)

    assertEquals(content.maxNestingDepth, 1)

  test("calculateNestingDepth - two level nested unordered list has depth 2"):
    val nestedItem = ListItem(
      List(TextSpan("Level 2 item", bold = false, italic = false, code = false)),
      nestedUnorderedLists = List(UnorderedList(List(
        ListItem(List(TextSpan("Level 2 nested", bold = false, italic = false, code = false)))
      )))
    )
    val list = UnorderedList(List(nestedItem))
    val content = FormattedContent(List.empty, List.empty, List.empty, List.empty, List(list), List.empty)

    assertEquals(content.maxNestingDepth, 2)

  test("calculateNestingDepth - three level nested list has depth 3"):
    val level3Item = ListItem(
      List(TextSpan("Level 3", bold = false, italic = false, code = false))
    )
    val level2Item = ListItem(
      List(TextSpan("Level 2", bold = false, italic = false, code = false)),
      nestedUnorderedLists = List(UnorderedList(List(level3Item)))
    )
    val level1Item = ListItem(
      List(TextSpan("Level 1", bold = false, italic = false, code = false)),
      nestedUnorderedLists = List(UnorderedList(List(level2Item)))
    )
    val list = UnorderedList(List(level1Item))
    val content = FormattedContent(List.empty, List.empty, List.empty, List.empty, List(list), List.empty)

    assertEquals(content.maxNestingDepth, 3)
