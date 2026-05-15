package com.tjmsolutions.mdslides.domain

/**
 * Navigation History Aggregate
 *
 * Manages history-based navigation (P/N keys) using visit stack and forward stack.
 *
 * Invariants:
 * - currentSlideIndex always in range [0, totalSlides - 1]
 * - visitStack preserves chronological order (FIFO append, LIFO pop)
 * - Forward stack cleared on non-P/N navigation
 * - Duplicate slides preserved in visit stack (temporal sequence)
 */
case class NavigationHistory(
  visitStack: List[Int],        // Previously viewed slides (LIFO for P key)
  forwardStack: List[Int],      // Forward navigation stack (redo capability)
  currentSlideIndex: Int,       // Currently displayed slide
  totalSlides: Int              // Total slides in deck (for validation)
):

  /**
   * Navigate to previous slide in history (P key).
   * Pops from visit stack (LIFO), pushes current to forward stack.
   * If visit stack empty, defaults to slide 0.
   */
  def navigatePrevious(): Either[NavigationError, (NavigationHistory, Int)] =
    visitStack.lastOption match
      case None =>
        // No history, default to slide 0
        Right((
          copy(forwardStack = currentSlideIndex :: forwardStack),
          0
        ))
      case Some(lastVisit) =>
        // Pop from end of visit stack (LIFO), push current to forward stack
        Right((
          copy(
            visitStack = visitStack.init,  // Remove last element
            forwardStack = currentSlideIndex :: forwardStack,
            currentSlideIndex = lastVisit
          ),
          lastVisit
        ))

  /**
   * Navigate to next slide (N key).
   * If forward stack exists: redo (pop from forward stack).
   * If forward stack empty: advance linearly.
   */
  def navigateNext(): Either[NavigationError, (NavigationHistory, Int)] =
    forwardStack match
      case Nil =>
        // No forward history, advance linearly
        val nextIndex = Math.min(currentSlideIndex + 1, totalSlides - 1)
        Right((
          copy(
            visitStack = if nextIndex > currentSlideIndex then visitStack :+ currentSlideIndex else visitStack,
            currentSlideIndex = nextIndex
          ),
          nextIndex
        ))
      case head :: tail =>
        // Pop from forward stack, append current to visit stack
        Right((
          copy(
            visitStack = visitStack :+ currentSlideIndex,  // Append to end (FIFO)
            forwardStack = tail,
            currentSlideIndex = head
          ),
          head
        ))

  /**
   * Push current slide to history and navigate to new slide.
   * Clears forward stack (new decision path).
   */
  def pushToHistory(newSlideIndex: Int): NavigationHistory =
    copy(
      visitStack = visitStack :+ currentSlideIndex,  // Append to end (FIFO)
      forwardStack = List.empty[Int],  // Forward history invalidated
      currentSlideIndex = newSlideIndex
    )

object NavigationHistory:
  def create(totalSlides: Int): NavigationHistory =
    NavigationHistory(
      visitStack = List.empty[Int],
      forwardStack = List.empty[Int],
      currentSlideIndex = 0,
      totalSlides = totalSlides
    )
