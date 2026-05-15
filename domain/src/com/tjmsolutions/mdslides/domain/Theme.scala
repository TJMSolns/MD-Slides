package com.tjmsolutions.mdslides.domain

/**
 * Theme aggregate - customizes visual appearance of presentations.
 *
 * Themes are loaded from JSON files and applied during rendering.
 * Supports colors, fonts, spacing, background images, and code syntax highlighting.
 *
 * Invariants:
 * - Name must be non-empty (1-50 chars)
 * - Version must follow semver format
 * - Background opacity must be 0.0 - 1.0
 * - Colors must meet WCAG AA contrast requirements (validated in infrastructure)
 * - templateBackgrounds is a map from template name to background image path
 * - templateConfig is a map from template name to TemplateConfiguration
 *
 * Related Governance:
 * - US-008: Theme System
 * - US-009: Built-in Themes
 * - US-012: Template-Specific Background Defaults
 * - PDR-007: Theme JSON Schema
 * - PDR-011: Background Image Architecture
 * - PDR-005: Accessibility Requirements (WCAG AA)
 * - POL-003: Pure Functional Domain (no I/O here, JSON parsing in infrastructure)
 */
case class Theme(
  name: String,
  version: String,
  background: Background,
  colors: ColorScheme,
  fonts: FontScheme,
  spacing: Spacing,
  syntax: SyntaxColors,
  slideCounter: SlideCounter,
  templateBackgrounds: Map[String, String] = Map.empty,
  templateConfig: Map[String, TemplateConfiguration] = Map.empty
)

/**
 * Background configuration - color and optional image.
 *
 * Background images support corporate branding (e.g., watermark logos).
 */
case class Background(
  color: String,
  image: Option[String] = None,
  opacity: Double = 1.0,
  position: Option[String] = None,
  size: Option[String] = None
)

/**
 * Color scheme - text, heading, accent, link, code colors.
 *
 * All colors must meet WCAG AA contrast requirements:
 * - text vs background: ≥ 4.5:1
 * - heading vs background: ≥ 4.5:1
 * - link vs background: ≥ 4.5:1
 *
 * Validation happens in infrastructure layer (ThemeJsonAdapter).
 */
case class ColorScheme(
  text: String,
  heading: String,
  accent: String,
  link: String,
  linkHover: String,
  codeBackground: String,
  codeText: String
)

/**
 * Font scheme - body, heading, and code font stacks.
 *
 * Font stacks follow CSS format: "Family, Fallback, Generic"
 * Example: "'Helvetica Neue', Helvetica, Arial, sans-serif"
 */
case class FontScheme(
  body: String,
  heading: String,
  code: String,
  googleFonts: List[String] = Nil
)

/**
 * Spacing configuration - margins, padding, line height.
 *
 * Uses CSS units: rem, em, px, etc.
 */
case class Spacing(
  slideMargin: String,
  headingMargin: String,
  paragraphMargin: String,
  lineHeight: String
)

/**
 * Syntax highlighting colors for code blocks.
 *
 * Applies to code blocks (US-004, future feature).
 */
case class SyntaxColors(
  keyword: String,
  string: String,
  comment: String,
  function: String,
  number: String,
  operator: String
)

/**
 * Slide counter styling - "1 / 10" indicator in bottom-right.
 */
case class SlideCounter(
  color: String,
  background: String,
  fontSize: String
)

/**
 * Theme companion object - default themes and factory methods.
 */
object Theme:

  /**
   * Light theme - preserves v0.1.0 default styling.
   *
   * Professional, clean, high contrast.
   */
  val light: Theme = Theme(
    name = "light",
    version = "1.0.0",
    background = Background(color = "#ffffff"),
    colors = ColorScheme(
      text = "#333333",
      heading = "#2c3e50",
      accent = "#0066CC",  // Changed from #3498db - WCAG AA compliant (4.54:1 contrast)
      link = "#0066CC",    // Changed from #3498db - WCAG AA compliant (4.54:1 contrast)
      linkHover = "#004C99",  // Changed from #2980b9 - darker for hover
      codeBackground = "#f4f4f4",
      codeText = "#333333"
    ),
    fonts = FontScheme(
      body = "Arial, Helvetica, sans-serif",
      heading = "'Helvetica Neue', Helvetica, Arial, sans-serif",
      code = "Consolas, 'Courier New', monospace"
    ),
    spacing = Spacing(
      slideMargin = "2rem",
      headingMargin = "1rem 0",
      paragraphMargin = "0.5rem 0",
      lineHeight = "1.6"
    ),
    syntax = SyntaxColors(
      keyword = "#569cd6",
      string = "#ce9178",
      comment = "#6a9955",
      function = "#dcdcaa",
      number = "#b5cea8",
      operator = "#d4d4d4"
    ),
    slideCounter = SlideCounter(
      color = "#666666",
      background = "rgba(255, 255, 255, 0.9)",
      fontSize = "0.9rem"
    )
  )

  /**
   * Dark theme - dark background, light text.
   *
   * Modern, eye-friendly for low-light environments.
   */
  val dark: Theme = Theme(
    name = "dark",
    version = "1.0.0",
    background = Background(color = "#1e1e1e"),
    colors = ColorScheme(
      text = "#d4d4d4",
      heading = "#4ec9b0",
      accent = "#569cd6",
      link = "#4ec9b0",
      linkHover = "#3aa89a",
      codeBackground = "#2d2d2d",
      codeText = "#d4d4d4"
    ),
    fonts = FontScheme(
      body = "Arial, Helvetica, sans-serif",
      heading = "'Helvetica Neue', Helvetica, Arial, sans-serif",
      code = "Consolas, 'Courier New', monospace"
    ),
    spacing = Spacing(
      slideMargin = "2rem",
      headingMargin = "1rem 0",
      paragraphMargin = "0.5rem 0",
      lineHeight = "1.6"
    ),
    syntax = SyntaxColors(
      keyword = "#569cd6",
      string = "#ce9178",
      comment = "#6a9955",
      function = "#dcdcaa",
      number = "#b5cea8",
      operator = "#d4d4d4"
    ),
    slideCounter = SlideCounter(
      color = "#cccccc",
      background = "rgba(30, 30, 30, 0.9)",
      fontSize = "0.9rem"
    )
  )

  /**
   * Corporate theme - professional with watermark support.
   *
   * Formal, suitable for business presentations.
   * Shows background image usage.
   */
  val corporate: Theme = Theme(
    name = "corporate",
    version = "1.0.0",
    background = Background(
      color = "#f8f9fa",
      image = None,  // No background image (logo-watermark.png doesn't exist)
      opacity = 0.05,
      position = Some("bottom right"),
      size = Some("200px auto")
    ),
    colors = ColorScheme(
      text = "#212529",
      heading = "#0056b3",
      accent = "#007bff",
      link = "#007bff",
      linkHover = "#0056b3",
      codeBackground = "#e9ecef",
      codeText = "#212529"
    ),
    fonts = FontScheme(
      body = "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif",
      heading = "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif",
      code = "'Courier New', Courier, monospace"
    ),
    spacing = Spacing(
      slideMargin = "3rem",
      headingMargin = "1.5rem 0 1rem 0",
      paragraphMargin = "0.75rem 0",
      lineHeight = "1.7"
    ),
    syntax = SyntaxColors(
      keyword = "#0056b3",
      string = "#28a745",
      comment = "#6c757d",
      function = "#007bff",
      number = "#e83e8c",
      operator = "#212529"
    ),
    slideCounter = SlideCounter(
      color = "#495057",
      background = "rgba(248, 249, 250, 0.95)",
      fontSize = "0.85rem"
    )
  )

  /**
   * Default theme - same as light.
   */
  def default: Theme = light

end Theme
