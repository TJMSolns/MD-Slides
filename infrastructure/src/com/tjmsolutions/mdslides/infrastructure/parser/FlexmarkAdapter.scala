package com.tjmsolutions.mdslides.infrastructure.parser

import com.tjmsolutions.mdslides.domain.{FormattedContent, TextSpan, Link, CodeBlock, ContentImage, UnorderedList, OrderedList, ListItem, ListElement, UnorderedListElement, OrderedListElement, UnorderedListElementDeprecated, OrderedListElementDeprecated, ContentElement, ParagraphElement, CodeBlockElement, ImageElement, MermaidDiagram, DiagramElement, Table, TableElement}
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.{Node, NodeVisitor, VisitHandler}
import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.ext.tables.{TableBlock, TableHead, TableBody, TableRow, TableCell}
import com.vladsch.flexmark.ext.tables.TablesExtension
import scala.jdk.CollectionConverters.*

/**
 * Anticorruption Layer for Flexmark markdown parser.
 *
 * Translates Flexmark AST to domain FormattedContent.
 * Isolates domain from external library dependencies.
 *
 * Supported inline formatting:
 * - Bold: **text** or __text__
 * - Italic: *text* or _text_
 * - Inline code: `text`
 * - Links: [text](url)
 *
 * Supported block elements:
 * - Fenced code blocks: ```language\ncode\n```
 * - Images: ![alt text](url)
 *
 * Related Governance:
 * - ADR-010: Markdown Library Selection (Flexmark)
 * - ADR-007: Anticorruption Layer
 * - US-003: Full Markdown Rendering
 * - US-004: Code Block Support
 * - US-005: Image Embedding
 * - PDR-008: Image Policy
 * - POL-003: Pure Functional Domain (this adapter keeps domain pure)
 */
object FlexmarkAdapter:

  private val parser: Parser = Parser.builder()
    .extensions(java.util.Arrays.asList(TablesExtension.create()))
    .build()

  /**
   * Parse markdown text into FormattedContent.
   *
   * Extracts inline formatting (bold, italic, code, links) and converts
   * to domain types (TextSpan, Link).
   *
   * @param markdown Raw markdown text
   * @return FormattedContent with parsed structure
   */
  def parseInlineFormatting(markdown: String): FormattedContent =
    if markdown.trim.isEmpty then
      return FormattedContent.empty

    val document = parser.parse(markdown)
    extractFormattedContent(document)

  /**
   * Extract FormattedContent from Flexmark document AST.
   *
   * Walks the AST and builds TextSpan, Link, CodeBlock, ContentImage, and List objects.
   */
  private def extractFormattedContent(node: Node): FormattedContent =
    val textSpans = scala.collection.mutable.ListBuffer.empty[TextSpan]
    val links = scala.collection.mutable.ListBuffer.empty[Link]
    val codeBlocks = scala.collection.mutable.ListBuffer.empty[CodeBlock]
    val contentImages = scala.collection.mutable.ListBuffer.empty[ContentImage]
    val unorderedLists = scala.collection.mutable.ListBuffer.empty[UnorderedList]
    val orderedLists = scala.collection.mutable.ListBuffer.empty[OrderedList]
    val lists = scala.collection.mutable.ListBuffer.empty[ListElement] // BUG-001 incomplete fix
    val content = scala.collection.mutable.ListBuffer.empty[ContentElement] // BUG-001 REAL fix

    // Track current paragraph text spans (accumulated until we hit a non-text element)
    val currentParagraph = scala.collection.mutable.ListBuffer.empty[TextSpan]

    // Track current formatting state as we traverse
    var currentBold = false
    var currentItalic = false
    var currentCode = false

    // Helper to flush current paragraph to content buffer
    def flushParagraph(): Unit =
      if currentParagraph.nonEmpty then
        content += ParagraphElement(currentParagraph.toList)
        currentParagraph.clear()

    def visitNode(n: Node): Unit =
      n match
        case text: Text =>
          // Plain text or text within formatting
          val span = TextSpan(
            text.getChars.toString,
            bold = currentBold,
            italic = currentItalic,
            code = currentCode
          )
          textSpans += span
          currentParagraph += span

        case strong: StrongEmphasis =>
          // Bold text: **text** or __text__
          val prevBold = currentBold
          currentBold = true
          n.getChildren.asScala.foreach(visitNode)
          currentBold = prevBold

        case emphasis: Emphasis =>
          // Italic text: *text* or _text_
          val prevItalic = currentItalic
          currentItalic = true
          n.getChildren.asScala.foreach(visitNode)
          currentItalic = prevItalic

        case code: Code =>
          // Inline code: `text`
          val span = TextSpan(
            code.getChars.toString.stripPrefix("`").stripSuffix("`"),
            bold = false,
            italic = false,
            code = true
          )
          textSpans += span
          currentParagraph += span

        case link: com.vladsch.flexmark.ast.Link =>
          // Link: [text](url)
          val linkText = link.getText.toString
          val linkUrl = link.getUrl.toString
          links += Link(linkText, linkUrl)

          // Also add link text as a text span for plain text extraction
          val span = TextSpan(
            linkText,
            bold = currentBold,
            italic = currentItalic,
            code = false
          )
          textSpans += span
          currentParagraph += span
          // Note: Don't visit children - we've already extracted the text

        case _: SoftLineBreak =>
          // Soft line break (single newline in source) -> space in output
          val span = TextSpan(" ", bold = false, italic = false, code = false)
          textSpans += span
          currentParagraph += span

        case _: HardLineBreak =>
          // Hard line break (two spaces + newline, or backslash + newline)
          val span = TextSpan("\n", bold = false, italic = false, code = false)
          textSpans += span
          currentParagraph += span

        case para: Paragraph =>
          // Paragraph - visit children
          n.getChildren.asScala.foreach(visitNode)
          // Add newline after paragraph (unless it's the last node)
          if n.getNext != null then
            val span = TextSpan("\n", bold = false, italic = false, code = false)
            textSpans += span
            currentParagraph += span
          // Flush paragraph after processing
          flushParagraph()

        case fencedCode: FencedCodeBlock =>
          // Fenced code block: ```language\ncode\n```
          // v2.0.0: Check if it's a Mermaid diagram
          flushParagraph() // Flush any text before code block
          val code = fencedCode.getContentChars.toString
          val language = Option(fencedCode.getInfo.toString).filter(_.nonEmpty)

          language match
            case Some("mermaid") =>
              // Mermaid diagram (v2.0.0)
              val diagramType = MermaidDiagram.detectDiagramType(code)
              val diagram = MermaidDiagram(code, diagramType, altText = None)
              content += DiagramElement(diagram)
            case _ =>
              // Regular code block
              val block = CodeBlock(code, language)
              codeBlocks += block
              content += CodeBlockElement(block) // Add to content in source order
          // Don't visit children - we've extracted the code/diagram

        case image: Image =>
          // Content image: ![alt text](url)
          flushParagraph() // Flush any text before image
          val url = image.getUrl.toString
          val altText = image.getText.toString
          // Use validated constructor to ensure alt text is non-empty
          ContentImage.validated(url, altText) match
            case Right(img) =>
              contentImages += img
              content += ImageElement(img) // Add to content in source order
            case Left(error) =>
              // Invalid image (empty alt text) - skip it
              // Validation layer will catch this
              ()
          // Don't visit children - we've extracted the image data

        case bulletList: BulletList =>
          // Unordered list: - Item one\n- Item two
          flushParagraph() // Flush any text before list
          val items = extractListItems(bulletList)
          if items.nonEmpty then
            val list = UnorderedList(items)
            unorderedLists += list
            lists += UnorderedListElementDeprecated(list) // Old deprecated field
            content += UnorderedListElement(list) // Add to content in source order
          // Don't visit children - we've extracted the list

        case orderedList: com.vladsch.flexmark.ast.OrderedList =>
          // Ordered list: 1. Item one\n2. Item two
          flushParagraph() // Flush any text before list
          val items = extractListItems(orderedList)
          if items.nonEmpty then
            val list = com.tjmsolutions.mdslides.domain.OrderedList(items)
            orderedLists += list
            lists += OrderedListElementDeprecated(list) // Old deprecated field
            content += OrderedListElement(list) // Add to content in source order
          // Don't visit children - we've extracted the list

        case _: BulletListItem | _: OrderedListItem =>
          // List items are handled by extractListItems, skip here
          ()

        case tableBlock: TableBlock =>
          // Markdown table
          flushParagraph() // Flush any text before table
          val (headers, rows, alignment) = extractTableContent(tableBlock)
          if headers.nonEmpty then
            val tbl = Table(headers, rows, alignment)
            content += TableElement(tbl) // Add to content in source order
          // Don't visit children - we've extracted the table data

        case _ =>
          // For other nodes, recursively visit children
          n.getChildren.asScala.foreach(visitNode)

    visitNode(node)

    // Flush any remaining paragraph text
    flushParagraph()

    // Merge consecutive text spans with same formatting
    val mergedSpans = mergeConsecutiveSpans(textSpans.toList)

    FormattedContent(
      mergedSpans,
      links.toList,
      codeBlocks.toList,
      contentImages.toList,
      unorderedLists.toList,
      orderedLists.toList,
      lists.toList, // BUG-001 incomplete fix (deprecated)
      content.toList // BUG-001 REAL fix: ALL content in source order
    )

  /**
   * Merge consecutive TextSpans with identical formatting.
   *
   * Optimization: reduces number of spans for rendering.
   * Example: [TextSpan("Hello", bold=true), TextSpan(" ", bold=true), TextSpan("world", bold=true)]
   *          -> [TextSpan("Hello world", bold=true)]
   */
  private def mergeConsecutiveSpans(spans: List[TextSpan]): List[TextSpan] =
    spans.foldLeft(List.empty[TextSpan]) { (acc, span) =>
      acc match
        case Nil => List(span)
        case prev :: rest if prev.bold == span.bold &&
                            prev.italic == span.italic &&
                            prev.code == span.code =>
          // Same formatting - merge text
          TextSpan(prev.text + span.text, prev.bold, prev.italic, prev.code) :: rest
        case _ =>
          // Different formatting - append new span
          span :: acc
    }.reverse

  /**
   * Extract list items from a BulletList or OrderedList node.
   *
   * Processes each list item and extracts its text spans with formatting.
   *
   * @param listNode The BulletList or OrderedList node
   * @return List of ListItem objects with formatted text
   */
  private def extractListItems(listNode: Node): List[ListItem] =
    listNode.getChildren.asScala.toList.flatMap {
      case item: BulletListItem =>
        Some(extractListItemContent(item))
      case item: OrderedListItem =>
        Some(extractListItemContent(item))
      case _ =>
        None
    }

  /**
   * Extract formatted content from a single list item.
   *
   * Recursively processes the list item's children to extract text spans
   * with inline formatting (bold, italic, code) and nested lists.
   *
   * US-003.3: Now extracts nested unordered and ordered lists.
   *
   * @param itemNode The list item node (BulletListItem or OrderedListItem)
   * @return ListItem with formatted text spans and nested lists
   */
  private def extractListItemContent(itemNode: Node): ListItem =
    val itemSpans = scala.collection.mutable.ListBuffer.empty[TextSpan]
    val nestedUnorderedLists = scala.collection.mutable.ListBuffer.empty[UnorderedList]
    val nestedOrderedLists = scala.collection.mutable.ListBuffer.empty[OrderedList]
    var currentBold = false
    var currentItalic = false
    var currentCode = false

    def visitItemNode(n: Node): Unit =
      n match
        case text: Text =>
          val span = TextSpan(
            text.getChars.toString,
            bold = currentBold,
            italic = currentItalic,
            code = currentCode
          )
          itemSpans += span

        case strong: StrongEmphasis =>
          val prevBold = currentBold
          currentBold = true
          n.getChildren.asScala.foreach(visitItemNode)
          currentBold = prevBold

        case emphasis: Emphasis =>
          val prevItalic = currentItalic
          currentItalic = true
          n.getChildren.asScala.foreach(visitItemNode)
          currentItalic = prevItalic

        case code: Code =>
          val span = TextSpan(
            code.getChars.toString.stripPrefix("`").stripSuffix("`"),
            bold = false,
            italic = false,
            code = true
          )
          itemSpans += span

        case _: SoftLineBreak =>
          itemSpans += TextSpan(" ", bold = false, italic = false, code = false)

        case _: Paragraph =>
          // Paragraph wrapper - visit children to extract text
          n.getChildren.asScala.foreach(visitItemNode)

        case bulletList: BulletList =>
          // Nested unordered list (US-003.3)
          val items = extractListItems(bulletList)
          if items.nonEmpty then
            nestedUnorderedLists += UnorderedList(items)
          // Don't visit children - we've extracted the list

        case orderedList: com.vladsch.flexmark.ast.OrderedList =>
          // Nested ordered list (US-003.3)
          val items = extractListItems(orderedList)
          if items.nonEmpty then
            nestedOrderedLists += com.tjmsolutions.mdslides.domain.OrderedList(items)
          // Don't visit children - we've extracted the list

        case _: BulletListItem | _: OrderedListItem =>
          // Nested list items are handled by extractListItems, skip here
          ()

        case _ =>
          // Recursively visit children
          n.getChildren.asScala.foreach(visitItemNode)

    // Visit all children of the list item (Paragraph, nested lists, etc.)
    itemNode.getChildren.asScala.foreach(visitItemNode)

    val mergedSpans = mergeConsecutiveSpans(itemSpans.toList)
    ListItem(mergedSpans, nestedUnorderedLists.toList, nestedOrderedLists.toList)

  private def extractTableContent(tableNode: Node): (List[String], List[List[String]], List[String]) =
    val headersList = scala.collection.mutable.ListBuffer.empty[String]
    val rowsList = scala.collection.mutable.ListBuffer.empty[List[String]]
    val alignmentList = scala.collection.mutable.ListBuffer.empty[String]
    var headerProcessed = false

    tableNode.getChildren.asScala.foreach {
      case th: TableHead =>
        th.getChildren.asScala.foreach {
          case tr: TableRow =>
            val cells = tr.getChildren.asScala.collect {
              case tc: TableCell =>
                val text = extractCellText(tc)
                if !headerProcessed then
                  alignmentList += extractCellAlignment(tc)
                text
            }.toList
            if !headerProcessed then 
              headersList ++= cells
              headerProcessed = true
        }
      case tb: TableBody =>
        tb.getChildren.asScala.foreach {
          case tr: TableRow =>
            val cells = tr.getChildren.asScala.collect {
              case tc: TableCell => extractCellText(tc)
            }.toList
            if cells.nonEmpty then rowsList += cells
        }
      case _ =>
    }

    (headersList.toList, rowsList.toList, alignmentList.toList)

  private def extractCellText(cell: Node): String =
    val parts = scala.collection.mutable.ListBuffer.empty[String]
    
    def extract(node: Node): Unit =
      if (node.isInstanceOf[Text]) {
        val text = node.asInstanceOf[Text].getChars.toString.trim()
        if text.nonEmpty then parts += text
      } else {
        node.getChildren.asScala.foreach(extract)
      }
    
    cell.getChildren.asScala.foreach(extract)
    parts.mkString("").trim()

  private def extractCellAlignment(cell: Node): String =
    try {
      cell match {
        case tc: TableCell =>
          Option(tc.getAlignment) match {
            case Some(TableCell.Alignment.LEFT) => "left"
            case Some(TableCell.Alignment.CENTER) => "center"
            case Some(TableCell.Alignment.RIGHT) => "right"
            case _ => ""
          }
        case _ => ""
      }
    } catch {
      case _: Exception => ""
    }

end FlexmarkAdapter
