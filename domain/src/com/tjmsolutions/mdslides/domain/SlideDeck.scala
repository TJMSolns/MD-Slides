package com.tjmsolutions.mdslides.domain

import cats.data.NonEmptyList
import cats.implicits.*

/**
 * SlideDeck aggregate representing a complete presentation.
 *
 * Invariants:
 * - Deck must have at least 1 slide (NonEmptyList enforces this)
 * - Deck must have at most 200 slides (PDR-003)
 * - All slides must pass validation
 *
 * Related Governance:
 * - PDR-003: Slide Deck Size Limits (1-200 slides)
 * - ADR-002: Validation Pipeline Architecture
 * - POL-001: Ubiquitous Language Enforcement
 * - POL-003: Pure Functional Domain
 */
case class SlideDeck(
  slides: NonEmptyList[Slide]
):
  /**
   * Get the number of slides in this deck.
   */
  def slideCount: Int = slides.length

  /**
   * Get a slide by index (0-based).
   *
   * @param index The slide index
   * @return Some(slide) if index is valid, None otherwise
   */
  def getSlide(index: Int): Option[Slide] =
    slides.toList.lift(index)

  /**
   * Get all slides as a list.
   */
  def toList: List[Slide] = slides.toList

end SlideDeck

object SlideDeck:
  /**
   * Create a validated SlideDeck.
   *
   * Performs:
   * - Structure validation (deck size limits)
   * - Individual slide validation
   *
   * @param slides The slides in this deck (must be non-empty)
   * @return Right(deck) if all validations pass, Left(errors) otherwise
   *
   * Related: ADR-002 (Validation Pipeline)
   */
  def validated(slides: NonEmptyList[Slide]): Either[NonEmptyList[ValidationError], SlideDeck] =
    val deck = SlideDeck(slides)

    // Phase 1: Deck-level structure validation
    val structureErrors = validateDeckStructure(deck)

    // Phase 2: Individual slide validation (parallel)
    val slideErrors =
      if structureErrors.isEmpty then
        deck.slides.toList.zipWithIndex.flatMap { case (slide, index) =>
          Slide.validated(slide.id, slide.templateName, slide.slots) match
            case Left(errors) => errors.toList
            case Right(_) => Nil
        }
      else Nil

    // Collect all errors
    val allErrors = structureErrors ++ slideErrors

    NonEmptyList.fromList(allErrors) match
      case Some(errors) => Left(errors)
      case None => Right(deck)

  /**
   * Validate deck-level structure.
   *
   * Checks:
   * - Deck has 1-200 slides
   * - All slide IDs are unique
   * - All slide IDs are in valid range (1-200)
   *
   * Related: PDR-003 (Slide Deck Size Limits)
   */
  private def validateDeckStructure(deck: SlideDeck): List[ValidationError] =
    val errors = List.newBuilder[ValidationError]

    // Check deck size (1-200 slides)
    val count = deck.slideCount
    if count > 200 then
      // Use first slide's ID for the error (deck-level error)
      errors += ValidationError.StructureError(
        deck.slides.head.id,
        s"Deck has $count slides, maximum 200 allowed (see PDR-003)"
      )

    // Check slide ID uniqueness
    val slideIds = deck.slides.toList.map(_.id.toInt)
    val duplicates = slideIds.groupBy(identity).filter(_._2.length > 1).keys
    if duplicates.nonEmpty then
      errors += ValidationError.StructureError(
        deck.slides.head.id,
        s"Duplicate slide IDs found: ${duplicates.mkString(", ")}"
      )

    // Check slide ID range (all IDs must be 1-200)
    val invalidIds = slideIds.filterNot(id => id >= 1 && id <= 200)
    if invalidIds.nonEmpty then
      errors += ValidationError.StructureError(
        deck.slides.head.id,
        s"Invalid slide IDs (must be 1-200): ${invalidIds.mkString(", ")}"
      )

    errors.result()

  /**
   * Create a SlideDeck from a list of slides.
   *
   * @param slides The slides (must be non-empty)
   * @return Right(deck) if slides is non-empty and valid, Left(error) otherwise
   */
  def fromList(slides: List[Slide]): Either[String, SlideDeck] =
    NonEmptyList.fromList(slides) match
      case Some(nel) => Right(SlideDeck(nel))
      case None => Left("Cannot create deck with zero slides (minimum 1 required, see PDR-003)")

  /**
   * Unsafe constructor for testing (assumes slides is non-empty).
   *
   * @param slides The slides
   * @return SlideDeck without validation
   */
  def unsafe(slides: Slide*): SlideDeck =
    SlideDeck(NonEmptyList.fromListUnsafe(slides.toList))

end SlideDeck
