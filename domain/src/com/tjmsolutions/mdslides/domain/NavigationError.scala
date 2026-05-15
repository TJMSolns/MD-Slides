package com.tjmsolutions.mdslides.domain

/**
 * Navigation Error
 *
 * Represents errors that can occur during navigation operations.
 */
enum NavigationError:
  /**
   * Invalid slide index (out of range).
   */
  case InvalidSlideIndex(index: Int, totalSlides: Int)
