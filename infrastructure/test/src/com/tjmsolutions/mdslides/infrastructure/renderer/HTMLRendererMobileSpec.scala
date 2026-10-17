package com.tjmsolutions.mdslides.infrastructure.renderer

import com.tjmsolutions.mdslides.domain.{Slide, SlideDeck, SlideId, SlotName}
import cats.data.NonEmptyList

/**
 * Tests for mobile presenting: touch gestures, on-screen navigation controls,
 * viewport behaviour and fit-to-slide scaling of overflowing content.
 */
class HTMLRendererMobileSpec extends munit.FunSuite:

  private def deckOf(n: Int): SlideDeck =
    val slides = (1 to n).toList.map { i =>
      Slide(
        id = SlideId.unsafe(i),
        templateName = "content",
        slots = Map(SlotName.Heading -> s"Slide $i", SlotName.Body -> "Body")
      )
    }
    SlideDeck(NonEmptyList.fromListUnsafe(slides))

  private lazy val html: String = HTMLRenderer.renderDeck(deckOf(3))

  test("Presenter on a phone: viewport extends under the notch (viewport-fit=cover)"):
    assert(html.contains("viewport-fit=cover"))

  test("Presenter on a phone: swipe gestures are recognised via touchstart and touchend"):
    assert(html.contains("addEventListener('touchstart'"))
    assert(html.contains("addEventListener('touchend'"))

  test("Presenter on a phone: previous, next and fullscreen buttons are rendered with accessible labels"):
    assert(html.contains("id=\"nav-prev\""))
    assert(html.contains("id=\"nav-next\""))
    assert(html.contains("id=\"nav-fullscreen\""))
    assert(html.contains("aria-label=\"Previous slide\""))
    assert(html.contains("aria-label=\"Next slide\""))
    assert(html.contains("aria-label=\"Toggle fullscreen\""))

  test("Presenter with a clicker: PageDown and PageUp navigate"):
    assert(html.contains("'PageDown'"))
    assert(html.contains("'PageUp'"))

  test("Presenter on a phone: active slide is fitted to the viewport"):
    assert(html.contains("fitActiveSlide"))
    assert(html.contains("fit-managed"))

  test("Presenter on a phone: slide frame uses dynamic viewport height"):
    assert(html.contains("100dvh"))

  test("Presenter on a phone: double-tap zoom delay is disabled"):
    assert(html.contains("touch-action: manipulation"))

  test("Presenter on a phone: current slide is kept in the URL hash without pushing history"):
    assert(html.contains("history.replaceState"))

  test("Presenter on a phone portrait: two-column slides collapse to a single column"):
    assert(html.contains("(max-width: 600px) and (orientation: portrait)"))
    assert(html.contains("grid-template-columns: 1fr !important"))

  test("Slide counter keeps its N / total format"):
    assert(html.contains("""id="slide-counter""""))
    assert(html.contains(""">1 / 3</button>"""))

  test("Obsolete scaleBodyContent (measured hidden slides) is gone"):
    assert(!html.contains("scaleBodyContent"))

  test("Fit-to-slide keeps content clear of the floating controls bar"):
    assert(html.contains("wrapper.style.paddingBottom"))

  test("Fit-to-slide measures diagrams by their own box, not clipped SVG internals"):
    assert(html.contains("closest('pre, svg')"))

  test("Scaled content is re-laid out at full slide width so wide tables re-wrap"):
    assert(html.contains("el.style.width = (fullWidthPx / s) + 'px'"))
