package com.tjmsolutions.mdslides.domain
package properties

import generators.DomainGenerators.*
import org.scalacheck.Prop.*

/**
 * Property-based tests for SlideDeck aggregate invariants.
 *
 * Properties tested:
 * 1. Validated deck always has 1-200 slides
 * 2. Validated deck has unique slide IDs
 * 3. Deck exceeding 200 slides always fails validation
 * 4. Deck with duplicate IDs always fails validation
 * 5. Deck with invalid slides always fails validation
 * 6. Validation is idempotent
 * 7. All slides in validated deck pass individual validation
 *
 * Related Governance:
 * - PDR-003: Slide Deck Size Limits
 * - ADR-002: Validation Pipeline Architecture
 * - POL-004: Property-Based Testing Requirements
 */
class SlideDeckProperties extends munit.ScalaCheckSuite:

  /**
   * Property 1: Validated deck always has 1-200 slides.
   *
   * Invariant: If validation succeeds, deck must have 1-200 slides.
   */
  property("validated deck always has 1-200 slides") {
    forAll(validSlideDeckGen) { deck =>
      SlideDeck.validated(deck.slides) match
        case Right(validDeck) =>
          val count = validDeck.slideCount
          count >= 1 && count <= 200

        case Left(_) =>
          // Validation failed, property doesn't apply
          true
    }
  }

  /**
   * Property 2: Validated deck has unique slide IDs.
   *
   * Invariant: If validation succeeds, all slide IDs must be unique.
   */
  property("validated deck has unique slide IDs") {
    forAll(validSlideDeckGen) { deck =>
      SlideDeck.validated(deck.slides) match
        case Right(validDeck) =>
          val slideIds = validDeck.toList.map(_.id.toInt)
          slideIds.distinct.length == slideIds.length

        case Left(_) =>
          // Validation failed, property doesn't apply
          true
    }
  }

  /**
   * Property 3: Deck exceeding 200 slides always fails validation.
   *
   * Invariant: Deck with > 200 slides triggers StructureError.
   */
  property("deck exceeding 200 slides always fails validation") {
    forAll(slideDeckExceedingMaxSizeGen) { deck =>
      SlideDeck.validated(deck.slides) match
        case Left(errors) =>
          errors.toList.exists {
            case ValidationError.StructureError(_, msg, _) =>
              msg.contains("maximum 200 allowed")
            case _ => false
          }

        case Right(_) =>
          // Should never succeed with > 200 slides
          false
    }
  }

  /**
   * Property 4: Deck with duplicate IDs always fails validation.
   *
   * Invariant: Duplicate slide IDs trigger StructureError.
   */
  property("deck with duplicate slide IDs always fails validation") {
    forAll(slideDeckWithDuplicateIdsGen) { deck =>
      SlideDeck.validated(deck.slides) match
        case Left(errors) =>
          errors.toList.exists {
            case ValidationError.StructureError(_, msg, _) =>
              msg.contains("Duplicate slide IDs")
            case _ => false
          }

        case Right(_) =>
          // Should never succeed with duplicate IDs
          false
    }
  }

  /**
   * Property 5: Deck with invalid slides always fails validation.
   *
   * Invariant: If any slide fails validation, deck validation fails.
   */
  property("deck with invalid slides always fails validation") {
    forAll(slideDeckWithInvalidSlidesGen) { deck =>
      SlideDeck.validated(deck.slides) match
        case Left(errors) =>
          // Should have at least one error
          errors.length >= 1

        case Right(_) =>
          // Should never succeed with invalid slides
          false
    }
  }

  /**
   * Property 6: Validation is idempotent.
   *
   * Invariant: Validating an already-validated deck produces the same result.
   */
  property("deck validation is idempotent") {
    forAll(validSlideDeckGen) { deck =>
      val result1 = SlideDeck.validated(deck.slides)
      val result2 = result1.flatMap(validDeck =>
        SlideDeck.validated(validDeck.slides)
      )

      result1 == result2
    }
  }

  /**
   * Property 7: All slides in validated deck pass individual validation.
   *
   * Invariant: If deck validation succeeds, each slide must be valid.
   */
  property("all slides in validated deck pass individual validation") {
    forAll(validSlideDeckGen) { deck =>
      SlideDeck.validated(deck.slides) match
        case Right(validDeck) =>
          validDeck.toList.forall { slide =>
            Slide.validated(slide.id, slide.templateName, slide.slots).isRight
          }

        case Left(_) =>
          // Deck validation failed, property doesn't apply
          true
    }
  }

  /**
   * Property 8: Deck slide count matches list length.
   *
   * Invariant: slideCount always equals the actual number of slides.
   */
  property("deck slide count matches actual slide list length") {
    forAll(validSlideDeckGen) { deck =>
      deck.slideCount == deck.toList.length
    }
  }

  /**
   * Property 9: getSlide returns None for out-of-bounds indices.
   *
   * Invariant: getSlide(index) returns None if index < 0 or index >= slideCount.
   */
  property("getSlide returns None for out-of-bounds indices") {
    forAll(validSlideDeckGen) { deck =>
      val negativeIndex = deck.getSlide(-1)
      val tooLargeIndex = deck.getSlide(deck.slideCount)

      negativeIndex.isEmpty && tooLargeIndex.isEmpty
    }
  }

  /**
   * Property 10: getSlide returns Some for valid indices.
   *
   * Invariant: getSlide(index) returns Some if 0 <= index < slideCount.
   */
  property("getSlide returns Some for valid indices") {
    forAll(validSlideDeckGen) { deck =>
      val validIndex = if deck.slideCount > 1 then deck.slideCount / 2 else 0
      deck.getSlide(validIndex).isDefined
    }
  }

end SlideDeckProperties
