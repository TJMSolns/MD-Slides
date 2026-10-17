package com.tjmsolutions.mdslides.domain

/**
 * Configuration management for MDSlides v2.0.0.
 *
 * Supports 4-layer configuration precedence:
 * 1. CLI arguments (highest priority)
 * 2. Project config (.mdslides/config.json in deck directory)
 * 3. Global config (~/.mdslides/config.json)
 * 4. Hard-coded defaults (lowest priority)
 *
 * Related Governance:
 * - v2.0.0: Configuration Management
 * - Example Mapping: Scenarios 32-47
 */

/**
 * Hard-coded default settings.
 *
 * Used when no configuration overrides are provided.
 */
case class DefaultSettings(
  theme: String,
  copyImages: Boolean,
  skipAccessibility: Boolean,
  breakScreen: Option[String]
)

/**
 * Global configuration (~/.mdslides/config.json).
 *
 * Contains user-wide defaults and preferences.
 */
case class GlobalConfig(
  defaults: Option[DefaultSettings],
  author: Option[Author],
  paths: Option[Paths]
)

/**
 * Project configuration (.mdslides/config.json).
 *
 * Contains project-specific overrides.
 */
case class ProjectConfig(
  theme: Option[String],
  copyImages: Option[Boolean],
  skipAccessibility: Option[Boolean],
  accessibilityReportPath: Option[String],
  outputDir: Option[String],
  header: Option[String],
  footer: Option[String],
  breakScreen: Option[String]
)

/**
 * CLI argument overrides.
 *
 * Highest priority configuration source.
 */
case class CLIOverrides(
  theme: Option[String],
  copyImages: Option[Boolean],
  skipAccessibility: Option[Boolean],
  accessibilityReportPath: Option[String],
  breakScreen: Option[String]
)

/**
 * Author information (global config only).
 */
case class Author(
  name: String,
  email: String
)

/**
 * Path configuration (global config only).
 */
case class Paths(
  themesDir: String
)

/**
 * Merged configuration combining all sources.
 *
 * Result of applying 4-layer precedence.
 */
case class MergedConfiguration(
  theme: String,
  copyImages: Boolean,
  skipAccessibility: Boolean,
  accessibilityReportPath: Option[String],
  outputDir: Option[String],
  author: Option[Author],
  themesDir: Option[String],
  header: Option[String],
  footer: Option[String],
  breakScreen: Option[String]
)

/**
 * Pure function for merging configurations.
 *
 * Applies precedence: CLI > project > global > defaults
 */
object ConfigurationMerger:

  /**
   * Merge all configuration sources into final configuration.
   *
   * @param defaults Hard-coded defaults
   * @param global Global config from ~/.mdslides/config.json
   * @param project Project config from .mdslides/config.json
   * @param cli CLI argument overrides
   * @return Merged configuration with all precedence rules applied
   */
  def merge(
    defaults: DefaultSettings,
    global: Option[GlobalConfig],
    project: Option[ProjectConfig],
    cli: CLIOverrides
  ): MergedConfiguration =

    // Theme: CLI > project > global > defaults
    val theme = cli.theme
      .orElse(project.flatMap(_.theme))
      .orElse(global.flatMap(_.defaults).map(_.theme))
      .getOrElse(defaults.theme)

    // CopyImages: CLI > project > global > defaults
    val copyImages = cli.copyImages
      .orElse(project.flatMap(_.copyImages))
      .orElse(global.flatMap(_.defaults).map(_.copyImages))
      .getOrElse(defaults.copyImages)

    // SkipAccessibility: CLI > project > global > defaults
    val skipAccessibility = cli.skipAccessibility
      .orElse(project.flatMap(_.skipAccessibility))
      .orElse(global.flatMap(_.defaults).map(_.skipAccessibility))
      .getOrElse(defaults.skipAccessibility)

    // AccessibilityReportPath: CLI > project (no global or defaults)
    val accessibilityReportPath = cli.accessibilityReportPath
      .orElse(project.flatMap(_.accessibilityReportPath))

    // OutputDir: project only (no CLI, global, or defaults)
    val outputDir = project.flatMap(_.outputDir)

    // Author: global only (no CLI, project, or defaults)
    val author = global.flatMap(_.author)

    // ThemesDir: global only (no CLI, project, or defaults)
    val themesDir = global.flatMap(_.paths).map(_.themesDir)

    // Header/Footer: project only (v3.0.0)
    val header = project.flatMap(_.header)
    val footer = project.flatMap(_.footer)

    // BreakScreen: CLI > project > global > defaults (v3.0.0)
    val breakScreen = cli.breakScreen
      .orElse(project.flatMap(_.breakScreen))
      .orElse(global.flatMap(_.defaults).flatMap(_.breakScreen))
      .orElse(defaults.breakScreen)

    MergedConfiguration(
      theme = theme,
      copyImages = copyImages,
      skipAccessibility = skipAccessibility,
      accessibilityReportPath = accessibilityReportPath,
      outputDir = outputDir,
      author = author,
      themesDir = themesDir,
      header = header,
      footer = footer,
      breakScreen = breakScreen
    )

end ConfigurationMerger
