package com.tjmsolutions.mdslides.infrastructure.theme

import munit.FunSuite

/**
 * Tests for Google Fonts JSON parsing (US-017).
 *
 * Rules:
 * - R1: googleFonts field in JSON → List[String] in FontScheme
 * - R2: Missing or empty googleFonts → Nil (backward compat)
 */
class ThemeJsonAdapterGoogleFontsSpec extends FunSuite:

  private def themeJson(fontsExtra: String): String =
    s"""{
      "name": "Test",
      "version": "1.0.0",
      "background": { "color": "#ffffff" },
      "colors": {
        "text": "#000000",
        "heading": "#000000",
        "accent": "#0066cc",
        "link": "#0066cc",
        "linkHover": "#004499",
        "codeBackground": "#f5f5f5",
        "codeText": "#333333"
      },
      "fonts": {
        "body": "Arial, sans-serif",
        "heading": "Arial, sans-serif",
        "code": "monospace"$fontsExtra
      },
      "spacing": {
        "slideMargin": "2rem",
        "headingMargin": "1rem",
        "paragraphMargin": "0.5rem",
        "lineHeight": "1.5"
      },
      "syntax": {
        "keyword": "#0000ff",
        "string": "#008000",
        "comment": "#808080",
        "number": "#ff0000",
        "function": "#795e26",
        "operator": "#d4d4d4"
      },
      "slideCounter": {
        "color": "#666666",
        "background": "transparent",
        "fontSize": "14px"
      }
    }"""

  // R1 — single font
  test("US-017 / R1 — single googleFont entry is parsed into FontScheme"):
    val json = themeJson(""", "googleFonts": ["Roboto"]""")
    val theme = ThemeJsonAdapter.parseTheme(json)
    assert(theme.isRight, s"Expected Right but got: $theme")
    assertEquals(theme.map(_.fonts.googleFonts), Right(List("Roboto")))

  // R1 — multiple fonts
  test("US-017 / R1 — multiple googleFonts entries are all preserved"):
    val json = themeJson(""", "googleFonts": ["Roboto", "Open+Sans:wght@400;700", "Source+Code+Pro"]""")
    val theme = ThemeJsonAdapter.parseTheme(json)
    assert(theme.isRight, s"Expected Right but got: $theme")
    assertEquals(
      theme.map(_.fonts.googleFonts),
      Right(List("Roboto", "Open+Sans:wght@400;700", "Source+Code+Pro"))
    )

  // R2 — field absent → empty list
  test("US-017 / R2 — absent googleFonts field defaults to empty list"):
    val json = themeJson("")
    val theme = ThemeJsonAdapter.parseTheme(json)
    assert(theme.isRight, s"Expected Right but got: $theme")
    assertEquals(theme.map(_.fonts.googleFonts), Right(Nil))

  // R2 — empty array → empty list
  test("US-017 / R2 — empty googleFonts array parses to empty list"):
    val json = themeJson(""", "googleFonts": []""")
    val theme = ThemeJsonAdapter.parseTheme(json)
    assert(theme.isRight, s"Expected Right but got: $theme")
    assertEquals(theme.map(_.fonts.googleFonts), Right(Nil))
