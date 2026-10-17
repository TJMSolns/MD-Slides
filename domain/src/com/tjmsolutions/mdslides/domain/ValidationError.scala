package com.tjmsolutions.mdslides.domain

/**
 * Domain validation errors.
 *
 * Error types:
 * - StructureError: Missing required slots, invalid template binding
 * - ContentError: Slot constraint violations (max lines, max chars, etc.)
 * - DensityWarning: Readability warnings (non-blocking)
 *
 * Related Governance:
 * - ADR-002: Validation Pipeline Architecture
 * - ADR-003: Error Collection Pattern
 * - POL-001: Ubiquitous Language Enforcement
 */
enum ValidationError:
  /**
   * Structural validation failure (blocking).
   *
   * Examples:
   * - Missing required slot
   * - Template not found
   * - Invalid slot name
   */
  case StructureError(slideId: SlideId, message: String, lineNum: Option[Int] = None)

  /**
   * Content constraint violation (blocking).
   *
   * Examples:
   * - Title exceeds max 2 lines
   * - Author exceeds max 80 chars
   * - Body exceeds max 150 words
   */
  case ContentError(slideId: SlideId, slotName: String, message: String, lineNum: Option[Int] = None)

  /**
   * Density/readability warning (non-blocking).
   *
   * Examples:
   * - Body has 13 lines (over recommended 12)
   * - Content word count high (readability concern)
   */
  case DensityWarning(slideId: SlideId, slotName: String, message: String, lineNum: Option[Int] = None)

  /** Source line number of this error's slide in the markdown file (US-019). */
  def lineNumber: Option[Int] = this match
    case StructureError(_, _, ln)     => ln
    case ContentError(_, _, _, ln)    => ln
    case DensityWarning(_, _, _, ln)  => ln

  /**
   * Human-readable error message, including source line number when available (US-019).
   */
  def displayMessage: String = this match
    case StructureError(id, msg, lineOpt) =>
      val loc = lineOpt.map(l => s" (line $l)").getOrElse("")
      s"Slide ${id.toInt}$loc: Structure Error - $msg"
    case ContentError(id, slot, msg, lineOpt) =>
      val loc = lineOpt.map(l => s" (line $l)").getOrElse("")
      s"Slide ${id.toInt}$loc: Content Error in '$slot' slot - $msg"
    case DensityWarning(id, slot, msg, lineOpt) =>
      val loc = lineOpt.map(l => s" (line $l)").getOrElse("")
      s"Slide ${id.toInt}$loc: Density Warning in '$slot' slot - $msg"

end ValidationError
