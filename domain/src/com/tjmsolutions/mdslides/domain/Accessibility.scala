package com.tjmsolutions.mdslides.domain

/**
 * WCAG 2.1 contrast ratio calculator.
 *
 * Implements the WCAG 2.1 formula for calculating contrast ratios between colors.
 * Used for accessibility validation to ensure text is readable.
 *
 * WCAG 2.1 Requirements:
 * - Normal text: contrast ratio ≥ 4.5:1 (AA level)
 * - Large text (18pt+): contrast ratio ≥ 3:1 (AA level)
 *
 * Related Governance:
 * - US-014: Accessibility Validation
 * - WCAG 2.1 Success Criterion 1.4.3 (Contrast - Minimum)
 */
object ContrastCalculator:

  /**
   * Calculate relative luminance of a color per WCAG 2.1.
   *
   * Formula from: https://www.w3.org/TR/WCAG21/#dfn-relative-luminance
   *
   * For sRGB colors:
   * 1. Convert hex to RGB (0-255)
   * 2. Normalize to 0-1 range
   * 3. Apply gamma correction
   * 4. Calculate luminance = 0.2126*R + 0.7152*G + 0.0722*B
   *
   * @param hexColor Color in hex format (e.g., "#FFFFFF" or "FFFFFF")
   * @return Relative luminance (0.0 = black, 1.0 = white)
   */
  def relativeLuminance(hexColor: String): Double =
    // Remove # prefix if present
    val hex = hexColor.stripPrefix("#")

    // Parse RGB components (0-255)
    val r = Integer.parseInt(hex.substring(0, 2), 16)
    val g = Integer.parseInt(hex.substring(2, 4), 16)
    val b = Integer.parseInt(hex.substring(4, 6), 16)

    // Normalize to 0-1 range
    val rNorm = r / 255.0
    val gNorm = g / 255.0
    val bNorm = b / 255.0

    // Apply gamma correction (sRGB)
    def gammaCorrect(channel: Double): Double =
      if channel <= 0.03928 then
        channel / 12.92
      else
        math.pow((channel + 0.055) / 1.055, 2.4)

    val rLinear = gammaCorrect(rNorm)
    val gLinear = gammaCorrect(gNorm)
    val bLinear = gammaCorrect(bNorm)

    // Calculate relative luminance
    0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear

  /**
   * Calculate contrast ratio between two colors per WCAG 2.1.
   *
   * Formula from: https://www.w3.org/TR/WCAG21/#dfn-contrast-ratio
   *
   * ratio = (L1 + 0.05) / (L2 + 0.05)
   * where L1 is the lighter color's luminance and L2 is the darker
   *
   * @param foreground Foreground color (text color)
   * @param background Background color
   * @return Contrast ratio (1:1 to 21:1, where 21:1 is maximum)
   */
  def contrastRatio(foreground: String, background: String): Double =
    val l1 = relativeLuminance(foreground)
    val l2 = relativeLuminance(background)

    // Ensure L1 is the lighter color (higher luminance)
    val lighter = math.max(l1, l2)
    val darker = math.min(l1, l2)

    (lighter + 0.05) / (darker + 0.05)

end ContrastCalculator

/**
 * Value object representing a contrast ratio with WCAG validation.
 *
 * @param ratio The contrast ratio (1:1 to 21:1)
 */
case class ContrastRatio(ratio: Double):
  require(ratio >= 1.0 && ratio <= 21.0, s"Contrast ratio must be between 1:1 and 21:1, got $ratio")

  /**
   * Check if contrast ratio passes WCAG 2.1 AA requirements.
   *
   * @param normalText true for normal text (requires 4.5:1), false for large text (requires 3:1)
   * @return true if ratio meets or exceeds threshold
   */
  def passesWCAG_AA(normalText: Boolean): Boolean =
    val threshold = if normalText then 4.5 else 3.0
    ratio >= threshold

  /**
   * Format ratio for display (e.g., "4.5:1")
   */
  def formatted: String = f"$ratio%.2f:1"

/**
 * Accessibility warning for non-compliant content.
 *
 * @param category Warning category (contrast, alt-text, keyboard, etc.)
 * @param message Human-readable warning message
 * @param wcagCriterion WCAG 2.1 success criterion (e.g., "1.4.3")
 */
case class AccessibilityWarning(
  category: String,
  message: String,
  wcagCriterion: String
)

/**
 * Accessibility validation report for a presentation.
 *
 * @param themeName Name of theme being validated
 * @param wcagLevel WCAG level being tested ("AA" or "AAA")
 * @param contrastChecks Results of contrast ratio checks
 * @param altTextChecks Results of alt text validation
 * @param keyboardChecks Results of keyboard navigation audit
 * @param warnings List of accessibility warnings
 */
case class AccessibilityReport(
  themeName: String,
  wcagLevel: String,
  contrastChecks: Map[String, ContrastRatio],
  altTextChecks: AltTextValidation,
  keyboardChecks: KeyboardValidation,
  warnings: List[AccessibilityWarning]
):
  /**
   * Check if all accessibility checks passed (no warnings).
   */
  def passes: Boolean = warnings.isEmpty

/**
 * Results of alt text validation.
 *
 * @param imagesChecked Total number of images validated
 * @param imagesMissingAlt Number of images with missing/empty alt text
 */
case class AltTextValidation(
  imagesChecked: Int,
  imagesMissingAlt: Int
)

/**
 * Results of keyboard navigation audit.
 *
 * @param handlersFound List of keyboard handlers found in HTML
 * @param handlersMissing List of expected handlers that are missing
 */
case class KeyboardValidation(
  handlersFound: List[String],
  handlersMissing: List[String]
)

/**
 * Theme contrast validator.
 *
 * Validates WCAG 2.1 AA contrast requirements for a theme's color scheme.
 * Checks text, heading, link, and accent colors against background.
 *
 * @param theme The theme to validate
 */
case class ThemeValidator(theme: Theme):

  /**
   * Validate all theme color contrasts against WCAG 2.1 AA.
   *
   * Checks:
   * - Text color vs background color (4.5:1 for normal text)
   * - Heading color vs background color (4.5:1 for normal text)
   * - Link color vs background color (4.5:1 for normal text)
   * - Accent color vs background color (4.5:1 for normal text)
   *
   * Does NOT check:
   * - Syntax highlighting colors (external from highlight.js)
   * - Background images (can't validate image content)
   *
   * @return Validation result with contrast ratios and warnings
   */
  def validateContrast(): ContrastValidationResult =
    val background = theme.background.color
    val warnings = scala.collection.mutable.ListBuffer.empty[AccessibilityWarning]
    val ratios = scala.collection.mutable.Map.empty[String, ContrastRatio]

    // Check text/background contrast
    val textRatio = ContrastCalculator.contrastRatio(theme.colors.text, background)
    ratios("text_background") = ContrastRatio(textRatio)
    if textRatio < 4.5 then
      warnings += AccessibilityWarning(
        category = "contrast",
        message = s"Low contrast ratio for text: ${f"$textRatio%.2f"}:1 (requires 4.5:1 for normal text)",
        wcagCriterion = "1.4.3"
      )

    // Check heading/background contrast
    val headingRatio = ContrastCalculator.contrastRatio(theme.colors.heading, background)
    ratios("heading_background") = ContrastRatio(headingRatio)
    if headingRatio < 4.5 then
      warnings += AccessibilityWarning(
        category = "contrast",
        message = s"Low contrast ratio for heading: ${f"$headingRatio%.2f"}:1 (requires 4.5:1 for normal text)",
        wcagCriterion = "1.4.3"
      )

    // Check link/background contrast
    val linkRatio = ContrastCalculator.contrastRatio(theme.colors.link, background)
    ratios("link_background") = ContrastRatio(linkRatio)
    if linkRatio < 4.5 then
      warnings += AccessibilityWarning(
        category = "contrast",
        message = s"Low contrast ratio for link: ${f"$linkRatio%.2f"}:1 (requires 4.5:1 for normal text)",
        wcagCriterion = "1.4.3"
      )

    // Check accent/background contrast
    val accentRatio = ContrastCalculator.contrastRatio(theme.colors.accent, background)
    ratios("accent_background") = ContrastRatio(accentRatio)
    if accentRatio < 4.5 then
      warnings += AccessibilityWarning(
        category = "contrast",
        message = s"Low contrast ratio for accent: ${f"$accentRatio%.2f"}:1 (requires 4.5:1 for normal text)",
        wcagCriterion = "1.4.3"
      )

    ContrastValidationResult(
      ratios = ratios.toMap,
      warnings = warnings.toList
    )

end ThemeValidator

/**
 * Result of theme contrast validation.
 *
 * @param ratios Map of color pair names to contrast ratios
 * @param warnings List of accessibility warnings for failing contrasts
 */
case class ContrastValidationResult(
  ratios: Map[String, ContrastRatio],
  warnings: List[AccessibilityWarning]
):
  /**
   * Check if all contrast checks passed (no warnings).
   */
  def passes: Boolean = warnings.isEmpty

/**
 * Alt text validator for images.
 *
 * Validates that all images have non-empty alt text per WCAG 2.1 Success Criterion 1.1.1.
 * This is a redundant check since domain validation already enforces alt text,
 * but provides comprehensive reporting for accessibility audits.
 *
 * Related Governance:
 * - US-014: Accessibility Validation
 * - WCAG 2.1 Success Criterion 1.1.1 (Non-text Content)
 */
object AltTextValidator:
  /**
   * Validate alt text for all images in a presentation.
   *
   * @param images List of images to validate
   * @return Validation result with counts of checked and missing alt text
   */
  def validate(images: List[ContentImage]): AltTextValidation =
    val missing = images.count(img => img.altText.trim.isEmpty)
    AltTextValidation(
      imagesChecked = images.length,
      imagesMissingAlt = missing
    )

/**
 * Keyboard navigation validator.
 *
 * Validates that generated HTML contains all required keyboard event handlers
 * per WCAG 2.1 Success Criterion 2.1.1 (Keyboard).
 *
 * Required keyboard handlers for MDSlides presentations:
 * - ArrowRight: Next slide
 * - ArrowLeft: Previous slide
 * - Space: Next slide
 * - Home: First slide
 * - End: Last slide
 * - s: Open speaker view (lowercase)
 * - S: Open speaker view (uppercase)
 *
 * Related Governance:
 * - US-014: Accessibility Validation
 * - WCAG 2.1 Success Criterion 2.1.1 (Keyboard)
 * - BUG-002: Missing uppercase 'S' handler (fixed in v1.3.1)
 */
object KeyboardValidator:
  /**
   * Required keyboard handlers for full accessibility.
   */
  val requiredHandlers: List[String] = List(
    "ArrowRight",
    "ArrowLeft",
    "Space",
    "Home",
    "End",
    "s",
    "S"
  )

  /**
   * Validate keyboard navigation handlers.
   *
   * @param foundHandlers List of keyboard handlers found in generated HTML
   * @return Validation result with found and missing handlers
   */
  def validate(foundHandlers: List[String]): KeyboardValidation =
    val missing = requiredHandlers.filterNot(foundHandlers.contains)
    KeyboardValidation(
      handlersFound = foundHandlers,
      handlersMissing = missing
    )
