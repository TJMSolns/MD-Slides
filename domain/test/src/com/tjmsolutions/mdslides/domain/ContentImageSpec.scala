package com.tjmsolutions.mdslides.domain

import munit.FunSuite

/**
 * Tests for ContentImage value object.
 *
 * ContentImage represents informational images embedded in slide body.
 * Validates alt text requirement for accessibility.
 *
 * Related Governance:
 * - US-005: Image Embedding
 * - PDR-008: Image Policy
 * - PDR-005: Accessibility Requirements
 */
class ContentImageSpec extends FunSuite:

  test("ContentImage.validated - creates valid image with alt text"):
    val result = ContentImage.validated("./diagrams/arch.png", "System architecture diagram")

    assert(result.isRight)
    val image = result.toOption.get
    assertEquals(image.url, "./diagrams/arch.png")
    assertEquals(image.altText, "System architecture diagram")

  test("ContentImage.validated - trims whitespace from alt text"):
    val result = ContentImage.validated("./img.png", "  Diagram  ")

    assert(result.isRight)
    val image = result.toOption.get
    assertEquals(image.altText, "Diagram")

  test("ContentImage.validated - rejects empty alt text"):
    val result = ContentImage.validated("./img.png", "")

    assert(result.isLeft)
    val error = result.left.toOption.get
    assert(error.contains("missing required alt text"))

  test("ContentImage.validated - rejects whitespace-only alt text"):
    val result = ContentImage.validated("./img.png", "   ")

    assert(result.isLeft)
    val error = result.left.toOption.get
    assert(error.contains("missing required alt text"))

  test("ContentImage.validated - accepts relative paths"):
    val result = ContentImage.validated("./images/chart.png", "Revenue chart")

    assert(result.isRight)
    assertEquals(result.toOption.get.url, "./images/chart.png")

  test("ContentImage.validated - accepts absolute URLs"):
    val result = ContentImage.validated("https://example.com/logo.png", "Company logo")

    assert(result.isRight)
    assertEquals(result.toOption.get.url, "https://example.com/logo.png")

  test("ContentImage.validated - accepts data URLs"):
    val dataUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA"
    val result = ContentImage.validated(dataUrl, "Small icon")

    assert(result.isRight)
    assertEquals(result.toOption.get.url, dataUrl)

  test("ContentImage.unsafe - creates image without validation"):
    val image = ContentImage.unsafe("./img.png", "")

    // Unsafe constructor allows empty alt text (for testing only)
    assertEquals(image.url, "./img.png")
    assertEquals(image.altText, "")

  test("ContentImage - case class equality"):
    val img1 = ContentImage("./a.png", "Image A")
    val img2 = ContentImage("./a.png", "Image A")
    val img3 = ContentImage("./b.png", "Image B")

    assertEquals(img1, img2)
    assert(img1 != img3)

end ContentImageSpec
