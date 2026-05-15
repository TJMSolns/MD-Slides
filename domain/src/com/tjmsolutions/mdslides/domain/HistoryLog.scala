package com.tjmsolutions.mdslides.domain

/**
 * History Log Aggregate
 *
 * Accumulates domain events during presentation playback.
 * Events are append-only and chronologically ordered.
 *
 * Invariants:
 * - Events are immutable once appended
 * - Events maintain chronological order (append-only)
 * - Log can be serialized to JSON for persistence
 */
case class HistoryLog(
  events: List[HistoryEvent]
):

  /**
   * Append a new event to the log.
   *
   * @param event The event to append
   * @return Updated HistoryLog with event appended
   */
  def appendEvent(event: HistoryEvent): HistoryLog =
    copy(events = events :+ event)

  /**
   * Serialize entire log to JSON string.
   *
   * @return JSON representation of the log
   */
  def serializeToJson(): String =
    val eventsJson = events.map(HistoryLog.eventToJson).mkString(",")
    s"""{"events":[$eventsJson]}"""

object HistoryLog:

  /**
   * Create an empty HistoryLog.
   *
   * @return New HistoryLog with no events
   */
  def create(): HistoryLog =
    HistoryLog(events = List.empty[HistoryEvent])

  /**
   * Serialize a single event to JSON.
   *
   * @param event The event to serialize
   * @return JSON string representation
   */
  def eventToJson(event: HistoryEvent): String =
    event match
      case HistoryEvent.SessionStarted(timestamp, totalSlides, metadata) =>
        val metadataJson = metadata.map { (k, v) =>
          s""""$k":"$v""""
        }.mkString(",")
        s"""{"eventType":"SessionStarted","timestamp":$timestamp,"totalSlides":$totalSlides,"metadata":{$metadataJson}}"""

      case HistoryEvent.SlideChanged(timestamp, fromSlide, toSlide, navigationMethod) =>
        s"""{"eventType":"SlideChanged","timestamp":$timestamp,"fromSlide":$fromSlide,"toSlide":$toSlide,"navigationMethod":"$navigationMethod"}"""

      case HistoryEvent.TimerStarted(timestamp) =>
        s"""{"eventType":"TimerStarted","timestamp":$timestamp}"""

      case HistoryEvent.TimerPaused(timestamp, elapsedMillis) =>
        s"""{"eventType":"TimerPaused","timestamp":$timestamp,"elapsedMillis":$elapsedMillis}"""

      case HistoryEvent.TimerResumed(timestamp) =>
        s"""{"eventType":"TimerResumed","timestamp":$timestamp}"""

      case HistoryEvent.BreakModeActivated(timestamp) =>
        s"""{"eventType":"BreakModeActivated","timestamp":$timestamp}"""

      case HistoryEvent.BreakModeDeactivated(timestamp, breakDurationMillis) =>
        s"""{"eventType":"BreakModeDeactivated","timestamp":$timestamp,"breakDurationMillis":$breakDurationMillis}"""

      case HistoryEvent.SessionEnded(timestamp, totalElapsedMillis) =>
        s"""{"eventType":"SessionEnded","timestamp":$timestamp,"totalElapsedMillis":$totalElapsedMillis}"""
