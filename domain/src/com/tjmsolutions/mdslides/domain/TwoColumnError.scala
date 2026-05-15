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
