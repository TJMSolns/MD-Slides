package com.tjmsolutions.mdslides.domain.timer

/**
 * Timer State Value Object
 *
 * Represents the current state of a PresentationTimer in the state machine.
 *
 * State Transitions:
 * - NotStarted → Running (via start())
 * - Running → Paused (via pause())
 * - Paused → Running (via resume())
 *
 * Invariant: Timer is always in exactly one state (State Exclusivity)
 *
 * @see PresentationTimer
 * @see doc/domain-models/aggregates/presentation-timer-aggregate.md
 */
enum TimerState:
  /** Timer has been created but not yet started */
  case NotStarted

  /** Timer is actively counting elapsed time */
  case Running

  /** Timer is temporarily stopped during a break (B key pressed) */
  case Paused
