package com.tjmsolutions.mdslides.domain

/**
 * Sealed trait representing ANY content element (paragraph, list, image, code block).
 * Used to preserve source order of ALL content types (BUG-001 REAL fix).
 */
sealed trait ContentElement

/**
 * Wrapper for a paragraph (group of text spans) in source-ordered content.
 */
case class ParagraphElement(spans: List[TextSpan]) extends ContentElement

/**
 * Wrapper for an unordered list in source-ordered content.
 */
case class UnorderedListElement(list: UnorderedList) extends ContentElement

/**
 * Wrapper for an ordered list in source-ordered content.
 */
case class OrderedListElement(list: OrderedList) extends ContentElement

/**
 * Wrapper for a code block in source-ordered content.
 */
case class CodeBlockElement(block: CodeBlock) extends ContentElement

/**
 * Wrapper for an image in source-ordered content.
 */
case class ImageElement(image: ContentImage) extends ContentElement

/**
 * Wrapper for a Mermaid diagram in source-ordered content (v2.0.0).
 */
case class DiagramElement(diagram: MermaidDiagram) extends ContentElement

/**
 * Wrapper for a table in source-ordered content.
 */
case class TableElement(table: Table) extends ContentElement

/**
 * DEPRECATED: Old sealed trait for list-only ordering (incomplete fix).
 * Use ContentElement instead which preserves ALL content ordering.
 */
sealed trait ListElement

/**
 * DEPRECATED: Use UnorderedListElement extends ContentElement instead.
 */
case class UnorderedListElementDeprecated(list: UnorderedList) extends ListElement

/**
 * DEPRECATED: Use OrderedListElement extends ContentElement instead.
 */
case class OrderedListElementDeprecated(list: OrderedList) extends ListElement

/**
 * Formatted content represents parsed markdown with structure.
 *
 * Contains:
 * - Text spans with inline formatting (bold, italic, code)
 * - Hyperlinks
 * - Code blocks (fenced code with optional language)
 * - Content images (informational images with alt text)
 * - Unordered lists (bullet points)
 * - Ordered lists (numbered items)
 *
 * Validation uses plain text (formatting stripped) for density checks.
 * Code blocks and images are excluded from body line/word counts (PDR-006, PDR-008).
 *
 * Related Governance:
 * - US-003: Full Markdown Rendering (inline formatting, lists)
 * - US-004: Code Block Support (fenced code blocks)
 * - US-005: Image Embedding (content images)
 * - PDR-001: Density Validation Limits
 * - PDR-006: Code Block Rendering Limits
 * - PDR-008: Image Policy
 * - POL-003: Pure Functional Domain
 *
 * @param textSpans DEPRECATED - use content field (kept for backward compat)
 * @param links Hyperlinks embedded in content
 * @param codeBlocks DEPRECATED - use content field (kept for backward compat)
 * @param contentImages DEPRECATED - use content field (kept for backward compat)
 * @param unorderedLists DEPRECATED - use content field (kept for backward compat)
 * @param orderedLists DEPRECATED - use content field (kept for backward compat)
 * @param lists DEPRECATED - use content field (kept for backward compat)
 * @param content ALL content in source order (BUG-001 REAL fix - paragraphs, lists, images, code blocks)
 */
case class FormattedContent(
  textSpans: List[TextSpan],
  links: List[Link],
  codeBlocks: List[CodeBlock] = List.empty,
  contentImages: List[ContentImage] = List.empty,
  unorderedLists: List[UnorderedList] = List.empty,
  orderedLists: List[OrderedList] = List.empty,
  lists: List[ListElement] = List.empty,
  content: List[ContentElement] = List.empty
):
  /**
   * Extract plain text (strip formatting).
   *
   * Used for density validation (word count, line count).
   * Includes link text but not URLs.
   * Includes list item text.
   */
  def plainText: String =
    val spanText = textSpans.map(_.text).mkString
    val linkText = links.map(_.text).mkString(" ")
    val unorderedListText = unorderedLists.flatMap(_.items.map(_.plainText)).mkString(" ")
    val orderedListText = orderedLists.flatMap(_.items.map(_.plainText)).mkString(" ")

    List(spanText, linkText, unorderedListText, orderedListText)
      .filter(_.nonEmpty)
      .mkString(" ")

  /**
   * Count words in plain text.
   *
   * Counts visible words only (no markdown syntax, no URLs).
   */
  def wordCount: Int =
    val text = plainText.trim
    if text.isEmpty then 0
    else text.split("\\s+").length

  /**
   * Count lines in plain text.
   *
   * Counts newline-separated lines.
   */
  def lineCount: Int =
    val text = plainText
    if text.isEmpty then 0
    else text.split("\n").length

  /**
   * Check visual density for content images (PDR-008).
   *
   * Returns warning message if too many images on one slide:
   * - 1-2 images: No warning
   * - 3-4 images: Warning (high density)
   * - 5+ images: Warning (excessive density)
   *
   * @return Optional warning message (to be wrapped by validation layer)
   */
  def visualDensityWarning: Option[String] =
    if contentImages.length >= 5 then
      Some(s"Excessive visual density (${contentImages.length} images). Consider splitting across slides.")
    else if contentImages.length >= 3 then
      Some(s"High visual density (${contentImages.length} images).")
    else
      None

  /**
   * Calculate the maximum nesting depth across all lists (US-003.3).
   *
   * Returns the maximum nesting depth found in any unordered or ordered list.
   * Returns 0 if no lists are present.
   */
  def maxNestingDepth: Int =
    val unorderedDepths = unorderedLists.map(_.maxNestingDepth)
    val orderedDepths = orderedLists.map(_.maxNestingDepth)
    (unorderedDepths ++ orderedDepths).maxOption.getOrElse(0)

object FormattedContent:
  /**
   * Empty formatted content (no text, no links, no code blocks, no images, no lists).
   */
  val empty: FormattedContent = FormattedContent(
    List.empty,
    List.empty,
    List.empty,
    List.empty,
    List.empty,
    List.empty
  )

/**
 * Text span with optional inline formatting.
 *
 * Represents a segment of text that may be bold, italic, or inline code.
 *
 * @param text The raw text content
 * @param bold True if text is bold (**text** or __text__)
 * @param italic True if text is italic (*text* or _text_)
 * @param code True if text is inline code (`text`)
 */
case class TextSpan(
  text: String,
  bold: Boolean,
  italic: Boolean,
  code: Boolean
)

/**
 * Hyperlink with text and URL.
 *
 * Represents markdown link: [text](url)
 *
 * @param text Link text (visible to user)
 * @param url Link target (href)
 */
case class Link(
  text: String,
  url: String
)

/**
 * List item containing formatted text spans and optional nested lists.
 *
 * Represents a single item in an ordered or unordered list.
 * List items can contain inline formatting (bold, italic, code).
 * List items can contain nested unordered and/or ordered lists (US-003.3).
 *
 * @param textSpans Formatted text content of the list item
 * @param nestedUnorderedLists Nested unordered lists (default empty)
 * @param nestedOrderedLists Nested ordered lists (default empty)
 */
case class ListItem(
  textSpans: List[TextSpan],
  nestedUnorderedLists: List[UnorderedList] = List.empty,
  nestedOrderedLists: List[OrderedList] = List.empty
):
  /**
   * Extract plain text from list item (including nested lists).
   */
  def plainText: String =
    val spanText = textSpans.map(_.text).mkString
    val nestedUnorderedText = nestedUnorderedLists.flatMap(_.items.map(_.plainText)).mkString(" ")
    val nestedOrderedText = nestedOrderedLists.flatMap(_.items.map(_.plainText)).mkString(" ")

    List(spanText, nestedUnorderedText, nestedOrderedText)
      .filter(_.nonEmpty)
      .mkString(" ")

  /**
   * Calculate the maximum nesting depth of this list item.
   *
   * Returns 1 for leaf items (no nested lists).
   * Returns 1 + max(child depths) for items with nested lists.
   */
  def maxNestingDepth: Int =
    val unorderedDepths = nestedUnorderedLists.map(_.maxNestingDepth)
    val orderedDepths = nestedOrderedLists.map(_.maxNestingDepth)
    val maxChildDepth = (unorderedDepths ++ orderedDepths).maxOption.getOrElse(0)

    if maxChildDepth == 0 then 1 else 1 + maxChildDepth

/**
 * Unordered list (bullet points).
 *
 * Represents markdown unordered list:
 * - Item one
 * - Item two
 *
 * Can be nested inside other lists (US-003.3).
 *
 * @param items List items in order
 */
case class UnorderedList(
  items: List[ListItem]
):
  /**
   * Calculate the maximum nesting depth of this list.
   *
   * Returns the maximum depth across all items.
   */
  def maxNestingDepth: Int =
    items.map(_.maxNestingDepth).maxOption.getOrElse(1)

/**
 * Ordered list (numbered items).
 *
 * Represents markdown ordered list:
 * 1. First item
 * 2. Second item
 *
 * Can be nested inside other lists (US-003.3).
 *
 * @param items List items in order
 */
case class OrderedList(
  items: List[ListItem]
):
  /**
   * Calculate the maximum nesting depth of this list.
   *
   * Returns the maximum depth across all items.
   */
  def maxNestingDepth: Int =
    items.map(_.maxNestingDepth).maxOption.getOrElse(1)

/**
 * Mermaid diagram (v2.0.0).
 *
 * Represents a Mermaid.js diagram embedded in markdown using ```mermaid code fence.
 * Rendered client-side via Mermaid.js CDN in the browser.
 *
 * Diagram types supported:
 * - flowchart (graph TD/LR)
 * - sequence (sequenceDiagram)
 * - class (classDiagram)
 * - state (stateDiagram/stateDiagram-v2)
 * - gantt (gantt)
 * - pie (pie)
 * - er (erDiagram)
 * - journey (journey)
 *
 * Related Governance:
 * - v2.0.0: Mermaid Diagram Support (US-022)
 * - POL-008: Client-side rendering only (no server-side SVG)
 * - POL-009: Graceful degradation if CDN unavailable
 * - POL-010: Alt text required for accessibility (WCAG 2.1 AA)
 *
 * @param source Mermaid diagram source code
 * @param diagramType Type of diagram (flowchart, sequence, class, etc.)
 * @param altText Accessibility alt text (required for WCAG 2.1 AA compliance)
 */
case class MermaidDiagram(
  source: String,
  diagramType: String,
  altText: Option[String]
)

object MermaidDiagram:
  /**
   * Detect diagram type from Mermaid source code.
   *
   * Examines the first non-whitespace line to identify diagram type.
   * Returns "unknown" if type cannot be determined.
   */
  def detectDiagramType(source: String): String =
    val trimmedSource = source.trim
    val firstLine = trimmedSource.linesIterator.nextOption().getOrElse("").trim.toLowerCase

    if firstLine.startsWith("graph ") || firstLine.startsWith("flowchart ") then
      "flowchart"
    else if firstLine.startsWith("sequencediagram") then
      "sequence"
    else if firstLine.startsWith("classdiagram") then
      "class"
    else if firstLine.startsWith("statediagram") then
      "state"
    else if firstLine.startsWith("gantt") then
      "gantt"
    else if firstLine.startsWith("pie") then
      "pie"
    else if firstLine.startsWith("erdiagram") then
      "er"
    else if firstLine.startsWith("journey") then
      "journey"
    else
      "unknown"

end MermaidDiagram
/**
 * Table representation for markdown tables.
 *
 * Represents a markdown table:
 * | Header 1 | Header 2 |
 * |----------|----------|
 * | Cell 1   | Cell 2   |
 *
 * @param headers Column headers
 * @param rows Table rows (each row is a list of cell values)
 * @param alignment Optional alignment for each column (left, center, right)
 */
case class Table(
  headers: List[String],
  rows: List[List[String]],
  alignment: List[String] = List.empty
)