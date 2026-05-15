package com.tjmsolutions.mdslides.domain

import munit.FunSuite

/**
 * Tests for WCAG 2.1 AA accessibility validation.
 *
 * Tests verify:
 * - Contrast ratio calculation (WCAG 2.1 formula)
 * - Theme color validation
 * - Accessibility report generation
 *
 * Related Governance:
 * - US-014: Accessibility Validation
 * - Example Mapping: accessibility-validation.md
 */
class AccessibilitySpec extends FunSuite:

  // Example Mapping Scenario 1: Calculate relative luminance for pure black
  test("relative luminance for pure black (#000000) is 0.0"):
    val color = "#000000"
    val luminance = ContrastCalculator.relativeLuminance(color)
    assertEquals(luminance, 0.0, 0.001)

  // Example Mapping Scenario 2: Calculate relative luminance for pure white
  test("relative luminance for pure white (#FFFFFF) is 1.0"):
    val color = "#FFFFFF"
    val luminance = ContrastCalculator.relativeLuminance(color)
    assertEquals(luminance, 1.0, 0.001)

  // Example Mapping Scenario 3: Calculate contrast ratio for black text on white background
  test("contrast ratio for black on white is 21.0 (maximum)"):
    val foreground = "#000000"
    val background = "#FFFFFF"
    val ratio = ContrastCalculator.contrastRatio(foreground, background)
    assertEquals(ratio, 21.0, 0.01)

  // Example Mapping Scenario 4: Calculate contrast ratio for gray on gray (fails WCAG)
  test("contrast ratio for gray (#888888) on light gray (#AAAAAA) fails WCAG AA"):
    val foreground = "#888888"
    val background = "#AAAAAA"
    val ratio = ContrastCalculator.contrastRatio(foreground, background)
    // Actual calculated value is ~1.526, which definitely fails both normal (4.5:1) and large (3:1) text
    assert(ratio < 3.0, s"Should fail even large text (3:1), got $ratio")
    assert(ratio > 1.5 && ratio < 1.6, s"Expected ratio between 1.5 and 1.6, got $ratio")

  // Example Mapping Scenario 5: Calculate contrast ratio for large text threshold
  test("contrast ratio #767676 on #FFFFFF passes both large text AND normal text"):
    val foreground = "#767676"
    val background = "#FFFFFF"
    val ratio = ContrastCalculator.contrastRatio(foreground, background)
    // Actual calculated value is ~4.542, which passes normal text threshold
    assert(ratio >= 4.5, s"Should pass normal text (4.5:1), got $ratio")
    assert(ratio > 3.0, s"Should pass large text (3:1), got $ratio")
    assert(ratio > 4.5 && ratio < 4.6, s"Expected ratio between 4.5 and 4.6, got $ratio")

  // WCAG level checks
  test("ContrastRatio.passesWCAG_AA returns true for 4.5:1 normal text"):
    val ratio = ContrastRatio(4.5)
    assert(ratio.passesWCAG_AA(normalText = true))

  test("ContrastRatio.passesWCAG_AA returns false for 4.4:1 normal text"):
    val ratio = ContrastRatio(4.4)
    assert(!ratio.passesWCAG_AA(normalText = true))

  test("ContrastRatio.passesWCAG_AA returns true for 3.0:1 large text"):
    val ratio = ContrastRatio(3.0)
    assert(ratio.passesWCAG_AA(normalText = false))

  test("ContrastRatio.passesWCAG_AA returns false for 2.9:1 large text"):
    val ratio = ContrastRatio(2.9)
    assert(!ratio.passesWCAG_AA(normalText = false))

  // Example Mapping Scenario 6: Light theme passes WCAG 2.1 AA
  test("light theme passes WCAG 2.1 AA contrast validation"):
    val theme = Theme.light
    val validator = ThemeValidator(theme)
    val result = validator.validateContrast()

    assert(result.passes, s"Light theme should pass, warnings: ${result.warnings}")
    assert(result.warnings.isEmpty, "Should have no warnings")

  // Example Mapping Scenario 7: Dark theme passes WCAG 2.1 AA
  test("dark theme passes WCAG 2.1 AA contrast validation"):
    val theme = Theme.dark
    val validator = ThemeValidator(theme)
    val result = validator.validateContrast()

    assert(result.passes, s"Dark theme should pass, warnings: ${result.warnings}")
    assert(result.warnings.isEmpty, "Should have no warnings")

  // Example Mapping Scenario 8: Custom theme fails contrast (low contrast warning)
  test("custom theme with low contrast generates warning but allows HTML generation"):
    import com.tjmsolutions.mdslides.domain.{Background, ColorScheme, FontScheme, Spacing, SyntaxColors, SlideCounter}

    val lowContrastTheme = Theme(
      name = "LowContrast",
      version = "1.0.0",
      background = Background(color = "#AAAAAA", image = None),
      colors = ColorScheme(
        text = "#888888",  // Low contrast with #AAAAAA background
        heading = "#2C3E50",
        accent = "#0000FF",
        link = "#3498DB",
        linkHover = "#2980B9",
        codeBackground = "#F0F0F0",
        codeText = "#000000"
      ),
      fonts = FontScheme(body = "Arial", heading = "Arial", code = "monospace"),
      spacing = Spacing(slideMargin = "2rem", headingMargin = "1rem 0", paragraphMargin = "0.5rem 0", lineHeight = "1.6"),
      syntax = SyntaxColors(keyword = "#0000FF", string = "#008000", comment = "#808080", function = "#000080", number = "#FF0000", operator = "#000000"),
      slideCounter = SlideCounter(color = "#666666", background = "rgba(255,255,255,0.9)", fontSize = "0.9rem")
    )

    val validator = ThemeValidator(lowContrastTheme)
    val result = validator.validateContrast()

    assert(!result.passes, "Should fail validation due to low contrast")
    assert(result.warnings.nonEmpty, "Should have warnings")
    assert(result.warnings.exists(_.category == "contrast"), "Should have contrast warning")
    assert(result.warnings.exists(_.message.contains("Low contrast ratio")), "Warning should mention low contrast")

  // Example Mapping Scenario 9: Syntax colors skipped (not checked)
  test("theme validation does NOT check syntax highlighting colors"):
    import com.tjmsolutions.mdslides.domain.{Background, ColorScheme, FontScheme, Spacing, SyntaxColors, SlideCounter}

    // Create theme with valid theme colors but terrible syntax colors (low contrast)
    val themeWithBadSyntax = Theme(
      name = "BadSyntax",
      version = "1.0.0",
      background = Background(color = "#FFFFFF", image = None),
      colors = ColorScheme(
        text = "#000000",  // Good contrast
        heading = "#000000",  // Good contrast
        accent = "#0066CC",  // Good contrast
        link = "#0066CC",  // Good contrast
        linkHover = "#004C99",
        codeBackground = "#f4f4f4",
        codeText = "#333333"
      ),
      fonts = FontScheme(body = "Arial", heading = "Arial", code = "monospace"),
      spacing = Spacing(slideMargin = "2rem", headingMargin = "1rem 0", paragraphMargin = "0.5rem 0", lineHeight = "1.6"),
      syntax = SyntaxColors(
        keyword = "#FFFF00",  // Yellow on white - terrible contrast, but should NOT be checked
        string = "#FFFFFF",  // White on white - terrible, but should NOT be checked
        comment = "#EEEEEE",  // Almost white - terrible, but should NOT be checked
        function = "#DDDDDD",  // Light gray - terrible, but should NOT be checked
        number = "#CCCCCC",  // Gray - terrible, but should NOT be checked
        operator = "#BBBBBB"  // Gray - terrible, but should NOT be checked
      ),
      slideCounter = SlideCounter(color = "#666666", background = "rgba(255,255,255,0.9)", fontSize = "0.9rem")
    )

    val validator = ThemeValidator(themeWithBadSyntax)
    val result = validator.validateContrast()

    // Should pass because syntax colors are NOT checked (they come from highlight.js)
    assert(result.passes, s"Should pass despite bad syntax colors, warnings: ${result.warnings}")
    assert(result.warnings.isEmpty, "Syntax colors should not generate warnings")

  // Example Mapping Scenario 10: Image with valid alt text passes
  test("image with valid alt text passes accessibility validation"):
    val images = List(
      ContentImage.unsafe("images/diagram.svg", "Architecture Diagram")
    )
    val result = AltTextValidator.validate(images)

    assertEquals(result.imagesChecked, 1)
    assertEquals(result.imagesMissingAlt, 0)

  // Example Mapping Scenario 11: Image with empty alt text fails
  test("image with empty alt text generates warning"):
    // Note: Domain validation should prevent this, but we check anyway for comprehensive reporting
    val images = List(
      ContentImage.unsafe("empty.png", "")  // Empty alt text
    )
    val result = AltTextValidator.validate(images)

    assertEquals(result.imagesChecked, 1)
    assertEquals(result.imagesMissingAlt, 1)

  // Example Mapping Scenario 12: Multiple images all validated
  test("multiple images are all validated for alt text"):
    val images = List(
      ContentImage.unsafe("img1.png", "First image"),
      ContentImage.unsafe("img2.png", "Second image"),
      ContentImage.unsafe("img3.png", "Third image")
    )
    val result = AltTextValidator.validate(images)

    assertEquals(result.imagesChecked, 3)
    assertEquals(result.imagesMissingAlt, 0)

  // Example Mapping Scenario 13: All required keyboard handlers present
  test("keyboard validation passes when all required handlers are present"):
    val foundHandlers = List("ArrowRight", "ArrowLeft", "Space", "Home", "End", "s", "S")
    val result = KeyboardValidator.validate(foundHandlers)

    assertEquals(result.handlersFound, foundHandlers)
    assertEquals(result.handlersMissing, List.empty[String])

  // Example Mapping Scenario 14: Missing 'S' key handler (should never happen after v1.3.1)
  test("keyboard validation detects missing 'S' key handler"):
    val foundHandlers = List("ArrowRight", "ArrowLeft", "Space", "Home", "End", "s")  // Missing 'S'
    val result = KeyboardValidator.validate(foundHandlers)

    assertEquals(result.handlersFound, foundHandlers)
    assert(result.handlersMissing.contains("S"), "Should detect missing 'S' handler")

  // Additional keyboard validation test: multiple missing handlers
  test("keyboard validation detects multiple missing handlers"):
    val foundHandlers = List("ArrowRight", "ArrowLeft")  // Missing Space, Home, End, s, S
    val result = KeyboardValidator.validate(foundHandlers)

    assertEquals(result.handlersFound, foundHandlers)
    assert(result.handlersMissing.contains("Space"), "Should detect missing Space")
    assert(result.handlersMissing.contains("Home"), "Should detect missing Home")
    assert(result.handlersMissing.contains("End"), "Should detect missing End")
    assert(result.handlersMissing.contains("s"), "Should detect missing 's'")
    assert(result.handlersMissing.contains("S"), "Should detect missing 'S'")

end AccessibilitySpec
