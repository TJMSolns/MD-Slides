package com.tjmsolutions.mdslides.infrastructure.parser

import com.tjmsolutions.mdslides.domain.{FormattedContent, TextSpan, Link, CodeBlock, ContentImage}
import munit.FunSuite

/**
 * Tests for FlexmarkAdapter image parsing (US-005).
 *
 * Verifies that markdown images are correctly translated to ContentImage domain types.
 *
 * Related Governance:
 * - ADR-010: Markdown Library Selection
 * - ADR-007: Anticorruption Layer
 * - US-005: Image Embedding
 * - PDR-008: Image Policy
 */
class FlexmarkAdapterImageSpec extends FunSuite:

  test("parse single content image with alt text"):
    val markdown = "![Architecture diagram](./diagrams/arch.png)"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(result.contentImages.length, 1)
    val image = result.contentImages.head
    assertEquals(image.url, "./diagrams/arch.png")
    assertEquals(image.altText, "Architecture diagram")

  test("parse multiple content images"):
    val markdown = """
      |![Layer 1](./img/layer1.png)
      |![Layer 2](./img/layer2.png)
      |![Layer 3](./img/layer3.png)
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(result.contentImages.length, 3)
    assertEquals(result.contentImages(0).url, "./img/layer1.png")
    assertEquals(result.contentImages(1).url, "./img/layer2.png")
    assertEquals(result.contentImages(2).url, "./img/layer3.png")

  test("parse image with absolute URL"):
    val markdown = "![Logo](https://example.com/logo.png)"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(result.contentImages.length, 1)
    assertEquals(result.contentImages.head.url, "https://example.com/logo.png")

  test("parse image with data URL"):
    val dataUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA"
    val markdown = s"![Icon]($dataUrl)"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(result.contentImages.length, 1)
    assertEquals(result.contentImages.head.url, dataUrl)

  test("parse text with images and other formatting"):
    val markdown = """
      |Our system has **three layers**:
      |
      |![Layer diagram](./diagrams/layers.png)
      |
      |Each layer is independently deployable.
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    // Should have text spans
    assert(result.textSpans.nonEmpty)
    assert(result.textSpans.exists(span => span.text == "three layers" && span.bold))

    // Should have image
    assertEquals(result.contentImages.length, 1)
    assertEquals(result.contentImages.head.url, "./diagrams/layers.png")

    // Plain text should not include image (PDR-008)
    assert(!result.plainText.contains("./diagrams/layers.png"))

  test("parse image with link-like URL"):
    val markdown = "![Docs](https://docs.example.com/guide.png)"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    // Should be parsed as image, not link
    assertEquals(result.contentImages.length, 1)
    assertEquals(result.links.length, 0)

  test("images don't appear in plain text"):
    val markdown = """
      |System overview:
      |![Architecture](./arch.png)
      |End of overview.
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    // Plain text should only contain text content, not image references
    assert(result.plainText.contains("System overview"))
    assert(result.plainText.contains("End of overview"))
    assert(!result.plainText.contains("Architecture"))
    assert(!result.plainText.contains("arch.png"))

  test("parse image with empty alt text - skipped"):
    val markdown = "![](./image.png)"
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    // Empty alt text violates PDR-008, should be skipped
    // (validation layer will catch this)
    assertEquals(result.contentImages.length, 0)

  test("parse mixed content - images, code blocks, formatting"):
    val markdown = """
      |## Component Diagram
      |
      |Our **architecture** looks like this:
      |
      |![System components](./diagrams/components.png)
      |
      |```scala
      |class Component
      |```
      |
      |![Data flow](./diagrams/flow.png)
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    // Should have both images
    assertEquals(result.contentImages.length, 2)
    assertEquals(result.contentImages(0).url, "./diagrams/components.png")
    assertEquals(result.contentImages(1).url, "./diagrams/flow.png")

    // Should have code block
    assertEquals(result.codeBlocks.length, 1)

    // Should have formatting
    assert(result.textSpans.exists(span => span.text == "architecture" && span.bold))

  test("image word count doesn't affect text word count"):
    val markdown = """
      |Short text.
      |![Very long descriptive alternative text that should not count toward word limit](./img.png)
      |""".stripMargin
    val result = FlexmarkAdapter.parseInlineFormatting(markdown)

    // Word count should only count "Short text." = 2 words
    // Alt text is not included in plain text (PDR-008)
    assertEquals(result.wordCount, 2)

end FlexmarkAdapterImageSpec
