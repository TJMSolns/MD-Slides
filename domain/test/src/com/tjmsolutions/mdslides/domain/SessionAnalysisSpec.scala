package com.tjmsolutions.mdslides.domain

import org.scalacheck.{Gen, Prop}

/**
 * Session analysis: turns a recorded presentation session (HistoryLog) into
 * per-slide time, the path taken, break time and navigation counts.
 */
class SessionAnalysisSpec extends munit.ScalaCheckSuite:

  private val t0 = 1_700_000_000_000L

  private def started(totalSlides: Int = 5, startSlide: Int = 0) =
    HistoryEvent.SessionStarted(t0, totalSlides, Map("deck" -> "my-talk", "startSlide" -> startSlide.toString))

  test("a session without a SessionStarted event cannot be analysed") {
    val log = HistoryLog(List(HistoryEvent.SlideChanged(t0, 0, 1, "keyboard")))
    assert(SessionAnalysis.analyze(log).isLeft)
  }

  test("time on each slide runs from arriving on it until leaving it") {
    val log = HistoryLog(List(
      started(),
      HistoryEvent.SlideChanged(t0 + 10_000, 0, 1, "keyboard"),
      HistoryEvent.SlideChanged(t0 + 40_000, 1, 2, "swipe"),
      HistoryEvent.SessionEnded(t0 + 100_000, 100_000)
    ))
    val analysis = SessionAnalysis.analyze(log).toOption.get
    assertEquals(analysis.slideTimes.map(st => st.slideIndex -> st.millis), List(0 -> 10_000L, 1 -> 30_000L, 2 -> 60_000L))
    assertEquals(analysis.wallMillis, 100_000L)
    assertEquals(analysis.presentingMillis, 100_000L)
    assert(analysis.ended)
  }

  test("the path records every slide visited, in order, starting from the opening slide") {
    val log = HistoryLog(List(
      started(startSlide = 2),
      HistoryEvent.SlideChanged(t0 + 1_000, 2, 3, "keyboard"),
      HistoryEvent.SlideChanged(t0 + 2_000, 3, 0, "goto"),
      HistoryEvent.SlideChanged(t0 + 3_000, 0, 3, "history")
    ))
    val analysis = SessionAnalysis.analyze(log).toOption.get
    assertEquals(analysis.path, List(2, 3, 0, 3))
    assertEquals(analysis.slideTimes.find(_.slideIndex == 3).map(_.visits), Some(2))
  }

  test("break time is excluded from slide time and reported separately") {
    val log = HistoryLog(List(
      started(),
      HistoryEvent.BreakModeActivated(t0 + 10_000),
      HistoryEvent.BreakModeDeactivated(t0 + 70_000, 60_000),
      HistoryEvent.SlideChanged(t0 + 80_000, 0, 1, "keyboard"),
      HistoryEvent.SessionEnded(t0 + 90_000, 30_000)
    ))
    val analysis = SessionAnalysis.analyze(log).toOption.get
    assertEquals(analysis.breakMillis, 60_000L)
    assertEquals(analysis.breakCount, 1)
    assertEquals(analysis.presentingMillis, 30_000L)
    assertEquals(analysis.slideTimes.map(st => st.slideIndex -> st.millis), List(0 -> 20_000L, 1 -> 10_000L))
  }

  test("a session saved without an end runs until its last event, and an open break until then too") {
    val log = HistoryLog(List(
      started(),
      HistoryEvent.SlideChanged(t0 + 5_000, 0, 1, "tap"),
      HistoryEvent.BreakModeActivated(t0 + 8_000)
    ))
    val analysis = SessionAnalysis.analyze(log).toOption.get
    assert(!analysis.ended)
    assertEquals(analysis.wallMillis, 8_000L)
    assertEquals(analysis.breakMillis, 0L)
  }

  test("navigation methods are counted") {
    val log = HistoryLog(List(
      started(),
      HistoryEvent.SlideChanged(t0 + 1, 0, 1, "keyboard"),
      HistoryEvent.SlideChanged(t0 + 2, 1, 2, "keyboard"),
      HistoryEvent.SlideChanged(t0 + 3, 2, 4, "goto")
    ))
    val analysis = SessionAnalysis.analyze(log).toOption.get
    assertEquals(analysis.navigationCounts, Map("keyboard" -> 2, "goto" -> 1))
    assertEquals(analysis.slidesViewed, 4)
    assertEquals(analysis.totalSlides, 5)
  }

  test("events arriving out of order are analysed in time order") {
    val log = HistoryLog(List(
      started(),
      HistoryEvent.SessionEnded(t0 + 30_000, 30_000),
      HistoryEvent.SlideChanged(t0 + 10_000, 0, 1, "keyboard")
    ))
    val analysis = SessionAnalysis.analyze(log).toOption.get
    assertEquals(analysis.slideTimes.map(st => st.slideIndex -> st.millis), List(0 -> 10_000L, 1 -> 20_000L))
  }

  // Property: slide time plus break time always accounts for the whole session.
  private val sessionGen: Gen[HistoryLog] =
    for
      steps  <- Gen.choose(0, 30)
      gaps   <- Gen.listOfN(steps, Gen.choose(0L, 120_000L))
      slides <- Gen.listOfN(steps, Gen.choose(0, 9))
      breaks <- Gen.listOfN(steps, Gen.oneOf(true, false))
      tail   <- Gen.choose(0L, 60_000L)
    yield
      val times = gaps.scanLeft(t0)(_ + _).tail
      val changes = times.zip(slides).zip(breaks).flatMap { case ((ts, to), isBreak) =>
        if isBreak then List(HistoryEvent.BreakModeActivated(ts), HistoryEvent.BreakModeDeactivated(ts + 1_000, 1_000))
        else List(HistoryEvent.SlideChanged(ts, 0, to, "keyboard"))
      }
      val end = times.lastOption.getOrElse(t0) + 1_000 + tail
      HistoryLog(started(totalSlides = 10) :: changes ::: List(HistoryEvent.SessionEnded(end, 0)))

  property("slide time plus break time equals the session's wall time") {
    Prop.forAll(sessionGen) { log =>
      val a = SessionAnalysis.analyze(log).toOption.get
      a.slideTimes.map(_.millis).sum + a.breakMillis == a.wallMillis
    }
  }
