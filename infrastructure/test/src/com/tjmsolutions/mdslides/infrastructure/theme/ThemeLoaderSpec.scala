package com.tjmsolutions.mdslides.infrastructure.theme

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import munit.FunSuite
import java.nio.file.{Files, Path, Paths}

/**
 * Tests for ThemeLoader - directory-based theme loading.
 *
 * Implements US-016: Directory-Based Themes
 *
 * Test phases (TDD order):
 * - Phase 1: Theme Discovery (find theme directories)
 * - Phase 2: Theme Loading (load theme.json from directory)
 * - Phase 3: Path Resolution (resolve theme-relative image paths)
 * - Phase 4: Error Handling (missing theme, invalid JSON, etc.)
 *
 * Related Governance:
 * - PDR-013: Directory-Based Theme Architecture
 * - event-storming-US-016.md
 * - v0.4.0.md (ceremony document)
 */
class ThemeLoaderSpec extends FunSuite:

  // Temporary test directory for theme fixtures
  val testThemeDir: Path = Paths.get(sys.props("java.io.tmpdir"), "mdslides-test-themes")

  override def beforeEach(context: BeforeEach): Unit =
    // Clean up test directory before each test
    if Files.exists(testThemeDir) then
      deleteRecursively(testThemeDir)
    Files.createDirectories(testThemeDir)

  override def afterEach(context: AfterEach): Unit =
    // Clean up test directory after each test
    if Files.exists(testThemeDir) then
      deleteRecursively(testThemeDir)

  // Helper to delete directory recursively
  private def deleteRecursively(path: Path): Unit =
    if Files.isDirectory(path) then
      Files.list(path).forEach(deleteRecursively)
    Files.deleteIfExists(path)

  // ===== Phase 1: Theme Discovery =====

  test("ThemeLoader finds theme directory in default location"):
    // Given: A theme directory with theme.json
    val themeDir = testThemeDir.resolve("minimal")
    Files.createDirectories(themeDir)
    Files.writeString(
      themeDir.resolve("theme.json"),
      """
      {
        "name": "minimal",
        "version": "1.0.0",
        "background": { "color": "#000000" },
        "colors": {
          "text": "#ffffff",
          "heading": "#ffffff",
          "accent": "#00ff00",
          "link": "#00ff00",
          "linkHover": "#00cc00",
          "codeBackground": "#222222",
          "codeText": "#ffffff"
        },
        "fonts": {
          "body": "Arial, sans-serif",
          "heading": "Arial, sans-serif",
          "code": "monospace"
        },
        "spacing": {
          "slideMargin": "2rem",
          "headingMargin": "1rem 0",
          "paragraphMargin": "0.5rem 0",
          "lineHeight": "1.5"
        },
        "syntax": {
          "keyword": "#ff0000",
          "string": "#00ff00",
          "comment": "#888888",
          "function": "#ffff00",
          "number": "#00ffff",
          "operator": "#ffffff"
        },
        "slideCounter": {
          "color": "#ffffff",
          "background": "rgba(0, 0, 0, 0.8)",
          "fontSize": "1rem"
        }
      }
      """
    )

    // When: Loading theme by name
    val result = ThemeLoader.loadTheme("minimal", testThemeDir).unsafeRunSync()

    // Then: Theme should load successfully
    assertEquals(result.name, "minimal")
    assertEquals(result.version, "1.0.0")
    assertEquals(result.background.color, "#000000")

  test("ThemeLoader returns error when theme not found"):
    // Given: Empty theme directory
    // (no theme directories created)

    // When: Loading non-existent theme
    val result = ThemeLoader.loadTheme("nonexistent", testThemeDir).attempt.unsafeRunSync()

    // Then: Should return error
    result match
      case Left(error) =>
        val message = error.getMessage
        assert(message.contains("Theme 'nonexistent' not found"), s"Expected theme not found error, got: $message")
      case Right(_) =>
        fail("Expected error for non-existent theme")

  test("ThemeLoader lists available themes"):
    // Given: Multiple theme directories
    val minimalDir = testThemeDir.resolve("minimal")
    val darkDir = testThemeDir.resolve("dark")
    val corporateDir = testThemeDir.resolve("corporate")

    Files.createDirectories(minimalDir)
    Files.createDirectories(darkDir)
    Files.createDirectories(corporateDir)

    // Create theme.json in each
    val minimalTheme = """{"name": "minimal", "version": "1.0.0", "background": {"color": "#000000"}, "colors": {"text": "#ffffff", "heading": "#ffffff", "accent": "#00ff00", "link": "#00ff00", "linkHover": "#00cc00", "codeBackground": "#222222", "codeText": "#ffffff"}, "fonts": {"body": "Arial", "heading": "Arial", "code": "monospace"}, "spacing": {"slideMargin": "2rem", "headingMargin": "1rem", "paragraphMargin": "0.5rem", "lineHeight": "1.5"}, "syntax": {"keyword": "#ff0000", "string": "#00ff00", "comment": "#888888", "function": "#ffff00", "number": "#00ffff", "operator": "#ffffff"}, "slideCounter": {"color": "#ffffff", "background": "rgba(0,0,0,0.8)", "fontSize": "1rem"}}"""
    Files.writeString(minimalDir.resolve("theme.json"), minimalTheme)
    Files.writeString(darkDir.resolve("theme.json"), minimalTheme.replace("minimal", "dark"))
    Files.writeString(corporateDir.resolve("theme.json"), minimalTheme.replace("minimal", "corporate"))

    // When: Listing available themes
    val themes = ThemeLoader.listAvailableThemes(testThemeDir).unsafeRunSync()

    // Then: Should return all theme names
    assertEquals(themes.sorted, List("corporate", "dark", "minimal"))

  // ===== Phase 2: Theme Loading =====

  test("ThemeJsonAdapter parses theme.json from directory"):
    // Given: Theme directory with theme.json
    val themeDir = testThemeDir.resolve("retisio")
    Files.createDirectories(themeDir)
    Files.writeString(
      themeDir.resolve("theme.json"),
      """
      {
        "name": "Retisio",
        "version": "1.0.0",
        "background": { "color": "#FFFFFF" },
        "colors": {
          "text": "#002C74",
          "heading": "#002C74",
          "accent": "#FCC010",
          "link": "#0B9655",
          "linkHover": "#0B9655",
          "codeBackground": "#F5F5F5",
          "codeText": "#002C74"
        },
        "fonts": {
          "body": "'Varela Round', Arial, sans-serif",
          "heading": "'Varela Round', Arial, sans-serif",
          "code": "monospace"
        },
        "spacing": {
          "slideMargin": "2rem",
          "headingMargin": "1.5rem 0",
          "paragraphMargin": "0.75rem 0",
          "lineHeight": "1.6"
        },
        "syntax": {
          "keyword": "#0B9655",
          "string": "#FCC010",
          "comment": "#888888",
          "function": "#002C74",
          "number": "#FCC010",
          "operator": "#002C74"
        },
        "slideCounter": {
          "color": "#002C74",
          "background": "rgba(252, 192, 16, 0.1)",
          "fontSize": "18px"
        }
      }
      """
    )

    // When: Loading theme
    val theme = ThemeLoader.loadTheme("retisio", testThemeDir).unsafeRunSync()

    // Then: Theme should load with correct values
    assertEquals(theme.name, "Retisio")
    assertEquals(theme.colors.text, "#002C74")
    assertEquals(theme.fonts.body, "'Varela Round', Arial, sans-serif")

  test("ThemeJsonAdapter handles missing theme.json"):
    // Given: Theme directory exists but no theme.json
    val themeDir = testThemeDir.resolve("incomplete")
    Files.createDirectories(themeDir)

    // When: Loading theme
    val result = ThemeLoader.loadTheme("incomplete", testThemeDir).attempt.unsafeRunSync()

    // Then: Should return error
    result match
      case Left(error) =>
        val message = error.getMessage
        assert(message.contains("theme.json"), s"Expected theme.json error, got: $message")
      case Right(_) =>
        fail("Expected error for missing theme.json")

  test("ThemeJsonAdapter handles invalid JSON"):
    // Given: Theme directory with invalid JSON
    val themeDir = testThemeDir.resolve("broken")
    Files.createDirectories(themeDir)
    Files.writeString(themeDir.resolve("theme.json"), "{ invalid json }")

    // When: Loading theme
    val result = ThemeLoader.loadTheme("broken", testThemeDir).attempt.unsafeRunSync()

    // Then: Should return error
    result match
      case Left(error) =>
        assert(error.getMessage.nonEmpty)
      case Right(_) =>
        fail("Expected error for invalid JSON")

end ThemeLoaderSpec
