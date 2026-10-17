package com.tjmsolutions.mdslides.domain

/**
 * Tests for PresentationState domain model.
 *
 * PresentationState represents the runtime state of a presentation,
 * including current slide position, timer state, and window sync status.
 *
 * Related User Story: US-034 - Speaker Notes Rendering
 */
class PresentationStateSpec extends munit.FunSuite:

  test("create initial PresentationState with valid slide count") {
    val state = PresentationState.initial(slideCount = 5)

    assertEquals(state.currentSlideIndex, 0)
    assertEquals(state.totalSlides, 5)
    assertEquals(state.timerStartTime, None)
    assertEquals(state.speakerViewOpen, false)
  }

  test("create initial PresentationState fails with invalid slide count") {
    val result = PresentationState.create(
      currentSlideIndex = 0,
      totalSlides = 0
    )

    result match
      case Left(error) =>
        assert(error.contains("totalSlides must be positive"))
      case Right(_) =>
        fail("Expected creation to fail with zero slides")
  }

  test("update current slide index within bounds") {
    val state = PresentationState.initial(slideCount = 5)

    val updated = state.goToSlide(2)

    updated match
      case Right(newState) =>
        assertEquals(newState.currentSlideIndex, 2)
      case Left(error) =>
        fail(s"Expected successful update, got error: $error")
  }

  test("update current slide index fails when out of bounds") {
    val state = PresentationState.initial(slideCount = 5)

    // Try to go to slide 5 (valid indices are 0-4)
    val result = state.goToSlide(5)

    result match
      case Left(error) =>
        assert(error.contains("out of bounds"))
      case Right(_) =>
        fail("Expected update to fail with out of bounds index")
  }

  test("start timer records start time") {
    val state = PresentationState.initial(slideCount = 3)

    assertEquals(state.timerStartTime, None)

    val currentTime = System.currentTimeMillis()
    val stateWithTimer = state.startTimer(currentTime)

    assertEquals(stateWithTimer.timerStartTime, Some(currentTime))
  }

  test("calculate elapsed time from start") {
    val startTime = 1000000L
    val state = PresentationState.initial(slideCount = 3).startTimer(startTime)

    // After 5 seconds (5000 ms)
    val currentTime = startTime + 5000L
    val elapsed = state.elapsedSeconds(currentTime)

    assertEquals(elapsed, 5)
  }

  test("elapsed time is zero when timer not started") {
    val state = PresentationState.initial(slideCount = 3)

    val currentTime = System.currentTimeMillis()
    val elapsed = state.elapsedSeconds(currentTime)

    assertEquals(elapsed, 0)
  }

  test("format elapsed time as MM:SS") {
    assertEquals(PresentationState.formatTime(0), "00:00")
    assertEquals(PresentationState.formatTime(5), "00:05")
    assertEquals(PresentationState.formatTime(59), "00:59")
    assertEquals(PresentationState.formatTime(60), "01:00")
    assertEquals(PresentationState.formatTime(90), "01:30")
    assertEquals(PresentationState.formatTime(599), "09:59")
    assertEquals(PresentationState.formatTime(600), "10:00")
    assertEquals(PresentationState.formatTime(3599), "59:59")
    assertEquals(PresentationState.formatTime(3600), "60:00")  // Past 1 hour
    assertEquals(PresentationState.formatTime(7200), "120:00") // 2 hours
  }

  test("toggle speaker view open state") {
    val state = PresentationState.initial(slideCount = 3)

    assertEquals(state.speakerViewOpen, false)

    val opened = state.openSpeakerView()
    assertEquals(opened.speakerViewOpen, true)

    val closed = opened.closeSpeakerView()
    assertEquals(closed.speakerViewOpen, false)
  }

  test("navigation helpers - next slide") {
    val state = PresentationState.initial(slideCount = 5)

    val next = state.nextSlide()
    next match
      case Right(newState) =>
        assertEquals(newState.currentSlideIndex, 1)
      case Left(error) =>
        fail(s"Expected successful navigation, got error: $error")
  }

  test("navigation helpers - previous slide") {
    val state = PresentationState.initial(slideCount = 5).goToSlide(2).toOption.get

    val prev = state.previousSlide()
    prev match
      case Right(newState) =>
        assertEquals(newState.currentSlideIndex, 1)
      case Left(error) =>
        fail(s"Expected successful navigation, got error: $error")
  }

  test("navigation helpers - first slide") {
    val state = PresentationState.initial(slideCount = 5).goToSlide(3).toOption.get

    val first = state.firstSlide()
    assertEquals(first.currentSlideIndex, 0)
  }

  test("navigation helpers - last slide") {
    val state = PresentationState.initial(slideCount = 5)

    val last = state.lastSlide()
    assertEquals(last.currentSlideIndex, 4) // Last index of 5 slides
  }

  test("next slide fails at end of presentation") {
    val state = PresentationState.initial(slideCount = 5).lastSlide()

    val result = state.nextSlide()
    result match
      case Left(error) =>
        assert(error.contains("Already at last slide"))
      case Right(_) =>
        fail("Expected navigation to fail at last slide")
  }

  test("previous slide fails at start of presentation") {
    val state = PresentationState.initial(slideCount = 5)

    val result = state.previousSlide()
    result match
      case Left(error) =>
        assert(error.contains("Already at first slide"))
      case Right(_) =>
        fail("Expected navigation to fail at first slide")
  }

  test("has next slide returns correct boolean") {
    val state = PresentationState.initial(slideCount = 5)
    assertEquals(state.hasNextSlide, true) // On slide 0, has next

    val lastSlide = state.lastSlide()
    assertEquals(lastSlide.hasNextSlide, false) // On slide 4, no next
  }

  test("has previous slide returns correct boolean") {
    val state = PresentationState.initial(slideCount = 5)
    assertEquals(state.hasPreviousSlide, false) // On slide 0, no previous

    val secondSlide = state.nextSlide().toOption.get
    assertEquals(secondSlide.hasPreviousSlide, true) // On slide 1, has previous
  }

end PresentationStateSpec
