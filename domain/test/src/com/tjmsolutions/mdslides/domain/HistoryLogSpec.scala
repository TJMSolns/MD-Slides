package com.tjmsolutions.mdslides.domain

/**
 * TDD Specification for HistoryLog Aggregate
 *
 * Feature: History Logging (v3.0.0 - Feature 3 of 10)
 * Acceptance Criteria: doc/acceptance-criteria/history-logging-acceptance-criteria.md
 * Example Mapping: doc/scenarios/example-maps/history-logging-examples.md
 */
class HistoryLogSpec extends munit.ScalaCheckSuite:

  test("SessionStarted event should capture timestamp and metadata") {
    val event = HistoryEvent.SessionStarted(
      timestamp = 1735488000000L,
      totalSlides = 20,
      metadata = Map("title" -> "Test Presentation")
    )
    event match
      case HistoryEvent.SessionStarted(ts, slides, meta) =>
        assertEquals(ts, 1735488000000L)
        assertEquals(slides, 20)
        assertEquals(meta("title"), "Test Presentation")
      case _ => fail("Expected SessionStarted")
  }

  test("SlideChanged event should capture slide indices and timestamp") {
    val event = HistoryEvent.SlideChanged(
      timestamp = 1735488060000L,
      fromSlide = 0,
      toSlide = 5,
      navigationMethod = "arrow-key"
    )
    event match
      case HistoryEvent.SlideChanged(_, from, to, method) =>
        assertEquals(from, 0)
        assertEquals(to, 5)
        assertEquals(method, "arrow-key")
      case _ => fail("Expected SlideChanged")
  }

  test("TimerStarted event should capture start timestamp") {
    val event = HistoryEvent.TimerStarted(timestamp = 1735488000000L)
    event match
      case HistoryEvent.TimerStarted(ts) =>
        assertEquals(ts, 1735488000000L)
      case _ => fail("Expected TimerStarted")
  }

  test("TimerPaused event should capture elapsed time") {
    val event = HistoryEvent.TimerPaused(
      timestamp = 1735488120000L,
      elapsedMillis = 120000L
    )
    event match
      case HistoryEvent.TimerPaused(_, elapsed) =>
        assertEquals(elapsed, 120000L)
      case _ => fail("Expected TimerPaused")
  }

  test("BreakModeActivated event should capture timestamp") {
    val event = HistoryEvent.BreakModeActivated(timestamp = 1735488180000L)
    event match
      case HistoryEvent.BreakModeActivated(ts) =>
        assertEquals(ts, 1735488180000L)
      case _ => fail("Expected BreakModeActivated")
  }

  test("BreakModeDeactivated event should capture break duration") {
    val event = HistoryEvent.BreakModeDeactivated(
      timestamp = 1735488300000L,
      breakDurationMillis = 120000L
    )
    event match
      case HistoryEvent.BreakModeDeactivated(_, duration) =>
        assertEquals(duration, 120000L)
      case _ => fail("Expected BreakModeDeactivated")
  }

  test("HistoryLog should start with empty events list") {
    val log = HistoryLog.create()
    assertEquals(log.events, List.empty[HistoryEvent])
  }

  test("appendEvent() should add event to events list") {
    var log = HistoryLog.create()
    val event = HistoryEvent.SessionStarted(
      timestamp = 1735488000000L,
      totalSlides = 20,
      metadata = Map.empty
    )
    log = log.appendEvent(event)
    assertEquals(log.events.length, 1)
    assertEquals(log.events.head, event)
  }

  test("appendEvent() should preserve chronological order") {
    var log = HistoryLog.create()
    val event1 = HistoryEvent.TimerStarted(timestamp = 1000L)
    val event2 = HistoryEvent.SlideChanged(
      timestamp = 2000L,
      fromSlide = 0,
      toSlide = 1,
      navigationMethod = "arrow-key"
    )
    val event3 = HistoryEvent.TimerPaused(timestamp = 3000L, elapsedMillis = 2000L)

    log = log.appendEvent(event1)
    log = log.appendEvent(event2)
    log = log.appendEvent(event3)

    assertEquals(log.events.length, 3)
    assertEquals(log.events(0), event1)
    assertEquals(log.events(1), event2)
    assertEquals(log.events(2), event3)
  }

  test("toJsonString() should serialize SessionStarted event") {
    val event = HistoryEvent.SessionStarted(
      timestamp = 1735488000000L,
      totalSlides = 20,
      metadata = Map("title" -> "Test")
    )
    val json = HistoryLog.eventToJson(event)
    assert(json.contains("\"eventType\":\"SessionStarted\""))
    assert(json.contains("\"timestamp\":1735488000000"))
    assert(json.contains("\"totalSlides\":20"))
  }

  test("toJsonString() should serialize SlideChanged event") {
    val event = HistoryEvent.SlideChanged(
      timestamp = 1735488060000L,
      fromSlide = 0,
      toSlide = 5,
      navigationMethod = "arrow-key"
    )
    val json = HistoryLog.eventToJson(event)
    assert(json.contains("\"eventType\":\"SlideChanged\""))
    assert(json.contains("\"fromSlide\":0"))
    assert(json.contains("\"toSlide\":5"))
    assert(json.contains("\"navigationMethod\":\"arrow-key\""))
  }

  test("toJsonString() should serialize TimerStarted event") {
    val event = HistoryEvent.TimerStarted(timestamp = 1735488000000L)
    val json = HistoryLog.eventToJson(event)
    assert(json.contains("\"eventType\":\"TimerStarted\""))
    assert(json.contains("\"timestamp\":1735488000000"))
  }

  test("toJsonString() should serialize BreakModeActivated event") {
    val event = HistoryEvent.BreakModeActivated(timestamp = 1735488180000L)
    val json = HistoryLog.eventToJson(event)
    assert(json.contains("\"eventType\":\"BreakModeActivated\""))
    assert(json.contains("\"timestamp\":1735488180000"))
  }

  test("serializeToJson() should serialize entire log") {
    var log = HistoryLog.create()
    val event1 = HistoryEvent.SessionStarted(
      timestamp = 1000L,
      totalSlides = 20,
      metadata = Map.empty
    )
    val event2 = HistoryEvent.TimerStarted(timestamp = 2000L)
    log = log.appendEvent(event1)
    log = log.appendEvent(event2)

    val json = log.serializeToJson()
    assert(json.contains("\"events\":["))
    assert(json.contains("\"SessionStarted\""))
    assert(json.contains("\"TimerStarted\""))
  }

  test("navigation method should be captured for different navigation types") {
    val arrowKey = HistoryEvent.SlideChanged(1000L, 0, 1, "arrow-key")
    val pKey = HistoryEvent.SlideChanged(2000L, 1, 0, "p-key")
    val nKey = HistoryEvent.SlideChanged(3000L, 0, 1, "n-key")
    val goto = HistoryEvent.SlideChanged(4000L, 1, 10, "goto")

    arrowKey match
      case HistoryEvent.SlideChanged(_, _, _, method) => assertEquals(method, "arrow-key")
      case _ => fail("Expected SlideChanged")

    pKey match
      case HistoryEvent.SlideChanged(_, _, _, method) => assertEquals(method, "p-key")
      case _ => fail("Expected SlideChanged")

    nKey match
      case HistoryEvent.SlideChanged(_, _, _, method) => assertEquals(method, "n-key")
      case _ => fail("Expected SlideChanged")

    goto match
      case HistoryEvent.SlideChanged(_, _, _, method) => assertEquals(method, "goto")
      case _ => fail("Expected SlideChanged")
  }
