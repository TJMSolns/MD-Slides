package com.tjmsolutions.mdslides.infrastructure.parser

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
