package com.tjmsolutions.mdslides.domain

/**
 * Represents the runtime state of a presentation.
 *
 * Tracks:
 * - Current slide position
 * - Total slide count
 * - Timer state (started/not started, elapsed time)
 * - Speaker view open/closed status
 *
 * This is a pure domain model with no I/O or rendering concerns.
 * All operations return Either for error handling or new immutable instances.
 *
 * Related User Story: US-034 - Speaker Notes Rendering
 */
case class PresentationState(
  currentSlideIndex: Int,
  totalSlides: Int,
  timerStartTime: Option[Long] = None,
  speakerViewOpen: Boolean = false
):

  /**
   * Navigate to a specific slide index.
   *
   * @param index The slide index (0-based)
   * @return Right(new state) if valid, Left(error) if out of bounds
   */
  def goToSlide(index: Int): Either[String, PresentationState] =
    if index < 0 || index >= totalSlides then
      Left(s"Slide index $index is out of bounds (valid: 0-${totalSlides - 1})")
    else
      Right(copy(currentSlideIndex = index))

  /**
   * Navigate to the next slide.
   *
   * @return Right(new state) if not at end, Left(error) if already at last slide
   */
  def nextSlide(): Either[String, PresentationState] =
    if currentSlideIndex >= totalSlides - 1 then
      Left("Already at last slide")
    else
      Right(copy(currentSlideIndex = currentSlideIndex + 1))

  /**
   * Navigate to the previous slide.
   *
   * @return Right(new state) if not at start, Left(error) if already at first slide
   */
  def previousSlide(): Either[String, PresentationState] =
    if currentSlideIndex <= 0 then
      Left("Already at first slide")
    else
      Right(copy(currentSlideIndex = currentSlideIndex - 1))

  /**
   * Navigate to the first slide.
   */
  def firstSlide(): PresentationState =
    copy(currentSlideIndex = 0)

  /**
   * Navigate to the last slide.
   */
  def lastSlide(): PresentationState =
    copy(currentSlideIndex = totalSlides - 1)

  /**
   * Check if there is a next slide.
   */
  def hasNextSlide: Boolean =
    currentSlideIndex < totalSlides - 1

  /**
   * Check if there is a previous slide.
   */
  def hasPreviousSlide: Boolean =
    currentSlideIndex > 0

  /**
   * Start the timer with the given timestamp.
   *
   * @param startTime The timestamp in milliseconds (System.currentTimeMillis)
   * @return New state with timer started
   */
  def startTimer(startTime: Long): PresentationState =
    copy(timerStartTime = Some(startTime))

  /**
   * Calculate elapsed time in seconds since timer started.
   *
   * @param currentTime The current timestamp in milliseconds
   * @return Elapsed seconds (0 if timer not started)
   */
  def elapsedSeconds(currentTime: Long): Int =
    timerStartTime match
      case Some(start) =>
        val elapsedMs = currentTime - start
        (elapsedMs / 1000).toInt
      case None =>
        0

  /**
   * Open the speaker view.
   */
  def openSpeakerView(): PresentationState =
    copy(speakerViewOpen = true)

  /**
   * Close the speaker view.
   */
  def closeSpeakerView(): PresentationState =
    copy(speakerViewOpen = false)

end PresentationState

object PresentationState:

  /**
   * Create a new PresentationState with validation.
   *
   * @param currentSlideIndex The current slide index (0-based)
   * @param totalSlides The total number of slides
   * @return Right(state) if valid, Left(error) otherwise
   */
  def create(
    currentSlideIndex: Int,
    totalSlides: Int,
    timerStartTime: Option[Long] = None,
    speakerViewOpen: Boolean = false
  ): Either[String, PresentationState] =
    if totalSlides <= 0 then
      Left("totalSlides must be positive")
    else if currentSlideIndex < 0 || currentSlideIndex >= totalSlides then
      Left(s"currentSlideIndex $currentSlideIndex is out of bounds (valid: 0-${totalSlides - 1})")
    else
      Right(PresentationState(currentSlideIndex, totalSlides, timerStartTime, speakerViewOpen))

  /**
   * Create initial presentation state (slide 1, timer not started).
   *
   * @param slideCount The total number of slides
   * @return Initial presentation state
   */
  def initial(slideCount: Int): PresentationState =
    require(slideCount > 0, "slideCount must be positive")
    PresentationState(
      currentSlideIndex = 0,
      totalSlides = slideCount,
      timerStartTime = None,
      speakerViewOpen = false
    )

  /**
   * Format elapsed time as MM:SS.
   *
   * Examples:
   * - 0 seconds → "00:00"
   * - 5 seconds → "00:05"
   * - 90 seconds → "01:30"
   * - 3600 seconds → "60:00" (continues past 1 hour)
   *
   * @param seconds Elapsed seconds
   * @return Formatted time string
   */
  def formatTime(seconds: Int): String =
    val minutes = seconds / 60
    val secs = seconds % 60
    f"$minutes%02d:$secs%02d"

end PresentationState
