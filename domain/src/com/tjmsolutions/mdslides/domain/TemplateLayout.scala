package com.tjmsolutions.mdslides.domain

/**
 * Template layout variant
 *
 * Defines the structural arrangement of template content.
 *
 * Domain Model: doc/domain-models/aggregates/template-configuration-aggregate.md
 * Acceptance Criteria: doc/acceptance-criteria/template-configuration-acceptance-criteria.md (AC-3)
 *
 * Invariants:
 * - Two-column layout requires column configuration to be defined
 * - Single-column is the default if not specified
 */
enum TemplateLayout:
  case SingleColumn
  case TwoColumn

  /**
   * Indicates whether this layout requires column configuration
   */
  def requiresColumnConfig: Boolean = this match
    case SingleColumn => false
    case TwoColumn    => true

object TemplateLayout:
  /**
   * Default layout when not specified
   */
  val default: TemplateLayout = SingleColumn

  /**
   * Parse template layout from string (case-insensitive)
   *
   * @param s Input string
   * @return Right(layout) if valid, Left(error message) if invalid
   */
  def fromString(s: String): Either[String, TemplateLayout] =
    s.toLowerCase match
      case "single-column" => Right(SingleColumn)
      case "two-column"    => Right(TwoColumn)
      case invalid         =>
        Left(s"Invalid template layout: $invalid. Valid values: single-column, two-column")
