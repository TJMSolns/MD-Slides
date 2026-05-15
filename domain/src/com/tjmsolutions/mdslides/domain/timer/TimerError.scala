package com.tjmsolutions.mdslides.domain.timer

/**
 * Timer Error Types
 *
 * Represents errors that can occur during timer state transitions.
 * All errors are domain-level violations of timer invariants.
 *
 * @see PresentationTimer
 * @see doc/domain-models/aggregates/presentation-timer-aggregate.md
 */
enum TimerError:
  /** Attempted to start a timer that is already running (violates Start Once invariant) */
  case TimerAlreadyStarted

  /** Attempted to pause timer when not in Running state (violates Pause Precondition) */
  case CannotPauseWhenNotRunning(currentState: TimerState)

  /** Attempted to resume timer when not in Paused state (violates Resume Precondition) */
  case CannotResumeWhenNotPaused(currentState: TimerState)

  /** Internal state consistency error (e.g., Paused but no pause timestamp) */
  case InvalidState(message: String)
