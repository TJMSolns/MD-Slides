package com.tjmsolutions.mdslides.infrastructure.theme

import com.tjmsolutions.mdslides.domain.Theme
import munit.FunSuite

/**
 * Tests for ThemeJsonAdapter - JSON parsing to Theme domain objects.
 *
 * Anticorruption Layer (ACL) pattern:
 * - Isolates domain from Circe JSON library
 * - Translates JSON → Theme aggregate
 * - Validates JSON structure and accessibility requirements
 *
 * Related Governance:
 * - ADR-007: Anticorruption Layer
 * - PDR-007: Theme JSON Schema
 * - PDR-005: Accessibility Requirements (WCAG AA)
 * - US-008: Theme System
 * - US-009: Built-in Themes
 */
class ThemeJsonAdapterSpec extends FunSuite:

  // ===== Valid JSON Parsing =====

  test("parseTheme - light theme"):
    val json = """
      {
        "name": "light",
        "version": "1.0.0",
        "background": {
          "color": "#ffffff"
        },
        "colors": {
          "text": "#333333",
          "heading": "#2c3e50",
          "accent": "#3498db",
          "link": "#3498db",
          "linkHover": "#2980b9",
          "codeBackground": "#f4f4f4",
          "codeText": "#333333"
        },
        "fonts": {
          "body": "Arial, sans-serif",
          "heading": "'Helvetica Neue', Arial, sans-serif",
          "code": "Consolas, monospace"
        },
        "spacing": {
          "slideMargin": "2rem",
          "headingMargin": "1rem 0",
          "paragraphMargin": "0.5rem 0",
          "lineHeight": "1.6"
        },
        "syntax": {
          "keyword": "#569cd6",
          "string": "#ce9178",
          "comment": "#6a9955",
          "function": "#dcdcaa",
          "number": "#b5cea8",
          "operator": "#d4d4d4"
        },
        "slideCounter": {
          "color": "#666666",
          "background": "rgba(255, 255, 255, 0.9)",
          "fontSize": "0.9rem"
        }
      }
    """

    ThemeJsonAdapter.parseTheme(json) match
      case Right(theme) =>
        assertEquals(theme.name, "light")
        assertEquals(theme.version, "1.0.0")
        assertEquals(theme.background.color, "#ffffff")
        assertEquals(theme.colors.text, "#333333")
        assertEquals(theme.fonts.body, "Arial, sans-serif")
      case Left(error) =>
        fail(s"Expected success but got error: $error")

  test("parseTheme - theme with background image"):
    val json = """
      {
        "name": "corporate",
        "version": "1.0.0",
        "background": {
          "color": "#f8f9fa",
          "image": "./logo.png",
          "opacity": 0.05,
          "position": "bottom right",
          "size": "200px auto"
        },
        "colors": {
          "text": "#212529",
          "heading": "#0056b3",
          "accent": "#007bff",
          "link": "#007bff",
          "linkHover": "#0056b3",
          "codeBackground": "#e9ecef",
          "codeText": "#212529"
        },
        "fonts": {
          "body": "'Segoe UI', sans-serif",
          "heading": "'Segoe UI', sans-serif",
          "code": "'Courier New', monospace"
        },
        "spacing": {
          "slideMargin": "3rem",
          "headingMargin": "1.5rem 0 1rem 0",
          "paragraphMargin": "0.75rem 0",
          "lineHeight": "1.7"
        },
        "syntax": {
          "keyword": "#0056b3",
          "string": "#28a745",
          "comment": "#6c757d",
          "function": "#007bff",
          "number": "#e83e8c",
          "operator": "#212529"
        },
        "slideCounter": {
          "color": "#495057",
          "background": "rgba(248, 249, 250, 0.95)",
          "fontSize": "0.85rem"
        }
      }
    """

    ThemeJsonAdapter.parseTheme(json) match
      case Right(theme) =>
        assertEquals(theme.background.image, Some("./logo.png"))
        assertEquals(theme.background.opacity, 0.05)
        assertEquals(theme.background.position, Some("bottom right"))
      case Left(error) =>
        fail(s"Expected success but got error: $error")

  test("parseTheme - theme without optional image fields"):
    val json = """
      {
        "name": "minimal",
        "version": "1.0.0",
        "background": {
          "color": "#000000"
        },
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

    ThemeJsonAdapter.parseTheme(json) match
      case Right(theme) =>
        assertEquals(theme.background.image, None)
        assertEquals(theme.background.opacity, 1.0)
        assertEquals(theme.background.position, None)
      case Left(error) =>
        fail(s"Expected success but got error: $error")

  // ===== Invalid JSON Parsing =====

  test("parseTheme - invalid JSON syntax"):
    val json = """{ invalid json }"""
    ThemeJsonAdapter.parseTheme(json) match
      case Left(error) =>
        // Error message varies but should indicate parsing failure
        assert(error.nonEmpty, s"Expected non-empty error message, got: $error")
      case Right(_) =>
        fail("Expected error for invalid JSON syntax")

  test("parseTheme - missing required field (name)"):
    val json = """
      {
        "version": "1.0.0",
        "background": { "color": "#ffffff" },
        "colors": {
          "text": "#000000",
          "heading": "#000000",
          "accent": "#0000ff",
          "link": "#0000ff",
          "linkHover": "#0000ff",
          "codeBackground": "#f0f0f0",
          "codeText": "#000000"
        },
        "fonts": {
          "body": "Arial",
          "heading": "Arial",
          "code": "monospace"
        },
        "spacing": {
          "slideMargin": "2rem",
          "headingMargin": "1rem",
          "paragraphMargin": "0.5rem",
          "lineHeight": "1.5"
        },
        "syntax": {
          "keyword": "#0000ff",
          "string": "#00ff00",
          "comment": "#888888",
          "function": "#ff00ff",
          "number": "#ff8800",
          "operator": "#000000"
        },
        "slideCounter": {
          "color": "#666666",
          "background": "white",
          "fontSize": "1rem"
        }
      }
    """
    ThemeJsonAdapter.parseTheme(json) match
      case Left(error) =>
        assert(error.contains("name") || error.contains("field"))
      case Right(_) =>
        fail("Expected error for missing 'name' field")

  test("parseTheme - missing required field (background.color)"):
    val json = """
      {
        "name": "broken",
        "version": "1.0.0",
        "background": {},
        "colors": {
          "text": "#000000",
          "heading": "#000000",
          "accent": "#0000ff",
          "link": "#0000ff",
          "linkHover": "#0000ff",
          "codeBackground": "#f0f0f0",
          "codeText": "#000000"
        },
        "fonts": {
          "body": "Arial",
          "heading": "Arial",
          "code": "monospace"
        },
        "spacing": {
          "slideMargin": "2rem",
          "headingMargin": "1rem",
          "paragraphMargin": "0.5rem",
          "lineHeight": "1.5"
        },
        "syntax": {
          "keyword": "#0000ff",
          "string": "#00ff00",
          "comment": "#888888",
          "function": "#ff00ff",
          "number": "#ff8800",
          "operator": "#000000"
        },
        "slideCounter": {
          "color": "#666666",
          "background": "white",
          "fontSize": "1rem"
        }
      }
    """
    ThemeJsonAdapter.parseTheme(json) match
      case Left(error) =>
        assert(error.contains("color") || error.contains("background"))
      case Right(_) =>
        fail("Expected error for missing 'background.color' field")

  // ===== Built-in Themes =====

  test("ThemeJsonAdapter.light - predefined light theme"):
    val theme = ThemeJsonAdapter.light
    assertEquals(theme.name, "light")
    assertEquals(theme.background.color, "#ffffff")

  test("ThemeJsonAdapter.dark - predefined dark theme"):
    val theme = ThemeJsonAdapter.dark
    assertEquals(theme.name, "dark")
    assertEquals(theme.background.color, "#1e1e1e")

  test("ThemeJsonAdapter.corporate - predefined corporate theme"):
    val theme = ThemeJsonAdapter.corporate
    assertEquals(theme.name, "corporate")
    assertEquals(theme.background.image, Some("./themes/corporate/logo-watermark.png"))

  // ===== US-012: Template-Specific Backgrounds =====

  test("parseTheme - theme with templateBackgrounds"):
    val json = """
      {
        "name": "Retisio",
        "version": "1.0.0",
        "background": {
          "color": "#FFFFFF"
        },
        "templateBackgrounds": {
          "title": "backgrounds/title-page.png",
          "content": "backgrounds/content-page.png",
          "diagram": "backgrounds/diagram-page.png"
        },
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

    ThemeJsonAdapter.parseTheme(json) match
      case Right(theme) =>
        assertEquals(theme.name, "Retisio")
        assertEquals(theme.templateBackgrounds.size, 3)
        assertEquals(theme.templateBackgrounds("title"), "backgrounds/title-page.png")
        assertEquals(theme.templateBackgrounds("content"), "backgrounds/content-page.png")
        assertEquals(theme.templateBackgrounds("diagram"), "backgrounds/diagram-page.png")
      case Left(error) =>
        fail(s"Expected success but got error: $error")

  test("parseTheme - theme with empty templateBackgrounds"):
    val json = """
      {
        "name": "minimal",
        "version": "1.0.0",
        "background": {
          "color": "#000000"
        },
        "templateBackgrounds": {},
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

    ThemeJsonAdapter.parseTheme(json) match
      case Right(theme) =>
        assertEquals(theme.templateBackgrounds, Map.empty[String, String])
      case Left(error) =>
        fail(s"Expected success but got error: $error")

  test("parseTheme - theme without templateBackgrounds field"):
    val json = """
      {
        "name": "minimal",
        "version": "1.0.0",
        "background": {
          "color": "#000000"
        },
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

    ThemeJsonAdapter.parseTheme(json) match
      case Right(theme) =>
        assertEquals(theme.templateBackgrounds, Map.empty[String, String])
      case Left(error) =>
        fail(s"Expected success but got error: $error")

  test("parseTheme - invalid templateBackgrounds (not object)"):
    val json = """
      {
        "name": "broken",
        "version": "1.0.0",
        "background": {
          "color": "#ffffff"
        },
        "templateBackgrounds": "not an object",
        "colors": {
          "text": "#000000",
          "heading": "#000000",
          "accent": "#0000ff",
          "link": "#0000ff",
          "linkHover": "#0000ff",
          "codeBackground": "#f0f0f0",
          "codeText": "#000000"
        },
        "fonts": {
          "body": "Arial",
          "heading": "Arial",
          "code": "monospace"
        },
        "spacing": {
          "slideMargin": "2rem",
          "headingMargin": "1rem",
          "paragraphMargin": "0.5rem",
          "lineHeight": "1.5"
        },
        "syntax": {
          "keyword": "#0000ff",
          "string": "#00ff00",
          "comment": "#888888",
          "function": "#ff00ff",
          "number": "#ff8800",
          "operator": "#000000"
        },
        "slideCounter": {
          "color": "#666666",
          "background": "white",
          "fontSize": "1rem"
        }
      }
    """

    ThemeJsonAdapter.parseTheme(json) match
      case Left(error) =>
        assert(error.contains("templateBackgrounds") || error.contains("object") || error.contains("String"))
      case Right(_) =>
        fail("Expected error for invalid templateBackgrounds")

end ThemeJsonAdapterSpec
