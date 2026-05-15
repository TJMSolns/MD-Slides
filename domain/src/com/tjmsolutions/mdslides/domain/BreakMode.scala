package com.tjmsolutions.mdslides.domain

/**
 * Break Mode Aggregate
 *
 * Represents the state of break mode during a presentation.
 * Break mode allows the presenter to take a break without showing slide content to the audience.
 * The timer pauses during break mode to exclude break time from elapsed presentation time.
 *
 * Invariants:
 * - If active, breakStartTimestamp must be Some
 * - If inactive, breakStartTimestamp must be None
 * - breakCount is always >= 0
 * - breakCount increments only on deactivation (not activation)
 */
case class BreakMode(
  isActive: Boolean = false,
  breakCount: Int = 0,
  breakStartTimestamp: Option[Long] = None,
  breakScreenPath: Option[String] = None
):

  /**
   * Activate break mode with optional custom break screen.
   *
   * @param screenPath Optional path to custom break screen image
   * @param timestamp Timestamp when break mode is activated (epoch milliseconds)
   * @return Either BreakModeError or activated BreakMode
   */
  def activate(
    screenPath: Option[String],
    timestamp: Long
  ): Either[BreakModeError, BreakMode] =
    if isActive then
      Left(BreakModeError.AlreadyActive)
    else
      Right(
        copy(
          isActive = true,
          breakStartTimestamp = Some(timestamp),
          breakScreenPath = screenPath
        )
      )

  /**
   * Deactivate break mode and increment break count.
   *
   * @return Either BreakModeError or deactivated BreakMode
   */
  def deactivate(): Either[BreakModeError, BreakMode] =
    if !isActive then
      Left(BreakModeError.NotActive)
    else
      Right(
        copy(
          isActive = false,
          breakCount = breakCount + 1,
          breakStartTimestamp = None,
          breakScreenPath = None
        )
      )

  /**
   * Toggle break mode (activate if inactive, deactivate if active).
   *
   * @param screenPath Optional path to custom break screen image (used only if activating)
   * @param timestamp Timestamp for activation (used only if activating)
   * @return Either BreakModeError or toggled BreakMode
   */
  def toggle(
    screenPath: Option[String],
    timestamp: Long
  ): Either[BreakModeError, BreakMode] =
    if isActive then
      deactivate()
    else
      activate(screenPath, timestamp)
