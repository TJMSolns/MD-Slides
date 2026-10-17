package com.tjmsolutions.mdslides.domain

/**
 * Validation constraints for a template slot.
 *
 * Related Governance:
 * - ADR-008: Slot-Based Content Model
 * - PDR-001: Density Validation Limits
 * - PDR-004: Template Constraints
 */
case class SlotConstraints(
  maxLines: Option[Int] = None,
  maxChars: Option[Int] = None,
  maxWords: Option[Int] = None,
  allowedContentTypes: Set[ContentType] = Set(ContentType.Text)
)

/**
 * Allowed content types in a slot.
 */
enum ContentType:
  case Text         // Plain text and paragraphs
  case List         // Bulleted/numbered lists
  case CodeBlock    // Syntax-highlighted code
  case Image        // Images with alt text
  case Table        // Data tables
  case Quote        // Block quotes

end ContentType
