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
