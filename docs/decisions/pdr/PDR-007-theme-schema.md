# PDR-007: Theme JSON Schema

**Status:** Accepted
**Date:** 2024-12-21
**Deciders:** Product Team, Development Team
**Related:** US-008, US-009, PDR-005 (Accessibility)

## Context

MDSlides v0.2.0 introduces a theme system allowing users to customize the visual appearance of their presentations. Themes must be:
- Defined in JSON format
- Support colors, fonts, spacing
- Support background images (user requirement)
- Preserve accessibility (WCAG AA compliance from PDR-005)
- Easy to create and understand

### User Requirements

From Event Storming (December 21, 2024):
- **Background images must be supported** in themes
- Themes apply to all slide types
- Default theme preserved from v0.1.0

### v0.1.0 Default Styling

Current inline CSS (to be preserved as "light" theme):
- Background: #ffffff (white)
- Text: #333333 (dark gray)
- Heading: #2c3e50 (darker blue-gray)
- Accent: #3498db (blue)
- Code background: #f4f4f4 (light gray)

## Decision

### Theme JSON Schema

```json
{
  "$schema": "https://mdslides.dev/schema/theme-v1.json",
  "name": "theme-name",
  "version": "1.0.0",
  "background": {
    "color": "#ffffff",
    "image": "./images/background.png",
    "opacity": 0.1,
    "position": "center",
    "size": "cover"
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
    "body": "Arial, Helvetica, sans-serif",
    "heading": "'Helvetica Neue', Helvetica, Arial, sans-serif",
    "code": "Consolas, 'Courier New', monospace"
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
```

### Field Descriptions

#### Metadata

- **name**: Theme identifier (e.g., "light", "dark", "corporate")
- **version**: Semantic version (for future compatibility)

#### Background Section

- **color**: Background color (hex, rgb, or named)
- **image**: (Optional) Path to background image (relative or absolute URL)
- **opacity**: (Optional) Background image opacity (0.0 - 1.0), default: 1.0
- **position**: (Optional) CSS background-position, default: "center"
- **size**: (Optional) CSS background-size, default: "cover"

#### Colors Section

- **text**: Body text color
- **heading**: Heading text color (h1, h2, h3)
- **accent**: Accent color for UI elements
- **link**: Hyperlink color
- **linkHover**: Hyperlink hover color
- **codeBackground**: Code block background
- **codeText**: Code block text color

#### Fonts Section

- **body**: Font stack for body text
- **heading**: Font stack for headings
- **code**: Font stack for code blocks

#### Spacing Section

- **slideMargin**: Margin around slide content
- **headingMargin**: Margin around headings
- **paragraphMargin**: Margin around paragraphs
- **lineHeight**: Line height for body text

#### Syntax Section (Code Highlighting)

- **keyword**: Color for language keywords (`def`, `class`, `if`, etc.)
- **string**: Color for string literals
- **comment**: Color for comments
- **function**: Color for function names
- **number**: Color for numeric literals
- **operator**: Color for operators (`+`, `-`, `=`, etc.)

#### Slide Counter Section

- **color**: Text color for slide counter ("1 / 10")
- **background**: Background color for counter (supports rgba for transparency)
- **fontSize**: Font size for counter

## Built-In Themes

### Light Theme (Default)

Preserves v0.1.0 styling:

```json
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
    "body": "Arial, Helvetica, sans-serif",
    "heading": "'Helvetica Neue', Helvetica, Arial, sans-serif",
    "code": "Consolas, 'Courier New', monospace"
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
```

### Dark Theme

```json
{
  "name": "dark",
  "version": "1.0.0",
  "background": {
    "color": "#1e1e1e"
  },
  "colors": {
    "text": "#d4d4d4",
    "heading": "#4ec9b0",
    "accent": "#569cd6",
    "link": "#4ec9b0",
    "linkHover": "#3aa89a",
    "codeBackground": "#2d2d2d",
    "codeText": "#d4d4d4"
  },
  "fonts": {
    "body": "Arial, Helvetica, sans-serif",
    "heading": "'Helvetica Neue', Helvetica, Arial, sans-serif",
    "code": "Consolas, 'Courier New', monospace"
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
    "color": "#cccccc",
    "background": "rgba(30, 30, 30, 0.9)",
    "fontSize": "0.9rem"
  }
}
```

### Corporate Theme

```json
{
  "name": "corporate",
  "version": "1.0.0",
  "background": {
    "color": "#f8f9fa",
    "image": "./themes/corporate/logo-watermark.png",
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
    "body": "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif",
    "heading": "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif",
    "code": "'Courier New', Courier, monospace"
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
```

## Validation Rules

### Required Fields

- `name` (string, 1-50 chars, alphanumeric + hyphens)
- `version` (string, semver format)
- `background.color` (valid CSS color)
- `colors.text`, `colors.heading`, `colors.accent` (valid CSS colors)

### Optional Fields

All other fields are optional and fall back to defaults.

### Accessibility Validation (PDR-005)

Theme colors must meet WCAG AA contrast requirements:

- `text` vs `background.color`: contrast ratio ≥ 4.5:1
- `heading` vs `background.color`: contrast ratio ≥ 4.5:1
- `link` vs `background.color`: contrast ratio ≥ 4.5:1

**Validation errors** if contrast too low:

```
✗ Theme validation failed:
  - Text color #888888 has insufficient contrast with background #ffffff (3.2:1, need ≥4.5:1)
  - Link color #aaaaaa has insufficient contrast with background #ffffff (2.8:1, need ≥4.5:1)
```

### Background Image Validation

- `background.image`: Must be valid path or URL
- File existence NOT validated (graceful fallback if missing)
- `opacity`: Must be 0.0 - 1.0

## CLI Usage

### Built-in Themes

```bash
# Use built-in light theme (default)
mill cli.run presentation.md output.html

# Use built-in dark theme
mill cli.run presentation.md output.html --theme dark

# Use built-in corporate theme
mill cli.run presentation.md output.html --theme corporate
```

### Custom Themes

```bash
# Use custom theme from JSON file
mill cli.run presentation.md output.html --theme ./themes/custom.json
```

## Implementation Notes

### Theme Loading

```scala
object ThemeLoader:
  import io.circe.parser._
  import io.circe.generic.auto._

  def loadTheme(themeName: String): Either[String, Theme] =
    if isBuiltIn(themeName) then
      loadBuiltInTheme(themeName)
    else
      loadCustomTheme(themeName)

  private def loadBuiltInTheme(name: String): Either[String, Theme] =
    val json = scala.io.Source.fromResource(s"themes/$name.json").mkString
    decode[Theme](json).left.map(_.getMessage)

  private def loadCustomTheme(path: String): Either[String, Theme] =
    val json = scala.io.Source.fromFile(path).mkString
    decode[Theme](json).left.map(_.getMessage)
```

### CSS Generation

```scala
object ThemeRenderer:
  def renderCSS(theme: Theme): String =
    s"""
    :root {
      --bg-color: ${theme.background.color};
      --text-color: ${theme.colors.text};
      --heading-color: ${theme.colors.heading};
      ...
    }

    body {
      background-color: var(--bg-color);
      ${theme.background.image.map(img =>
        s"background-image: url('$img'); opacity: ${theme.background.opacity};"
      ).getOrElse("")}
      color: var(--text-color);
      font-family: ${theme.fonts.body};
    }
    ...
    """
```

## Consequences

### Positive

- **Flexibility**: Users can fully customize appearance
- **Background images**: Supports corporate branding
- **Accessibility**: Enforces WCAG AA compliance
- **Built-in themes**: Users can start quickly
- **JSON format**: Human-readable, easy to edit

### Negative

- **Complexity**: Many configuration options
- **Validation overhead**: Must validate colors, contrast, etc.
- **File management**: Users must manage theme JSON files

### Mitigation

- **Sensible defaults**: Most fields optional
- **Clear documentation**: Examples for common use cases
- **Validation errors**: Actionable feedback on invalid themes

## References

- [US-008: Theme System](../CEREMONIES-v0.2.0.md#us-008-theme-system)
- [US-009: Built-in Themes](../CEREMONIES-v0.2.0.md#us-009-built-in-themes)
- [PDR-005: Accessibility Requirements](PDR-005-accessibility.md)
- [WCAG 2.1 Contrast Guidelines](https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html)

## Revision History

- **2024-12-21**: Initial version (v0.2.0)
