package com.tjmsolutions.mdslides.domain

/**
 * TDD Specification for BreakMode Aggregate
 *
 * Feature: Break Mode (v3.0.0 - Feature 2 of 10)
 * Acceptance Criteria: doc/acceptance-criteria/break-mode-acceptance-criteria.md
 * Example Mapping: doc/scenarios/example-maps/break-mode-examples.md
 */
class BreakModeSpec extends munit.ScalaCheckSuite:

  test("new BreakMode should start inactive with zero break count") {
    val breakMode = BreakMode()
    assertEquals(breakMode.isActive, false)
    assertEquals(breakMode.breakCount, 0)
    assertEquals(breakMode.breakStartTimestamp, None)
    assertEquals(breakMode.breakScreenPath, None)
  }

  test("activate() should activate break mode with timestamp") {
    val breakMode = BreakMode()
    val result = breakMode.activate(None, 1234567890L)
    assert(result.isRight)
    val activated = result.getOrElse(fail("Expected Right"))
    assertEquals(activated.isActive, true)
    assertEquals(activated.breakStartTimestamp, Some(1234567890L))
  }

  test("activate() should accept custom break screen path") {
    val breakMode = BreakMode()
    val result = breakMode.activate(Some("images/break.png"), 1234567890L)
    val activated = result.getOrElse(fail("Expected Right"))
    assertEquals(activated.breakScreenPath, Some("images/break.png"))
  }

  test("activate() should fail if already active") {
    val breakMode = BreakMode(isActive = true, breakCount = 1, breakStartTimestamp = Some(1000L))
    val result = breakMode.activate(None, 2000L)
    assert(result.isLeft)
    assertEquals(result.swap.getOrElse(fail("Expected Left")), BreakModeError.AlreadyActive)
  }

  test("activate() should preserve break count") {
    val breakMode = BreakMode(breakCount = 2)
    val result = breakMode.activate(None, 1234567890L)
    val activated = result.getOrElse(fail("Expected Right"))
    assertEquals(activated.breakCount, 2)
  }

  test("deactivate() should deactivate break mode and increment count") {
    val breakMode = BreakMode(isActive = true, breakCount = 0, breakStartTimestamp = Some(1000L))
    val result = breakMode.deactivate()
    assert(result.isRight)
    val deactivated = result.getOrElse(fail("Expected Right"))
    assertEquals(deactivated.isActive, false)
    assertEquals(deactivated.breakCount, 1)
    assertEquals(deactivated.breakStartTimestamp, None)
    assertEquals(deactivated.breakScreenPath, None)
  }

  test("deactivate() should fail if not active") {
    val breakMode = BreakMode()
    val result = breakMode.deactivate()
    assert(result.isLeft)
    assertEquals(result.swap.getOrElse(fail("Expected Left")), BreakModeError.NotActive)
  }

  test("deactivate() should preserve and increment break count") {
    val breakMode = BreakMode(isActive = true, breakCount = 3, breakStartTimestamp = Some(1000L))
    val result = breakMode.deactivate()
    val deactivated = result.getOrElse(fail("Expected Right"))
    assertEquals(deactivated.breakCount, 4)
  }

  test("deactivate() should clear break screen path") {
    val breakMode = BreakMode(isActive = true, breakCount = 0, breakStartTimestamp = Some(1000L), breakScreenPath = Some("images/break.png"))
    val result = breakMode.deactivate()
    val deactivated = result.getOrElse(fail("Expected Right"))
    assertEquals(deactivated.breakScreenPath, None)
  }

  test("toggle() should activate when inactive") {
    val breakMode = BreakMode()
    val result = breakMode.toggle(None, 1234567890L)
    assert(result.isRight)
    val toggled = result.getOrElse(fail("Expected Right"))
    assertEquals(toggled.isActive, true)
  }

  test("toggle() should deactivate when active") {
    val breakMode = BreakMode(isActive = true, breakCount = 1, breakStartTimestamp = Some(1000L))
    val result = breakMode.toggle(None, 2000L)
    assert(result.isRight)
    val toggled = result.getOrElse(fail("Expected Right"))
    assertEquals(toggled.isActive, false)
    assertEquals(toggled.breakCount, 2)
  }

  test("invariant: active implies timestamp exists") {
    val breakMode = BreakMode(isActive = true, breakCount = 0, breakStartTimestamp = Some(1000L))
    assertEquals(breakMode.isActive, true)
    assert(breakMode.breakStartTimestamp.isDefined)
  }

  test("invariant: inactive implies no timestamp") {
    val breakMode = BreakMode()
    assertEquals(breakMode.isActive, false)
    assertEquals(breakMode.breakStartTimestamp, None)
  }

  test("invariant: break count is non-negative") {
    val breakMode = BreakMode(breakCount = 5)
    assert(breakMode.breakCount >= 0)
  }

  test("invariant: break count increments only on deactivation") {
    val initial = BreakMode()
    val activated = initial.activate(None, 1000L).getOrElse(fail("Expected Right"))
    assertEquals(activated.breakCount, 0)
    val deactivated = activated.deactivate().getOrElse(fail("Expected Right"))
    assertEquals(deactivated.breakCount, 1)
  }

  test("multiple break sessions should track cumulative count") {
    var breakMode = BreakMode()
    breakMode = breakMode.activate(None, 1000L).getOrElse(fail("Expected Right"))
    breakMode = breakMode.deactivate().getOrElse(fail("Expected Right"))
    assertEquals(breakMode.breakCount, 1)
    breakMode = breakMode.activate(None, 2000L).getOrElse(fail("Expected Right"))
    breakMode = breakMode.deactivate().getOrElse(fail("Expected Right"))
    assertEquals(breakMode.breakCount, 2)
    breakMode = breakMode.activate(None, 3000L).getOrElse(fail("Expected Right"))
    breakMode = breakMode.deactivate().getOrElse(fail("Expected Right"))
    assertEquals(breakMode.breakCount, 3)
  }

  test("different break screens can be used for different sessions") {
    var breakMode = BreakMode()
    breakMode = breakMode.activate(Some("screen-a.png"), 1000L).getOrElse(fail("Expected Right"))
    assertEquals(breakMode.breakScreenPath, Some("screen-a.png"))
    breakMode = breakMode.deactivate().getOrElse(fail("Expected Right"))
    breakMode = breakMode.activate(Some("screen-b.png"), 2000L).getOrElse(fail("Expected Right"))
    assertEquals(breakMode.breakScreenPath, Some("screen-b.png"))
  }

  test("rapid toggle (activate → deactivate → activate)") {
    var breakMode = BreakMode()
    breakMode = breakMode.toggle(None, 1000L).getOrElse(fail("Expected Right"))
    assertEquals(breakMode.isActive, true)
    breakMode = breakMode.toggle(None, 2000L).getOrElse(fail("Expected Right"))
    assertEquals(breakMode.isActive, false)
    assertEquals(breakMode.breakCount, 1)
    breakMode = breakMode.toggle(None, 3000L).getOrElse(fail("Expected Right"))
    assertEquals(breakMode.isActive, true)
    assertEquals(breakMode.breakCount, 1)
  }
