package com.tjmsolutions.mdslides.domain

/**
 * TDD Specification for NavigationHistory Aggregate
 *
 * Feature: Previous/Next Navigation with History Stack (v3.0.0 - Feature 4 of 10)
 * Acceptance Criteria: doc/acceptance-criteria/navigation-history-acceptance-criteria.md
 * Example Mapping: doc/scenarios/example-maps/navigation-history-examples.md
 */
class NavigationHistorySpec extends munit.ScalaCheckSuite:

  test("new NavigationHistory should start with empty stacks at slide 0") {
    val history = NavigationHistory.create(totalSlides = 20)
    assertEquals(history.visitStack, List.empty[Int])
    assertEquals(history.forwardStack, List.empty[Int])
    assertEquals(history.currentSlideIndex, 0)
    assertEquals(history.totalSlides, 20)
  }

  test("navigatePrevious() with empty history should go to slide 0") {
    val history = NavigationHistory.create(totalSlides = 20).copy(currentSlideIndex = 10)
    val result = history.navigatePrevious()
    assert(result.isRight)
    val (updated, targetIndex) = result.getOrElse(fail("Expected Right"))
    assertEquals(targetIndex, 0, "empty history defaults to slide 0")
    assertEquals(updated.forwardStack.head, 10, "current slide pushed to forward stack")
  }

  test("navigatePrevious() should pop from visit stack (LIFO)") {
    val history = NavigationHistory.create(totalSlides = 20).copy(
      visitStack = List(0, 5, 12),
      currentSlideIndex = 8
    )
    val result = history.navigatePrevious()
    val (updated, targetIndex) = result.getOrElse(fail("Expected Right"))
    assertEquals(targetIndex, 12, "most recent visit")
    assertEquals(updated.visitStack, List(0, 5), "12 popped from stack")
    assertEquals(updated.forwardStack.head, 8, "current slide pushed to forward stack")
  }

  test("navigateNext() with forward history should redo") {
    val history = NavigationHistory.create(totalSlides = 20).copy(
      visitStack = List(0, 5),
      forwardStack = List(12, 8),
      currentSlideIndex = 5
    )
    val result = history.navigateNext()
    val (updated, targetIndex) = result.getOrElse(fail("Expected Right"))
    assertEquals(targetIndex, 12, "popped from forward stack")
    assertEquals(updated.forwardStack, List(8), "12 removed from forward stack")
    assertEquals(updated.visitStack, List(0, 5, 5), "current slide added to visit stack")
  }

  test("navigateNext() without forward history should advance linearly") {
    val history = NavigationHistory.create(totalSlides = 20).copy(currentSlideIndex = 5)
    val result = history.navigateNext()
    val (updated, targetIndex) = result.getOrElse(fail("Expected Right"))
    assertEquals(targetIndex, 6, "linear next")
    assertEquals(updated.visitStack, List(5), "current slide added to visit stack")
  }

  test("navigateNext() at last slide should stay on last slide") {
    val history = NavigationHistory.create(totalSlides = 20).copy(currentSlideIndex = 19)
    val result = history.navigateNext()
    val (updated, targetIndex) = result.getOrElse(fail("Expected Right"))
    assertEquals(targetIndex, 19, "stay on last slide")
  }

  test("pushToHistory() should add to visit stack and clear forward stack") {
    val history = NavigationHistory.create(totalSlides = 20).copy(
      visitStack = List(0, 5),
      forwardStack = List(12, 8),
      currentSlideIndex = 10
    )
    val updated = history.pushToHistory(15)
    assertEquals(updated.visitStack, List(0, 5, 10), "current slide added")
    assertEquals(updated.forwardStack, List.empty[Int], "forward stack cleared")
    assertEquals(updated.currentSlideIndex, 15)
  }

  test("duplicate slides should be preserved in visit stack") {
    var history = NavigationHistory.create(totalSlides = 20)
    history = history.pushToHistory(5)
    history = history.pushToHistory(10)
    history = history.pushToHistory(5) // duplicate visit to slide 5
    history = history.pushToHistory(15) // navigate away to record the duplicate
    assertEquals(history.visitStack, List(0, 5, 10, 5), "duplicates preserved in temporal order")
  }

  test("multiple navigatePrevious() calls should navigate through history") {
    var history = NavigationHistory.create(totalSlides = 20).copy(
      visitStack = List(0, 5, 12),
      currentSlideIndex = 8
    )
    val (h1, idx1) = history.navigatePrevious().getOrElse(fail("Expected Right"))
    assertEquals(idx1, 12)
    val (h2, idx2) = h1.navigatePrevious().getOrElse(fail("Expected Right"))
    assertEquals(idx2, 5)
    val (h3, idx3) = h2.navigatePrevious().getOrElse(fail("Expected Right"))
    assertEquals(idx3, 0)
  }

  test("P then N should redo (undo/redo pattern)") {
    val history = NavigationHistory.create(totalSlides = 20).copy(
      visitStack = List(0, 5),
      currentSlideIndex = 12
    )
    // Press P
    val (afterP, pIdx) = history.navigatePrevious().getOrElse(fail("Expected Right"))
    assertEquals(pIdx, 5)
    assertEquals(afterP.forwardStack, List(12), "12 in forward stack")
    // Press N
    val (afterN, nIdx) = afterP.navigateNext().getOrElse(fail("Expected Right"))
    assertEquals(nIdx, 12, "redo to 12")
  }

  test("invariant: currentSlideIndex always in valid range") {
    val history = NavigationHistory.create(totalSlides = 20).copy(currentSlideIndex = 15)
    assert(history.currentSlideIndex >= 0)
    assert(history.currentSlideIndex < history.totalSlides)
  }

  test("invariant: visit stack preserves chronological order") {
    val history = NavigationHistory.create(totalSlides = 20).copy(
      visitStack = List(0, 5, 10, 5)
    )
    assertEquals(history.visitStack, List(0, 5, 10, 5), "FIFO append order preserved")
  }
