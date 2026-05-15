package com.tjmsolutions.mdslides.infrastructure.renderer

import com.tjmsolutions.mdslides.domain.{Slide, SlideDeck, SlideId, Theme}
import cats.data.NonEmptyList
import munit.FunSuite

/**
 * Test HTMLRenderer with images.
 */
class HTMLRendererImageSpec extends FunSuite:

  test("render slide with image"):
    val bodyMarkdown = """Images work:

![Test Image](https://example.com/img.png)

End of slide."""

    val slideId = SlideId(1).toOption.get
    val slide = Slide(
      id = slideId,
      templateName = "content",
      slots = Map(
        "heading" -> "Image Test",
        "body" -> bodyMarkdown
      )
    )

    val deck = SlideDeck.validated(NonEmptyList.one(slide)).toOption.get
    val html = HTMLRenderer.renderDeck(deck, Theme.light)

    println(s"\n=== Full HTML Output ===")
    println(html)
    println(s"\n=== Image Tags ===")
    println(s"Contains <img: ${html.contains("<img")}")
    println(s"Contains raw markdown: ${html.contains("![Test Image]")}")

    // Check that <img> tag exists
    assert(html.contains("<img"), "HTML should contain <img> tag")
    assert(html.contains("class=\"content-image\""), "Image should have content-image class")
    assert(html.contains("alt=\"Test Image\""), "Image should have alt text")
    assert(html.contains("https://example.com/img.png"), "Image should have src URL")

    // Check that raw markdown does NOT appear
    assert(!html.contains("![Test Image]"), "Raw markdown should NOT appear in HTML")

end HTMLRendererImageSpec
