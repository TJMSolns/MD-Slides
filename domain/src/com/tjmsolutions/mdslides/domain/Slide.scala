package com.tjmsolutions.mdslides.domain

import cats.data.NonEmptyList

/**
 * Slide aggregate representing a single presentation slide.
 *
 * Invariants:
 * - Slide ID must be valid (1-200)
 * - Template must exist
 * - All required slots must be present (validated separately)
 * - Slot content must satisfy template constraints (validated separately)
 *
 * Background images (US-011):
 * - Per-slide background images override template and theme defaults
 * - Background is presentation metadata (not content/slot)
 * - Supports both simple string paths and BackgroundConfig objects
 *
 * Speaker notes (US-004):
 * - Per-slide speaker notes for presenter reference
 * - Notes are presentation metadata (not rendered content)
 * - Optional field (None if no notes provided)
 *
 * Frontmatter metadata (MS-017 design note, Q2a):
 * - `header`/`footer`/`vertical-align` frontmatter keys are carried in `metadata`, a
 *   separate raw-`String`-keyed channel — NOT in `slots`. They are config, not
 *   template-declared content, so they are deliberately out of scope for the `SlotName`
 *   ADT that closes the LL-003 gap for template content slots.
 *
 * Related Governance:
 * - ADR-008: Slot-Based Content Model
 * - PDR-011: Background Image Architecture
 * - POL-001: Ubiquitous Language Enforcement
 * - POL-003: Pure Functional Domain
 */
case class Slide(
  id: SlideId,
  templateName: String,
  slots: Map[SlotName, String],  // Slot name → content (template-declared content only)
  backgroundImage: Option[SlideBackground] = None,  // Optional per-slide background override
  notes: Option[String] = None,  // Optional speaker notes (US-004)
  metadata: Map[String, String] = Map.empty  // Frontmatter metadata (header/footer/vertical-align), raw String keys (Q2a)
):
  /**
   * Get content for a template-declared slot.
   *
   * @param slotName The slot to retrieve
   * @return Some(content) if slot exists, None otherwise
   */
  def getSlot(slotName: SlotName): Option[String] =
    slots.get(slotName)

  /**
   * Get a frontmatter metadata value (header/footer/vertical-align) by its raw string key.
   *
   * This is a separate channel from `slots` (Q2a) — it is not validated against
   * `SlotDefinition`/`Template` and carries no compile-time enforcement.
   *
   * @param key The metadata key (e.g., "header", "footer", "vertical-align")
   * @return Some(value) if present, None otherwise
   */
  def getSlot(key: String): Option[String] =
    metadata.get(key)

  /**
   * Check if a slot is present (even if empty).
   */
  def hasSlot(slotName: SlotName): Boolean =
    slots.contains(slotName)

  /**
   * Get all slot names present in this slide.
   */
  def slotNames: Set[SlotName] =
    slots.keySet

end Slide

object Slide:
  /**
   * Create a validated Slide.
   *
   * Performs structure and content validation.
   *
   * @param id Slide identifier
   * @param templateName Template to bind to
   * @param slots Slot content map
   * @param backgroundImage Optional per-slide background image
   * @param notes Optional speaker notes (US-004)
   * @return Right(slide) if all validations pass, Left(errors) otherwise
   *
   * Related: ADR-002 (Validation Pipeline)
   */
  def validated(
    id: SlideId,
    templateName: String,
    slots: Map[SlotName, String],
    backgroundImage: Option[SlideBackground] = None,
    notes: Option[String] = None,
    metadata: Map[String, String] = Map.empty
  ): Either[NonEmptyList[ValidationError], Slide] =
    val slide = Slide(id, templateName, slots, backgroundImage, notes, metadata)

    // Phase 1: Structure validation
    val structureErrors = validateStructure(slide)

    // Phase 2: Content validation (only if structure is valid)
    val contentErrors =
      if structureErrors.isEmpty then validateContent(slide)
      else Nil

    // Collect all errors
    val allErrors = structureErrors ++ contentErrors

    NonEmptyList.fromList(allErrors) match
      case Some(errors) => Left(errors)
      case None => Right(slide)

  /**
   * Validate slide structure.
   *
   * Checks:
   * - Template exists
   * - All required slots present
   *
   * Related: ADR-002 (Validation Pipeline - Phase 1)
   */
  private def validateStructure(slide: Slide): List[ValidationError] =
    Template.fromName(slide.templateName) match
      case Left(error) =>
        List(ValidationError.StructureError(slide.id, error))

      case Right(template) =>
        val missingRequired = template.requiredSlots
          .filterNot(slotDef => slide.hasSlot(slotDef.name))
          .map(slotDef =>
            ValidationError.StructureError(
              slide.id,
              s"Required slot '${slotDef.name.value}' is missing"
            )
          )

        missingRequired

  /**
   * Validate slide content.
   *
   * Checks:
   * - Slot content satisfies constraints (max lines, max chars, etc.)
   *
   * Related: ADR-002 (Validation Pipeline - Phase 2b)
   */
  private def validateContent(slide: Slide): List[ValidationError] =
    Template.fromName(slide.templateName) match
      case Left(_) => Nil  // Structure validation failed, skip content

      case Right(template) =>
        slide.slots.toList.flatMap { case (slotName, content) =>
          template.getSlot(slotName) match
            case None => Nil  // Unknown slot (ignore for now)
            case Some(slotDef) =>
              validateSlotContent(slide.id, slotName.value, content, slotDef.constraints)
        }

  /**
   * Validate individual slot content against constraints.
   */
  private def validateSlotContent(
    slideId: SlideId,
    slotName: String,
    content: String,
    constraints: SlotConstraints
  ): List[ValidationError] =
    val errors = List.newBuilder[ValidationError]

    // Check max lines (PDR-001: warnings not errors)
    constraints.maxLines.foreach { maxLines =>
      val lines = SlotContent.fromPlainText(content).lineCount
      if lines > maxLines then
        errors += ValidationError.DensityWarning(
          slideId,
          slotName,
          s"Content exceeds recommended line limit (actual: $lines, guidance: reduce to $maxLines lines for readability)"
        )
    }

    // Check max chars
    constraints.maxChars.foreach { maxChars =>
      val chars = content.length
      if chars > maxChars then
        errors += ValidationError.ContentError(
          slideId,
          slotName,
          s"Content exceeds max $maxChars characters (has $chars characters)"
        )
    }

    // Check max words (PDR-001: warnings not errors)
    constraints.maxWords.foreach { maxWords =>
      val words = SlotContent.fromPlainText(content).wordCount
      if words > maxWords then
        errors += ValidationError.DensityWarning(
          slideId,
          slotName,
          s"Content exceeds recommended word limit (actual: $words, guidance: reduce to $maxWords words for readability)"
        )
    }

    errors.result()

  /**
   * Validate template-specific content requirements (v2.0.0).
   *
   * Template-specific validation that requires examining parsed content.
   * This is called AFTER parsing in the infrastructure layer.
   *
   * Template-specific rules:
   * - Diagram template MUST contain at least one Mermaid diagram (Scenario 17)
   * - Other templates have no diagram requirements
   *
   * @param slide The slide to validate
   * @param parsedSlots Map of slot name to parsed FormattedContent
   * @return List of validation errors
   *
   * Related: v2.0.0 Additional Templates, Example Mapping Scenario 17
   */
  def validateTemplateSpecificContent(
    slide: Slide,
    parsedSlots: Map[SlotName, FormattedContent]
  ): List[ValidationError] =
    slide.templateName match
      case "diagram" =>
        // Diagram template requires at least one Mermaid diagram
        val hasMermaidDiagram = parsedSlots.values.exists { formattedContent =>
          formattedContent.content.exists {
            case DiagramElement(_) => true
            case _ => false
          }
        }

        if !hasMermaidDiagram then
          List(ValidationError.ContentError(
            slide.id,
            "content",
            "Diagram template requires at least one Mermaid diagram. Add a ```mermaid code fence to your slide content."
          ))
        else
          Nil

      case _ =>
        // Other templates have no diagram requirements
        Nil

end Slide
