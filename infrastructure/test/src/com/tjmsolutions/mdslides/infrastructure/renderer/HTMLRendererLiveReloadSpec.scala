package com.tjmsolutions.mdslides.infrastructure.renderer

import com.tjmsolutions.mdslides.domain.{Slide, SlideDeck, SlideId, Theme}
import cats.data.NonEmptyList
import munit.FunSuite

/**
 * Tests for live-reload meta-refresh injection (US-018).
 *
 * Rules:
 * - R2: liveReload=true → <meta http-equiv="refresh" content="2"> in head
 * - R5: liveReload=false (default) → no meta-refresh
 */
class HTMLRendererLiveReloadSpec extends FunSuite:

  private def anyDeck: SlideDeck =
    SlideDeck(NonEmptyList.one(Slide(
      id = SlideId.unsafe(1),
      templateName = "title",
      slots = Map("title" -> "Test", "subtitle" -> "sub")
    )))

  // R5 — default: no meta refresh
  test("US-018 / R5 — liveReload=false (default) produces no meta-refresh tag"):
    val html = HTMLRenderer.renderDeck(anyDeck)
    assert(!html.contains("http-equiv"), "Default render must not include meta http-equiv refresh")

  // R2 — live reload injects meta-refresh
  test("US-018 / R2 — liveReload=true injects meta http-equiv refresh in head"):
    val html = HTMLRenderer.renderDeck(anyDeck, liveReload = true)
    assert(
      html.contains("http-equiv") && html.contains("refresh"),
      "liveReload=true must inject <meta http-equiv='refresh'> tag"
    )

  // R2 — refresh interval is 2 seconds
  test("US-018 / R2 — meta-refresh content value is 2 seconds"):
    val html = HTMLRenderer.renderDeck(anyDeck, liveReload = true)
    assert(
      html.contains("content=\"2\"") || html.contains("content=2"),
      "Refresh interval must be 2 seconds"
    )

  // R2 — meta-refresh is inside <head>
  test("US-018 / R2 — meta-refresh tag appears inside the head element"):
    val html = HTMLRenderer.renderDeck(anyDeck, liveReload = true)
    val headEnd = html.indexOf("</head>")
    val metaRefresh = html.indexOf("http-equiv")
    assert(headEnd > 0, "<head> must be present")
    assert(metaRefresh >= 0 && metaRefresh < headEnd, "meta-refresh must appear before </head>")
