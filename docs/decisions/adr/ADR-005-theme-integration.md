# ADR-005: Theme Integration Architecture

**Status**: Accepted
**Date**: 2024-12-20
**Deciders**: Tony Moores (Architect, Bench Developer)
**Related Ceremony**: US-008 (Theme Loading), US-012 (Density Validation), US-009 (Theme Validation)

---

## Context

Themes control slide appearance (colors, fonts, layout) AND density validation limits. This creates a dependency:
- **Density Validation** needs theme-configurable limits (max lines, max words, max chars)
- **Rendering** needs theme styling (CSS generation)

**Questions**:
1. When is theme loaded in the pipeline?
2. How are theme density limits applied?
3. What if theme file is invalid?
4. Should there be a default theme?

**Constraints**:
- Density limits must be theme-specific (corporate theme might be more strict)
- Theme must be loaded BEFORE validation runs
- Invalid theme should fail early (not during rendering)
- CLI should support `--theme <name>` option

---

## Decision

**Load theme BEFORE validation**, integrate into pipeline as early dependency:

```
Pipeline Order:
1. Parse CLI arguments (input file, --theme option)
2. Read input markdown file
3. Parse markdown → SlideDeck AST
4. 🎨 LOAD THEME (before validation) <--- KEY DECISION
5. Validate Structure
6. Validate Density (uses theme.layout.maxBodyLines, etc.)
7. Validate Content
8. Render HTML (uses theme.colors, theme.typography)
9. Write output file
```

**Theme Structure** (JSON):
```json
{
  "name": "default",
  "colors": {
    "background": "#FFFFFF",
    "text": "#333333",
    "accent": "#0066CC"
  },
  "typography": {
    "titleFont": "Helvetica Neue",
    "bodyFont": "Georgia",
    "codeFont": "Consolas",
    "titleSize": "48px",
    "bodySize": "24px"
  },
  "layout": {
    "slideWidth": "1920px",
    "slideHeight": "1080px",
    "padding": "60px",
    "maxBodyLines": 12,      // Density limit
    "maxBodyWords": 150,     // Density limit
    "maxHeadingChars": 80    // Density limit
  }
}
```

**Theme Loading Logic**:
```scala
def loadTheme(themeName: String): Either[ThemeError, Theme] = {
  val themePath = s"themes/$themeName.json"
  for {
    json  <- readFile(themePath)
    theme <- parseTheme(json)  // Circe decoder
    _     <- validateTheme(theme) // US-009 validation
  } yield theme
}
```

**Default Theme Fallback**:
- CLI `--theme` option defaults to `"default"`
- File `themes/default.json` ships with MDSlides
- If missing, hard-coded fallback theme in code

---

## Consequences

### Positive

1. **Early Failure**: Invalid theme detected before validation/rendering (better UX)
2. **Configurable Density**: Different themes can have different "fits on slide" limits
3. **Separation of Concerns**: Theme parsing separate from rendering
4. **Testability**: Can test validation with different theme limits
5. **Flexibility**: Corporate users can create strict themes (e.g., max 8 lines)
6. **Pipeline Clarity**: Theme as explicit dependency (not hidden in rendering)

### Negative

1. **Additional I/O**: Must read theme file before validation (small overhead)
2. **Dependency**: Validation coupled to theme (can't validate without theme)
3. **Complexity**: Pipeline has 9 steps instead of 7

### Risks

1. **Risk**: Theme file missing → pipeline fails
   - **Mitigation**: Ship default theme, hard-coded fallback
2. **Risk**: Invalid theme JSON → unhelpful error message
   - **Mitigation**: US-009 theme validation with clear error messages
3. **Risk**: Theme density limits too strict → all decks fail
   - **Mitigation**: Document theme limits, warnings not errors (ADR-002)

---

## Alternatives Considered

### Alternative A: Load Theme During Rendering Only
**Structure**:
```
Parse → Validate (hardcoded limits) → Render (load theme) → Output
```
**Why Rejected**:
- Density limits not theme-configurable (defeats purpose)
- Validation assumes one-size-fits-all limits
- Corporate users can't customize density standards

### Alternative B: Theme Optional (Use Defaults)
**Structure**:
```
Parse → Validate (with defaults) → Render (with defaults or theme)
```
**Why Rejected**:
- Two sets of density limits (default vs. theme) = confusing
- Which defaults apply? (validation vs. rendering)
- Doesn't solve theme-specific density problem

### Alternative C: Embed Theme in Markdown Frontmatter
**Syntax**:
```markdown
---
theme: dark
maxBodyLines: 10
---

# Slide 1
```
**Why Rejected**:
- Mixes content with configuration (bad separation)
- Every deck needs frontmatter (verbose)
- Can't reuse themes across decks
- Harder to enforce corporate standards

### Alternative D: Compile-Time Theme
**Structure**:
- Themes compiled into binary (not runtime loading)
**Why Rejected**:
- Can't add custom themes without recompiling
- Inflexible for users
- Corporate users can't distribute custom themes

---

## Implementation Notes

### Theme ADT

```scala
case class Theme(
  name: String,
  colors: ColorScheme,
  typography: Typography,
  layout: LayoutSettings
)

case class ColorScheme(
  background: String,
  text: String,
  accent: String,
  heading: String,
  code: String
)

case class Typography(
  titleFont: String,
  bodyFont: String,
  codeFont: String,
  titleSize: String,
  bodySize: String,
  codeSize: String
)

case class LayoutSettings(
  slideWidth: String,
  slideHeight: String,
  padding: String,
  maxBodyLines: Int,
  maxBodyWords: Int,
  maxHeadingChars: Int
)
```

### Circe Decoder

```scala
import io.circe._
import io.circe.generic.semiauto._

object Theme {
  implicit val colorSchemeDecoder: Decoder[ColorScheme] = deriveDecoder
  implicit val typographyDecoder: Decoder[Typography] = deriveDecoder
  implicit val layoutDecoder: Decoder[LayoutSettings] = deriveDecoder
  implicit val themeDecoder: Decoder[Theme] = deriveDecoder

  def fromJson(json: String): Either[Error, Theme] = {
    parser.decode[Theme](json)
  }
}
```

### Theme Validation (US-009)

```scala
def validateTheme(theme: Theme): Either[ThemeValidationError, Theme] = {
  val errors = List.newBuilder[String]

  // Validate colors (hex codes)
  if (!isValidHex(theme.colors.background)) {
    errors += s"Invalid background color: ${theme.colors.background}"
  }

  // Validate font sizes (must be >= 18px)
  val bodySize = parsePx(theme.typography.bodySize)
  if (bodySize < 18) {
    errors += s"Body font size too small: ${bodySize}px (minimum 18px)"
  }

  // Validate layout dimensions
  val slideWidth = parsePx(theme.layout.slideWidth)
  if (slideWidth < 800 || slideWidth > 3840) {
    errors += s"Slide width out of range: ${slideWidth}px (800-3840)"
  }

  // Validate contrast ratio (WCAG AA = 4.5:1)
  val contrastRatio = calculateContrast(theme.colors.text, theme.colors.background)
  if (contrastRatio < 4.5) {
    // Warning, not error (allow low contrast with warning)
    println(s"Warning: Low contrast ratio ${contrastRatio}:1 (WCAG AA requires 4.5:1)")
  }

  errors.result() match {
    case Nil  => Right(theme)
    case errs => Left(ThemeValidationError(errs))
  }
}
```

### Default Theme

```scala
object Theme {
  val default: Theme = Theme(
    name = "default",
    colors = ColorScheme(
      background = "#FFFFFF",
      text = "#333333",
      accent = "#0066CC",
      heading = "#000000",
      code = "#F5F5F5"
    ),
    typography = Typography(
      titleFont = "Helvetica Neue, Arial, sans-serif",
      bodyFont = "Georgia, serif",
      codeFont = "Consolas, Monaco, monospace",
      titleSize = "48px",
      bodySize = "24px",
      codeSize = "18px"
    ),
    layout = LayoutSettings(
      slideWidth = "1920px",
      slideHeight = "1080px",
      padding = "60px",
      maxBodyLines = 12,
      maxBodyWords = 150,
      maxHeadingChars = 80
    )
  )
}
```

### Pipeline Integration

```scala
def processSlideDeck(
  inputFile: String,
  themeName: String
): Either[PipelineError, String] = {
  for {
    markdown <- readFile(inputFile)
    deck     <- parseMarkdown(markdown)
    theme    <- loadTheme(themeName)  // <--- Before validation
    _        <- validateStructure(deck)
    warnings <- validateDensity(deck, theme)  // <--- Uses theme.layout limits
    _        <- validateContent(deck)
    html     <- renderHTML(deck, theme)       // <--- Uses theme.colors, theme.typography
  } yield html
}
```

### CLI Integration

```scala
import com.monovore.decline._

val themeOpt = Opts.option[String](
  "theme",
  help = "Theme name (default: default)"
).withDefault("default")

val opts = (inputFile, outputFile, themeOpt).tupled

Command("mdslides", "Markdown to Slides")(opts).parse(args) match {
  case Right((input, output, themeName)) =>
    processSlideDeck(input, themeName) match {
      case Right(html) => writeFile(output, html)
      case Left(error) => System.err.println(error); sys.exit(1)
    }
}
```

### Theme Directory Structure

```
mdslides/
├─ themes/
│  ├─ default.json       (ships with MDSlides)
│  ├─ dark.json          (built-in dark theme)
│  ├─ corporate.json     (example strict theme)
│  └─ minimal.json       (example minimal theme)
├─ templates/
│  ├─ title.yaml
│  └─ content.yaml
└─ doc/
```

### Future Extensions

**v1.1**: Theme inheritance
```json
{
  "name": "dark",
  "extends": "default",
  "colors": {
    "background": "#1E1E1E",
    "text": "#D4D4D4"
  }
  // Inherits typography, layout from default
}
```

**v1.2**: Theme bundles
```
mdslides --theme corporate/quarterly-report
  → loads themes/corporate/quarterly-report.json
```

---

**ADR Type**: Architectural Integration
**Impact**: Pipeline (parse → validate → render), all modules
**Reversibility**: Medium (refactoring possible but affects pipeline)
**Validation**: Validated in ceremonies US-008, US-009, US-012
**Key Decision Rationale**: Theme loaded before validation enables theme-specific density limits (corporate customization)
