package com.tjmsolutions.mdslides.infrastructure.theme

import com.tjmsolutions.mdslides.domain.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.parser.*

/**
 * Anticorruption Layer for Theme JSON parsing.
 *
 * Translates JSON theme files to domain Theme objects.
 * Isolates domain from Circe JSON library.
 *
 * Supported formats:
 * - Built-in themes: "light", "dark", "corporate"
 * - Custom JSON files following PDR-007 schema
 *
 * Validation:
 * - JSON structure validation (Circe)
 * - Required fields: name, version, background.color, colors, fonts, spacing
 * - Optional fields: background.image, background.opacity, etc.
 * - WCAG AA contrast validation (future: validate in domain/infrastructure)
 *
 * Related Governance:
 * - ADR-007: Anticorruption Layer
 * - ADR-010: Markdown Library Selection (same pattern for Flexmark)
 * - PDR-007: Theme JSON Schema
 * - US-008: Theme System
 * - US-009: Built-in Themes
 */
object ThemeJsonAdapter:

  /**
   * Parse JSON string to Theme.
   *
   * @param json JSON string following PDR-007 schema
   * @return Either error message or Theme
   */
  def parseTheme(json: String): Either[String, Theme] =
    decode[Theme](json).left.map(_.getMessage)

  /**
   * Predefined light theme (same as Theme.light).
   *
   * Preserves v0.1.0 default styling.
   */
  def light: Theme = Theme.light

  /**
   * Predefined dark theme (same as Theme.dark).
   *
   * Modern dark mode styling.
   */
  def dark: Theme = Theme.dark

  /**
   * Predefined corporate theme (same as Theme.corporate).
   *
   * Professional business styling with watermark support.
   */
  def corporate: Theme = Theme.corporate

  // ===== Circe Decoders =====

  /**
   * Decoder for Background - handles optional image fields.
   *
   * Default values:
   * - opacity: 1.0
   * - image, position, size: None
   */
  given Decoder[Background] = (c: HCursor) =>
    for
      color <- c.downField("color").as[String]
      image <- c.downField("image").as[Option[String]]
      opacity <- c.downField("opacity").as[Option[Double]].map(_.getOrElse(1.0))
      position <- c.downField("position").as[Option[String]]
      size <- c.downField("size").as[Option[String]]
    yield Background(color, image, opacity, position, size)

  /**
   * Decoder for ColorScheme - all fields required.
   */
  given Decoder[ColorScheme] = deriveDecoder[ColorScheme]

  /**
   * Decoder for FontScheme — body/heading/code required; googleFonts optional (default Nil).
   */
  given Decoder[FontScheme] = (c: HCursor) =>
    for
      body        <- c.downField("body").as[String]
      heading     <- c.downField("heading").as[String]
      code        <- c.downField("code").as[String]
      googleFonts <- c.downField("googleFonts").as[Option[List[String]]].map(_.getOrElse(Nil))
    yield FontScheme(body, heading, code, googleFonts)

  /**
   * Decoder for Spacing - all fields required.
   */
  given Decoder[Spacing] = deriveDecoder[Spacing]

  /**
   * Decoder for SyntaxColors - all fields required.
   */
  given Decoder[SyntaxColors] = deriveDecoder[SyntaxColors]

  /**
   * Decoder for SlideCounter - all fields required.
   */
  given Decoder[SlideCounter] = deriveDecoder[SlideCounter]

  /**
   * Decoder for VerticalAlignment - parses string to enum.
   */
  given Decoder[VerticalAlignment] = Decoder.decodeString.emap { str =>
    VerticalAlignment.fromString(str)
  }

  /**
   * Decoder for TemplateLayout - parses string to enum.
   */
  given Decoder[TemplateLayout] = Decoder.decodeString.emap { str =>
    TemplateLayout.fromString(str)
  }

  /**
   * Decoder for TemplateColors - optional color fields.
   */
  given Decoder[TemplateColors] = (c: HCursor) =>
    for
      heading <- c.downField("heading").as[Option[String]]
      subtitle <- c.downField("subtitle").as[Option[String]]
      body <- c.downField("body").as[Option[String]]
      author <- c.downField("author").as[Option[String]]
    yield TemplateColors(heading, subtitle, body, author)

  /**
   * Decoder for ColumnSpec - optional width and colors.
   */
  given Decoder[ColumnSpec] = (c: HCursor) =>
    for
      width <- c.downField("width").as[Option[String]]
      colors <- c.downField("colors").as[Option[TemplateColors]]
    yield ColumnSpec(width, colors)

  /**
   * Decoder for ColumnConfiguration - left and right column specs.
   */
  given Decoder[ColumnConfiguration] = (c: HCursor) =>
    for
      leftColumn <- c.downField("leftColumn").as[ColumnSpec]
      rightColumn <- c.downField("rightColumn").as[ColumnSpec]
    yield ColumnConfiguration(leftColumn, rightColumn)

  /**
   * Decoder for TemplateConfiguration - all fields optional except templateName.
   */
  given Decoder[TemplateConfiguration] = (c: HCursor) =>
    for
      templateName <- c.downField("templateName").as[Option[String]].map(_.getOrElse(""))
      verticalAlign <- c.downField("verticalAlign").as[Option[VerticalAlignment]]
      layout <- c.downField("layout").as[Option[TemplateLayout]]
      colors <- c.downField("colors").as[Option[TemplateColors]]
      columnConfig <- c.downField("columnConfig").as[Option[ColumnConfiguration]]
      header <- c.downField("header").as[Option[String]]
      footer <- c.downField("footer").as[Option[String]]
    yield TemplateConfiguration(templateName, verticalAlign, layout, colors, columnConfig, header, footer)

  /**
   * Decoder for Theme - top-level aggregate.
   *
   * Handles optional templateBackgrounds field (US-012) and templateConfig.
   */
  given Decoder[Theme] = (c: HCursor) =>
    for
      name <- c.downField("name").as[String]
      version <- c.downField("version").as[String]
      background <- c.downField("background").as[Background]
      colors <- c.downField("colors").as[ColorScheme]
      fonts <- c.downField("fonts").as[FontScheme]
      spacing <- c.downField("spacing").as[Spacing]
      syntax <- c.downField("syntax").as[SyntaxColors]
      slideCounter <- c.downField("slideCounter").as[SlideCounter]
      templateBackgrounds <- c.downField("templateBackgrounds").as[Option[Map[String, String]]].map(_.getOrElse(Map.empty))
      templateConfig <- c.downField("templateConfig").as[Option[Map[String, TemplateConfiguration]]].map { optMap =>
        optMap.getOrElse(Map.empty).map { case (templateName, config) =>
          // Set templateName from map key if not already set
          (templateName, if config.templateName.isEmpty then config.copy(templateName = templateName) else config)
        }
      }
    yield Theme(name, version, background, colors, fonts, spacing, syntax, slideCounter, templateBackgrounds, templateConfig)

end ThemeJsonAdapter
