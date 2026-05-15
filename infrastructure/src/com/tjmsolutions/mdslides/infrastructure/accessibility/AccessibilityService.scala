package com.tjmsolutions.mdslides.infrastructure.accessibility

import com.tjmsolutions.mdslides.domain.*
import scala.io.Source
import java.nio.file.{Files, Path}

/**
 * Infrastructure service for WCAG 2.1 AA accessibility validation.
 *
 * Coordinates all accessibility checks:
 * - Theme contrast validation (domain)
 * - Alt text validation (domain)
 * - Keyboard navigation audit (HTML parsing + domain validation)
 *
 * Related Governance:
 * - US-014: Accessibility Validation
 * - Example Mapping: accessibility-validation.md
 */
class AccessibilityService:

  /**
   * Validate accessibility for a rendered presentation.
   *
   * @param theme Theme used for rendering
   * @param images List of images in presentation
   * @param htmlPath Path to generated index.html file
   * @return Complete accessibility report
   */
  def validate(
    theme: Theme,
    images: List[ContentImage],
    htmlPath: Path
  ): AccessibilityReport =

    // Validate theme contrast
    val themeValidator = ThemeValidator(theme)
    val contrastResult = themeValidator.validateContrast()

    // Validate alt text
    val altTextResult = AltTextValidator.validate(images)

    // Validate keyboard navigation (parse HTML)
    val keyboardResult = validateKeyboardHandlers(htmlPath)

    // Collect all warnings
    val warnings = scala.collection.mutable.ListBuffer.empty[AccessibilityWarning]

    // Add contrast warnings
    warnings ++= contrastResult.warnings

    // Add alt text warnings
    if altTextResult.imagesMissingAlt > 0 then
      warnings += AccessibilityWarning(
        category = "alt-text",
        message = s"${altTextResult.imagesMissingAlt} of ${altTextResult.imagesChecked} images missing alt text",
        wcagCriterion = "1.1.1"
      )

    // Add keyboard navigation warnings
    if keyboardResult.handlersMissing.nonEmpty then
      keyboardResult.handlersMissing.foreach { handler =>
        warnings += AccessibilityWarning(
          category = "keyboard",
          message = s"Missing keyboard handler for '$handler' key",
          wcagCriterion = "2.1.1"
        )
      }

    AccessibilityReport(
      themeName = theme.name,
      wcagLevel = "AA",
      contrastChecks = contrastResult.ratios,
      altTextChecks = altTextResult,
      keyboardChecks = keyboardResult,
      warnings = warnings.toList
    )

  /**
   * Parse generated HTML to extract keyboard event handlers.
   *
   * Looks for event.key comparisons in <script> tags to find which
   * keyboard handlers are implemented.
   *
   * @param htmlPath Path to generated HTML file
   * @return Keyboard validation result
   */
  private def validateKeyboardHandlers(htmlPath: Path): KeyboardValidation =
    if !Files.exists(htmlPath) then
      // If HTML doesn't exist yet, return all handlers as missing
      return KeyboardValidation(
        handlersFound = List.empty,
        handlersMissing = KeyboardValidator.requiredHandlers
      )

    val htmlContent = Files.readString(htmlPath)

    // Extract keyboard handlers from HTML
    // Look for patterns like: event.key === 'ArrowRight'
    // or: case 'ArrowRight':
    // or: case ' ': (space key as literal space character)
    val foundHandlers = scala.collection.mutable.Set.empty[String]

    // Pattern 1: event.key === 'KEY' or event.key == 'KEY'
    val eventKeyPattern = """event\.key\s*===?\s*['"]([^'"]+)['"]""".r
    eventKeyPattern.findAllMatchIn(htmlContent).foreach { m =>
      foundHandlers += m.group(1)
    }

    // Pattern 2: case 'KEY':
    val casePattern = """case\s+['"]([^'"]+)['"]\s*:""".r
    casePattern.findAllMatchIn(htmlContent).foreach { m =>
      val key = m.group(1)
      // Normalize: literal space character ' ' becomes 'Space'
      if key == " " then
        foundHandlers += "Space"
      else
        foundHandlers += key
    }

    // Validate using domain validator
    KeyboardValidator.validate(foundHandlers.toList)

end AccessibilityService
