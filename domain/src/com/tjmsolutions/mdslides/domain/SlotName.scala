package com.tjmsolutions.mdslides.domain

/**
 * Name of a content slot in a template.
 *
 * Closed set of slot names used by the six built-in templates (ADR-008): the full
 * content-slot keyspace is `title`, `subtitle`, `author`, `heading`, `body`, `caption`,
 * `leftColumn`, `rightColumn` — 8 names, closed, all known today (no config-driven or
 * custom-template mechanism exists yet to introduce more).
 *
 * This ADT is enforced across the parser -> domain -> renderer boundary (LL-003, MS-017):
 * a typo like `SlotName.Heding` fails to compile, whereas the equivalent raw string
 * `"heding"` previously type-checked silently and rendered nothing, with no error and no
 * test failure (MS-005 / HL-002).
 *
 * Scope note (MS-017 design note, Q2a): frontmatter metadata keys (`header`, `footer`,
 * `vertical-align`) are deliberately NOT part of this ADT. They are a separate,
 * already-consistent contract (`MarkdownParser` and `HTMLRenderer`/`HeaderFooter` already
 * agree on 3 literal strings with no `SlotDefinition` indirection in between) and remain
 * raw `String` keys, per the approved scope.
 *
 * Related Governance:
 * - POL-001: Ubiquitous Language Enforcement
 * - ADR-008: Slot-Based Content Model
 */
enum SlotName derives CanEqual:
  case Title, Subtitle, Author, Heading, Body, Caption, LeftColumn, RightColumn

object SlotName:
  /**
   * Parse a SlotName from its string representation.
   *
   * Used at the JSON/Markdown boundary, where a raw string necessarily still arrives.
   *
   * @param value The slot name string (e.g., "title", "leftColumn")
   * @return Right(slotName) if recognized, Left(error) otherwise
   */
  def fromString(value: String): Either[String, SlotName] =
    value match
      case "title"       => Right(Title)
      case "subtitle"    => Right(Subtitle)
      case "author"      => Right(Author)
      case "heading"     => Right(Heading)
      case "body"        => Right(Body)
      case "caption"     => Right(Caption)
      case "leftColumn"  => Right(LeftColumn)
      case "rightColumn" => Right(RightColumn)
      case _ =>
        Left(
          s"Invalid slot name: '$value' (must be one of: title, subtitle, author, heading, body, caption, leftColumn, rightColumn)"
        )

  extension (name: SlotName)
    /** The slot name's string representation (kebab/camel string per the existing literal). */
    def value: String = name match
      case Title       => "title"
      case Subtitle    => "subtitle"
      case Author      => "author"
      case Heading     => "heading"
      case Body        => "body"
      case Caption     => "caption"
      case LeftColumn  => "leftColumn"
      case RightColumn => "rightColumn"
