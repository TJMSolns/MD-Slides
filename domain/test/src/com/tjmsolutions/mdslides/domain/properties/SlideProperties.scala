package com.tjmsolutions.mdslides.domain
package properties

import generators.DomainGenerators.*
import org.scalacheck.Prop.*

/**
 * Property-based tests for Slide aggregate invariants.
 *
 * Properties tested:
 * 1. Validated slide always has required slots
 * 2. Validated slide never exceeds slot constraints
 * 3. Validation is idempotent
 * 4. Invalid slide always fails validation
 * 5. Validation collects all errors (not fail-fast)
 *
 * Related Governance:
 * - POL-004: Property-Based Testing Requirements
 * - ADR-009: Property-Based Testing Strategy
 */
class SlideProperties extends munit.ScalaCheckSuite:

  /**
   * Property 1: Validated slide always has required slots.
   *
   * Invariant: If validation succeeds, all required slots must be present.
   */
  property("validated title slide always has required slots") {
    forAll(validTitleSlideGen) { slide =>
      Slide.validated(slide.id, slide.templateName, slide.slots) match
        case Right(validSlide) =>
          // Title template requires "title" slot
          validSlide.hasSlot("title") == true

        case Left(_) =>
          // Validation failed, property doesn't apply
          true
    }
  }

  /**
   * Property 2: Validated slide never exceeds slot constraints.
   *
   * Invariant: If validation succeeds, all slot content must satisfy constraints.
   */
  property("validated title slot never exceeds 2 lines") {
    forAll(validTitleSlideGen) { slide =>
      Slide.validated(slide.id, slide.templateName, slide.slots) match
        case Right(validSlide) =>
          validSlide.getSlot("title") match
            case Some(title) =>
              val lineCount = SlotContent.fromPlainText(title).lineCount
              lineCount <= 2

            case None =>
              // Title is required, should never be None
              false

        case Left(_) =>
          // Validation failed, property doesn't apply
          true
    }
  }

  /**
   * Property 3: Validated author slot never exceeds 80 chars.
   */
  property("validated author slot never exceeds 80 characters") {
    forAll(validTitleSlideGen) { slide =>
      Slide.validated(slide.id, slide.templateName, slide.slots) match
        case Right(validSlide) =>
          validSlide.getSlot("author") match
            case Some(author) =>
              author.length <= 80

            case None =>
              // Author is optional, None is valid
              true

        case Left(_) =>
          // Validation failed, property doesn't apply
          true
    }
  }

  /**
   * Property 4: Validation is idempotent.
   *
   * Invariant: Validating an already-validated slide produces the same result.
   */
  property("validation is idempotent") {
    forAll(validTitleSlideGen) { slide =>
      val result1 = Slide.validated(slide.id, slide.templateName, slide.slots)
      val result2 = result1.flatMap(validSlide =>
        Slide.validated(validSlide.id, validSlide.templateName, validSlide.slots)
      )

      result1 == result2
    }
  }

  /**
   * Property 5: Slide with missing required slots always fails structure validation.
   *
   * Invariant: Missing required slots triggers StructureError.
   */
  property("slide with missing title always fails validation") {
    forAll(slideWithStructureErrorGen) { slide =>
      Slide.validated(slide.id, slide.templateName, slide.slots) match
        case Left(errors) =>
          // Should have at least one StructureError
          errors.toList.exists {
            case ValidationError.StructureError(_, msg, _) =>
              msg.contains("title") && msg.contains("missing")
            case _ => false
          }

        case Right(_) =>
          // Should never succeed with missing required slots
          false
    }
  }

  /**
   * Property 6: Slide with constraint violations always fails content validation.
   *
   * Invariant: Exceeding max lines/chars triggers ContentError.
   */
  property("slide with title exceeding 2 lines always fails validation") {
    forAll(slideWithContentErrorGen) { slide =>
      Slide.validated(slide.id, slide.templateName, slide.slots) match
        case Left(errors) =>
          // Should have at least one ContentError about lines
          errors.toList.exists {
            case ValidationError.ContentError(_, slotName, msg, _) =>
              slotName == "title" && msg.contains("exceeds max 2 lines")
            case _ => false
          }

        case Right(_) =>
          // Should never succeed with constraint violations
          false
    }
  }

  /**
   * Property 7: Validation collects all errors (not fail-fast).
   *
   * Invariant: If multiple constraints violated, all errors reported.
   */
  property("validation collects all errors") {
    val slideId = SlideId.unsafe(1)
    val invalidSlide = Slide(
      id = slideId,
      templateName = "title",
      slots = Map(
        "title" -> "Line 1\nLine 2\nLine 3",  // Exceeds max 2 lines
        "author" -> ("x" * 100)                // Exceeds max 80 chars
      )
    )

    Slide.validated(invalidSlide.id, invalidSlide.templateName, invalidSlide.slots) match
      case Left(errors) =>
        // Should have exactly 2 errors (title lines + author chars)
        errors.length == 2

      case Right(_) =>
        false
  }

  /**
   * Property 8: Valid SlideId range (1-200).
   *
   * Invariant: Slide ID must be within valid range.
   */
  property("slide ID always within 1-200 range") {
    forAll(slideIdGen) { id =>
      val intValue = id.toInt
      intValue >= 1 && intValue <= 200
    }
  }

  /**
   * Property 9: Template resolution is deterministic.
   *
   * Invariant: Same template name always resolves to same template.
   */
  property("template resolution is deterministic") {
    forAll(templateNameGen) { name =>
      val result1 = Template.fromName(name)
      val result2 = Template.fromName(name)
      result1 == result2
    }
  }

  /**
   * Property 10: Non-existent template always fails validation.
   *
   * Invariant: Invalid template name triggers StructureError.
   */
  property("non-existent template always fails validation") {
    val invalidTemplateName = "this-template-does-not-exist"
    forAll(slideIdGen, validTitleSlotsGen) { (id, slots) =>
      Slide.validated(id, invalidTemplateName, slots) match
        case Left(errors) =>
          errors.toList.exists {
            case ValidationError.StructureError(_, msg, _) =>
              msg.contains("Unknown template")
            case _ => false
          }

        case Right(_) =>
          false
    }
  }

end SlideProperties
