package com.tjmsolutions.mdslides.domain

/**
 * Break Mode Error
 *
 * Represents errors that can occur when interacting with BreakMode aggregate.
 */
enum BreakModeError:
  /**
   * Attempted to activate break mode when it is already active.
   */
  case AlreadyActive

  /**
   * Attempted to deactivate break mode when it is not active.
   */
  case NotActive
