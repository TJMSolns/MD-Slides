package com.tjmsolutions.mdslides.infrastructure.parser

import com.tjmsolutions.mdslides.domain.{Slide, SlideDeck, SlideId, Template}
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.implicits.*

/**
 * Markdown parser for MDSlides format.
 *
 * Parses markdown files with slide delimiters (---) and YAML-like frontmatter
 * to extract slide structure and content.
 *
 * Format:
 * ```markdown
 * ---
 * template: title
 * ---
 * # My Title
 * ## Subtitle
 * Author Name
 *
 * ---
 * template: content
 * ---
 * ## Heading
 * Body content here
 * ```
 *
 * Current Implementation (MVP):
 * - Parses slide delimiters (---)
 * - Extracts template type from frontmatter
 * - Extracts slot content based on template type
 * - Does NOT render markdown formatting (preserved as-is)
 *
 * Future Enhancements (US-003):
 * - Full markdown rendering (bold, italic, links, etc.)
 * - Code blocks
 * - Images
 * - Lists
 *
 * Related Governance:
 * - ADR-006: Rendering Architecture
 * - ADR-007: Anticorruption Layer for Flexmark
 */
object MarkdownParser:

  /**
   * Parse markdown content into a SlideDeck.
   *
   * @param markdown The markdown content
   * @return Right(deck) if parsing succeeds, Left(error) otherwise
   */
  def parse(markdown: String): Either[String, SlideDeck] =
    // Split by double --- delimiter (slide separators)
    // Format: ---\nfrontmatter\n---\ncontent\n\n---\nfrontmatter\n---\ncontent
    val rawSlides = splitSlides(markdown)

    if rawSlides.isEmpty then
      return Left("No slides found in markdown (use --- to separate slides)")

    // Parse each slide
    val parsedSlides = rawSlides.zipWithIndex.map { case (rawSlide, index) =>
      parseSlide(rawSlide, index + 1)
    }

    // Collect errors or build deck
    parsedSlides.toList.sequence match
      case Left(error) => Left(error)
      case Right(slides) =>
        NonEmptyList.fromList(slides) match
          case None => Left("No valid slides found")
          case Some(nel) => Right(SlideDeck(nel))

  /**
   * Compute the source line number (1-based) of each slide's opening --- delimiter.
   *
   * Returns Map[slideIndex (1-based), lineNumber (1-based)].
   * Slides are delimited by pairs of --- lines: the odd-indexed --- lines open slides.
   * Example: slide 1 opens at separator[0], slide 2 at separator[2], etc.
   */
  def slideLineNumbers(markdown: String): Map[Int, Int] =
    val lines = markdown.split("\n", -1)
    val separatorLines = lines.zipWithIndex.collect {
      case (l, idx) if l.trim == "---" => idx + 1  // 1-based line number
    }.toList
    // Even-indexed separators (0, 2, 4...) open slides 1, 2, 3...
    separatorLines.zipWithIndex.collect {
      case (lineNum, idx) if idx % 2 == 0 => (idx / 2 + 1) -> lineNum
    }.toMap

  /**
   * Split markdown into raw slide chunks.
   *
   * Each slide has format:
   * ---
   * frontmatter
   * ---
   * content
   */
  private def splitSlides(markdown: String): List[String] =
    // Split by --- on its own line
    val parts = markdown.split("(?m)^---$").map(_.trim).filter(_.nonEmpty).toList

    // Group parts into slides: each slide is (frontmatter, content) pairs
    // Parts alternate: frontmatter1, content1, frontmatter2, content2, ...
    parts.grouped(2).map {
      case List(frontmatter, content) => s"$frontmatter\n---\n$content"
      case List(single) => single  // Last slide might only have frontmatter or content
      case Nil => ""  // Empty list (shouldn't happen but need for exhaustiveness)
      case _ => ""    // Catch-all for unexpected cases
    }.filter(_.nonEmpty).toList

  /**
   * Parse a single raw slide into a Slide.
   *
   * @param raw The raw slide content
   * @param slideNumber The slide number (1-based)
   * @return Right(slide) if parsing succeeds, Left(error) otherwise
   */
  private def parseSlide(raw: String, slideNumber: Int): Either[String, Slide] =
    // Extract frontmatter and content
    val (frontmatter, content) = extractFrontmatter(raw)

    // Get template name from frontmatter
    val templateName = frontmatter.getOrElse("template", "content")

    // Get background image from frontmatter (optional, US-011)
    val backgroundImage = frontmatter.get("background")

    // Get speaker notes from frontmatter OR HTML comments (optional, US-004)
    val (contentWithoutNotes, extractedNotes) = extractSpeakerNotes(content)
    val notes = frontmatter.get("notes").orElse(extractedNotes)

    // Validate template exists (v2.0.0: supports diagram, closing, section-title)
    Template.fromName(templateName) match
      case Left(error) => return Left(s"Slide $slideNumber: $error")
      case Right(_) => () // Template is valid

    // Parse content based on template (use contentWithoutNotes to exclude speaker notes from slots)
    val baseSlots = templateName match
      case "title" => parseTitleSlide(contentWithoutNotes)
      case "content" => parseContentSlide(contentWithoutNotes)
      case "diagram" => parseContentSlide(contentWithoutNotes) // Same parsing as content
      case "closing" => parseContentSlide(contentWithoutNotes) // Same parsing as content
      case "section-title" => parseContentSlide(contentWithoutNotes) // Same parsing as content
      case "two-column" => parseTwoColumnSlide(contentWithoutNotes, slideNumber) match
        case Left(error) => return Left(s"Slide $slideNumber: $error")
        case Right(twoColumnSlots) => twoColumnSlots
      case _ => return Left(s"Slide $slideNumber: Unknown template '$templateName'")

    // Add optional frontmatter fields to slots (v3.0.0)
    var slots = baseSlots
    frontmatter.get("title").foreach(v => slots = slots + ("title" -> v))
    frontmatter.get("vertical-align").foreach(v => slots = slots + ("vertical-align" -> v))
    frontmatter.get("header").foreach(v => slots = slots + ("header" -> v))
    frontmatter.get("footer").foreach(v => slots = slots + ("footer" -> v))

    // Create slide ID
    SlideId(slideNumber) match
      case Left(error) => Left(s"Slide $slideNumber: $error")
      case Right(id) => Right(Slide(id, templateName, slots, backgroundImage, notes))

  /**
   * Extract frontmatter from raw slide content.
   *
   * Frontmatter format:
   * ```
   * template: title
   * key: value
   * notes:
   *   - "item 1"
   *   - "item 2"
   * ---
   * content here
   * ```
   *
   * Supports both simple string values and YAML arrays.
   * Arrays are joined with newlines.
   *
   * @param raw The raw slide content
   * @return (frontmatter map, remaining content)
   */
  private def extractFrontmatter(raw: String): (Map[String, String], String) =
    val lines = raw.split("\n")

    // Find end of frontmatter (--- delimiter or start of content)
    val frontmatterEndIndex = lines.indexWhere(_.trim == "---", 0)
    val frontmatterLines = if frontmatterEndIndex >= 0 then
      lines.take(frontmatterEndIndex)
    else
      lines.takeWhile(line => line.trim.nonEmpty && (line.contains(":") || line.trim.startsWith("-")))

    // Content starts after --- delimiter or after frontmatter
    val contentStartIndex = if frontmatterEndIndex >= 0 then frontmatterEndIndex + 1 else frontmatterLines.length
    val contentLines = lines.drop(contentStartIndex).dropWhile(_.trim.isEmpty)

    // Parse frontmatter key-value pairs
    val frontmatter = parseFrontmatterLines(frontmatterLines.toList)

    (frontmatter, contentLines.mkString("\n"))

  /**
   * Parse frontmatter lines into key-value pairs.
   *
   * Handles both simple values and arrays:
   * - `key: value` → Map("key" -> "value")
   * - `key:\n  - item1\n  - item2` → Map("key" -> "item1\nitem2")
   */
  private def parseFrontmatterLines(lines: List[String]): Map[String, String] =
    val result = Map.newBuilder[String, String]
    var currentKey: Option[String] = None
    val currentArrayItems = List.newBuilder[String]

    lines.foreach { line =>
      val trimmed = line.trim

      if trimmed.startsWith("- ") then
        // Array item
        val item = trimmed.drop(2).trim.stripPrefix("\"").stripSuffix("\"")
        currentArrayItems += item
      else if trimmed.contains(":") then
        // Finish previous key if it was an array
        currentKey.foreach { key =>
          val items = currentArrayItems.result()
          if items.nonEmpty then
            result += (key -> items.mkString("\n"))
          currentArrayItems.clear()
        }

        // New key-value pair
        val parts = trimmed.split(":", 2)
        if parts.length == 2 then
          val key = parts(0).trim
          val rawValue = parts(1).trim
          val value = rawValue.stripPrefix("\"").stripSuffix("\"")

          // Check if this is an explicit empty string ("") or truly empty (array follows)
          if rawValue.startsWith("\"") && rawValue.endsWith("\"") then
            // Explicit string value (including empty string)
            result += (key -> value)
            currentKey = None
          else if value.nonEmpty then
            // Simple non-quoted value
            result += (key -> value)
            currentKey = None
          else
            // Array starting on next line
            currentKey = Some(key)
        end if
      end if
    }

    // Finish last key if it was an array
    currentKey.foreach { key =>
      val items = currentArrayItems.result()
      if items.nonEmpty then
        result += (key -> items.mkString("\n"))
    }

    result.result()

  /**
   * Extract speaker notes from HTML comments.
   *
   * Searches for HTML comments in the format:
   * <!-- Speaker notes: content here -->
   *
   * Returns content with speaker note comments removed, and the extracted notes.
   *
   * @param content Markdown content that may contain speaker note comments
   * @return (content without speaker note comments, extracted speaker notes)
   */
  private def extractSpeakerNotes(content: String): (String, Option[String]) =
    // Pattern: <!-- Speaker notes: ... -->
    val speakerNotePattern = """<!--\s*Speaker notes?:\s*(.+?)\s*-->""".r

    val notes = scala.collection.mutable.ListBuffer.empty[String]
    var contentWithoutNotes = content

    speakerNotePattern.findAllMatchIn(content).foreach { m =>
      notes += m.group(1).trim
      // Remove the comment from content
      contentWithoutNotes = contentWithoutNotes.replace(m.matched, "")
    }

    val extractedNotes = if notes.nonEmpty then Some(notes.mkString("\n\n")) else None
    (contentWithoutNotes, extractedNotes)

  /**
   * Parse title slide content.
   *
   * Expected format:
   * ```
   * # Title (required)
   * ## Subtitle (optional)
   * Author (optional, plain text)
   * ```
   */
  private def parseTitleSlide(content: String): Map[String, String] =
    val lines = content.split("\n").map(_.trim).filter(_.nonEmpty)
    val slots = Map.newBuilder[String, String]

    // Extract title (# heading)
    lines.find(_.startsWith("# ")).foreach { line =>
      slots += ("title" -> line.drop(2).trim)
    }

    // Extract subtitle (## heading)
    lines.find(_.startsWith("## ")).foreach { line =>
      slots += ("subtitle" -> line.drop(3).trim)
    }

    // Extract author (any remaining non-heading line)
    lines.find(line => !line.startsWith("#") && line.nonEmpty).foreach { line =>
      slots += ("author" -> line.trim)
    }

    slots.result()

  /**
   * Parse content slide content.
   *
   * Expected format:
   * ```
   * ## Heading (required)
   * Body content (required, everything after heading)
   * ```
   */
  private def parseContentSlide(content: String): Map[String, String] =
    val lines = content.split("\n")
    val slots = Map.newBuilder[String, String]

    // Find heading (## line)
    val headingIndex = lines.indexWhere(_.trim.startsWith("## "))
    if headingIndex >= 0 then
      val heading = lines(headingIndex).trim.drop(3).trim
      slots += ("heading" -> heading)

      // Everything after heading is body
      val bodyLines = lines.drop(headingIndex + 1).dropWhile(_.trim.isEmpty)
      if bodyLines.nonEmpty then
        slots += ("body" -> bodyLines.mkString("\n"))

    slots.result()

  /**
   * Parse two-column slide content.
   *
   * Expected format:
   * ```
   * ## Heading (optional)
   * Left column content
   *
   * ---column---
   *
   * Right column content
   * ```
   *
   * @param content Slide content with ---column--- delimiter
   * @param slideNumber Slide number for error messages
   * @return Either error or slots map
   */
  private def parseTwoColumnSlide(content: String, slideNumber: Int): Either[String, Map[String, String]] =
    import com.tjmsolutions.mdslides.domain.TwoColumnSlide

    // Split content by ---column--- delimiter
    TwoColumnSlide.parseColumns(content) match
      case Left(error) => Left(error.toString)
      case Right((leftColumn, rightColumn)) =>
        Right(Map(
          "leftColumn" -> leftColumn,
          "rightColumn" -> rightColumn
        ))

end MarkdownParser
