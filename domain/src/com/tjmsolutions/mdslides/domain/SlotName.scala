package com.tjmsolutions.mdslides.domain

/**
 * Name of a content slot in a template.
 *
 * Valid slot names: "title", "subtitle", "author", "heading", "body", "code", "image", etc.
 *
 * Invariant: Slot name must be non-empty alphanumeric + hyphen/underscore
 *
 * Related Governance:
 * - POL-001: Ubiquitous Language Enforcement
 * - ADR-008: Slot-Based Content Model
 */
opaque type SlotName = String

object SlotName:
  private val ValidPattern = "^[a-z][a-z0-9_-]*$".r

  /**
   * Creates a SlotName from a String.
   *
   * @param value The slot name (e.g., "title", "subtitle")
   * @return Right(slotName) if valid, Left(error) otherwise
   */
  def apply(value: String): Either[String, SlotName] =
    value match
      case ValidPattern() => Right(value)
      case _ => Left(s"Invalid slot name: '$value' (must match pattern: ${ValidPattern.regex})")

  /**
   * Unsafe constructor for testing (assumes valid name).
   *
   * @param value The slot name
   * @return SlotName without validation
   */
  def unsafe(value: String): SlotName = value

  extension (name: SlotName)
    def value: String = name
