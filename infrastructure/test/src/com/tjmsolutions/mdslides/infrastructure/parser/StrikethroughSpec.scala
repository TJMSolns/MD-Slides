package com.tjmsolutions.mdslides.infrastructure.parser

import com.tjmsolutions.mdslides.infrastructure.renderer.HTMLRenderer

/** ~~Strikethrough~~ in slide text and list items. */
class StrikethroughSpec extends munit.FunSuite:

  test("~~text~~ is parsed as struck-through text") {
    val spans = FlexmarkAdapter.parseInlineFormatting("keep ~~drop~~ keep").textSpans
    assert(spans.exists(s => s.text == "drop" && s.strike), spans)
    assert(spans.filter(_.text.contains("keep")).forall(!_.strike), spans)
  }

  test("struck-through text renders as <del> in paragraphs and list items") {
    val md = "---\ntemplate: content\n---\n\n## S\n\nNow ~~then~~\n\n- item ~~old~~\n"
    val html = HTMLRenderer.renderDeck(MarkdownParser.parse(md).toOption.get)
    assert(html.contains("<del>then</del>"), "paragraph")
    assert(html.contains("<del>old</del>"), "list item")
  }
