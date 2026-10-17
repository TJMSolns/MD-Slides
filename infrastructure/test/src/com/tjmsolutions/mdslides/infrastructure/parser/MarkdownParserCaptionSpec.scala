package com.tjmsolutions.mdslides.infrastructure.parser

import com.tjmsolutions.mdslides.domain.SlotName
import com.tjmsolutions.mdslides.infrastructure.renderer.HTMLRenderer

/** The `caption:` frontmatter key on diagram slides. */
class MarkdownParserCaptionSpec extends munit.FunSuite:

  private val md =
    """---
      |template: diagram
      |caption: The render pipeline
      |---
      |
      |## Pipeline
      |
      |Some text
      |""".stripMargin

  test("a diagram slide's caption: frontmatter becomes its caption") {
    val deck = MarkdownParser.parse(md).toOption.get
    assertEquals(deck.getSlide(0).flatMap(_.slots.get(SlotName.Caption)), Some("The render pipeline"))
  }

  test("the caption is rendered below the diagram") {
    val html = HTMLRenderer.renderDeck(MarkdownParser.parse(md).toOption.get)
    assert(html.contains("diagram-caption"), "no caption element")
    assert(html.contains("The render pipeline"))
  }

/** Images must have alt text (WCAG 1.1.1); one without it is an error, not silently dropped. */
class MarkdownParserImageAltSpec extends munit.FunSuite:

  private def deck(body: String) =
    s"""---
       |template: content
       |---
       |
       |## Pictures
       |
       |$body
       |""".stripMargin

  test("an image without alt text is reported with its slide and path") {
    val result = MarkdownParser.parse(deck("![](./photo.png)"))
    assert(result.left.exists(e => e.contains("Slide 1") && e.contains("./photo.png") && e.contains("alt text")), result)
  }

  test("an image with alt text parses") {
    assert(MarkdownParser.parse(deck("![A photo](./photo.png)")).isRight)
  }

  test("image syntax inside a code sample is not an image") {
    assert(MarkdownParser.parse(deck("```\n![](./photo.png)\n```\n\nand `![](x.png)` inline")).isRight)
  }
