package com.tjmsolutions.mdslides.infrastructure.parser

import munit.FunSuite

/**
 * Debug test to verify image parsing with realistic body content.
 */
class FlexmarkAdapterDebugSpec extends FunSuite:

  test("parse realistic body content with image"):
    val bodyContent = """Images are embedded using markdown syntax:

![MDSlides Logo](https://via.placeholder.com/400x200/002C74/FFFFFF?text=MDSlides)

Images are responsive and scale to fit the slide."""

    val result = FlexmarkAdapter.parseInlineFormatting(bodyContent)

    // Debug output
    println(s"\n=== FlexmarkAdapter Output ===")
    println(s"Text spans: ${result.textSpans.length}")
    result.textSpans.foreach(span => println(s"  - '${span.text.replace("\n", "\\n")}'"))

    println(s"Images: ${result.contentImages.length}")
    result.contentImages.foreach(img => println(s"  - ${img.altText}: ${img.url}"))

    println(s"Plain text: '${result.plainText.replace("\n", "\\n")}'")

    // Check if image markdown appears in text spans (it SHOULD NOT)
    val hasImageMarkdown = result.textSpans.exists(_.text.contains("!["))
    println(s"Image markdown in text spans: $hasImageMarkdown")

    // Assertions
    assertEquals(result.contentImages.length, 1)
    assertEquals(result.contentImages.head.altText, "MDSlides Logo")
    assertEquals(result.contentImages.head.url, "https://via.placeholder.com/400x200/002C74/FFFFFF?text=MDSlides")

    // Image markdown should NOT appear in plain text or text spans
    assert(!result.plainText.contains("![MDSlides"), "Image markdown found in plainText!")
    assert(!result.plainText.contains("via.placeholder"), "Image URL found in plainText!")
    assert(!hasImageMarkdown, "Image markdown found in textSpans!")

end FlexmarkAdapterDebugSpec
