package com.tjmsolutions.mdslides.domain

/**
 * Goto Command
 *
 * Handles navigation to a specific slide by user input (1-indexed).
 * Converts 1-indexed user input to 0-indexed slide index.
 *
 * Responsibilities:
 * - Parse user input (string → integer)
 * - Validate slide index is in range [1, totalSlides]
 * - Convert to 0-indexed representation
 *
 * Business Rules:
 * - User sees slides numbered 1, 2, 3, ... (1-indexed)
 * - Internal representation uses 0, 1, 2, ... (0-indexed)
 * - Input "5" navigates to slide 5 (internal index 4)
 */
object GotoCommand:

  /**
   * Navigate to a slide by 1-indexed user input.
   *
   * @param userInput 1-indexed slide number (user types "5" for slide 5)
   * @param totalSlides Total number of slides in the deck
   * @return Either GotoError or 0-indexed slide index
   */
  def gotoSlide(userInput: Int, totalSlides: Int): Either[GotoError, Int] =
    if userInput < 1 || userInput > totalSlides then
      Left(GotoError.InvalidSlideIndex(userInput, totalSlides))
    else
      Right(userInput - 1)  // Convert to 0-indexed

  /**
   * Parse user input string to integer.
   *
   * @param input User input string (e.g., "5", "  10  ")
   * @return Either GotoError.InvalidInput or parsed integer
   */
  def parseUserInput(input: String): Either[GotoError, Int] =
    val trimmed = input.trim
    if trimmed.isEmpty then
      Left(GotoError.InvalidInput(input))
    else
      trimmed.toIntOption match
        case Some(value) => Right(value)
        case None => Left(GotoError.InvalidInput(input))

  /**
   * Parse and validate user input in one step.
   *
   * @param input User input string
   * @param totalSlides Total number of slides
   * @return Either GotoError or 0-indexed slide index
   */
  def parseAndGotoSlide(input: String, totalSlides: Int): Either[GotoError, Int] =
    for
      parsed <- parseUserInput(input)
      slideIndex <- gotoSlide(parsed, totalSlides)
    yield slideIndex
