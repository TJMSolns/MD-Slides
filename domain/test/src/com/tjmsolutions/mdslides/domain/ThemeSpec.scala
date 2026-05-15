package com.tjmsolutions.mdslides.domain

import munit.FunSuite

/**
 * Tests for Theme aggregate and value objects.
 *
 * Related Governance:
 * - US-008: Theme System
 * - US-009: Built-in Themes
 * - US-012: Template-Specific Background Defaults
 * - PDR-007: Theme JSON Schema
 * - PDR-011: Background Image Architecture
 * - PDR-005: Accessibility Requirements
 */
class ThemeSpec extends FunSuite:

  // ===== Background Tests =====

  test("Background - create with color only"):
    val bg = Background(color = "#ffffff")
    assertEquals(bg.color, "#ffffff")
    assertEquals(bg.image, None)
    assertEquals(bg.opacity, 1.0)

  test("Background - create with image"):
    val bg = Background(
      color = "#ffffff",
      image = Some("./bg.png"),
      opacity = 0.1,
      position = Some("center"),
      size = Some("cover")
    )
    assertEquals(bg.image, Some("./bg.png"))
    assertEquals(bg.opacity, 0.1)

  test("Background - opacity defaults to 1.0"):
    val bg = Background(color = "#000000")
    assertEquals(bg.opacity, 1.0)

  // ===== ColorScheme Tests =====

  test("ColorScheme - create with all colors"):
    val colors = ColorScheme(
      text = "#333333",
      heading = "#2c3e50",
      accent = "#3498db",
      link = "#3498db",
      linkHover = "#2980b9",
      codeBackground = "#f4f4f4",
      codeText = "#333333"
    )
    assertEquals(colors.text, "#333333")
    assertEquals(colors.heading, "#2c3e50")

  test("ColorScheme - link hover defaults to link color"):
    val colors = ColorScheme(
      text = "#333",
      heading = "#000",
      accent = "#00f",
      link = "#00f",
      linkHover = "#00f",
      codeBackground = "#f0f0f0",
      codeText = "#000"
    )
    assertEquals(colors.linkHover, colors.link)

  // ===== FontScheme Tests =====

  test("FontScheme - create with font stacks"):
    val fonts = FontScheme(
      body = "Arial, sans-serif",
      heading = "'Helvetica Neue', Arial, sans-serif",
      code = "Consolas, monospace"
    )
    assertEquals(fonts.body, "Arial, sans-serif")
    assertEquals(fonts.heading, "'Helvetica Neue', Arial, sans-serif")

  // ===== Spacing Tests =====

  test("Spacing - create with CSS values"):
    val spacing = Spacing(
      slideMargin = "2rem",
      headingMargin = "1rem 0",
      paragraphMargin = "0.5rem 0",
      lineHeight = "1.6"
    )
    assertEquals(spacing.slideMargin, "2rem")
    assertEquals(spacing.lineHeight, "1.6")

  // ===== SyntaxColors Tests =====

  test("SyntaxColors - create with code highlighting colors"):
    val syntax = SyntaxColors(
      keyword = "#569cd6",
      string = "#ce9178",
      comment = "#6a9955",
      function = "#dcdcaa",
      number = "#b5cea8",
      operator = "#d4d4d4"
    )
    assertEquals(syntax.keyword, "#569cd6")
    assertEquals(syntax.comment, "#6a9955")

  // ===== SlideCounter Tests =====

  test("SlideCounter - create with styling"):
    val counter = SlideCounter(
      color = "#666666",
      background = "rgba(255, 255, 255, 0.9)",
      fontSize = "0.9rem"
    )
    assertEquals(counter.color, "#666666")
    assertEquals(counter.background, "rgba(255, 255, 255, 0.9)")

  // ===== Theme Tests =====

  test("Theme - create light theme"):
    val theme = Theme(
      name = "light",
      version = "1.0.0",
      background = Background(color = "#ffffff"),
      colors = ColorScheme(
        text = "#333333",
        heading = "#2c3e50",
        accent = "#3498db",
        link = "#3498db",
        linkHover = "#2980b9",
        codeBackground = "#f4f4f4",
        codeText = "#333333"
      ),
      fonts = FontScheme(
        body = "Arial, sans-serif",
        heading = "'Helvetica Neue', Arial, sans-serif",
        code = "Consolas, monospace"
      ),
      spacing = Spacing(
        slideMargin = "2rem",
        headingMargin = "1rem 0",
        paragraphMargin = "0.5rem 0",
        lineHeight = "1.6"
      ),
      syntax = SyntaxColors(
        keyword = "#569cd6",
        string = "#ce9178",
        comment = "#6a9955",
        function = "#dcdcaa",
        number = "#b5cea8",
        operator = "#d4d4d4"
      ),
      slideCounter = SlideCounter(
        color = "#666666",
        background = "rgba(255, 255, 255, 0.9)",
        fontSize = "0.9rem"
      )
    )
    assertEquals(theme.name, "light")
    assertEquals(theme.version, "1.0.0")

  test("Theme - create dark theme"):
    val theme = Theme(
      name = "dark",
      version = "1.0.0",
      background = Background(color = "#1e1e1e"),
      colors = ColorScheme(
        text = "#d4d4d4",
        heading = "#4ec9b0",
        accent = "#569cd6",
        link = "#4ec9b0",
        linkHover = "#3aa89a",
        codeBackground = "#2d2d2d",
        codeText = "#d4d4d4"
      ),
      fonts = FontScheme(
        body = "Arial, sans-serif",
        heading = "'Helvetica Neue', Arial, sans-serif",
        code = "Consolas, monospace"
      ),
      spacing = Spacing(
        slideMargin = "2rem",
        headingMargin = "1rem 0",
        paragraphMargin = "0.5rem 0",
        lineHeight = "1.6"
      ),
      syntax = SyntaxColors(
        keyword = "#569cd6",
        string = "#ce9178",
        comment = "#6a9955",
        function = "#dcdcaa",
        number = "#b5cea8",
        operator = "#d4d4d4"
      ),
      slideCounter = SlideCounter(
        color = "#cccccc",
        background = "rgba(30, 30, 30, 0.9)",
        fontSize = "0.9rem"
      )
    )
    assertEquals(theme.name, "dark")
    assertEquals(theme.background.color, "#1e1e1e")

  test("Theme - create with background image"):
    val theme = Theme(
      name = "corporate",
      version = "1.0.0",
      background = Background(
        color = "#f8f9fa",
        image = Some("./logo.png"),
        opacity = 0.05,
        position = Some("bottom right"),
        size = Some("200px auto")
      ),
      colors = ColorScheme(
        text = "#212529",
        heading = "#0056b3",
        accent = "#007bff",
        link = "#007bff",
        linkHover = "#0056b3",
        codeBackground = "#e9ecef",
        codeText = "#212529"
      ),
      fonts = FontScheme(
        body = "'Segoe UI', sans-serif",
        heading = "'Segoe UI', sans-serif",
        code = "'Courier New', monospace"
      ),
      spacing = Spacing(
        slideMargin = "3rem",
        headingMargin = "1.5rem 0 1rem 0",
        paragraphMargin = "0.75rem 0",
        lineHeight = "1.7"
      ),
      syntax = SyntaxColors(
        keyword = "#0056b3",
        string = "#28a745",
        comment = "#6c757d",
        function = "#007bff",
        number = "#e83e8c",
        operator = "#212529"
      ),
      slideCounter = SlideCounter(
        color = "#495057",
        background = "rgba(248, 249, 250, 0.95)",
        fontSize = "0.85rem"
      )
    )
    assertEquals(theme.background.image, Some("./logo.png"))
    assertEquals(theme.background.opacity, 0.05)

  // ===== US-012: Template-Specific Background Defaults =====

  test("Theme accepts templateBackgrounds field"):
    val theme = Theme(
      name = "retisio",
      version = "1.0.0",
      background = Background(color = "#FFFFFF"),
      colors = ColorScheme(
        text = "#002C74",
        heading = "#002C74",
        accent = "#FCC010",
        link = "#0B9655",
        linkHover = "#0B9655",
        codeBackground = "#F5F5F5",
        codeText = "#002C74"
      ),
      fonts = FontScheme(
        body = "'Varela Round', Arial, sans-serif",
        heading = "'Varela Round', Arial, sans-serif",
        code = "monospace"
      ),
      spacing = Spacing(
        slideMargin = "2rem",
        headingMargin = "1.5rem 0",
        paragraphMargin = "0.75rem 0",
        lineHeight = "1.6"
      ),
      syntax = SyntaxColors(
        keyword = "#0B9655",
        string = "#FCC010",
        comment = "#888888",
        function = "#002C74",
        number = "#FCC010",
        operator = "#002C74"
      ),
      slideCounter = SlideCounter(
        color = "#002C74",
        background = "rgba(252, 192, 16, 0.1)",
        fontSize = "18px"
      ),
      templateBackgrounds = Map(
        "title" -> "backgrounds/title-page.png",
        "content" -> "backgrounds/content-page.png",
        "diagram" -> "backgrounds/diagram-page.png"
      )
    )
    assertEquals(theme.templateBackgrounds("title"), "backgrounds/title-page.png")
    assertEquals(theme.templateBackgrounds("content"), "backgrounds/content-page.png")
    assertEquals(theme.templateBackgrounds("diagram"), "backgrounds/diagram-page.png")

  test("Theme with empty templateBackgrounds defaults to Map.empty"):
    val theme = Theme(
      name = "minimal",
      version = "1.0.0",
      background = Background(color = "#000000"),
      colors = ColorScheme(
        text = "#ffffff",
        heading = "#ffffff",
        accent = "#00ff00",
        link = "#00ff00",
        linkHover = "#00cc00",
        codeBackground = "#222222",
        codeText = "#ffffff"
      ),
      fonts = FontScheme(
        body = "Arial, sans-serif",
        heading = "Arial, sans-serif",
        code = "monospace"
      ),
      spacing = Spacing(
        slideMargin = "2rem",
        headingMargin = "1rem 0",
        paragraphMargin = "0.5rem 0",
        lineHeight = "1.5"
      ),
      syntax = SyntaxColors(
        keyword = "#ff0000",
        string = "#00ff00",
        comment = "#888888",
        function = "#ffff00",
        number = "#00ffff",
        operator = "#ffffff"
      ),
      slideCounter = SlideCounter(
        color = "#ffffff",
        background = "rgba(0, 0, 0, 0.8)",
        fontSize = "1rem"
      ),
      templateBackgrounds = Map.empty
    )
    assertEquals(theme.templateBackgrounds, Map.empty[String, String])

  test("Theme with no templateBackgrounds field defaults to Map.empty"):
    val theme = Theme(
      name = "minimal",
      version = "1.0.0",
      background = Background(color = "#000000"),
      colors = ColorScheme(
        text = "#ffffff",
        heading = "#ffffff",
        accent = "#00ff00",
        link = "#00ff00",
        linkHover = "#00cc00",
        codeBackground = "#222222",
        codeText = "#ffffff"
      ),
      fonts = FontScheme(
        body = "Arial, sans-serif",
        heading = "Arial, sans-serif",
        code = "monospace"
      ),
      spacing = Spacing(
        slideMargin = "2rem",
        headingMargin = "1rem 0",
        paragraphMargin = "0.5rem 0",
        lineHeight = "1.5"
      ),
      syntax = SyntaxColors(
        keyword = "#ff0000",
        string = "#00ff00",
        comment = "#888888",
        function = "#ffff00",
        number = "#00ffff",
        operator = "#ffffff"
      ),
      slideCounter = SlideCounter(
        color = "#ffffff",
        background = "rgba(0, 0, 0, 0.8)",
        fontSize = "1rem"
      )
      // Note: templateBackgrounds omitted - should default to Map.empty
    )
    assertEquals(theme.templateBackgrounds, Map.empty[String, String])

end ThemeSpec
