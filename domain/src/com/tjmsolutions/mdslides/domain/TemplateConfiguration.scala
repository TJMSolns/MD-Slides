package com.tjmsolutions.mdslides.domain

/**
 * Template Configuration Aggregate
 *
 * Theme-level defaults for a specific template type. Enables theme authors to configure
 * presentation-specific overrides for individual slide templates (title, content, diagram, etc.).
 *
 * Domain Model: doc/domain-models/aggregates/template-configuration-aggregate.md
 * Acceptance Criteria: doc/acceptance-criteria/template-configuration-acceptance-criteria.md
 *
 * Configuration Hierarchy (BR-1):
 * 1. Slide frontmatter (highest priority)
 * 2. Theme template configuration
 * 3. Global configuration
 * 4. Theme defaults (lowest priority)
 *
 * Invariants:
 * - Template name must be valid (BR-5)
 * - Two-column layout requires column config (BR-2)
 * - Color values must be valid CSS (BR-3)
 * - Vertical alignment must be valid
 */
case class TemplateConfiguration(
  templateName: String,              // "title", "content", "diagram", etc.
  verticalAlign: Option[VerticalAlignment],
  layout: Option[TemplateLayout],
  colors: Option[TemplateColors],
  columnConfig: Option[ColumnConfiguration],  // For two-column layouts
  header: Option[String],            // Theme default header template for this template type
  footer: Option[String]             // Theme default footer template for this template type
):
  /**
   * Validate template name against known template names (BR-5)
   *
   * @param validTemplateNames Set of valid template names
   * @return Right(this) if valid, Left(error message) if invalid
   */
  def validateTemplateName(validTemplateNames: Seq[String]): Either[String, TemplateConfiguration] =
    if validTemplateNames.contains(templateName) then
      Right(this)
    else
      Left(s"Unknown template: $templateName. Valid templates: ${validTemplateNames.mkString(", ")}")

  /**
   * Validate that two-column layout has column config (BR-2)
   *
   * @return Right(this) if valid, Left(error message) if invalid
   */
  def validateLayoutConstraints(): Either[String, TemplateConfiguration] =
    layout match
      case Some(TemplateLayout.TwoColumn) if columnConfig.isEmpty =>
        Left(s"Two-column layout requires columnConfig for template: $templateName")
      case _ =>
        Right(this)

  /**
   * Check for unused column config (warning, not error)
   *
   * @return Some(warning) if column config exists but layout is not two-column, None otherwise
   */
  def checkUnusedColumnConfig(): Option[String] =
    (layout, columnConfig) match
      case (Some(TemplateLayout.SingleColumn), Some(_)) =>
        Some("Column config specified but layout is not two-column (will be ignored)")
      case (None, Some(_)) =>
        Some("Column config specified but layout is not two-column (will be ignored)")
      case _ =>
        None

  /**
   * Validate all constraints
   *
   * @param validTemplateNames Set of valid template names
   * @return Right(this) if valid, Left(error message) if invalid
   */
  def validate(validTemplateNames: Seq[String]): Either[String, TemplateConfiguration] =
    for
      _ <- validateTemplateName(validTemplateNames)
      _ <- validateLayoutConstraints()
      _ <- colors.map(_.validate()).getOrElse(Right(()))
      _ <- columnConfig.map(_.validate()).getOrElse(Right(()))
    yield this

object TemplateConfiguration:
  /**
   * Valid template names (standard templates)
   */
  val standardTemplateNames: Seq[String] = Seq(
    "title",
    "content",
    "diagram",
    "closing",
    "section-title",
    "two-column"
  )

  /**
   * Create empty template configuration (all None except template name)
   *
   * @param templateName Template name
   * @return Empty configuration
   */
  def empty(templateName: String): TemplateConfiguration =
    TemplateConfiguration(
      templateName = templateName,
      verticalAlign = None,
      layout = None,
      colors = None,
      columnConfig = None,
      header = None,
      footer = None
    )
