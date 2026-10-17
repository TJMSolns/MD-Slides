package com.tjmsolutions.mdslides.domain

/**
 * Vertical alignment for slide content positioning
 *
 * Maps to CSS flexbox justify-content values for vertical positioning of slide content.
 *
 * Domain Model: doc/domain-models/aggregates/template-configuration-aggregate.md
 * Acceptance Criteria: doc/acceptance-criteria/template-configuration-acceptance-criteria.md (AC-2)
 *
 * Invariants:
 * - Must be one of the three defined values (Top, Center, Bottom)
 * - Maps to CSS flexbox justify-content values
 */
enum VerticalAlignment:
  case Top
  case Center
  case Bottom

  /**
   * Convert to CSS flexbox justify-content value
   */
  def toCSSValue: String = this match
    case Top    => "flex-start"
    case Center => "center"
    case Bottom => "flex-end"

object VerticalAlignment:
  /**
   * Parse vertical alignment from string (case-insensitive)
   *
   * @param s Input string
   * @return Right(alignment) if valid, Left(error message) if invalid
   */
  def fromString(s: String): Either[String, VerticalAlignment] =
    s.toLowerCase match
      case "top"    => Right(Top)
      case "center" => Right(Center)
      case "bottom" => Right(Bottom)
      case invalid  =>
        Left(s"Invalid vertical alignment: $invalid. Valid values: top, center, bottom")
