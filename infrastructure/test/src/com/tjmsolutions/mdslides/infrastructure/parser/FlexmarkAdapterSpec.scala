package com.tjmsolutions.mdslides.infrastructure.parser

import com.tjmsolutions.mdslides.domain.{FormattedContent, TextSpan, Link, CodeBlock}
import munit.FunSuite

/**
 * Tests for FlexmarkAdapter (Anticorruption Layer).
 *
 * Verifies that Flexmark AST is correctly translated to domain types.
 *
 * Related Governance:
 * - ADR-010: Markdown Library Selection
 * - ADR-007: Anticorruption Layer
 * - US-003: Full Markdown Rendering
 * - US-004: Code Block Support
 */
class FlexmarkAdapterSpec extends FunSuite:

  test("parse plain text without formatting"):
    val markdown = "Hello world"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(result.textSpans.length, 1)
    assertEquals(result.textSpans.head.text, "Hello world")
    assertEquals(result.textSpans.head.bold, false)
    assertEquals(result.textSpans.head.italic, false)
    assertEquals(result.textSpans.head.code, false)
    assertEquals(result.links, List.empty)

  test("parse bold text with **"):
    val markdown = "This is **bold** text"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assert(result.textSpans.exists(span => span.text == "bold" && span.bold))
    assertEquals(result.plainText.trim, "This is bold text")

  test("parse bold text with __"):
    val markdown = "This is __bold__ text"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assert(result.textSpans.exists(span => span.text == "bold" && span.bold))

  test("parse italic text with *"):
    val markdown = "This is *italic* text"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assert(result.textSpans.exists(span => span.text == "italic" && span.italic))
    assertEquals(result.plainText.trim, "This is italic text")

  test("parse italic text with _"):
    val markdown = "This is _italic_ text"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assert(result.textSpans.exists(span => span.text == "italic" && span.italic))

  test("parse inline code with backticks"):
    val markdown = "Use `println` to print"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assert(result.textSpans.exists(span => span.text == "println" && span.code))
    assertEquals(result.plainText.trim, "Use println to print")

  test("parse link"):
    val markdown = "Visit [our docs](https://example.com) for more"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(result.links.length, 1)
    assertEquals(result.links.head.text, "our docs")
    assertEquals(result.links.head.url, "https://example.com")

    // Link text should appear in plain text
    assert(result.plainText.contains("our docs"))
    assert(!result.plainText.contains("https://example.com"))

  test("parse combined bold and italic"):
    val markdown = "This is ***bold and italic*** text"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assert(result.textSpans.exists(span =>
      span.text == "bold and italic" && span.bold && span.italic
    ))

  test("parse multiple formatting in same line"):
    val markdown = "MDSlides is **domain-driven** and *well-tested*"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assert(result.textSpans.exists(span => span.text == "domain-driven" && span.bold))
    assert(result.textSpans.exists(span => span.text == "well-tested" && span.italic))
    assertEquals(result.plainText.trim, "MDSlides is domain-driven and well-tested")

  test("parse markdown with link and formatting"):
    val markdown = "**MDSlides** is documented at [our site](https://example.com)"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assert(result.textSpans.exists(span => span.text == "MDSlides" && span.bold))
    assertEquals(result.links.length, 1)
    assertEquals(result.links.head.text, "our site")

  test("parse empty string"):
    val markdown = ""
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(result, FormattedContent.empty)

  test("parse multiline text"):
    val markdown = "Line one\nLine two"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assert(result.plainText.contains("Line one"))
    assert(result.plainText.contains("Line two"))

  test("word count strips markdown syntax"):
    val markdown = "MDSlides is **domain-driven** and *well-tested*"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    // "MDSlides is domain-driven and well-tested" = 5 words
    assertEquals(result.wordCount, 5)

  test("plain text includes link text but not URL"):
    val markdown = "Learn more at [our docs](https://example.com)"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assert(result.plainText.contains("our docs"))
    assert(!result.plainText.contains("https://example.com"))

  test("handles nested formatting correctly"):
    val markdown = "This is **bold with *italic inside* it**"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    // Should have both bold and italic text
    assert(result.textSpans.exists(span => span.bold))
    assert(result.textSpans.exists(span => span.italic))

  test("parse fenced code block without language"):
    val markdown = "```\ndef hello():\n    print('Hello')\n```"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(result.codeBlocks.length, 1)
    val codeBlock = result.codeBlocks.head
    assertEquals(codeBlock.code, "def hello():\n    print('Hello')\n")
    assertEquals(codeBlock.language, None)

  test("parse fenced code block with language"):
    val markdown = "```scala\ndef add(a: Int, b: Int): Int =\n  a + b\n```"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(result.codeBlocks.length, 1)
    val codeBlock = result.codeBlocks.head
    assert(codeBlock.code.contains("def add"))
    assertEquals(codeBlock.language, Some("scala"))

  test("parse multiple code blocks"):
    val markdown = """
      |Some text
      |```python
      |print('hello')
      |```
      |More text
      |```javascript
      |console.log('world');
      |```
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(result.codeBlocks.length, 2)
    assertEquals(result.codeBlocks(0).language, Some("python"))
    assertEquals(result.codeBlocks(1).language, Some("javascript"))

  test("parse text with inline code and code block"):
    val markdown = """
      |Use `println` for output.
      |
      |Example:
      |```scala
      |println("Hello")
      |```
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    // Should have inline code span
    assert(result.textSpans.exists(span => span.text == "println" && span.code))
    // Should have code block
    assertEquals(result.codeBlocks.length, 1)
    assert(result.codeBlocks.head.code.contains("println"))

  test("code blocks don't appear in plain text"):
    val markdown = """
      |Some text
      |```
      |code here
      |```
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    // Plain text should not contain code block content (per PDR-006)
    assert(!result.plainText.contains("code here"))
    assert(result.plainText.contains("Some text"))

  // Nested list support tests (US-003.3 - v1.2 TDD Phase 2)

  test("parse 2-level nested unordered list"):
    val markdown = """
      |- Level 1 item A
      |  - Level 2 item A.1
      |  - Level 2 item A.2
      |- Level 1 item B
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(result.unorderedLists.length, 1)
    val list = result.unorderedLists.head
    assertEquals(list.items.length, 2)

    // First item should have 2 nested items
    val firstItem = list.items(0)
    assertEquals(firstItem.textSpans.head.text.trim, "Level 1 item A")
    assertEquals(firstItem.nestedUnorderedLists.length, 1)
    assertEquals(firstItem.nestedUnorderedLists.head.items.length, 2)
    assertEquals(firstItem.nestedUnorderedLists.head.items(0).plainText.trim, "Level 2 item A.1")
    assertEquals(firstItem.nestedUnorderedLists.head.items(1).plainText.trim, "Level 2 item A.2")

    // Second item should have no nesting
    val secondItem = list.items(1)
    assertEquals(secondItem.textSpans.head.text.trim, "Level 1 item B")
    assertEquals(secondItem.nestedUnorderedLists.length, 0)

  test("parse 2-level nested ordered list"):
    val markdown = """
      |1. First main point
      |   1. First sub-point
      |   2. Second sub-point
      |2. Second main point
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(result.orderedLists.length, 1)
    val list = result.orderedLists.head
    assertEquals(list.items.length, 2)

    // First item should have 2 nested items
    val firstItem = list.items(0)
    assertEquals(firstItem.textSpans.head.text.trim, "First main point")
    assertEquals(firstItem.nestedOrderedLists.length, 1)
    assertEquals(firstItem.nestedOrderedLists.head.items.length, 2)

  test("parse 3-level nested list (maximum depth)"):
    val markdown = """
      |- Level 1
      |  - Level 2
      |    - Level 3
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(result.unorderedLists.length, 1)
    val level1List = result.unorderedLists.head
    assertEquals(level1List.items.length, 1)

    val level1Item = level1List.items.head
    assertEquals(level1Item.nestedUnorderedLists.length, 1)

    val level2List = level1Item.nestedUnorderedLists.head
    assertEquals(level2List.items.length, 1)

    val level2Item = level2List.items.head
    assertEquals(level2Item.nestedUnorderedLists.length, 1)

    val level3List = level2Item.nestedUnorderedLists.head
    assertEquals(level3List.items.length, 1)
    assertEquals(level3List.items.head.plainText.trim, "Level 3")

  test("parse mixed nesting - ordered within unordered"):
    val markdown = """
      |- Overview
      |  1. Step one
      |  2. Step two
      |- Conclusion
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(result.unorderedLists.length, 1)
    val list = result.unorderedLists.head
    assertEquals(list.items.length, 2)

    val firstItem = list.items(0)
    assertEquals(firstItem.textSpans.head.text.trim, "Overview")
    assertEquals(firstItem.nestedOrderedLists.length, 1)
    assertEquals(firstItem.nestedOrderedLists.head.items.length, 2)

  test("parse mixed nesting - unordered within ordered"):
    val markdown = """
      |1. Introduction
      |   - Point A
      |   - Point B
      |2. Body
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(result.orderedLists.length, 1)
    val list = result.orderedLists.head
    assertEquals(list.items.length, 2)

    val firstItem = list.items(0)
    assertEquals(firstItem.textSpans.head.text.trim, "Introduction")
    assertEquals(firstItem.nestedUnorderedLists.length, 1)
    assertEquals(firstItem.nestedUnorderedLists.head.items.length, 2)

  test("parse complex mixed 3-level nesting"):
    val markdown = """
      |1. Design Phase
      |   - Create wireframes
      |   - Review with stakeholders
      |2. Implementation
      |   1. Backend development
      |      - API design
      |      - Database schema
      |   2. Frontend development
      |3. Testing
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(result.orderedLists.length, 1)
    val list = result.orderedLists.head
    assertEquals(list.items.length, 3)

    // Check "Implementation" item with deep nesting
    val implItem = list.items(1)
    assertEquals(implItem.textSpans.head.text.trim, "Implementation")
    assertEquals(implItem.nestedOrderedLists.length, 1)

    val backendItem = implItem.nestedOrderedLists.head.items(0)
    assertEquals(backendItem.nestedUnorderedLists.length, 1)
    assertEquals(backendItem.nestedUnorderedLists.head.items.length, 2)

  test("parse nested list with formatted text"):
    val markdown = """
      |- **Bold level 1**
      |  - *Italic level 2*
      |    - `Code level 3`
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(result.unorderedLists.length, 1)
    val level1Item = result.unorderedLists.head.items.head

    // Level 1 should be bold
    assert(level1Item.textSpans.exists(span => span.text.contains("Bold") && span.bold))

    // Level 2 should be italic
    val level2Item = level1Item.nestedUnorderedLists.head.items.head
    assert(level2Item.textSpans.exists(span => span.text.contains("Italic") && span.italic))

    // Level 3 should be code
    val level3Item = level2Item.nestedUnorderedLists.head.items.head
    assert(level3Item.textSpans.exists(span => span.text.contains("Code") && span.code))

  test("parse list with empty nested list - graceful handling"):
    val markdown = """
      |- Item with content
      |  -
      |- Item without nesting
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(result.unorderedLists.length, 1)
    // Should parse successfully without error

  test("parse multiple top-level lists with nesting"):
    val markdown = """
      |- List 1 item 1
      |  - Nested in list 1
      |
      |- List 2 item 1
      |  - Nested in list 2
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    // Flexmark may merge these into one list or keep separate
    // Just verify parsing succeeds
    assert(result.unorderedLists.nonEmpty)

  test("parse nested list preserves depth in maxNestingDepth"):
    val markdown = """
      |- Level 1
      |  - Level 2
      |    - Level 3
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    // Verify domain model calculation works with parsed data
    assertEquals(result.maxNestingDepth, 3)

  // Example Mapping US-003.3 Scenario 8: Mixed ordered → unordered (SIBLING LISTS)
  test("parse mixed ordered then unordered lists preserves source order"):
    val markdown = """
      |**Ordered Lists:**
      |1. First ordered item
      |2. Second ordered item
      |
      |**Unordered Lists:**
      |- First unordered item
      |- Second unordered item
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    // Verify we have both list types
    assertEquals(result.orderedLists.length, 1, "Should have 1 ordered list")
    assertEquals(result.unorderedLists.length, 1, "Should have 1 unordered list")

    // CRITICAL: Verify new lists field preserves source order
    assertEquals(result.lists.length, 2, "Should have 2 lists in order-preserving field")

    result.lists(0) match
      case com.tjmsolutions.mdslides.domain.OrderedListElementDeprecated(list) =>
        assertEquals(list.items.length, 2)
        assert(list.items(0).plainText.contains("First ordered"))
      case _ => fail("First list should be OrderedListElementDeprecated")

    result.lists(1) match
      case com.tjmsolutions.mdslides.domain.UnorderedListElementDeprecated(list) =>
        assertEquals(list.items.length, 2)
        assert(list.items(0).plainText.contains("First unordered"))
      case _ => fail("Second list should be UnorderedListElementDeprecated")

  // Example Mapping US-003.3 Scenario 9: Mixed unordered → ordered (SIBLING LISTS)
  test("parse mixed unordered then ordered lists preserves source order"):
    val markdown = """
      |**Unordered Lists:**
      |- Apple
      |- Banana
      |
      |**Ordered Lists:**
      |1. First step
      |2. Second step
      |3. Third step
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    // Verify we have both list types
    assertEquals(result.unorderedLists.length, 1, "Should have 1 unordered list")
    assertEquals(result.orderedLists.length, 1, "Should have 1 ordered list")

    // CRITICAL: Verify new lists field preserves source order
    assertEquals(result.lists.length, 2, "Should have 2 lists in order-preserving field")

    result.lists(0) match
      case com.tjmsolutions.mdslides.domain.UnorderedListElementDeprecated(list) =>
        assertEquals(list.items.length, 2)
        assert(list.items(0).plainText.contains("Apple"))
      case _ => fail("First list should be UnorderedListElementDeprecated")

    result.lists(1) match
      case com.tjmsolutions.mdslides.domain.OrderedListElementDeprecated(list) =>
        assertEquals(list.items.length, 3)
        assert(list.items(0).plainText.contains("First step"))
      case _ => fail("Second list should be OrderedListElementDeprecated")

end FlexmarkAdapterSpec
