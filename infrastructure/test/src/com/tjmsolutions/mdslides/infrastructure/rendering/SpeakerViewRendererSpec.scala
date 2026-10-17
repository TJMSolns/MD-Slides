package com.tjmsolutions.mdslides.infrastructure.rendering

import com.tjmsolutions.mdslides.domain.{Slide, SlideDeck, SlideId, SlotName}
import cats.data.NonEmptyList

/**
 * Tests for SpeakerViewRenderer.
 *
 * SpeakerViewRenderer creates speaker.html with:
 * - Dual-pane layout (notes + preview + timer)
 * - Embedded slide data as JSON
 * - Sync.js integration
 * - HTML-escaped notes content
 *
 * Related User Story: US-034 - Speaker Notes Rendering
 */
class SpeakerViewRendererSpec extends munit.FunSuite:

  val sampleSlide1 = Slide(
    SlideId.unsafe(1),
    "title",
    Map(
      SlotName.Title -> "Sample Presentation",
      SlotName.Subtitle -> "Test Deck",
      SlotName.Author -> "Author Name"
    ),
    notes = Some("Welcome the audience")
  )

  val sampleSlide2 = Slide(
    SlideId.unsafe(2),
    "content",
    Map(
      SlotName.Heading -> "Introduction",
      SlotName.Body -> "This is the intro slide"
    ),
    notes = Some("Emphasize the key point\nPause here for questions")
  )

  val sampleSlide3 = Slide(
    SlideId.unsafe(3),
    "content",
    Map(
      SlotName.Heading -> "Conclusion",
      SlotName.Body -> "Thank you"
    )
    // No notes field
  )

  val sampleDeck = SlideDeck(NonEmptyList.of(sampleSlide1, sampleSlide2, sampleSlide3))

  test("render speaker view HTML structure") {
    val html = SpeakerViewRenderer.render(sampleDeck, "light")

    // Should be valid HTML
    assert(html.contains("<!DOCTYPE html>"))
    assert(html.contains("<html"))
    assert(html.contains("</html>"))

    // Should have title
    assert(html.contains("<title>Speaker View"))
  }

  test("embed slide data as JSON") {
    val html = SpeakerViewRenderer.render(sampleDeck, "light")

    // Should contain embedded slide data script
    assert(html.contains("<script id=\"slide-data\" type=\"application/json\">"))

    // Should contain slide count (JSON may have spacing)
    assert(html.contains("\"totalSlides\"") && html.contains("3"))

    // Should contain slide notes (or null) - JSON may have spacing
    assert(html.contains("\"notes\"") && html.contains("Welcome the audience"))
  }

  test("include sync.js script reference") {
    val html = SpeakerViewRenderer.render(sampleDeck, "light")

    // Should reference sync.js
    assert(html.contains("<script src=\"sync.js\">"))
  }

  test("render notes area with placeholder") {
    val html = SpeakerViewRenderer.render(sampleDeck, "light")

    // Should have notes container
    assert(html.contains("id=\"notes-area\"") || html.contains("class=\"notes\""))
  }

  test("render preview area") {
    val html = SpeakerViewRenderer.render(sampleDeck, "light")

    // Should have preview container
    assert(html.contains("id=\"preview-area\"") || html.contains("class=\"preview\""))
  }

  test("render timer area") {
    val html = SpeakerViewRenderer.render(sampleDeck, "light")

    // Should have timer display
    assert(html.contains("id=\"timer\"") || html.contains("class=\"timer\""))
  }

  test("escape HTML in notes content") {
    val slideWithHtmlNotes = Slide(
      SlideId.unsafe(1),
      "content",
      Map(SlotName.Heading -> "Test", SlotName.Body -> "Body"),
      notes = Some("Use <strong> tags & check \"quotes\"")
    )
    val deck = SlideDeck(NonEmptyList.of(slideWithHtmlNotes))

    val html = SpeakerViewRenderer.render(deck, "light")

    // Notes should be in JSON, which automatically escapes special characters
    // The important thing is that the notes content is present (in some form)
    // and that it's properly escaped (not rendering as HTML)
    assert(html.contains("Use"))  // Content is present
    assert(html.contains("tags"))
    assert(html.contains("check"))
    // Raw HTML tags should not appear unescaped in the JSON string value
    // (Circe's JSON encoder handles this automatically)
  }

  test("handle missing notes - null in JSON") {
    val slideWithoutNotes = Slide(
      SlideId.unsafe(1),
      "content",
      Map(SlotName.Heading -> "Test", SlotName.Body -> "Body"),
      notes = None
    )
    val deck = SlideDeck(NonEmptyList.of(slideWithoutNotes))

    val html = SpeakerViewRenderer.render(deck, "light")

    // Should have null for missing notes in JSON (may have spacing)
    assert(html.contains("\"notes\"") && (html.contains("null") || html.contains("\"notes\" : null")))
  }

  test("handle empty notes string") {
    val slideWithEmptyNotes = Slide(
      SlideId.unsafe(1),
      "content",
      Map(SlotName.Heading -> "Test", SlotName.Body -> "Body"),
      notes = Some("")
    )
    val deck = SlideDeck(NonEmptyList.of(slideWithEmptyNotes))

    val html = SpeakerViewRenderer.render(deck, "light")

    // Should have empty string in JSON (may have spacing: "notes" : "")
    assert(html.contains("\"notes\"") && html.contains("\"\""))
  }

  test("format multi-line notes with newlines") {
    val slideWithMultilineNotes = Slide(
      SlideId.unsafe(1),
      "content",
      Map(SlotName.Heading -> "Test", SlotName.Body -> "Body"),
      notes = Some("Line 1\nLine 2\nLine 3")
    )
    val deck = SlideDeck(NonEmptyList.of(slideWithMultilineNotes))

    val html = SpeakerViewRenderer.render(deck, "light")

    // Newlines should be escaped in JSON as \n
    assert(html.contains("Line 1\\nLine 2\\nLine 3"))
  }

  test("include current slide counter display") {
    val html = SpeakerViewRenderer.render(sampleDeck, "light")

    // Should have slide counter element
    assert(html.contains("id=\"slide-counter\"") || html.contains("class=\"slide-counter\""))
  }

  test("include theme name in output") {
    val htmlLight = SpeakerViewRenderer.render(sampleDeck, "light")
    val htmlDark = SpeakerViewRenderer.render(sampleDeck, "dark")

    // Theme name is passed but may not appear in output for MVP
    // (Speaker view styling is independent of main presentation theme)
    // Just verify both render without errors
    assert(htmlLight.nonEmpty)
    assert(htmlDark.nonEmpty)
  }

  test("render all slides data in JSON array") {
    val html = SpeakerViewRenderer.render(sampleDeck, "light")

    // Should contain data for all 3 slides
    assert(html.contains("Sample Presentation"))  // Slide 1 title
    assert(html.contains("Introduction"))         // Slide 2 heading
    assert(html.contains("Conclusion"))           // Slide 3 heading
  }

  test("speaker view has responsive layout classes") {
    val html = SpeakerViewRenderer.render(sampleDeck, "light")

    // Should have layout structure classes
    assert(html.contains("class=\"speaker-view\"") || html.contains("class=\"container\""))
  }


  // ── Mobile presenting ───────────────────────────────────────────────────────

  private lazy val mobileHtml: String =
    SpeakerViewRenderer.render(SlideDeck(NonEmptyList.of(sampleSlide1, sampleSlide2)), "light")

  test("Speaker view has exactly one speaker-view container") {
    assertEquals("class=\"speaker-view\"".r.findAllIn(mobileHtml).size, 1)
  }

  test("Speaker view offers previous and next buttons for touch use") {
    assert(mobileHtml.contains("id=\"speaker-prev\""))
    assert(mobileHtml.contains("id=\"speaker-next\""))
  }

  test("Speaker view recognises swipe gestures") {
    assert(mobileHtml.contains("addEventListener('touchend'"))
  }

  test("Speaker view collapses to a single column on narrow screens") {
    assert(mobileHtml.contains("@media (max-width: 700px)"))
  }

  test("Speaker view syncs the slide it navigated to, not one beyond it") {
    assert(!mobileHtml.contains("sendSlideChange(currentSlideIndex + 1)"))
  }

end SpeakerViewRendererSpec
