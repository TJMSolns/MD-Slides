package com.tjmsolutions.mdslides.infrastructure.session

import com.tjmsolutions.mdslides.domain.HistoryEvent

/**
 * Parses the session log a presentation saves when the presenter presses L.
 */
class SessionLogParserSpec extends munit.FunSuite:

  private val sample = """{
    "format": "mdslides-session-log",
    "version": 1,
    "deck": "my-talk",
    "title": "My Talk",
    "theme": "dark",
    "totalSlides": 3,
    "slideTitles": ["My Talk", "Agenda", "Thanks"],
    "events": [
      {"eventType":"SessionStarted","timestamp":1000,"totalSlides":3,"metadata":{"deck":"my-talk","startSlide":"0"}},
      {"eventType":"SlideChanged","timestamp":2000,"fromSlide":0,"toSlide":1,"navigationMethod":"keyboard"},
      {"eventType":"TimerPaused","timestamp":2500,"elapsedMillis":1500},
      {"eventType":"TimerResumed","timestamp":2600},
      {"eventType":"BreakModeActivated","timestamp":3000},
      {"eventType":"BreakModeDeactivated","timestamp":4000,"breakDurationMillis":1000},
      {"eventType":"SessionEnded","timestamp":5000,"totalElapsedMillis":3000}
    ]
  }"""

  test("a saved session log is parsed into its deck details and events") {
    val parsed = SessionLogParser.parse(sample)
    assert(parsed.isRight, parsed)
    val file = parsed.toOption.get
    assertEquals(file.deck, "my-talk")
    assertEquals(file.title, Some("My Talk"))
    assertEquals(file.theme, Some("dark"))
    assertEquals(file.slideTitles, List("My Talk", "Agenda", "Thanks"))
    assertEquals(file.log.events.size, 7)
    assertEquals(file.log.events(1), HistoryEvent.SlideChanged(2000, 0, 1, "keyboard"))
    assertEquals(file.log.events(5), HistoryEvent.BreakModeDeactivated(4000, 1000))
  }

  test("unknown event types are skipped so older tools can read newer logs") {
    val json = """{"format":"mdslides-session-log","version":1,"deck":"d","totalSlides":1,
      "events":[{"eventType":"SessionStarted","timestamp":1,"totalSlides":1,"metadata":{}},{"eventType":"Wave","timestamp":2}]}"""
    assertEquals(SessionLogParser.parse(json).map(_.log.events.size), Right(1))
  }

  test("a file that is not a session log is rejected with a reason") {
    assert(SessionLogParser.parse("""{"hello":"world"}""").left.exists(_.contains("session log")))
    assert(SessionLogParser.parse("not json").isLeft)
  }

  test("a malformed event is rejected with its position") {
    val json = """{"format":"mdslides-session-log","version":1,"deck":"d","totalSlides":1,
      "events":[{"eventType":"SlideChanged","timestamp":2}]}"""
    assert(SessionLogParser.parse(json).left.exists(_.contains("event 1")))
  }
