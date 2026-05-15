package com.tjmsolutions.mdslides.domain

/**
 * Goto Error
 *
 * Represents errors that can occur during goto command operations.
 */
enum GotoError:
  /**
   * Invalid slide index (out of range [1, totalSlides]).
   *
   * @param userInput The 1-indexed user input
   * @param totalSlides Total number of slides in the deck
   */
  case InvalidSlideIndex(userInput: Int, totalSlides: Int)

  /**
   * Invalid user input (not a valid integer).
   *
   * @param input The invalid input string
   */
  case InvalidInput(input: String)
