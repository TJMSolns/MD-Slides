package com.tjmsolutions.mdslides.domain

/**
 * Test suite for TemplateConfiguration aggregate
 *
 * Domain Model: doc/domain-models/aggregates/template-configuration-aggregate.md
 * Acceptance Criteria: doc/acceptance-criteria/template-configuration-acceptance-criteria.md
 */
class TemplateConfigurationSpec extends munit.FunSuite:

  val validTemplates = Seq("title", "content", "diagram", "closing", "section-title", "two-column")

  test("empty should create minimal configuration") {
    val config = TemplateConfiguration.empty("content")
    assertEquals(config.templateName, "content")
    assertEquals(config.verticalAlign, None)
    assertEquals(config.layout, None)
    assertEquals(config.colors, None)
    assertEquals(config.columnConfig, None)
    assertEquals(config.header, None)
    assertEquals(config.footer, None)
  }

  test("validateTemplateName should accept valid template names") {
    val config = TemplateConfiguration.empty("title")
    assertEquals(config.validateTemplateName(validTemplates), Right(config))
  }

  test("validateTemplateName should reject unknown template names") {
    val config = TemplateConfiguration.empty("custom-template")
    assert(config.validateTemplateName(validTemplates).isLeft)
  }

  test("validateLayoutConstraints should fail when two-column has no column config") {
    val config = TemplateConfiguration(
      templateName = "section-title",
      verticalAlign = None,
      layout = Some(TemplateLayout.TwoColumn),
      colors = None,
      columnConfig = None,  // Missing!
      header = None,
      footer = None
    )
    assert(config.validateLayoutConstraints().isLeft)
  }

  test("validateLayoutConstraints should pass when two-column has column config") {
    val columnConfig = ColumnConfiguration(
      leftColumn = ColumnSpec(width = Some("40%"), colors = None),
      rightColumn = ColumnSpec(width = Some("60%"), colors = None)
    )
    val config = TemplateConfiguration(
      templateName = "section-title",
      verticalAlign = None,
      layout = Some(TemplateLayout.TwoColumn),
      colors = None,
      columnConfig = Some(columnConfig),
      header = None,
      footer = None
    )
    assertEquals(config.validateLayoutConstraints(), Right(config))
  }

  test("checkUnusedColumnConfig should warn when config exists but layout is single-column") {
    val columnConfig = ColumnConfiguration(
      leftColumn = ColumnSpec(width = Some("40%"), colors = None),
      rightColumn = ColumnSpec(width = Some("60%"), colors = None)
    )
    val config = TemplateConfiguration(
      templateName = "content",
      verticalAlign = None,
      layout = Some(TemplateLayout.SingleColumn),
      colors = None,
      columnConfig = Some(columnConfig),
      header = None,
      footer = None
    )
    assert(config.checkUnusedColumnConfig().isDefined)
  }

  test("validate should accept complete valid configuration") {
    val colors = TemplateColors(
      heading = Some("#FFFFFF"),
      subtitle = Some("#CCCCCC"),
      body = None,
      author = None
    )
    val columnConfig = ColumnConfiguration(
      leftColumn = ColumnSpec(width = Some("40%"), colors = None),
      rightColumn = ColumnSpec(width = Some("60%"), colors = None)
    )
    val config = TemplateConfiguration(
      templateName = "section-title",
      verticalAlign = Some(VerticalAlignment.Center),
      layout = Some(TemplateLayout.TwoColumn),
      colors = Some(colors),
      columnConfig = Some(columnConfig),
      header = Some("Elapsed: {{timer}}"),
      footer = Some("{{pageNumber}}/{{totalPages}}")
    )
    assertEquals(config.validate(validTemplates), Right(config))
  }

  test("standardTemplateNames should include all expected templates") {
    val expected = Set("title", "content", "diagram", "closing", "section-title", "two-column")
    assertEquals(TemplateConfiguration.standardTemplateNames.toSet, expected)
  }
