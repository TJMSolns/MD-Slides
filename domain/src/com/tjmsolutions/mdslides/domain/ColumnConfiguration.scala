package com.tjmsolutions.mdslides.domain

/**
 * Column specification for two-column layouts
 *
 * Defines width and optional color overrides for a single column.
 *
 * Domain Model: doc/domain-models/aggregates/template-configuration-aggregate.md
 * Acceptance Criteria: doc/acceptance-criteria/template-configuration-acceptance-criteria.md (AC-6)
 *
 * Invariants:
 * - Width must be valid CSS value if specified
 * - Defaults to 50% if not specified
 */
case class ColumnSpec(
  width: Option[String],
  colors: Option[TemplateColors]
):
  /**
   * Get width or default to 50%
   */
  def widthOrDefault: String = width.getOrElse("50%")

  /**
   * Validate width and colors
   *
   * @return Right(this) if valid, Left(error message) if invalid
   */
  def validate(): Either[String, ColumnSpec] =
    // Validate width if specified
    width match
      case Some(w) if !ColumnSpec.isValidCSSWidth(w) =>
        Left(s"Invalid CSS width: $w")
      case _ =>
        // Validate colors if specified
        colors match
          case Some(c) => c.validate().map(_ => this)
          case None    => Right(this)

object ColumnSpec:
  /**
   * Check if a string is a valid CSS width value
   *
   * Validates:
   * - Percentage (50%)
   * - Pixels (300px)
   * - Rem/Em (20rem, 15em)
   * - Viewport units (100vw, 50vh)
   *
   * @param width Width string to validate
   * @return true if valid CSS width, false otherwise
   */
  def isValidCSSWidth(width: String): Boolean =
    val widthPattern = "^\\d+(\\.\\d+)?(px|%|rem|em|vw|vh)$".r
    widthPattern.matches(width.trim)

/**
 * Column configuration for two-column layouts
 *
 * Defines left and right column specifications including widths and optional color overrides.
 *
 * Domain Model: doc/domain-models/aggregates/template-configuration-aggregate.md
 * Acceptance Criteria: doc/acceptance-criteria/template-configuration-acceptance-criteria.md (AC-6)
 *
 * Invariants:
 * - Widths must be valid CSS values
 * - Only applicable when layout = TwoColumn
 */
case class ColumnConfiguration(
  leftColumn: ColumnSpec,
  rightColumn: ColumnSpec
):
  /**
   * Validate both columns
   *
   * @return Right(this) if valid, Left(error message) if invalid
   */
  def validate(): Either[String, ColumnConfiguration] =
    for
      _ <- leftColumn.validate()
      _ <- rightColumn.validate()
    yield this

  /**
   * Validate total width if both columns use percentages
   *
   * Returns warning if total exceeds 100%, but does not fail validation
   * (CSS will handle overflow).
   *
   * @return Some(warning) if total exceeds 100%, None otherwise
   */
  def validateTotalWidth(): Option[String] =
    (leftColumn.width, rightColumn.width) match
      case (Some(left), Some(right)) if left.endsWith("%") && right.endsWith("%") =>
        val leftPercent = left.stripSuffix("%").toDoubleOption.getOrElse(0.0)
        val rightPercent = right.stripSuffix("%").toDoubleOption.getOrElse(0.0)
        val total = leftPercent + rightPercent

        if total > 100.0 then
          Some(s"Column widths exceed 100%: $left + $right = ${total.toInt}%")
        else
          None

      case _ =>
        None  // Can't validate non-percentage or missing widths
