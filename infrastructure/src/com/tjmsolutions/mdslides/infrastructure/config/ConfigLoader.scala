package com.tjmsolutions.mdslides.infrastructure.config

import com.tjmsolutions.mdslides.domain
import com.tjmsolutions.mdslides.domain.{GlobalConfig, ProjectConfig, DefaultSettings, Author}
import io.circe._
import io.circe.parser._
import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters._

/**
 * Infrastructure service for loading configuration files.
 *
 * Responsibilities:
 * - Load global config from ~/.mdslides/config.json
 * - Load project config from .mdslides/config.json (with upward search)
 * - Parse and validate JSON
 * - Handle missing config files gracefully
 *
 * Related Governance:
 * - v2.0.0: Configuration Management
 * - Example Mapping: Scenarios 32-36, 42-44
 */
class ConfigLoader:

  /**
   * Load global configuration from ~/.mdslides/config.json.
   *
   * @param homeDir Optional home directory override (for testing)
   * @return Right(Some(config)) if found and valid, Right(None) if missing, Left(error) if invalid
   */
  def loadGlobalConfig(homeDir: Option[Path] = None): Either[String, Option[GlobalConfig]] =
    val home = homeDir.getOrElse(Paths.get(System.getProperty("user.home")))
    val configPath = home.resolve(".mdslides").resolve("config.json")

    if !Files.exists(configPath) then
      Right(None)
    else
      loadAndParseGlobalConfig(configPath)

  /**
   * Load project configuration from .mdslides/config.json.
   *
   * Searches upward from current directory until .mdslides/config.json found
   * or home directory reached.
   *
   * @param startDir Directory to start search from
   * @return Right(Some(config)) if found and valid, Right(None) if missing, Left(error) if invalid
   */
  def loadProjectConfig(startDir: Path): Either[String, Option[ProjectConfig]] =
    findProjectConfigPath(startDir) match
      case Some(configPath) => loadAndParseProjectConfig(configPath)
      case None => Right(None)

  /**
   * Search upward from startDir for .mdslides/config.json.
   *
   * Stops at home directory or filesystem root.
   *
   * Made public for config command display.
   */
  def findProjectConfigPath(startDir: Path): Option[Path] =
    val home = Paths.get(System.getProperty("user.home"))

    @annotation.tailrec
    def searchUpward(current: Path): Option[Path] =
      if current == null then
        None
      else if current == home then
        None  // Stop BEFORE checking home directory (global config lives there, don't load it as project config)
      else
        val configPath = current.resolve(".mdslides").resolve("config.json")
        if Files.exists(configPath) then
          Some(configPath)
        else
          val parent = current.getParent
          if parent == null || parent == current then
            None  // Reached filesystem root
          else
            searchUpward(parent)

    searchUpward(startDir.toAbsolutePath.normalize())

  private def loadAndParseGlobalConfig(configPath: Path): Either[String, Option[GlobalConfig]] =
    try
      val jsonContent = Files.readString(configPath)
      parse(jsonContent) match
        case Left(parseError) =>
          Left(s"Invalid JSON in ${configPath}: ${parseError.message}")

        case Right(json) =>
          decodeGlobalConfig(json) match
            case Left(decodeError) => Left(decodeError)
            case Right(config) => Right(Some(config))

    catch
      case e: Exception =>
        Left(s"Error reading config file ${configPath}: ${e.getMessage}")

  private def loadAndParseProjectConfig(configPath: Path): Either[String, Option[ProjectConfig]] =
    try
      val jsonContent = Files.readString(configPath)
      parse(jsonContent) match
        case Left(parseError) =>
          Left(s"Invalid JSON in ${configPath}: ${parseError.message}")

        case Right(json) =>
          decodeProjectConfig(json) match
            case Left(decodeError) => Left(decodeError)
            case Right(config) => Right(Some(config))

    catch
      case e: Exception =>
        Left(s"Error reading config file ${configPath}: ${e.getMessage}")

  /**
   * Decode global config JSON with strict validation.
   *
   * Rejects unknown fields to catch typos.
   */
  private def decodeGlobalConfig(json: Json): Either[String, GlobalConfig] =
    json.asObject match
      case None => Left("Global config must be a JSON object")
      case Some(obj) =>
        // Check for unknown fields (strict mode)
        val allowedFields = Set("defaults", "author", "paths")
        val unknownFields = obj.keys.filterNot(allowedFields.contains).toList
        if unknownFields.nonEmpty then
          Left(s"Unknown field(s) in global config: ${unknownFields.mkString(", ")}")
        else
          // Parse defaults (optional)
          val defaultsResult = obj("defaults") match
            case None => Right(None)
            case Some(defaultsJson) =>
              decodeDefaultSettings(defaultsJson).map(Some(_))

          // Parse author (optional)
          val authorResult = obj("author") match
            case None => Right(None)
            case Some(authorJson) =>
              decodeAuthor(authorJson).map(Some(_))

          // Parse paths (optional)
          val pathsResult = obj("paths") match
            case None => Right(None)
            case Some(pathsJson) =>
              decodePaths(pathsJson).map(Some(_))

          // Combine results
          for
            defaults <- defaultsResult
            author <- authorResult
            pathsData <- pathsResult
          yield GlobalConfig(defaults, author, pathsData)

  /**
   * Decode project config JSON with strict validation.
   */
  private def decodeProjectConfig(json: Json): Either[String, ProjectConfig] =
    json.asObject match
      case None => Left("Project config must be a JSON object")
      case Some(obj) =>
        // Check for unknown fields
        val allowedFields = Set("theme", "copyImages", "skipAccessibility", "accessibilityReportPath", "outputDir", "header", "footer", "breakScreen")
        val unknownFields = obj.keys.filterNot(allowedFields.contains).toList
        if unknownFields.nonEmpty then
          Left(s"Unknown field(s) in project config: ${unknownFields.mkString(", ")}")
        else
          // Parse each field
          val theme = obj("theme").flatMap(_.asString)
          val copyImages = obj("copyImages").flatMap(_.asBoolean)
          val skipAccessibility = obj("skipAccessibility").flatMap(_.asBoolean)
          val accessibilityReportPath = obj("accessibilityReportPath").flatMap(_.asString)
          val outputDir = obj("outputDir").flatMap(_.asString)
          val header = obj("header").flatMap(_.asString)
          val footer = obj("footer").flatMap(_.asString)
          val breakScreen = obj("breakScreen").flatMap(_.asString)

          // Validate types if present
          if obj.contains("theme") && theme.isEmpty then
            Left("Field 'theme' must be a String")
          else if obj.contains("copyImages") && copyImages.isEmpty then
            Left("Field 'copyImages' must be a Boolean")
          else if obj.contains("skipAccessibility") && skipAccessibility.isEmpty then
            Left("Field 'skipAccessibility' must be a Boolean")
          else if obj.contains("accessibilityReportPath") && accessibilityReportPath.isEmpty then
            Left("Field 'accessibilityReportPath' must be a String")
          else if obj.contains("outputDir") && outputDir.isEmpty then
            Left("Field 'outputDir' must be a String")
          else if obj.contains("header") && header.isEmpty then
            Left("Field 'header' must be a String")
          else if obj.contains("footer") && footer.isEmpty then
            Left("Field 'footer' must be a String")
          else if obj.contains("breakScreen") && breakScreen.isEmpty then
            Left("Field 'breakScreen' must be a String")
          else
            Right(ProjectConfig(theme, copyImages, skipAccessibility, accessibilityReportPath, outputDir, header, footer, breakScreen))

  private def decodeDefaultSettings(json: Json): Either[String, DefaultSettings] =
    json.asObject match
      case None => Left("defaults must be a JSON object")
      case Some(obj) =>
        val theme = obj("theme").flatMap(_.asString)
        val copyImages = obj("copyImages").flatMap(_.asBoolean)
        val skipAccessibility = obj("skipAccessibility").flatMap(_.asBoolean)
        val breakScreen = obj("breakScreen").flatMap(_.asString)

        if !obj.contains("theme") then
          Left("defaults.theme is required")
        else if theme.isEmpty then
          Left("defaults.theme must be a String")
        else if obj.contains("copyImages") && copyImages.isEmpty then
          Left("defaults.copyImages must be a Boolean")
        else if obj.contains("skipAccessibility") && skipAccessibility.isEmpty then
          Left("defaults.skipAccessibility must be a Boolean")
        else if obj.contains("breakScreen") && breakScreen.isEmpty then
          Left("defaults.breakScreen must be a String")
        else
          Right(DefaultSettings(
            theme.get,
            copyImages.getOrElse(true),
            skipAccessibility.getOrElse(false),
            breakScreen
          ))

  private def decodeAuthor(json: Json): Either[String, Author] =
    json.asObject match
      case None => Left("author must be a JSON object")
      case Some(obj) =>
        val name = obj("name").flatMap(_.asString)
        val email = obj("email").flatMap(_.asString)

        if !obj.contains("name") || name.isEmpty then
          Left("author.name is required and must be a String")
        else if !obj.contains("email") || email.isEmpty then
          Left("author.email is required and must be a String")
        else
          Right(Author(name.get, email.get))

  private def decodePaths(json: Json): Either[String, domain.Paths] =
    json.asObject match
      case None => Left("paths must be a JSON object")
      case Some(obj) =>
        val themesDir = obj("themesDir").flatMap(_.asString)

        if !obj.contains("themesDir") || themesDir.isEmpty then
          Left("paths.themesDir is required and must be a String")
        else
          Right(domain.Paths(themesDir.get))

end ConfigLoader
