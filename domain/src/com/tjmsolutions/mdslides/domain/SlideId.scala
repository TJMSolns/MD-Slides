package com.tjmsolutions.mdslides.domain

/**
 * Unique identifier for a slide within a deck.
 *
 * Invariant: SlideId must be positive (1-200 per PDR-003)
 *
 * Related Governance:
 * - POL-001: Ubiquitous Language Enforcement
 * - PDR-003: Slide Deck Size Limits
 */
opaque type SlideId = Int

object SlideId:
  /**
   * Creates a SlideId from an Int.
   *
   * @param value The numeric slide identifier (must be 1-200)
   * @return Right(slideId) if valid, Left(error) otherwise
   */
  def apply(value: Int): Either[String, SlideId] =
    if value >= 1 && value <= 200 then Right(value)
    else Left(s"Slide ID must be between 1 and 200, got: $value")

  /**
   * Unsafe constructor for testing (assumes valid ID).
   *
   * @param value The numeric slide identifier
   * @return SlideId without validation
   */
  def unsafe(value: Int): SlideId = value

  extension (id: SlideId)
    def toInt: Int = id
