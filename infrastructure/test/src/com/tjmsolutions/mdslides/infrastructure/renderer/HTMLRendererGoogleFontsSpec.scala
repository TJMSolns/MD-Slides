package com.tjmsolutions.mdslides.infrastructure.renderer

import com.tjmsolutions.mdslides.domain.*
import cats.data.NonEmptyList
import munit.FunSuite

/**
 * Tests for Google Fonts link injection in rendered HTML (US-017).
 *
 * Rules:
 * - R3: googleFonts.nonEmpty → preconnect + stylesheet links in <head>
 * - R4: URL format: https://fonts.googleapis.com/css2?family=A&family=B&display=swap
 * - R5: googleFonts.isEmpty → no Google Fonts links
 */
class HTMLRendererGoogleFontsSpec extends FunSuite:

  private def anySlide = Slide(
    id = SlideId.unsafe(1),
    templateName = "title",
    slots = Map("title" -> "Test", "subtitle" -> "sub")
  )

  private def renderWithFonts(googleFonts: List[String]): String =
    val deck = SlideDeck(NonEmptyList.one(anySlide))
    val theme = Theme.light.copy(fonts = Theme.light.fonts.copy(googleFonts = googleFonts))
    HTMLRenderer.renderDeck(deck, theme)

  // R5 — no fonts configured → no injection
  test("US-017 / R5 — no googleFonts configured → no Google Fonts link in output"):
    val html = renderWithFonts(Nil)
    assert(!html.contains("fonts.googleapis.com"), "Must not inject Google Fonts link when none configured")
    assert(!html.contains("fonts.gstatic.com"), "Must not inject preconnect when no fonts configured")

  // R3 — single font → all three link tags present
  test("US-017 / R3 — single googleFont → preconnect and stylesheet links injected"):
    val html = renderWithFonts(List("Roboto"))
    assert(html.contains("fonts.googleapis.com"), "Must include fonts.googleapis.com preconnect")
    assert(html.contains("fonts.gstatic.com"), "Must include fonts.gstatic.com preconnect")
    assert(html.contains("rel=\"stylesheet\"") || html.contains("rel=stylesheet"), "Must include stylesheet link")

  // R4 — URL format with single family (& is HTML-encoded as &amp; in attributes)
  test("US-017 / R4 — single font URL uses correct Google Fonts v2 format"):
    val html = renderWithFonts(List("Roboto"))
    assert(
      html.contains("fonts.googleapis.com/css2?family=Roboto"),
      "URL must use /css2 endpoint with family param"
    )
    assert(html.contains("display=swap"), "URL must include display=swap")

  // R4 — multiple families joined in one URL
  test("US-017 / R4 — multiple fonts joined into single stylesheet URL"):
    val html = renderWithFonts(List("Roboto", "Open+Sans:wght@400;700"))
    assert(
      html.contains("family=Roboto") && html.contains("family=Open+Sans"),
      "All font families must appear in the stylesheet URL"
    )
    assert(html.contains("display=swap"), "URL must include display=swap")

  // R3 — crossorigin on gstatic preconnect
  test("US-017 / R3 — gstatic preconnect includes crossorigin attribute"):
    val html = renderWithFonts(List("Roboto"))
    val gstaticIdx = html.indexOf("fonts.gstatic.com")
    assert(gstaticIdx >= 0, "gstatic preconnect must be present")
    val surrounding = html.substring(gstaticIdx - 100 max 0, gstaticIdx + 100 min html.length)
    assert(surrounding.contains("crossorigin"), "gstatic preconnect must have crossorigin attribute")
