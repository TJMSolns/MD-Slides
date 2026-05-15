package com.tjmsolutions.mdslides.infrastructure.renderer

import com.tjmsolutions.mdslides.domain.{Slide, SlideDeck, SlideId}
import cats.data.NonEmptyList
import munit.FunSuite

/**
 * Tests for CSS overflow behaviour (BUG-004).
 *
 * Verifies that the generated CSS correctly constrains slide content within
 * the viewport using the standard flexbox min-height: 0 pattern and
 * overflow-y: auto on the slide body so dense content is visible (not silent).
 *
 * Related Governance:
 * - BUG-004: Slides Overflow Vertical Limits
 * - example-mapping-BUG-004.md: Rules 1–4
 * - three-amigos-BUG-004.md: AC1–AC4
 */
class HTMLRendererOverflowSpec extends munit.FunSuite:

  private def anyDeck: SlideDeck =
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "content",
      slots = Map("heading" -> "Test", "body" -> "Normal content.")
    )
    SlideDeck(NonEmptyList.one(slide))

  // ── AC1: CSS Flexbox Clipping Fix ────────────────────────────────────────────

  test("BUG-004 / AC1 — slide-content-wrapper CSS includes min-height: 0"):
    val html = HTMLRenderer.renderDeck(anyDeck)
    assert(
      html.contains("min-height: 0"),
      "CSS must contain 'min-height: 0' to allow flex child to shrink below content height"
    )

  // ── AC2: Overflow Visibility ──────────────────────────────────────────────────

  test("BUG-004 / AC2 — slide-body CSS uses overflow-y: auto not hidden"):
    val html = HTMLRenderer.renderDeck(anyDeck)
    assert(
      html.contains("overflow-y: auto"),
      "slide-body must use overflow-y: auto so dense content is scrollable rather than silently clipped"
    )

  // ── AC2: Dense content present in render output ───────────────────────────────

  test("BUG-004 / AC2 — dense slide body content all present in rendered HTML"):
    val denseBody = (1 to 25).map(i => s"- Item $i with some detail text").mkString("\n")
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "content",
      slots = Map("heading" -> "Dense Slide", "body" -> denseBody)
    )
    val html = HTMLRenderer.renderDeck(SlideDeck(NonEmptyList.one(slide)))

    (1 to 25).foreach { i =>
      assert(html.contains(s"Item $i"), s"Item $i must be present in rendered HTML (not silently dropped)")
    }

  // ── AC4: No Regression ───────────────────────────────────────────────────────

  test("BUG-004 / AC4 — normal content slide still renders correctly"):
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "content",
      slots = Map("heading" -> "Architecture Overview", "body" -> "Key points here.")
    )
    val html = HTMLRenderer.renderDeck(SlideDeck(NonEmptyList.one(slide)))

    assert(html.contains("Architecture Overview"), "Heading must render")
    assert(html.contains("Key points here"), "Body content must render")
    assert(html.contains("slide-heading"), "Heading CSS class must be present")
    assert(html.contains("slide-body"), "Body CSS class must be present")

  test("BUG-004 / AC4 — title slide unaffected by overflow fix"):
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "title",
      slots = Map("title" -> "My Presentation", "subtitle" -> "A subtitle")
    )
    val html = HTMLRenderer.renderDeck(SlideDeck(NonEmptyList.one(slide)))

    assert(html.contains("My Presentation"), "Title must render")
    assert(html.contains("A subtitle"), "Subtitle must render")
    assert(html.contains("slide-title"), "Title CSS class must be present")
