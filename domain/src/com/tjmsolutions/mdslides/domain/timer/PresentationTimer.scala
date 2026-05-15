package com.tjmsolutions.mdslides.domain.timer

/**
 * Presentation Timer Aggregate Root
 *
 * Tracks elapsed time during a live presentation session with pause/resume capability.
 *
 * Responsibility: Manage presentation session timing with accurate elapsed time calculation,
 * pause/resume state transitions, and cross-window synchronization.
 *
 * Invariants:
 * 1. State Exclusivity: Timer can only be in exactly one state at any time
 * 2. Monotonic Time: Elapsed time is always monotonically increasing
 * 3. Pause Precondition: Cannot pause timer unless currently running
 * 4. Resume Precondition: Cannot resume timer unless currently paused
 * 5. Paused Duration Bound: Total paused duration cannot exceed total runtime
 * 6. Start Once: Timer can only be started once per session (no reset capability)
 *
 * @param state Current timer state (NotStarted, Running, or Paused)
 * @param startTimestamp Epoch milliseconds when timer started (0 if not started)
 * @param totalPausedDuration Cumulative milliseconds spent paused
 * @param lastPauseTimestamp Epoch milliseconds of most recent pause (None if not paused)
 *
 * @see doc/domain-models/aggregates/presentation-timer-aggregate.md
 * @see features/presentation-timer.feature
 */
case class PresentationTimer(
  state: TimerState,
  startTimestamp: Long,
  totalPausedDuration: Long,
  lastPauseTimestamp: Option[Long]
):

  /**
   * Start the timer (NotStarted → Running)
   *
   * Preconditions:
   * - state == NotStarted
   *
   * Postconditions:
   * - state == Running
   * - startTimestamp set to current time
   * - totalPausedDuration == 0
   * - lastPauseTimestamp == None
   *
   * @return Right(PresentationTimer) if successful, Left(TimerError) if already started
   */
  def start(): Either[TimerError, PresentationTimer] =
    if state != TimerState.NotStarted then
      Left(TimerError.TimerAlreadyStarted)
    else
      val now = System.currentTimeMillis()
      Right(PresentationTimer(
        state = TimerState.Running,
        startTimestamp = now,
        totalPausedDuration = 0,
        lastPauseTimestamp = None
      ))

  /**
   * Pause the timer (Running → Paused)
   *
   * Preconditions:
   * - state == Running
   *
   * Postconditions:
   * - state == Paused
   * - lastPauseTimestamp set to current time
   * - totalPausedDuration unchanged (will be updated on resume)
   *
   * @return Right(PresentationTimer) if successful, Left(TimerError) if not running
   */
  def pause(): Either[TimerError, PresentationTimer] =
    if state != TimerState.Running then
      Left(TimerError.CannotPauseWhenNotRunning(state))
    else
      val now = System.currentTimeMillis()
      Right(copy(
        state = TimerState.Paused,
        lastPauseTimestamp = Some(now)
      ))

  /**
   * Resume the timer after a break (Paused → Running)
   *
   * Preconditions:
   * - state == Paused
   * - lastPauseTimestamp.isDefined
   *
   * Postconditions:
   * - state == Running
   * - totalPausedDuration updated to include latest pause
   * - lastPauseTimestamp == None
   *
   * @return Right(PresentationTimer) if successful, Left(TimerError) if not paused
   */
  def resume(): Either[TimerError, PresentationTimer] =
    if state != TimerState.Paused then
      Left(TimerError.CannotResumeWhenNotPaused(state))
    else
      lastPauseTimestamp match
        case None =>
          Left(TimerError.InvalidState("Paused but no pause timestamp"))
        case Some(pauseTime) =>
          val now = System.currentTimeMillis()
          val pauseDuration = now - pauseTime
          Right(copy(
            state = TimerState.Running,
            totalPausedDuration = totalPausedDuration + pauseDuration,
            lastPauseTimestamp = None
          ))

  /**
   * Calculate elapsed time in seconds (excluding paused time)
   *
   * Formula:
   * elapsed = (currentTime - startTime - totalPausedDuration) / 1000
   *
   * If paused:
   * tempPausedDuration = currentTime - lastPauseTimestamp
   * elapsed = (currentTime - startTime - totalPausedDuration - tempPausedDuration) / 1000
   *
   * @return Seconds elapsed since start (0 if not started)
   */
  def elapsedSeconds(): Long =
    if startTimestamp == 0 then 0
    else
      val now = System.currentTimeMillis()
      val totalRuntime = now - startTimestamp

      // If currently paused, include current pause duration
      val effectivePausedDuration = state match
        case TimerState.Paused =>
          lastPauseTimestamp match
            case Some(pauseTime) =>
              totalPausedDuration + (now - pauseTime)
            case None =>
              totalPausedDuration  // Should not happen (invariant violation)
        case _ =>
          totalPausedDuration

      val effectiveRuntime = totalRuntime - effectivePausedDuration
      effectiveRuntime / 1000

  /**
   * Get human-readable time in hh:mm:ss format
   *
   * Format supports unbounded hours (e.g., "100:00:00" for 100+ hour presentations)
   * Hours use minimum 2 digits, with additional digits as needed
   * Minutes and seconds always use exactly 2 digits with zero-padding
   *
   * Examples:
   * - 0 seconds → "00:00:00"
   * - 59 seconds → "00:00:59"
   * - 60 seconds → "00:01:00"
   * - 3600 seconds → "01:00:00"
   * - 86400 seconds → "24:00:00"
   * - 360000 seconds → "100:00:00"
   *
   * @return Formatted time string (e.g., "00:15:30")
   */
  def formattedTime(): String =
    val seconds = elapsedSeconds()
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    f"$hours%02d:$minutes%02d:$secs%02d"

  /**
   * Get current timer state
   *
   * @return NotStarted, Running, or Paused
   */
  def currentState(): TimerState = state

end PresentationTimer

/**
 * PresentationTimer companion object
 */
object PresentationTimer:
  /**
   * Create a new timer in NotStarted state
   *
   * @return PresentationTimer ready to be started
   */
  def create(): PresentationTimer =
    PresentationTimer(
      state = TimerState.NotStarted,
      startTimestamp = 0,
      totalPausedDuration = 0,
      lastPauseTimestamp = None
    )
