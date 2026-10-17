package com.tjmsolutions.mdslides.infrastructure.renderer

import com.tjmsolutions.mdslides.domain.{Slide, SlideDeck, SlideId, SlotName}
import cats.data.NonEmptyList

/**
 * Speaker-view synchronisation, session logging and deck metadata in the rendered page.
 * (Behaviour in a real browser is checked by the release verification run; these pin the wiring.)
 */
class HTMLRendererSessionSpec extends munit.FunSuite:

  private val deck = SlideDeck(NonEmptyList.of(
    Slide(SlideId.unsafe(1), "title", Map(SlotName.Title -> "Quarterly **Review**")),
    Slide(SlideId.unsafe(2), "content", Map(SlotName.Heading -> "Agenda", SlotName.Body -> "Body"))
  ))

  private lazy val html: String = HTMLRenderer.renderDeck(deck)

  test("the page title is the deck title"):
    assert(html.contains("<title>Quarterly Review</title>"), html.take(600))

  test("the deck title and theme are exposed for {{title}} and the session log"):
    assert(html.contains("""<meta name="mdslides-title" content="Quarterly Review" />"""))
    assert(html.contains("""<meta name="mdslides-theme" content="""))

  test("the presentation loads the speaker-view sync module before navigation"):
    val sync = html.indexOf("""<script src="sync.js">""")
    val nav  = html.indexOf("function showSlide(")
    assert(sync >= 0 && sync < nav)

  test("every slide change is announced with how it happened and sent to speaker view"):
    assert(html.contains("new CustomEvent('mdslides:slidechange'"))
    assert(html.contains("window.MDSlidesSync.sendSlideChange(index)"))
    assert(html.contains("receiveSlideChange((i) => showSlide(i, 'speaker-view'))"))
    List("'swipe'", "'tap'", "'button'", "'link'", "'keyboard'").foreach(m => assert(html.contains(m), m))

  test("pressing L saves the session log"):
    assert(html.contains("case 'L':"))
    assert(html.contains("MDSlidesHistoryLogger.downloadLog()"))

  test("on-screen buttons navigate with a method, not the click event"):
    assert(html.contains("addEventListener('click', () => prevSlide('button'))"))
    assert(!html.contains("addEventListener('click', prevSlide)"))

  test("the slide counter is a button that opens goto, with an accessible name"):
    assert(html.contains("""<button type="button" class="slide-counter" id="slide-counter" aria-haspopup="dialog" aria-label="Slide 1 of 2, go to slide">1 / 2</button>"""))

  test("printing (Save as PDF) shows every slide on its own page and hides the controls"):
    val print = html.substring(html.indexOf("@media print"))
    assert(print.contains("display: flex !important"))
    assert(print.contains("break-after: page"))
    assert(print.contains(".controls"))
