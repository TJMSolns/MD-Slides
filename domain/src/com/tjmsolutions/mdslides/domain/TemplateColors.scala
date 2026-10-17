package com.tjmsolutions.mdslides.domain

/**
 * Template-specific color overrides
 *
 * Allows templates to override theme default colors for specific elements.
 * Unspecified elements inherit from theme defaults.
 *
 * Domain Model: doc/domain-models/aggregates/template-configuration-aggregate.md
 * Acceptance Criteria: doc/acceptance-criteria/template-configuration-acceptance-criteria.md (AC-4)
 *
 * Invariants:
 * - Color values must be valid CSS (hex, rgb, named colors)
 * - Optional - None means inherit from theme default colors
 */
case class TemplateColors(
  heading: Option[String],
  subtitle: Option[String],
  body: Option[String],
  author: Option[String]
):
  /**
   * Validate all color values
   *
   * @return Right(this) if all colors are valid, Left(error message) if any invalid
   */
  def validate(): Either[String, TemplateColors] =
    val invalidColors = Seq(
      heading.map("heading" -> _),
      subtitle.map("subtitle" -> _),
      body.map("body" -> _),
      author.map("author" -> _)
    ).flatten.filterNot { case (_, color) =>
      TemplateColors.isValidCSSColor(color)
    }

    if invalidColors.isEmpty then
      Right(this)
    else
      val errorMsg = invalidColors
        .map { case (field, color) => s"$field: $color" }
        .mkString("Invalid CSS color values: ", ", ", "")
      Left(errorMsg)

  /**
   * Merge template colors with theme defaults
   *
   * Template colors override theme defaults for specified elements.
   *
   * @param themeDefaults Map of theme default colors
   * @return Merged color map
   */
  def mergeWithDefaults(themeDefaults: Map[String, String]): Map[String, String] =
    var merged = themeDefaults

    heading.foreach(c => merged = merged.updated("heading", c))
    subtitle.foreach(c => merged = merged.updated("subtitle", c))
    body.foreach(c => merged = merged.updated("body", c))
    author.foreach(c => merged = merged.updated("author", c))

    merged

object TemplateColors:
  /**
   * Empty template colors (all None)
   */
  val empty: TemplateColors = TemplateColors(None, None, None, None)

  /**
   * Check if a string is a valid CSS color value
   *
   * Validates:
   * - Hex colors (#FFFFFF, #fff)
   * - RGB/RGBA (rgb(255, 255, 255), rgba(0, 0, 0, 0.5))
   * - HSL/HSLA (hsl(0, 100%, 50%), hsla(0, 100%, 50%, 0.5))
   * - Named colors (red, blue, transparent, etc.)
   *
   * @param color Color string to validate
   * @return true if valid CSS color, false otherwise
   */
  def isValidCSSColor(color: String): Boolean =
    // Hex color: #RRGGBB or #RGB
    val hexPattern = "^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{3})$".r

    // RGB/RGBA: rgb(r, g, b) or rgba(r, g, b, a)
    val rgbPattern = "^rgba?\\(\\s*\\d{1,3}\\s*,\\s*\\d{1,3}\\s*,\\s*\\d{1,3}\\s*(,\\s*[0-9.]+\\s*)?\\)$".r

    // HSL/HSLA: hsl(h, s%, l%) or hsla(h, s%, l%, a)
    val hslPattern = "^hsla?\\(\\s*\\d{1,3}\\s*,\\s*\\d{1,3}%\\s*,\\s*\\d{1,3}%\\s*(,\\s*[0-9.]+\\s*)?\\)$".r

    // Named colors (common CSS color names)
    val namedColors = Set(
      "transparent", "black", "white", "red", "green", "blue", "yellow", "cyan", "magenta",
      "orange", "purple", "pink", "brown", "gray", "grey", "silver", "gold", "navy",
      "teal", "lime", "aqua", "maroon", "olive", "fuchsia", "indigo", "violet"
    )

    val trimmed = color.trim.toLowerCase

    hexPattern.matches(trimmed) ||
    rgbPattern.matches(trimmed) ||
    hslPattern.matches(trimmed) ||
    namedColors.contains(trimmed)
