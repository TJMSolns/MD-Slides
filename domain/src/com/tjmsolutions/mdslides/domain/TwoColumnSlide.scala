package com.tjmsolutions.mdslides.domain

/**
 * Two-Column Slide Aggregate
 *
 * Represents a slide with two-column layout (50/50 split).
 * Columns separated by `---column---` delimiter.
 *
 * Invariants:
 * - Exactly one delimiter in content
 * - Both columns must be non-empty (after trimming)
 * - Delimiter is case-sensitive and exact match
 * - Markdown formatting preserved in columns
 *
 * Business Rules:
 * - Template: `template: two-column`
 * - Delimiter: `---column---` (exact match, case-sensitive)
 * - Left column = content before delimiter
 * - Right column = content after delimiter
 * - Both columns validated independently for density
 */
case class TwoColumnSlide(
  leftColumn: String,
  rightColumn: String,
  template: String,
  frontmatter: Map[String, String]
)

object TwoColumnSlide:
  val COLUMN_DELIMITER = "---column---"

  /**
   * Parse slide content into left and right columns.
   *
   * @param content Slide content with column delimiter
   * @return Either TwoColumnError or (leftColumn, rightColumn)
   */
  def parseColumns(content: String): Either[TwoColumnError, (String, String)] =
    val lines = content.split("\n")
    val delimiterIndices = lines.zipWithIndex
      .filter((line, _) => line.trim == COLUMN_DELIMITER)
      .map(_._2)
      .toList

    delimiterIndices match
      case Nil =>
        Left(TwoColumnError.MissingDelimiter)
      case delimiterIndex :: Nil =>
        // Exactly one delimiter found
        val leftLines = lines.take(delimiterIndex)
        val rightLines = lines.drop(delimiterIndex + 1)
        val leftContent = leftLines.mkString("\n").trim
        val rightContent = rightLines.mkString("\n").trim

        if leftContent.isEmpty then
          Left(TwoColumnError.EmptyColumn("left"))
        else if rightContent.isEmpty then
          Left(TwoColumnError.EmptyColumn("right"))
        else
          Right((leftContent, rightContent))
      case indices =>
        Left(TwoColumnError.MultipleDelimiters(indices.length))

  /**
   * Create a TwoColumnSlide from content and frontmatter.
   *
   * @param content Slide content with column delimiter
   * @param frontmatter Slide frontmatter (must include template: two-column)
   * @return Either TwoColumnError or TwoColumnSlide
   */
  def create(
    content: String,
    frontmatter: Map[String, String]
  ): Either[TwoColumnError, TwoColumnSlide] =
    parseColumns(content).map { (leftColumn, rightColumn) =>
      TwoColumnSlide(
        leftColumn = leftColumn,
        rightColumn = rightColumn,
        template = frontmatter.getOrElse("template", ""),
        frontmatter = frontmatter
      )
    }
