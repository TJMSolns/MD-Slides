package com.tjmsolutions.mdslides.infrastructure.session

import com.tjmsolutions.mdslides.domain.{HistoryEvent, HistoryLog, SessionAnalysis}
import java.nio.file.{Files, Path}
import java.nio.file.attribute.FileTime
import java.time.ZoneOffset

class SessionReportSpec extends munit.FunSuite:

  private val t0 = 1_700_000_000_000L
  private val file = SessionLogFile(
    deck = "my-talk", title = Some("My Talk"), theme = Some("dark"),
    slideTitles = List("My Talk", "Agenda", "Thanks"),
    log = HistoryLog(List(
      HistoryEvent.SessionStarted(t0, 3, Map("startSlide" -> "0")),
      HistoryEvent.SlideChanged(t0 + 60_000, 0, 1, "keyboard"),
      HistoryEvent.BreakModeActivated(t0 + 90_000),
      HistoryEvent.BreakModeDeactivated(t0 + 150_000, 60_000),
      HistoryEvent.SlideChanged(t0 + 200_000, 1, 0, "goto"),
      HistoryEvent.SessionEnded(t0 + 260_000, 200_000)
    ))
  )

  private def report = SessionReport.render(file, SessionAnalysis.analyze(file.log).toOption.get, ZoneOffset.UTC)

  test("the report names the deck and gives the session length with breaks separated") {
    assert(report.contains("My Talk (my-talk)"), report)
    assert(report.contains("00:04:20"), report)          // wall time
    assert(report.contains("presenting 00:03:20"), report)
    assert(report.contains("1 break, 00:01:00"), report)
  }

  test("the report shows time per slide with slide titles, and the path taken") {
    assert(report.contains("Agenda"), report)
    assert(report.contains("1 → 2 → 1"), report)
    assert(report.contains("Slides shown: 2 of 3"), report)
    assert(report.contains("keyboard 1"), report)
    assert(report.contains("goto 1"), report)
  }

  test("a log saved mid-session says so") {
    val midway = file.copy(log = HistoryLog(file.log.events.dropRight(1)))
    val text = SessionReport.render(midway, SessionAnalysis.analyze(midway.log).toOption.get, ZoneOffset.UTC)
    assert(text.contains("saved mid-session"), text)
  }

  test("the newest saved log for the deck is found across the search folders") {
    val a = Files.createTempDirectory("logs-a")
    val b = Files.createTempDirectory("logs-b")
    def touch(dir: Path, name: String, millis: Long): Path =
      val p = dir.resolve(name); Files.writeString(p, "{}"); Files.setLastModifiedTime(p, FileTime.fromMillis(millis)); p
    touch(a, "my-talk-20261017-100000.log.json", 1_000L)
    val newest = touch(b, "my-talk-20261017-120000.log.json", 2_000L)
    touch(b, "other-deck-20261017-130000.log.json", 3_000L)
    touch(b, "my-talk-notes.txt", 4_000L)
    assertEquals(SessionLogLocator.newest("my-talk", List(a, b, a.resolve("missing"))), Some(newest))
    assertEquals(SessionLogLocator.newest("nope", List(a, b)), None)
    assertEquals(SessionLogLocator.newest("my", List(a, b)), None)  // "my" is not "my-talk"
  }
