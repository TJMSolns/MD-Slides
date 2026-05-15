package com.tjmsolutions.mdslides.domain

/**
 * Slide template defining available slots and constraints.
 *
 * Templates control:
 * - Which slots are required/optional
 * - Content constraints per slot (max lines, max chars, etc.)
 * - Allowed content types per slot
 *
 * Example templates:
 * - "title": title, subtitle (opt), author (opt)
 * - "content": heading, body
 * - "code": heading, code (with syntax highlighting)
 *
 * Related Governance:
 * - ADR-008: Slot-Based Content Model
 * - PDR-004: Template Constraints
 */
case class Template(
  name: String,
  slots: List[SlotDefinition]
):
  /**
   * Get required slots for this template.
   */
  def requiredSlots: List[SlotDefinition] =
    slots.filter(_.required)

  /**
   * Get optional slots for this template.
   */
  def optionalSlots: List[SlotDefinition] =
    slots.filterNot(_.required)

  /**
   * Find slot definition by name.
   */
  def getSlot(name: String): Option[SlotDefinition] =
    slots.find(_.name == name)

end Template

object Template:
  /**
   * Title template.
   *
   * Slots:
   * - title (required, max 2 lines)
   * - subtitle (optional, max 2 lines)
   * - author (optional, max 80 chars)
   *
   * Related: US-001 (Create Title Slide)
   */
  val title: Template =
    Template(
      name = "title",
      slots = List(
        SlotDefinition.title,
        SlotDefinition.subtitle,
        SlotDefinition.author
      )
    )

  /**
   * Content template.
   *
   * Slots:
   * - heading (required, max 80 chars)
   * - body (required, max 12 lines, max 150 words)
   *
   * Related: US-002 (Create Content Slide)
   */
  val content: Template =
    Template(
      name = "content",
      slots = List(
        SlotDefinition.heading,
        SlotDefinition.body
      )
    )

  /**
   * Diagram template (v2.0.0).
   *
   * Optimized for displaying Mermaid diagrams.
   *
   * Slots:
   * - heading (required, max 80 chars)
   * - caption (optional, max 100 chars) - displayed below diagram
   *
   * Note: Requires at least one Mermaid diagram in content (validated separately)
   *
   * Related: US-022 (Mermaid Diagram Support)
   */
  val diagram: Template =
    Template(
      name = "diagram",
      slots = List(
        SlotDefinition.heading,
        SlotDefinition.caption
      )
    )

  /**
   * Closing template (v2.0.0).
   *
   * For thank you slides and contact information.
   *
   * Slots:
   * - heading (required, max 80 chars) - typically "Thank You"
   * - body (optional, max 12 lines) - contact info, closing remarks
   *
   * Related: v2.0.0 Additional Templates
   */
  val closing: Template =
    Template(
      name = "closing",
      slots = List(
        SlotDefinition.heading,
        SlotDefinition.bodyOptional
      )
    )

  /**
   * Section title template (v2.0.0).
   *
   * For section dividers in long presentations.
   *
   * Slots:
   * - heading (required, max 80 chars) - section name
   * - body (optional, max 12 lines) - subtitle/description
   *
   * Related: v2.0.0 Additional Templates
   */
  val sectionTitle: Template =
    Template(
      name = "section-title",
      slots = List(
        SlotDefinition.heading,
        SlotDefinition.bodyOptional
      )
    )

  /**
   * Two-column template (v3.0.0).
   *
   * For side-by-side content (code + explanation, before/after, etc.).
   *
   * Slots:
   * - leftColumn (required) - content for left column
   * - rightColumn (required) - content for right column
   *
   * Content separated by ---column--- delimiter.
   * Supports all markdown formatting in both columns.
   *
   * Related: v3.0.0 Two-Column Layout
   */
  val twoColumn: Template =
    Template(
      name = "two-column",
      slots = List(
        SlotDefinition("leftColumn", required = true, constraints = SlotConstraints()),
        SlotDefinition("rightColumn", required = true, constraints = SlotConstraints())
      )
    )

  /**
   * Get template by name.
   */
  def fromName(name: String): Either[String, Template] =
    name match
      case "title" => Right(title)
      case "content" => Right(content)
      case "diagram" => Right(diagram)
      case "closing" => Right(closing)
      case "section-title" => Right(sectionTitle)
      case "two-column" => Right(twoColumn)
      case _ => Left(s"Unknown template '$name'. Valid templates: title, content, diagram, closing, section-title, two-column")

end Template
