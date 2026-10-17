package com.tjmsolutions.mdslides.domain

import munit.FunSuite

/**
 * Tests for SlideBackground value object.
 *
 * Supports two forms:
 * 1. Simple string: "backgrounds/custom.png"
 * 2. Extended object: BackgroundConfig(image = "backgrounds/custom.png", opacity = Some(0.3))
 *
 * Related:
 * - US-011: Per-Slide Background Images
 * - PDR-011: Background Image Architecture
 */
class SlideBackgroundSpec extends FunSuite:

  test("BackgroundConfig accepts valid image path"):
    val config = BackgroundConfig(image = "backgrounds/custom.png")
    assertEquals(config.image, "backgrounds/custom.png")
    assertEquals(config.opacity, None)

  test("BackgroundConfig accepts image with opacity"):
    val config = BackgroundConfig(
      image = "backgrounds/custom.png",
      opacity = Some(0.5)
    )
    assertEquals(config.image, "backgrounds/custom.png")
    assertEquals(config.opacity, Some(0.5))

  test("BackgroundConfig validates opacity range (0.0 to 1.0)"):
    // Valid opacities
    val valid1 = BackgroundConfig(image = "bg.png", opacity = Some(0.0))
    assertEquals(valid1.opacity, Some(0.0))

    val valid2 = BackgroundConfig(image = "bg.png", opacity = Some(1.0))
    assertEquals(valid2.opacity, Some(1.0))

    val valid3 = BackgroundConfig(image = "bg.png", opacity = Some(0.5))
    assertEquals(valid3.opacity, Some(0.5))

  test("BackgroundConfig rejects opacity < 0.0"):
    val caught = intercept[IllegalArgumentException] {
      BackgroundConfig(image = "bg.png", opacity = Some(-0.1))
    }
    assert(caught.getMessage.contains("opacity must be between 0.0 and 1.0"))

  test("BackgroundConfig rejects opacity > 1.0"):
    val caught = intercept[IllegalArgumentException] {
      BackgroundConfig(image = "bg.png", opacity = Some(1.5))
    }
    assert(caught.getMessage.contains("opacity must be between 0.0 and 1.0"))

end SlideBackgroundSpec
