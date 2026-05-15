package com.tjmsolutions.mdslides.domain

/**
 * Definition of a slot in a template.
 *
 * Example:
 * - name: "title"
 * - required: true
 * - constraints: maxLines = 2
 *
 * Related Governance:
 * - ADR-008: Slot-Based Content Model
 */
case class SlotDefinition(
  name: String,          // Slot identifier (e.g., "title", "subtitle")
  required: Boolean,     // Must be present in slide
  constraints: SlotConstraints
)

object SlotDefinition:
  /**
   * Title slot definition (title template).
   *
   * Constraints per PDR-004:
   * - Required
   * - Max 2 lines
   */
  val title: SlotDefinition =
    SlotDefinition(
      name = "title",
      required = true,
      constraints = SlotConstraints(maxLines = Some(2))
    )

  /**
   * Subtitle slot definition (title template).
   *
   * Constraints per PDR-004:
   * - Optional
   * - Max 2 lines
   */
  val subtitle: SlotDefinition =
    SlotDefinition(
      name = "subtitle",
      required = false,
      constraints = SlotConstraints(maxLines = Some(2))
    )

  /**
   * Author slot definition (title template).
   *
   * Constraints per PDR-004:
   * - Optional
   * - Max 80 chars
   */
  val author: SlotDefinition =
    SlotDefinition(
      name = "author",
      required = false,
      constraints = SlotConstraints(maxChars = Some(80))
    )

  /**
   * Heading slot definition (content template).
   *
   * Constraints per PDR-004:
   * - Required
   * - Max 80 chars
   */
  val heading: SlotDefinition =
    SlotDefinition(
      name = "heading",
      required = true,
      constraints = SlotConstraints(maxChars = Some(80))
    )

  /**
   * Body slot definition (content template).
   *
   * Constraints per PDR-001:
   * - Required
   * - Max 12 lines (recommended)
   * - Max 150 words (recommended)
   */
  val body: SlotDefinition =
    SlotDefinition(
      name = "body",
      required = true,
      constraints = SlotConstraints(
        maxLines = Some(12),
        maxWords = Some(150),
        allowedContentTypes = Set(ContentType.Text, ContentType.List)
      )
    )

  /**
   * Caption slot definition (diagram template, v2.0.0).
   *
   * Constraints:
   * - Optional
   * - Max 1 line
   * - Max 100 chars
   */
  val caption: SlotDefinition =
    SlotDefinition(
      name = "caption",
      required = false,
      constraints = SlotConstraints(
        maxLines = Some(1),
        maxChars = Some(100)
      )
    )

  /**
   * Body (optional) slot definition for templates where body is optional (v2.0.0).
   *
   * Used by: closing, section-title templates
   */
  val bodyOptional: SlotDefinition =
    SlotDefinition(
      name = "body",
      required = false,
      constraints = SlotConstraints(
        maxLines = Some(12),
        maxWords = Some(150),
        allowedContentTypes = Set(ContentType.Text, ContentType.List)
      )
    )

end SlotDefinition
