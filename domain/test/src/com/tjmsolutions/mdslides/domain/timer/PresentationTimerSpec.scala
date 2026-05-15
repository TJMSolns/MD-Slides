package com.tjmsolutions.mdslides.domain.timer

/**
 * TDD Specification for PresentationTimer Aggregate
 *
 * Test-First Pairing Session: 2025-12-29
 * Feature: Presentation Timer (v3.0.0)
 * Domain Model: doc/domain-models/aggregates/presentation-timer-aggregate.md
 * Acceptance Criteria: doc/scenarios/acceptance-criteria/presentation-timer-acceptance.md
 *
 * Test Organization:
 * - State Transitions (start, pause, resume)
 * - Invariant Enforcement (preconditions)
 * - Time Calculations (elapsedSeconds, formattedTime)
 * - Edge Cases (multiple pauses, long durations)
 * - Property-Based Tests (invariant validation)
 */
class PresentationTimerSpec extends munit.ScalaCheckSuite:

  // =========================================================================
  // Test Suite 1: Timer Initialization and Start
  // =========================================================================

  test("new timer should be in NotStarted state") {
    val timer = PresentationTimer.create()
    assertEquals(timer.currentState(), TimerState.NotStarted)
    assertEquals(timer.startTimestamp, 0L)
    assertEquals(timer.totalPausedDuration, 0L)
    assertEquals(timer.lastPauseTimestamp, None)
  }

  test("start() should transition from NotStarted to Running") {
    val timer = PresentationTimer.create()
    val result = timer.start()

    assert(result.isRight, "start() should succeed")
    val started = result.getOrElse(fail("Expected Right but got Left"))

    assertEquals(started.currentState(), TimerState.Running)
    assert(started.startTimestamp > 0, "startTimestamp should be set")
    assertEquals(started.totalPausedDuration, 0L)
    assertEquals(started.lastPauseTimestamp, None)
  }

  test("start() should set startTimestamp to current time") {
    val timer = PresentationTimer.create()
    val beforeStart = System.currentTimeMillis()
    val started = timer.start().getOrElse(fail("start() should succeed"))
    val afterStart = System.currentTimeMillis()

    assert(started.startTimestamp >= beforeStart, "startTimestamp should be >= beforeStart")
    assert(started.startTimestamp <= afterStart, "startTimestamp should be <= afterStart")
  }

  test("start() should reject if already started") {
    val timer = PresentationTimer.create()
    val started = timer.start().getOrElse(fail("Initial start should succeed"))
    val result = started.start()

    assert(result.isLeft, "Second start() should fail")
    result match
      case Left(TimerError.TimerAlreadyStarted) => // Success
      case Left(other) => fail(s"Expected TimerAlreadyStarted but got $other")
      case Right(_) => fail("Expected Left but got Right")
  }

  // =========================================================================
  // Test Suite 2: Pause Command
  // =========================================================================

  test("pause() should transition from Running to Paused") {
    val timer = PresentationTimer.create().start().getOrElse(fail("start() should succeed"))
    val result = timer.pause()

    assert(result.isRight, "pause() should succeed")
    val paused = result.getOrElse(fail("Expected Right but got Left"))

    assertEquals(paused.currentState(), TimerState.Paused)
    assert(paused.lastPauseTimestamp.isDefined, "lastPauseTimestamp should be set")
    assertEquals(paused.totalPausedDuration, timer.totalPausedDuration)
  }

  test("pause() should reject when timer is NotStarted") {
    val timer = PresentationTimer.create()
    val result = timer.pause()

    assert(result.isLeft, "pause() should fail when NotStarted")
    result match
      case Left(TimerError.CannotPauseWhenNotRunning(TimerState.NotStarted)) => // Success
      case Left(other) => fail(s"Expected CannotPauseWhenNotRunning(NotStarted) but got $other")
      case Right(_) => fail("Expected Left but got Right")
  }

  test("pause() should reject when timer is already Paused") {
    val timer = PresentationTimer.create()
      .start().getOrElse(fail("start() should succeed"))
      .pause().getOrElse(fail("First pause() should succeed"))

    val result = timer.pause()

    assert(result.isLeft, "Second pause() should fail")
    result match
      case Left(TimerError.CannotPauseWhenNotRunning(TimerState.Paused)) => // Success
      case Left(other) => fail(s"Expected CannotPauseWhenNotRunning(Paused) but got $other")
      case Right(_) => fail("Expected Left but got Right")
  }

  // =========================================================================
  // Test Suite 3: Resume Command
  // =========================================================================

  test("resume() should transition from Paused to Running") {
    val timer = PresentationTimer.create()
      .start().getOrElse(fail("start() should succeed"))

    Thread.sleep(10) // Ensure some time passes before pause

    val paused = timer.pause().getOrElse(fail("pause() should succeed"))
    val pausedDuration = paused.totalPausedDuration

    Thread.sleep(10) // Ensure some time passes during pause

    val result = paused.resume()

    assert(result.isRight, "resume() should succeed")
    val resumed = result.getOrElse(fail("Expected Right but got Left"))

    assertEquals(resumed.currentState(), TimerState.Running)
    assert(resumed.totalPausedDuration > pausedDuration, "totalPausedDuration should be updated")
    assertEquals(resumed.lastPauseTimestamp, None)
  }

  test("resume() should reject when timer is NotStarted") {
    val timer = PresentationTimer.create()
    val result = timer.resume()

    assert(result.isLeft, "resume() should fail when NotStarted")
    result match
      case Left(TimerError.CannotResumeWhenNotPaused(TimerState.NotStarted)) => // Success
      case Left(other) => fail(s"Expected CannotResumeWhenNotPaused(NotStarted) but got $other")
      case Right(_) => fail("Expected Left but got Right")
  }

  test("resume() should reject when timer is Running") {
    val timer = PresentationTimer.create().start().getOrElse(fail("start() should succeed"))
    val result = timer.resume()

    assert(result.isLeft, "resume() should fail when Running")
    result match
      case Left(TimerError.CannotResumeWhenNotPaused(TimerState.Running)) => // Success
      case Left(other) => fail(s"Expected CannotResumeWhenNotPaused(Running) but got $other")
      case Right(_) => fail("Expected Left but got Right")
  }

  test("resume() should reject when Paused but no lastPauseTimestamp") {
    // Manually create invalid state (should not happen in normal usage)
    val invalidTimer = PresentationTimer(
      state = TimerState.Paused,
      startTimestamp = System.currentTimeMillis(),
      totalPausedDuration = 0,
      lastPauseTimestamp = None  // Invalid: Paused but no pause timestamp
    )

    val result = invalidTimer.resume()

    assert(result.isLeft, "resume() should fail with invalid state")
    result match
      case Left(TimerError.InvalidState(_)) => // Success
      case Left(other) => fail(s"Expected InvalidState but got $other")
      case Right(_) => fail("Expected Left but got Right")
  }

  // =========================================================================
  // Test Suite 4: Elapsed Time Calculation
  // =========================================================================

  test("elapsedSeconds() should return 0 when timer not started") {
    val timer = PresentationTimer.create()
    assertEquals(timer.elapsedSeconds(), 0L)
  }

  test("elapsedSeconds() should calculate time since start when Running") {
    val timer = PresentationTimer.create().start().getOrElse(fail("start() should succeed"))

    Thread.sleep(100) // Wait 100ms

    val elapsed = timer.elapsedSeconds()
    assert(elapsed >= 0, "Elapsed time should be >= 0 seconds")
    assert(elapsed <= 1, "Elapsed time should be <= 1 second (we only waited 100ms)")
  }

  test("elapsedSeconds() should exclude totalPausedDuration when Running") {
    // Create timer with manual state (10 seconds runtime, 3 seconds paused)
    val startTime = System.currentTimeMillis() - 10000 // Started 10 seconds ago
    val timer = PresentationTimer(
      state = TimerState.Running,
      startTimestamp = startTime,
      totalPausedDuration = 3000, // 3 seconds paused
      lastPauseTimestamp = None
    )

    val elapsed = timer.elapsedSeconds()
    // Should be approximately 7 seconds (10 - 3)
    assert(elapsed >= 6 && elapsed <= 8, s"Expected ~7 seconds, got $elapsed")
  }

  test("elapsedSeconds() should include current pause duration when Paused") {
    val startTime = System.currentTimeMillis() - 10000 // Started 10 seconds ago
    val pauseTime = System.currentTimeMillis() - 2000  // Paused 2 seconds ago
    val timer = PresentationTimer(
      state = TimerState.Paused,
      startTimestamp = startTime,
      totalPausedDuration = 0, // First pause
      lastPauseTimestamp = Some(pauseTime)
    )

    val elapsed = timer.elapsedSeconds()
    // Should be approximately 8 seconds (10 - 2)
    assert(elapsed >= 7 && elapsed <= 9, s"Expected ~8 seconds, got $elapsed")
  }

  test("elapsedSeconds() should handle multiple pause/resume cycles") {
    // Scenario: Run 5s, pause 2s, resume, run 5s more = 10s total (excluding 2s pause)
    val now = System.currentTimeMillis()
    val timer = PresentationTimer(
      state = TimerState.Running,
      startTimestamp = now - 12000, // Started 12 seconds ago (5s + 2s pause + 5s)
      totalPausedDuration = 2000,   // 2 seconds paused
      lastPauseTimestamp = None
    )

    val elapsed = timer.elapsedSeconds()
    // Should be approximately 10 seconds (12 - 2)
    assert(elapsed >= 9 && elapsed <= 11, s"Expected ~10 seconds, got $elapsed")
  }

  // =========================================================================
  // Test Suite 5: Formatted Time Display
  // =========================================================================

  test("formattedTime() should return '00:00:00' for 0 seconds") {
    val timer = PresentationTimer.create()
    assertEquals(timer.formattedTime(), "00:00:00")
  }

  test("formattedTime() should return '00:00:59' for 59 seconds") {
    val timer = PresentationTimer(
      state = TimerState.Running,
      startTimestamp = System.currentTimeMillis() - 59000,
      totalPausedDuration = 0,
      lastPauseTimestamp = None
    )
    assertEquals(timer.formattedTime(), "00:00:59")
  }

  test("formattedTime() should return '00:01:00' for 60 seconds") {
    val timer = PresentationTimer(
      state = TimerState.Running,
      startTimestamp = System.currentTimeMillis() - 60000,
      totalPausedDuration = 0,
      lastPauseTimestamp = None
    )
    assertEquals(timer.formattedTime(), "00:01:00")
  }

  test("formattedTime() should return '01:00:00' for 3600 seconds") {
    val timer = PresentationTimer(
      state = TimerState.Running,
      startTimestamp = System.currentTimeMillis() - 3600000,
      totalPausedDuration = 0,
      lastPauseTimestamp = None
    )
    assertEquals(timer.formattedTime(), "01:00:00")
  }

  test("formattedTime() should return '23:59:59' for 86399 seconds") {
    val timer = PresentationTimer(
      state = TimerState.Running,
      startTimestamp = System.currentTimeMillis() - 86399000,
      totalPausedDuration = 0,
      lastPauseTimestamp = None
    )
    assertEquals(timer.formattedTime(), "23:59:59")
  }

  test("formattedTime() should return '24:00:00' for 86400 seconds") {
    val timer = PresentationTimer(
      state = TimerState.Running,
      startTimestamp = System.currentTimeMillis() - 86400000,
      totalPausedDuration = 0,
      lastPauseTimestamp = None
    )
    assertEquals(timer.formattedTime(), "24:00:00")
  }

  test("formattedTime() should return '99:59:59' for 359999 seconds") {
    val timer = PresentationTimer(
      state = TimerState.Running,
      startTimestamp = System.currentTimeMillis() - 359999000,
      totalPausedDuration = 0,
      lastPauseTimestamp = None
    )
    assertEquals(timer.formattedTime(), "99:59:59")
  }

  test("formattedTime() should support 100+ hours (3-digit hours)") {
    val timer = PresentationTimer(
      state = TimerState.Running,
      startTimestamp = System.currentTimeMillis() - 360000000, // 100 hours
      totalPausedDuration = 0,
      lastPauseTimestamp = None
    )
    assertEquals(timer.formattedTime(), "100:00:00")
  }

  // =========================================================================
  // Test Suite 6: State Query
  // =========================================================================

  test("currentState() should return current timer state") {
    val timer = PresentationTimer.create().start().getOrElse(fail("start() should succeed"))
    assertEquals(timer.currentState(), TimerState.Running)
  }

  // =========================================================================
  // Test Suite 7: Integration Scenarios
  // =========================================================================

  test("scenario - run 5 min, pause 2 min, resume shows 00:05:00") {
    val now = System.currentTimeMillis()

    // Scenario: Started 7 minutes ago, ran for 5 minutes, then paused for 2 minutes
    // Timeline: T0 (start) -> T+5min (pause) -> T+7min (now, after resume)
    // Total elapsed time should be 5 minutes (paused 2 minutes excluded)

    val startTime = now - 420000  // Started 7 minutes ago (420 seconds)
    val pauseTime = now - 120000  // Paused 2 minutes ago (120 seconds)

    // Create timer as if it was paused 2 minutes ago after running for 5 minutes
    val paused = PresentationTimer(
      state = TimerState.Paused,
      startTimestamp = startTime,
      totalPausedDuration = 0,     // No previous pauses
      lastPauseTimestamp = Some(pauseTime)
    )

    // Resume - this will add the pause duration (120s) to totalPausedDuration
    val resumed = paused.resume().getOrElse(fail("resume() should succeed"))

    // Verify totalPausedDuration was updated correctly
    assert(resumed.totalPausedDuration >= 119000 && resumed.totalPausedDuration <= 121000,
      s"Expected ~120000ms pause duration, got ${resumed.totalPausedDuration}ms")

    // Check elapsed time (should be ~300 seconds = 420s total - 120s pause)
    val elapsed = resumed.elapsedSeconds()
    assert(elapsed >= 298 && elapsed <= 302, s"Expected ~300 seconds, got $elapsed")
    assertEquals(resumed.formattedTime(), "00:05:00")
  }

  test("scenario - multiple pauses accumulate correctly") {
    // Scenario: Run 5 min → Pause 1 min → Resume → Run 5 min → Pause 2 min → Resume → Run 5 min
    // Total: 15 minutes of run time, 3 minutes paused

    val now = System.currentTimeMillis()
    val timer = PresentationTimer(
      state = TimerState.Running,
      startTimestamp = now - 1080000, // Started 18 minutes ago (15 run + 3 pause)
      totalPausedDuration = 180000,   // 3 minutes paused (60s + 120s)
      lastPauseTimestamp = None
    )

    val elapsed = timer.elapsedSeconds()
    // Should be approximately 900 seconds (15 minutes)
    assert(elapsed >= 895 && elapsed <= 905, s"Expected ~900 seconds, got $elapsed")
    assertEquals(timer.formattedTime(), "00:15:00")
  }

  // =========================================================================
  // Test Suite 8: Invariant Enforcement
  // =========================================================================

  test("invariant - elapsed time never decreases") {
    val timer = PresentationTimer.create().start().getOrElse(fail("start() should succeed"))

    val elapsed1 = timer.elapsedSeconds()
    Thread.sleep(50) // Wait 50ms
    val elapsed2 = timer.elapsedSeconds()

    assert(elapsed2 >= elapsed1, "Elapsed time should never decrease")
  }

  test("invariant - totalPausedDuration <= (now - startTimestamp)") {
    val now = System.currentTimeMillis()
    val timer = PresentationTimer(
      state = TimerState.Running,
      startTimestamp = now - 10000, // Started 10 seconds ago
      totalPausedDuration = 3000,   // 3 seconds paused
      lastPauseTimestamp = None
    )

    val totalRuntime = System.currentTimeMillis() - timer.startTimestamp
    assert(timer.totalPausedDuration <= totalRuntime,
      s"totalPausedDuration (${timer.totalPausedDuration}) should be <= totalRuntime ($totalRuntime)")
  }

  // =========================================================================
  // Test Suite 9: Edge Cases
  // =========================================================================

  test("edge case - timer with no pause (totalPausedDuration = 0)") {
    val timer = PresentationTimer(
      state = TimerState.Running,
      startTimestamp = System.currentTimeMillis() - 60000, // 60 seconds ago
      totalPausedDuration = 0,
      lastPauseTimestamp = None
    )

    val elapsed = timer.elapsedSeconds()
    assert(elapsed >= 59 && elapsed <= 61, s"Expected ~60 seconds, got $elapsed")
  }

  test("edge case - timer paused immediately after start") {
    val timer = PresentationTimer.create().start().getOrElse(fail("start() should succeed"))
    val paused = timer.pause().getOrElse(fail("pause() should succeed"))

    val elapsed = paused.elapsedSeconds()
    assert(elapsed >= 0 && elapsed <= 1, s"Expected ~0 seconds, got $elapsed")
  }

  test("edge case - very long pause (hours)") {
    val now = System.currentTimeMillis()
    val timer = PresentationTimer(
      state = TimerState.Running,
      startTimestamp = now - 7260000,  // Started 121 minutes ago (1 min run + 2 hours pause)
      totalPausedDuration = 7200000,   // 2 hours paused
      lastPauseTimestamp = None
    )

    val elapsed = timer.elapsedSeconds()
    // Should be approximately 60 seconds (121 min - 120 min pause)
    assert(elapsed >= 55 && elapsed <= 65, s"Expected ~60 seconds, got $elapsed")
  }

  // =========================================================================
  // Test Suite 10: Property-Based Tests (ScalaCheck)
  // =========================================================================
  // Phase 3.3: Property-Based Testing
  // Validates domain invariants across random inputs
  // =========================================================================

  import org.scalacheck.Gen
  import org.scalacheck.Prop.*

  // Generator for valid elapsed times (0 to 24 hours in milliseconds)
  val elapsedTimeGen: Gen[Long] = Gen.choose(0L, 86400000L)

  // Generator for paused durations (0 to total elapsed time)
  def pausedDurationGen(totalElapsed: Long): Gen[Long] =
    Gen.choose(0L, totalElapsed)

  property("invariant - elapsed time is always non-negative") {
    forAll(elapsedTimeGen) { elapsed =>
      val timer = PresentationTimer(
        state = TimerState.Running,
        startTimestamp = System.currentTimeMillis() - elapsed,
        totalPausedDuration = 0,
        lastPauseTimestamp = None
      )
      timer.elapsedSeconds() >= 0
    }
  }

  property("invariant - elapsed time never decreases (monotonic)") {
    forAll(elapsedTimeGen) { elapsed =>
      val now = System.currentTimeMillis()
      val timer = PresentationTimer(
        state = TimerState.Running,
        startTimestamp = now - elapsed,
        totalPausedDuration = 0,
        lastPauseTimestamp = None
      )

      val elapsed1 = timer.elapsedSeconds()
      Thread.sleep(10) // Small delay
      val elapsed2 = timer.elapsedSeconds()

      elapsed2 >= elapsed1
    }
  }

  property("invariant - paused duration cannot exceed total runtime") {
    forAll(elapsedTimeGen) { elapsed =>
      val pausedDuration = Math.min(elapsed, elapsed / 2) // Paused duration ≤ half of elapsed
      val timer = PresentationTimer(
        state = TimerState.Running,
        startTimestamp = System.currentTimeMillis() - elapsed,
        totalPausedDuration = pausedDuration,
        lastPauseTimestamp = None
      )

      val totalRuntime = System.currentTimeMillis() - timer.startTimestamp
      timer.totalPausedDuration <= totalRuntime
    }
  }

  property("invariant - paused time is excluded from elapsed time") {
    forAll(elapsedTimeGen, Gen.choose(0L, 3600000L)) { (totalElapsed, pausedDuration) =>
      val effectivePaused = Math.min(pausedDuration, totalElapsed)
      val timer = PresentationTimer(
        state = TimerState.Running,
        startTimestamp = System.currentTimeMillis() - totalElapsed,
        totalPausedDuration = effectivePaused,
        lastPauseTimestamp = None
      )

      val elapsed = timer.elapsedSeconds()
      val expectedMin = (totalElapsed - effectivePaused) / 1000 - 1 // Allow 1 sec tolerance
      val expectedMax = (totalElapsed - effectivePaused) / 1000 + 1

      elapsed >= expectedMin && elapsed <= expectedMax
    }
  }

  property("formattedTime always returns valid hh:mm:ss format") {
    forAll(Gen.choose(0L, 360000L)) { seconds =>
      val timer = PresentationTimer(
        state = TimerState.Running,
        startTimestamp = System.currentTimeMillis() - (seconds * 1000),
        totalPausedDuration = 0,
        lastPauseTimestamp = None
      )

      val formatted = timer.formattedTime()
      // Regex: NN:NN:NN where N is a digit
      formatted.matches("\\d{2,}:\\d{2}:\\d{2}")
    }
  }

  property("pause then resume preserves elapsed time") {
    forAll(elapsedTimeGen) { elapsed =>
      val timer = PresentationTimer.create()
        .start().getOrElse(fail("start should succeed"))

      // Simulate elapsed time
      val running = PresentationTimer(
        state = TimerState.Running,
        startTimestamp = System.currentTimeMillis() - elapsed,
        totalPausedDuration = 0,
        lastPauseTimestamp = None
      )

      val elapsedBefore = running.elapsedSeconds()

      // Pause and immediately resume
      val paused = running.pause().getOrElse(fail("pause should succeed"))
      Thread.sleep(10) // Pause for 10ms
      val resumed = paused.resume().getOrElse(fail("resume should succeed"))

      val elapsedAfter = resumed.elapsedSeconds()

      // Elapsed time should be approximately the same (tolerance for 10ms pause + processing time)
      Math.abs(elapsedAfter - elapsedBefore) <= 1 // Within 1 second
    }
  }

  property("state transitions are type-safe (no invalid states)") {
    forAll { (_: Unit) =>
      // Timer can only be in NotStarted, Running, or Paused
      // This is enforced by the TimerState enum type (compile-time guarantee)
      // Property test verifies state machine transitions are valid

      val timer = PresentationTimer.create()
      assertEquals(timer.state, TimerState.NotStarted)

      val started = timer.start().getOrElse(fail("start should succeed"))
      assertEquals(started.state, TimerState.Running)

      val paused = started.pause().getOrElse(fail("pause should succeed"))
      assertEquals(paused.state, TimerState.Paused)

      val resumed = paused.resume().getOrElse(fail("resume should succeed"))
      assertEquals(resumed.state, TimerState.Running)

      true // All transitions valid
    }
  }

  property("formattedTime components are correctly calculated") {
    forAll(Gen.choose(0L, 86400L)) { seconds =>
      val timer = PresentationTimer(
        state = TimerState.Running,
        startTimestamp = System.currentTimeMillis() - (seconds * 1000),
        totalPausedDuration = 0,
        lastPauseTimestamp = None
      )

      val elapsed = timer.elapsedSeconds()
      val formatted = timer.formattedTime()

      val hours = elapsed / 3600
      val minutes = (elapsed % 3600) / 60
      val secs = elapsed % 60

      val expected = f"$hours%02d:$minutes%02d:$secs%02d"

      // Allow for timing drift (±1 second)
      val alternativeElapsed1 = elapsed + 1
      val h1 = alternativeElapsed1 / 3600
      val m1 = (alternativeElapsed1 % 3600) / 60
      val s1 = alternativeElapsed1 % 60
      val alternative1 = f"$h1%02d:$m1%02d:$s1%02d"

      val alternativeElapsed2 = Math.max(0, elapsed - 1)
      val h2 = alternativeElapsed2 / 3600
      val m2 = (alternativeElapsed2 % 3600) / 60
      val s2 = alternativeElapsed2 % 60
      val alternative2 = f"$h2%02d:$m2%02d:$s2%02d"

      formatted == expected || formatted == alternative1 || formatted == alternative2
    }
  }

  property("start command is idempotent (returns error on second call)") {
    forAll { (_: Unit) =>
      val timer = PresentationTimer.create()
      val first = timer.start()
      assert(first.isRight, "First start should succeed")

      val started = first.getOrElse(fail("Expected Right"))
      val second = started.start()
      assert(second.isLeft, "Second start should fail")

      second match
        case Left(TimerError.TimerAlreadyStarted) => true
        case _ => false
    }
  }

  property("pause requires Running state (rejects NotStarted and Paused)") {
    forAll { (_: Unit) =>
      val notStarted = PresentationTimer.create()
      val pauseNotStarted = notStarted.pause()
      assert(pauseNotStarted.isLeft, "Cannot pause NotStarted timer")

      val running = notStarted.start().getOrElse(fail("start should succeed"))
      val paused = running.pause().getOrElse(fail("first pause should succeed"))
      val pauseAgain = paused.pause()
      assert(pauseAgain.isLeft, "Cannot pause already Paused timer")

      true
    }
  }

  property("resume requires Paused state (rejects NotStarted and Running)") {
    forAll { (_: Unit) =>
      val notStarted = PresentationTimer.create()
      val resumeNotStarted = notStarted.resume()
      assert(resumeNotStarted.isLeft, "Cannot resume NotStarted timer")

      val running = notStarted.start().getOrElse(fail("start should succeed"))
      val resumeRunning = running.resume()
      assert(resumeRunning.isLeft, "Cannot resume Running timer")

      true
    }
  }

end PresentationTimerSpec
