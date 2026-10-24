package com.tjmsolutions.mdslides.infrastructure.parser

import com.tjmsolutions.mdslides.domain.SlotName

/** MS-032: a frontmatter value that merely ends in a quotation mark kept losing it. */
class FrontmatterQuoteSpec extends munit.FunSuite:

  private def caption(raw: String): Option[String] =
    val md = s"---\ntemplate: diagram\ncaption: $raw\n---\n\n## Diagram\n\n```mermaid\ngraph LR\n  A --> B\n```\n"
    MarkdownParser.parse(md).toOption.flatMap(_.slides.head.getSlot(SlotName.Caption))

  test("a caption that ends with a quoted word keeps its closing quotation mark") {
    assertEquals(caption("""Part of a typed concept graph for the word "chocolate""""), Some("""Part of a typed concept graph for the word "chocolate""""))
  }

  test("a caption that starts with a quoted word keeps its opening quotation mark") {
    assertEquals(caption(""""Chocolate" has four senses"""), Some(""""Chocolate" has four senses"""))
  }

  test("a fully quoted caption is unquoted") {
    assertEquals(caption(""""The render pipeline""""), Some("The render pipeline"))
  }

  test("a caption that starts and ends with a quoted word keeps every quotation mark") {
    assertEquals(caption(""""Chocolate" and "vanilla""""), Some(""""Chocolate" and "vanilla""""))
    assertEquals(caption(""""Starts quoted" then trails "end""""), Some(""""Starts quoted" then trails "end""""))
  }

  test("a fully quoted caption with escaped quotation marks inside is unquoted and unescaped") {
    assertEquals(caption(""""The word \"chocolate\" has four senses""""), Some("""The word "chocolate" has four senses"""))
  }
