package com.tjmsolutions.mdslides.infrastructure.theme

import cats.effect.IO
import cats.syntax.all.*
import com.tjmsolutions.mdslides.domain.Theme
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

/**
 * Theme loader for directory-based themes.
 *
 * Implements US-016: Directory-Based Themes
 *
 * Responsibilities:
 * - Discover themes in configured theme directory
 * - Load theme.json from theme directory
 * - Validate theme directory structure
 * - Provide clear error messages
 *
 * Theme directory structure:
 * {{{
 * themes/
 *   retisio/
 *     theme.json           ← Required
 *     backgrounds/         ← Optional
 *       title-page.png
 *       content-page.png
 *     logos/               ← Optional (future use)
 *       logo.png
 * }}}
 *
 * Related Governance:
 * - PDR-013: Directory-Based Theme Architecture
 * - event-storming-US-016.md
 * - v0.4.0.md (ceremony document)
 */
object ThemeLoader:

  /**
   * Load theme by name from configured theme directory.
   *
   * @param themeName Name of theme (directory name)
   * @param themeDir Parent directory containing themes
   * @return Theme if found and valid
   * @throws ThemeNotFoundException if theme directory or theme.json not found
   * @throws ThemeParseException if theme.json is invalid
   *
   * Example:
   * {{{
   * // Load "retisio" theme from ./themes/retisio/theme.json
   * ThemeLoader.loadTheme("retisio", Paths.get("./themes"))
   * }}}
   */
  def loadTheme(themeName: String, themeDir: Path): IO[Theme] =
    for
      // Locate theme directory
      themeDirectory <- IO(themeDir.resolve(themeName))
      _ <- validateThemeDirectory(themeName, themeDirectory, themeDir)

      // Read theme.json
      themeJsonPath = themeDirectory.resolve("theme.json")
      themeJsonContent <- IO(Files.readString(themeJsonPath))
        .adaptError { case e =>
          ThemeNotFoundException(
            s"Theme '$themeName' found but theme.json is missing or unreadable in ${themeDirectory}\n" +
            s"Expected file: ${themeJsonPath}\n" +
            s"Cause: ${e.getMessage}"
          )
        }

      // Parse theme JSON
      theme <- IO.fromEither(
        ThemeJsonAdapter.parseTheme(themeJsonContent)
          .left.map(errorMsg => ThemeParseException(
            s"Theme '$themeName' has invalid JSON in ${themeJsonPath}\n" +
            s"Cause: $errorMsg"
          ))
      )
    yield theme

  /**
   * List all available themes in theme directory.
   *
   * A directory is considered a valid theme if:
   * - It is a directory (not a file)
   * - It contains a theme.json file
   *
   * @param themeDir Parent directory containing themes
   * @return List of theme names (sorted alphabetically)
   *
   * Example:
   * {{{
   * // List themes in ./themes/
   * // Returns: List("minimal", "retisio", "tjm-solutions")
   * ThemeLoader.listAvailableThemes(Paths.get("./themes"))
   * }}}
   */
  def listAvailableThemes(themeDir: Path): IO[List[String]] =
    IO {
      if !Files.exists(themeDir) then
        List.empty[String]
      else
        Files.list(themeDir)
          .iterator()
          .asScala
          .filter(path => Files.isDirectory(path))
          .filter(path => Files.exists(path.resolve("theme.json")))
          .map(path => path.getFileName.toString)
          .toList
          .sorted
    }

  /**
   * Validate theme directory structure.
   *
   * Checks:
   * - Theme directory exists
   * - Theme directory is a directory (not a file)
   * - theme.json file exists
   *
   * @param themeName Name of theme (for error messages)
   * @param themeDirectory Full path to theme directory
   * @param parentThemeDir Parent directory (for error messages)
   * @return Unit if valid, fails with ThemeNotFoundException if invalid
   */
  private def validateThemeDirectory(
    themeName: String,
    themeDirectory: Path,
    parentThemeDir: Path
  ): IO[Unit] =
    IO.blocking {
      if !Files.exists(themeDirectory) then ()
      else if !Files.isDirectory(themeDirectory) then ()
      else
        val themeJsonPath = themeDirectory.resolve("theme.json")
        if !Files.exists(themeJsonPath) then ()
    }.flatMap { _ =>
      if !Files.exists(themeDirectory) then
        IO.raiseError(ThemeNotFoundException(
          s"Theme '$themeName' not found in ${parentThemeDir}\n" +
          s"\n" +
          s"Searched for: ${themeDirectory}\n" +
          s"\n" +
          s"Available themes:\n" +
          listAvailableThemesSync(parentThemeDir).map(name => s"  - $name").mkString("\n") +
          s"\n" +
          s"\n" +
          s"Use 'mdslides config set theme-dir /path' to change theme directory."
        ))
      else if !Files.isDirectory(themeDirectory) then
        IO.raiseError(ThemeNotFoundException(
          s"Theme '$themeName' is not a directory: ${themeDirectory}\n" +
          s"Themes must be directories containing theme.json"
        ))
      else
        val themeJsonPath = themeDirectory.resolve("theme.json")
        if !Files.exists(themeJsonPath) then
          IO.raiseError(ThemeNotFoundException(
            s"Theme '$themeName' found but theme.json is missing\n" +
            s"Expected file: ${themeJsonPath}"
          ))
        else
          IO.unit
    }

  /**
   * Synchronous version of listAvailableThemes for error messages.
   */
  private def listAvailableThemesSync(themeDir: Path): List[String] =
    if !Files.exists(themeDir) then
      List.empty[String]
    else
      Files.list(themeDir)
        .iterator()
        .asScala
        .filter(path => Files.isDirectory(path))
        .filter(path => Files.exists(path.resolve("theme.json")))
        .map(path => path.getFileName.toString)
        .toList
        .sorted

end ThemeLoader

/**
 * Exception thrown when theme directory or theme.json not found.
 */
case class ThemeNotFoundException(message: String) extends Exception(message)

/**
 * Exception thrown when theme.json cannot be parsed.
 */
case class ThemeParseException(message: String) extends Exception(message)
