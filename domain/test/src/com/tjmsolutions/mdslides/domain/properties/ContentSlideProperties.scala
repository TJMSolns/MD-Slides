package com.tjmsolutions.mdslides.domain
package properties

import generators.DomainGenerators.*
import org.scalacheck.Prop.*

/**
 * Property-based tests for Content Slide invariants (US-002).
 *
 * Properties tested:
 * 1. Validated content slide always has required slots (heading, body)
 * 2. Validated heading never exceeds 80 characters
 * 3. Validated body never exceeds 12 lines
 * 4. Validated body never exceeds 150 words
 * 5. Missing heading always fails validation
 * 6. Missing body always fails validation
 * 7. Body exceeding 12 lines always fails validation
 * 8. Body exceeding 150 words always fails validation
 *
 * Related Governance:
 * - POL-004: Property-Based Testing Requirements
 * - ADR-009: Property-Based Testing Strategy
 * - PDR-001: Density Validation Limits
 */
class ContentSlideProperties extends munit.ScalaCheckSuite:

  /**
   * Property 1: Validated content slide always has required slots.
   *
   * Invariant: If validation succeeds, heading and body must be present.
   */
  property("validated content slide always has required slots") {
    forAll(validContentSlideGen) { slide =>
      Slide.validated(slide.id, slide.templateName, slide.slots) match
        case Right(validSlide) =>
          // Content template requires "heading" and "body" slots
          validSlide.hasSlot("heading") && validSlide.hasSlot("body")

        case Left(_) =>
          // Validation failed, property doesn't apply
          true
    }
  }

  /**
   * Property 2: Validated heading never exceeds 80 characters.
   *
   * Invariant: If validation succeeds, heading must be ≤ 80 chars.
   */
  property("validated heading never exceeds 80 characters") {
    forAll(validContentSlideGen) { slide =>
      Slide.validated(slide.id, slide.templateName, slide.slots) match
        case Right(validSlide) =>
          validSlide.getSlot("heading") match
            case Some(heading) =>
              heading.length <= 80

            case None =>
              // Heading is required, should never be None
              false

        case Left(_) =>
          // Validation failed, property doesn't apply
          true
    }
  }

  /**
   * Property 3: Validated body never exceeds 12 lines.
   *
   * Invariant: If validation succeeds, body must be ≤ 12 lines.
   */
  property("validated body never exceeds 12 lines") {
    forAll(validContentSlideGen) { slide =>
      Slide.validated(slide.id, slide.templateName, slide.slots) match
        case Right(validSlide) =>
          validSlide.getSlot("body") match
            case Some(body) =>
              val lineCount = SlotContent.fromPlainText(body).lineCount
              lineCount <= 12

            case None =>
              // Body is required, should never be None
              false

        case Left(_) =>
          // Validation failed, property doesn't apply
          true
    }
  }

  /**
   * Property 4: Validated body never exceeds 150 words.
   *
   * Invariant: If validation succeeds, body must be ≤ 150 words.
   */
  property("validated body never exceeds 150 words") {
    forAll(validContentSlideGen) { slide =>
      Slide.validated(slide.id, slide.templateName, slide.slots) match
        case Right(validSlide) =>
          validSlide.getSlot("body") match
            case Some(body) =>
              val wordCount = SlotContent.fromPlainText(body).wordCount
              wordCount <= 150

            case None =>
              // Body is required, should never be None
              false

        case Left(_) =>
          // Validation failed, property doesn't apply
          true
    }
  }

  /**
   * Property 5: Slide with missing heading always fails structure validation.
   *
   * Invariant: Missing heading triggers StructureError.
   */
  property("content slide with missing heading always fails validation") {
    forAll(contentSlideWithMissingHeadingGen) { slide =>
      Slide.validated(slide.id, slide.templateName, slide.slots) match
        case Left(errors) =>
          // Should have at least one StructureError
          errors.toList.exists {
            case ValidationError.StructureError(_, msg, _) =>
              msg.contains("heading") && msg.contains("missing")
            case _ => false
          }

        case Right(_) =>
          // Should never succeed with missing required slots
          false
    }
  }

  /**
   * Property 6: Slide with missing body always fails structure validation.
   *
   * Invariant: Missing body triggers StructureError.
   */
  property("content slide with missing body always fails validation") {
    forAll(contentSlideWithMissingBodyGen) { slide =>
      Slide.validated(slide.id, slide.templateName, slide.slots) match
        case Left(errors) =>
          // Should have at least one StructureError
          errors.toList.exists {
            case ValidationError.StructureError(_, msg, _) =>
              msg.contains("body") && msg.contains("missing")
            case _ => false
          }

        case Right(_) =>
          // Should never succeed with missing required slots
          false
    }
  }

  /**
   * Property 7: Body exceeding 12 lines always fails content validation.
   *
   * Invariant: Exceeding max lines triggers ContentError.
   */
  property("content slide with body exceeding 12 lines always fails validation") {
    forAll(contentSlideWithExcessiveLinesGen) { slide =>
      Slide.validated(slide.id, slide.templateName, slide.slots) match
        case Left(errors) =>
          // Should have at least one ContentError about lines
          errors.toList.exists {
            case ValidationError.ContentError(_, slotName, msg, _) =>
              slotName == "body" && msg.contains("exceeds max 12 lines")
            case _ => false
          }

        case Right(_) =>
          // Should never succeed with constraint violations
          false
    }
  }

  /**
   * Property 8: Body exceeding 150 words always fails content validation.
   *
   * Invariant: Exceeding max words triggers ContentError.
   */
  property("content slide with body exceeding 150 words always fails validation") {
    forAll(contentSlideWithExcessiveWordsGen) { slide =>
      Slide.validated(slide.id, slide.templateName, slide.slots) match
        case Left(errors) =>
          // Should have at least one ContentError about words
          errors.toList.exists {
            case ValidationError.ContentError(_, slotName, msg, _) =>
              slotName == "body" && msg.contains("exceeds max 150 words")
            case _ => false
          }

        case Right(_) =>
          // Should never succeed with constraint violations
          false
    }
  }

  /**
   * Property 9: Validation is idempotent for content slides.
   *
   * Invariant: Validating an already-validated slide produces the same result.
   */
  property("content slide validation is idempotent") {
    forAll(validContentSlideGen) { slide =>
      val result1 = Slide.validated(slide.id, slide.templateName, slide.slots)
      val result2 = result1.flatMap(validSlide =>
        Slide.validated(validSlide.id, validSlide.templateName, validSlide.slots)
      )

      result1 == result2
    }
  }

end ContentSlideProperties
