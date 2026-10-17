package com.tjmsolutions.mdslides.infrastructure.parser

import munit.FunSuite

/**
 * Tests for MarkdownParser.slideLineNumbers (US-019).
 *
 * Rules:
 * - R3: returns Map[slideIndex(1-based), lineNumber(1-based)] of each slide's opening ---
 * - R3: single slide → Map(1 -> 1) when --- is on line 1
 * - R3: multi-slide → correct line numbers for each slide's opening ---
 */
class MarkdownParserLineNumberSpec extends FunSuite:

  // R3 — single slide
  test("US-019 / R3 — single slide returns Map(1 -> 1) when --- on first line"):
    val markdown =
      """|---
         |template: title
         |---
         |# Title""".stripMargin
    val map = MarkdownParser.slideLineNumbers(markdown)
    assertEquals(map.get(1), Some(1), "Slide 1 must start at line 1")

  // R3 — two slides
  test("US-019 / R3 — two slides returns correct line numbers for each opening ---"):
    val markdown =
      """|---
         |template: title
         |---
         |# Title
         |
         |---
         |template: content
         |---
         |## Heading
         |body""".stripMargin
    val map = MarkdownParser.slideLineNumbers(markdown)
    assertEquals(map.get(1), Some(1), "Slide 1 must start at line 1")
    assertEquals(map.get(2), Some(6), "Slide 2 must start at line 6")

  // R3 — three slides
  test("US-019 / R3 — three slides all have correct line numbers"):
    val markdown =
      """|---
         |template: title
         |---
         |# Slide 1
         |
         |---
         |template: content
         |---
         |## Slide 2
         |
         |---
         |template: content
         |---
         |## Slide 3""".stripMargin
    val map = MarkdownParser.slideLineNumbers(markdown)
    assertEquals(map.get(1), Some(1))
    assertEquals(map.get(2), Some(6))
    assertEquals(map.get(3), Some(11))

  // R3 — map contains entry for every slide
  test("US-019 / R3 — slideLineNumbers map contains one entry per slide"):
    val markdown =
      """|---
         |template: title
         |---
         |# Title
         |
         |---
         |template: content
         |---
         |## Heading""".stripMargin
    val map = MarkdownParser.slideLineNumbers(markdown)
    assertEquals(map.size, 2, "Map must have one entry per slide")
