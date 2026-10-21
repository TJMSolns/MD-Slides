package com.tjmsolutions.mdslides.infrastructure.renderer

import com.tjmsolutions.mdslides.domain.{Slide, SlideDeck, SlideId, SlotName}
import cats.data.NonEmptyList

/** MS-031: a diagram caption with "&" and "<special>" rendered as "&amp;amp;  chars", losing "<special>". */
class HTMLRendererCaptionEscapeSpec extends munit.FunSuite:

  private def captionHtml(caption: String): String =
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "diagram",
      slots = Map(
        SlotName.Heading -> "Diagram",
        SlotName.Body -> "```mermaid\ngraph LR\n  A --> B\n```",
        SlotName.Caption -> caption
      )
    )
    val html = HTMLRenderer.renderDeck(SlideDeck(NonEmptyList.one(slide)))
    val start = html.indexOf("""class="diagram-caption"""")
    assert(start >= 0, "no diagram caption in the HTML")
    html.substring(html.indexOf('>', start) + 1, html.indexOf("</p>", start))

  test("an ampersand in a caption is escaped once") {
    val caption = captionHtml("Fish & chips")
    assert(caption.contains("Fish &amp; chips"), s"got: $caption")
    assert(!caption.contains("&amp;amp;"), s"double-escaped: $caption")
  }

  test("angle-bracketed text in a caption is shown, not dropped") {
    val caption = captionHtml("Verifier caption & <special> chars")
    assert(caption.contains("&lt;special&gt;"), s"got: $caption")
  }
