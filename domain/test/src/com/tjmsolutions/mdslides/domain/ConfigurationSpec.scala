package com.tjmsolutions.mdslides.domain

import munit.FunSuite

/**
 * Tests for Configuration Management (v2.0.0).
 *
 * Tests verify:
 * - Configuration merging (4-layer precedence: CLI > project > global > defaults)
 * - Field validation (supported fields, correct types)
 * - Scoping rules (global-only vs project-only fields)
 *
 * Related Governance:
 * - v2.0.0: Configuration Management
 * - Example Mapping: Scenarios 32-47
 */
class ConfigurationSpec extends FunSuite:

  // Example Mapping Scenario 41: Full precedence chain
  test("configuration precedence: CLI > project > global > defaults"):
    // Hard-coded default
    val defaults = DefaultSettings(theme = "light", copyImages = true, skipAccessibility = false, breakScreen = None)

    // Global config
    val global = Some(GlobalConfig(
      defaults = Some(DefaultSettings(theme = "dark", copyImages = true, skipAccessibility = false, breakScreen = Some("~/default-break.png"))),
      author = None,
      paths = None
    ))

    // Project config
    val project = Some(ProjectConfig(
      theme = Some("corporate"),
      copyImages = None,
      skipAccessibility = None,
      accessibilityReportPath = None,
      outputDir = None,
      header = None,
      footer = None,
      breakScreen = None
    ))

    // CLI arguments
    val cli = CLIOverrides(
      theme = Some("retisio"),
      copyImages = None,
      skipAccessibility = None,
      accessibilityReportPath = None,
      breakScreen = None
    )

    // Merge all layers
    val merged = ConfigurationMerger.merge(defaults, global, project, cli)

    // CLI wins for theme
    assertEquals(merged.theme, "retisio")
    // No project or CLI override for copyImages, so global wins (true)
    assertEquals(merged.copyImages, true)
    // No overrides for skipAccessibility, so defaults win (false)
    assertEquals(merged.skipAccessibility, false)

  // Example Mapping Scenario 38: CLI overrides project config
  test("CLI argument overrides project config"):
    val defaults = DefaultSettings(theme = "light", copyImages = true, skipAccessibility = false, breakScreen = None)
    val project = Some(ProjectConfig(theme = Some("dark"), copyImages = None, skipAccessibility = None, accessibilityReportPath = None, outputDir = None, header = None, footer = None, breakScreen = None))
    val cli = CLIOverrides(theme = Some("light"), copyImages = None, skipAccessibility = None, accessibilityReportPath = None, breakScreen = None)

    val merged = ConfigurationMerger.merge(defaults, None, project, cli)

    assertEquals(merged.theme, "light") // CLI wins

  // Example Mapping Scenario 39: Project overrides global config
  test("project config overrides global config"):
    val defaults = DefaultSettings(theme = "light", copyImages = true, skipAccessibility = false, breakScreen = None)
    val global = Some(GlobalConfig(
      defaults = Some(DefaultSettings(theme = "light", copyImages = true, skipAccessibility = false, breakScreen = None)),
      author = None,
      paths = None
    ))
    val project = Some(ProjectConfig(theme = Some("dark"), copyImages = None, skipAccessibility = None, accessibilityReportPath = None, outputDir = None, header = None, footer = None, breakScreen = None))
    val cli = CLIOverrides(theme = None, copyImages = None, skipAccessibility = None, accessibilityReportPath = None, breakScreen = None)

    val merged = ConfigurationMerger.merge(defaults, global, project, cli)

    assertEquals(merged.theme, "dark") // Project wins

  // Example Mapping Scenario 40: Global overrides hard-coded defaults
  test("global config overrides hard-coded defaults"):
    val defaults = DefaultSettings(theme = "light", copyImages = true, skipAccessibility = false, breakScreen = None)
    val global = Some(GlobalConfig(
      defaults = Some(DefaultSettings(theme = "dark", copyImages = false, skipAccessibility = true, breakScreen = None)),
      author = None,
      paths = None
    ))
    val cli = CLIOverrides(theme = None, copyImages = None, skipAccessibility = None, accessibilityReportPath = None, breakScreen = None)

    val merged = ConfigurationMerger.merge(defaults, global, None, cli)

    assertEquals(merged.theme, "dark") // Global wins
    assertEquals(merged.copyImages, false) // Global wins
    assertEquals(merged.skipAccessibility, true) // Global wins

  // Example Mapping Scenario 45: All config fields supported
  test("all configuration fields are supported"):
    val global = GlobalConfig(
      defaults = Some(DefaultSettings(theme = "dark", copyImages = false, skipAccessibility = true, breakScreen = None)),
      author = Some(Author(name = "Tony Moores", email = "tony@tjmsolutions.com")),
      paths = Some(Paths(themesDir = "~/my-themes"))
    )

    assertEquals(global.defaults.get.theme, "dark")
    assertEquals(global.defaults.get.copyImages, false)
    assertEquals(global.defaults.get.skipAccessibility, true)
    assertEquals(global.author.get.name, "Tony Moores")
    assertEquals(global.author.get.email, "tony@tjmsolutions.com")
    assertEquals(global.paths.get.themesDir, "~/my-themes")

  // Example Mapping Scenario: Project config fields
  test("project config supports project-specific fields"):
    val project = ProjectConfig(
      theme = Some("corporate"),
      copyImages = Some(false),
      skipAccessibility = Some(true),
      accessibilityReportPath = Some("report.json"),
      outputDir = Some("dist"),
      header = None,
      footer = None,
      breakScreen = None
    )

    assertEquals(project.theme.get, "corporate")
    assertEquals(project.copyImages.get, false)
    assertEquals(project.skipAccessibility.get, true)
    assertEquals(project.accessibilityReportPath.get, "report.json")
    assertEquals(project.outputDir.get, "dist")

  // Scenario: CLI overrides
  test("CLI overrides support all overridable fields"):
    val cli = CLIOverrides(
      theme = Some("retisio"),
      copyImages = Some(false),
      skipAccessibility = Some(true),
      accessibilityReportPath = Some("a11y.json"),
      breakScreen = None
    )

    assertEquals(cli.theme.get, "retisio")
    assertEquals(cli.copyImages.get, false)
    assertEquals(cli.skipAccessibility.get, true)
    assertEquals(cli.accessibilityReportPath.get, "a11y.json")

  // Scenario: Merged config with all fields
  test("merged configuration combines all sources"):
    val defaults = DefaultSettings(theme = "light", copyImages = true, skipAccessibility = false, breakScreen = None)
    val global = Some(GlobalConfig(
      defaults = Some(DefaultSettings(theme = "dark", copyImages = true, skipAccessibility = false, breakScreen = Some("~/global-break.png"))),
      author = Some(Author(name = "Tony", email = "tony@example.com")),
      paths = Some(Paths(themesDir = "~/themes"))
    ))
    val project = Some(ProjectConfig(
      theme = Some("corporate"),
      copyImages = Some(false),
      skipAccessibility = None,
      accessibilityReportPath = Some("report.json"),
      outputDir = Some("dist"),
      header = None,
      footer = None,
      breakScreen = Some("images/project-break.png")
    ))
    val cli = CLIOverrides(theme = None, copyImages = None, skipAccessibility = Some(true), accessibilityReportPath = None, breakScreen = None)

    val merged = ConfigurationMerger.merge(defaults, global, project, cli)

    // Theme: project wins (corporate)
    assertEquals(merged.theme, "corporate")
    // CopyImages: project wins (false)
    assertEquals(merged.copyImages, false)
    // SkipAccessibility: CLI wins (true)
    assertEquals(merged.skipAccessibility, true)
    // AccessibilityReportPath: project wins (report.json)
    assertEquals(merged.accessibilityReportPath, Some("report.json"))
    // OutputDir: project only (dist)
    assertEquals(merged.outputDir, Some("dist"))
    // Author: global only
    assertEquals(merged.author, Some(Author("Tony", "tony@example.com")))
    // ThemesDir: global only
    assertEquals(merged.themesDir, Some("~/themes"))
    // BreakScreen: project wins (v3.0.0)
    assertEquals(merged.breakScreen, Some("images/project-break.png"))

  // Scenario: No configs provided - use all defaults
  test("no configs provided uses hard-coded defaults"):
    val defaults = DefaultSettings(theme = "light", copyImages = true, skipAccessibility = false, breakScreen = None)
    val cli = CLIOverrides(theme = None, copyImages = None, skipAccessibility = None, accessibilityReportPath = None, breakScreen = None)

    val merged = ConfigurationMerger.merge(defaults, None, None, cli)

    assertEquals(merged.theme, "light")
    assertEquals(merged.copyImages, true)
    assertEquals(merged.skipAccessibility, false)
    assertEquals(merged.accessibilityReportPath, None)
    assertEquals(merged.outputDir, None)
    assertEquals(merged.author, None)
    assertEquals(merged.themesDir, None)

end ConfigurationSpec
