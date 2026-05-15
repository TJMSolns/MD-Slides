package com.tjmsolutions.mdslides.domain

/**
 * History Event
 *
 * Domain events captured during presentation playback.
 * All events are immutable and timestamped.
 *
 * Event Types:
 * - SessionStarted: Presentation session begins
 * - SlideChanged: User navigates to different slide
 * - TimerStarted: Presentation timer started
 * - TimerPaused: Presentation timer paused
 * - TimerResumed: Presentation timer resumed
 * - BreakModeActivated: Break mode activated
 * - BreakModeDeactivated: Break mode deactivated
 * - SessionEnded: Presentation session ends
 */
enum HistoryEvent:
  /**
   * Session started event.
   *
   * @param timestamp Epoch milliseconds when session started
   * @param totalSlides Total number of slides in deck
   * @param metadata Presentation metadata (title, author, etc.)
   */
  case SessionStarted(
    timestamp: Long,
    totalSlides: Int,
    metadata: Map[String, String]
  )

  /**
   * Slide changed event.
   *
   * @param timestamp Epoch milliseconds when slide changed
   * @param fromSlide 0-indexed source slide
   * @param toSlide 0-indexed target slide
   * @param navigationMethod How user navigated (arrow-key, p-key, n-key, goto, etc.)
   */
  case SlideChanged(
    timestamp: Long,
    fromSlide: Int,
    toSlide: Int,
    navigationMethod: String
  )

  /**
   * Timer started event.
   *
   * @param timestamp Epoch milliseconds when timer started
   */
  case TimerStarted(timestamp: Long)

  /**
   * Timer paused event.
   *
   * @param timestamp Epoch milliseconds when timer paused
   * @param elapsedMillis Total elapsed time in milliseconds
   */
  case TimerPaused(timestamp: Long, elapsedMillis: Long)

  /**
   * Timer resumed event.
   *
   * @param timestamp Epoch milliseconds when timer resumed
   */
  case TimerResumed(timestamp: Long)

  /**
   * Break mode activated event.
   *
   * @param timestamp Epoch milliseconds when break mode activated
   */
  case BreakModeActivated(timestamp: Long)

  /**
   * Break mode deactivated event.
   *
   * @param timestamp Epoch milliseconds when break mode deactivated
   * @param breakDurationMillis Duration of break in milliseconds
   */
  case BreakModeDeactivated(timestamp: Long, breakDurationMillis: Long)

  /**
   * Session ended event.
   *
   * @param timestamp Epoch milliseconds when session ended
   * @param totalElapsedMillis Total presentation time (excluding breaks)
   */
  case SessionEnded(timestamp: Long, totalElapsedMillis: Long)
