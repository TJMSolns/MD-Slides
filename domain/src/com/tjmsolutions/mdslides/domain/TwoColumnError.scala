package com.tjmsolutions.mdslides.domain

/**
 * Two-Column Layout Error
 *
 * Represents errors that can occur during two-column slide parsing.
 */
enum TwoColumnError:
  /**
   * Missing column delimiter (zero delimiters found).
   */
  case MissingDelimiter

  /**
   * Multiple column delimiters found (expected exactly one).
   *
   * @param count Number of delimiters found
   */
  case MultipleDelimiters(count: Int)

  /**
   * Empty column (left or right column has no content after trimming).
   *
   * @param column Which column is empty ("left" or "right")
   */
  case EmptyColumn(column: String)

  /** What the author needs to do, in plain words (MS-031: the raw case name was being printed). */
  def displayMessage: String = this match
    case MissingDelimiter => "a two-column slide needs one ---column--- line between the columns (found none)"
    case MultipleDelimiters(count) => s"a two-column slide needs exactly one ---column--- line (found $count)"
    case EmptyColumn(column) => s"the $column column of a two-column slide is empty"

