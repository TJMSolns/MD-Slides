# PDR-012: Logo Positioning Architecture

**Status:** Deferred to v0.5.0+
**Date:** 2024-12-26
**Deferral Date:** 2024-12-26
**Deciders:** Product Team, Technical Lead
**Related:** US-010, product-backlog.md, PDR-011, v0.4.0-decisions-summary.md

## Context

Corporate presentations need consistent branding via logo placement. We need to support:

1. **Theme-level logos** - Corporate identity (e.g., Retisio logo on all slides)
2. **Template-specific logos** - Different logos for different slide types (optional)
3. **Per-slide logo overrides** - Individual slide customization (optional)

**Key Design Questions:**

1. Should logo be theme-level, template-level, or both?
2. How do we position logos (fixed positions vs custom CSS)?
3. Should logos be part of background or separate foreground elements?
4. How do logo images get copied to output (integration with US-006)?

## Decision

### Logo as Theme Property (Primary Use Case)

**Logos are theme-level branding - they represent corporate identity.**

**Domain Model:**

```scala
// PROPOSED (v0.4.0)
case class Theme(
  name: String,
  version: String,
  background: Background,
  colors: ColorScheme,
  fonts: FontScheme,
  spacing: Spacing,
  syntax: SyntaxColors,
  slideCounter: SlideCounter,
  logo: Option[LogoConfig] = None  // NEW: theme-level logo
)

/**
 * Logo configuration for corporate branding.
 *
 * Logo is rendered as a foreground element (above background, below content).
 * Higher z-index than background images, lower than slide content.
 */
case class LogoConfig(
  url: String,                    // Image path or URL
  position: LogoPosition,         // Where to place logo
  size: LogoSize,                 // How large to render
  opacity: Double = 1.0           // Opacity (0.0 - 1.0)
)

enum LogoPosition:
  case TopLeft
  case TopRight
  case BottomLeft
  case BottomRight

enum LogoSize:
  case Small      // 50px height
  case Medium     // 100px height
  case Large      // 150px height
  case Custom(cssValue: String)  // e.g., "80px" or "5rem"
```

**Rationale:**
- **Most common use case**: One logo for entire presentation (corporate branding)
- **Theme-level makes sense**: Logo is part of visual identity (like colors, fonts)
- **Simple mental model**: "Retisio theme includes Retisio logo"

### Template-Specific Logo Support (Future Extension)

**Theme can optionally specify different logos per template.**

```scala
// FUTURE (v0.5.0+)
case class Theme(
  // ... existing fields
  logo: Option[LogoConfig] = None,                    // Default logo
  templateLogos: Map[String, LogoConfig] = Map.empty  // FUTURE: per-template logos
)
```

**Example Use Case:**
- Title slide: Large centered company logo
- Content slides: Small top-right logo watermark
- End slide: Partner logos (different image)

**Decision for v0.4.0:** **Defer template-specific logos to future release.**

**Rationale:**
- YAGNI (You Aren't Gonna Need It) - No user has requested this yet
- Keep v0.4.0 scope manageable
- Theme-level logo covers 90% of use cases
- Can add `templateLogos` later without breaking changes (backward compatible)

### Per-Slide Logo Overrides: NOT SUPPORTED

**Individual slides cannot override logo.**

**Rationale:**
- **Branding consistency**: Corporate logos should be uniform across all slides
- **Not a content decision**: Authors shouldn't change branding on individual slides
- **Different from background**: Backgrounds are stylistic (vary per slide), logos are identity (constant)
- **Alternative**: If a slide needs different branding, use a different theme

**Exception:** Users can disable logo on specific slides via CSS class:

```markdown
---
template: content
class: no-logo
---

## Slide Without Logo

This slide has custom CSS class 'no-logo' which hides the logo.
```

**CSS:**
```css
.slide.no-logo .logo {
  display: none;
}
```

**Decision:** Logo is theme-level only. No per-slide overrides.

### Logo vs Background Image

**Logos are FOREGROUND elements, not background elements.**

**Differences:**

| Property | Background Image | Logo |
|----------|------------------|------|
| **Purpose** | Decorative, sets mood/context | Branding, corporate identity |
| **Opacity** | Often low (0.1-0.3 for watermarks) | High (0.8-1.0 for visibility) |
| **z-index** | Lowest (behind all content) | Middle (above background, below content) |
| **Positioning** | Full-slide coverage or tiled | Corner placement (top-right, etc.) |
| **Size** | Variable, often full-slide | Fixed, small (50-150px) |

**Rendering Order (z-index from back to front):**

```
1. Background color (theme.background.color)
2. Background image (theme.background.image)  ← Lowest z-index
3. Logo (theme.logo)                          ← Middle z-index
4. Slide content (heading, body, images)      ← Highest z-index
5. Slide counter (UI overlay)                 ← Highest z-index
```

**CSS Implementation:**

```css
.slide {
  position: relative;
  background-color: #FFFFFF;  /* z-index: 0 */
  background-image: url('background.png');  /* z-index: 0 */
}

.slide .logo {
  position: absolute;
  z-index: 10;  /* Above background, below content */
  opacity: 1.0;
}

.slide .slide-content {
  position: relative;
  z-index: 20;  /* Above logo */
}
```

## Logo Positioning

### Fixed Positions (Simple, Predictable)

**Use predefined corner positions.**

**Supported positions:**

```scala
enum LogoPosition:
  case TopLeft      // top: 20px, left: 20px
  case TopRight     // top: 20px, right: 20px
  case BottomLeft   // bottom: 20px, left: 20px
  case BottomRight  // bottom: 20px, right: 20px
```

**CSS Generation:**

```scala
def generateLogoCSS(logo: LogoConfig): String =
  val position = logo.position match
    case LogoPosition.TopLeft     => "top: 20px; left: 20px;"
    case LogoPosition.TopRight    => "top: 20px; right: 20px;"
    case LogoPosition.BottomLeft  => "bottom: 60px; left: 20px;"  // Above slide counter
    case LogoPosition.BottomRight => "bottom: 60px; right: 20px;" // Above slide counter

  val size = logo.size match
    case LogoSize.Small  => "height: 50px;"
    case LogoSize.Medium => "height: 100px;"
    case LogoSize.Large  => "height: 150px;"
    case LogoSize.Custom(css) => s"height: $css;"

  s"""
  .slide .logo {
    position: absolute;
    $position
    $size
    width: auto;
    opacity: ${logo.opacity};
    z-index: 10;
  }
  """
```

**Rationale:**
- **Simple**: Four corners cover 95% of use cases
- **Predictable**: Users know exactly where logo will appear
- **No CSS knowledge required**: Select from predefined options
- **Responsive**: Positions relative to slide edges (works at any resolution)

**Future Extension:** Could add `LogoPosition.Custom(top, right, bottom, left)` for advanced users.

### Logo Size Options

**Three predefined sizes + custom option.**

```scala
enum LogoSize:
  case Small      // 50px  - Subtle watermark
  case Medium     // 100px - Standard branding
  case Large      // 150px - Prominent branding (title slide)
  case Custom(cssValue: String)  // "80px", "5rem", "10%", etc.
```

**Rationale:**
- **Small**: For content slides (non-distracting)
- **Medium**: Default, balanced visibility
- **Large**: For title/end slides (prominent branding)
- **Custom**: Escape hatch for special needs

**Width is always `auto`** - maintains aspect ratio.

## Theme JSON Syntax

### Basic Logo Configuration

```json
{
  "name": "Retisio Corporate",
  "logo": {
    "url": "logos/retisio-logo-white.png",
    "position": "top-right",
    "size": "medium",
    "opacity": 0.9
  }
}
```

### No Logo (Default)

```json
{
  "name": "Minimal Theme"
  // No logo field - no logo rendered
}
```

### Logo with Custom Size

```json
{
  "name": "TJM Solutions",
  "logo": {
    "url": "logos/tjm-solutions-logo.png",
    "position": "bottom-right",
    "size": "80px",
    "opacity": 1.0
  }
}
```

**Validation:**

- `url`: Required if logo present, string (path or URL)
- `position`: Required, one of: `"top-left"`, `"top-right"`, `"bottom-left"`, `"bottom-right"`
- `size`: Required, one of: `"small"`, `"medium"`, `"large"`, or CSS value (e.g., `"80px"`)
- `opacity`: Optional, number 0.0-1.0, default 1.0

## Image Copying Integration (US-006)

**Logo images follow same copying rules as background and content images.**

### Copy Logic

```scala
// Main.scala
def extractAllImages(deck: SlideDeck, theme: Theme): List[String] =
  val contentImages = extractImageUrls(deck)        // From slide content
  val backgroundImages = extractBackgroundImages(deck, theme)  // From backgrounds
  val logoImages = theme.logo.map(_.url).toList     // From theme logo

  (contentImages ++ backgroundImages ++ logoImages).distinct
```

**Behavior:**

1. **Local logos**: `logos/retisio-logo.png` → Copied to `output-dir/logos/retisio-logo.png`
2. **External logos**: `https://cdn.retisio.com/logo.png` → Referenced directly (not copied)
3. **Data URLs**: `data:image/png;base64,...` → Embedded directly (not copied)

**Respects --no-copy-images flag:**
- If `--no-copy-images`, logo not copied (user responsible for deployment)
- Consistent with all other image types

### Path Resolution

```scala
// Logo paths resolved relative to theme JSON location
val themeDir = themeJsonPath.getParent
val logoPath = themeDir.resolve(logo.url)
```

**Example:**

```
themes/
  retisio.json          ← theme file
  logos/
    retisio-logo.png    ← referenced as "logos/retisio-logo.png" in JSON
```

**Absolute paths also supported:**

```json
{
  "logo": {
    "url": "/usr/share/branding/company-logo.png"  // Absolute path
  }
}
```

## Alternatives Considered

### Alternative 1: Logo as Slot

**Approach:** Add `logo` slot to templates

**Pros:**
- Uses existing slot mechanism

**Cons:**
- **Conceptually wrong**: Logo is branding (theme-level), not content (slide-level)
- Slots are for user-written content, not corporate identity
- Would require logo on every slide (repetitive)
- Violates DRY (Don't Repeat Yourself)

**Decision:** Rejected. Logo is theme property, not slide content.

### Alternative 2: Logo as Background Layer

**Approach:** Render logo as part of background image

**Pros:**
- No new domain concepts (reuses background)

**Cons:**
- **Mixed concerns**: Background decorates, logo brands (different purposes)
- Can't have both background image AND logo (users want both)
- Logo positioning harder (background uses `background-position`, logo uses absolute positioning)
- Logo opacity conflicts with background opacity

**Decision:** Rejected. Logo and background are distinct concepts.

### Alternative 3: Custom CSS Positioning

**Approach:** Allow arbitrary CSS in `position` field

```json
{
  "logo": {
    "position": {"top": "30px", "right": "5%"}
  }
}
```

**Pros:**
- Maximum flexibility

**Cons:**
- **Complexity**: Requires CSS knowledge
- **Validation hard**: How do we validate arbitrary CSS?
- **Responsive issues**: Pixel values don't scale, percentages can break layout
- **YAGNI**: Fixed corners cover almost all use cases

**Decision:** Deferred. Start with fixed positions, add custom positioning later if users request it.

### Alternative 4: Per-Slide Logo Overrides

**Approach:** Allow slides to override logo (different image, position, or hide)

**Pros:**
- Maximum flexibility

**Cons:**
- **Inconsistent branding**: Slides with different logos look unprofessional
- **Not a content decision**: Authors shouldn't change corporate branding
- **Complexity**: Three-level fallback (slide → template → theme)
- **Low value**: No user has requested this

**Decision:** Rejected. Logos are corporate identity (constant), not slide styling (variable).

## Validation Strategy

### Theme JSON Validation (ThemeJsonAdapter)

```scala
def parseTheme(json: String): Either[String, Theme] =
  // Parse logo (optional)
  val logo = json.hcursor.downField("logo").as[Option[LogoConfig]] match
    case Right(Some(cfg)) =>
      // Validate position
      cfg.position match
        case "top-left" | "top-right" | "bottom-left" | "bottom-right" => ()
        case invalid => return Left(s"Invalid logo position: $invalid")

      // Validate opacity
      if cfg.opacity < 0.0 || cfg.opacity > 1.0 then
        return Left(s"Logo opacity must be 0.0-1.0, got: ${cfg.opacity}")

      // Validate size
      cfg.size match
        case "small" | "medium" | "large" => ()
        case custom if custom.matches("""^\d+(px|rem|em|%)$""") => ()
        case invalid => return Left(s"Invalid logo size: $invalid")

      Some(cfg)

    case Right(None) => None
    case Left(error) => return Left(s"Logo parse error: $error")

  // Logo URL validated at render time (like other images)
```

### Render-Time Validation

```scala
theme.logo match
  case Some(logo) if ImageAssetCopier.isLocalPath(logo.url) =>
    // Local file - check existence
    ImageAssetCopier.resolvePath(logo.url, themeDir) match
      case Right(path) if Files.exists(path) =>
        copyAndRenderLogo(logo)
      case _ =>
        IO.println(s"⚠ Warning: Logo image not found: ${logo.url}")
        renderWithoutLogo()

  case Some(logo) =>
    // External URL or data URL - render directly
    renderLogo(logo)

  case None =>
    // No logo configured
    renderWithoutLogo()
```

**Graceful degradation:** Missing logos warn but don't fail presentation generation.

## CSS Implementation

### Logo CSS Generation

```scala
def generateLogoCSS(logo: LogoConfig): String =
  val positionCSS = logo.position match
    case LogoPosition.TopLeft => "top: 20px; left: 20px;"
    case LogoPosition.TopRight => "top: 20px; right: 20px;"
    case LogoPosition.BottomLeft => "bottom: 80px; left: 20px;"   // Above counter
    case LogoPosition.BottomRight => "bottom: 80px; right: 20px;"

  val sizeCSS = logo.size match
    case LogoSize.Small => "height: 50px;"
    case LogoSize.Medium => "height: 100px;"
    case LogoSize.Large => "height: 150px;"
    case LogoSize.Custom(value) => s"height: $value;"

  s"""
  .slide .logo {
    position: absolute;
    $positionCSS
    $sizeCSS
    width: auto;
    max-width: 300px;  /* Prevent oversized logos */
    opacity: ${logo.opacity};
    z-index: 10;
    pointer-events: none;  /* Don't interfere with clicks */
  }
  """
```

### Logo HTML Rendering

```scala
def renderLogo(logo: LogoConfig): Frag =
  img(
    cls := "logo",
    src := logo.url,
    alt := "Logo",  // Accessibility
    attr("aria-hidden") := "true"  // Decorative image
  )

// In slide rendering:
div(cls := "slide")(
  theme.logo.map(renderLogo),  // Logo (if configured)
  div(cls := "slide-content")(
    // Slide content here
  )
)
```

## Migration Path

### From v0.3.0 to v0.4.0

**No breaking changes.**

**Existing themes:**
- Themes without `logo` field → No logo rendered
- Works unchanged

**New themes:**
- Add `logo` field to enable logo rendering
- Opt-in feature

## Success Metrics

**Target:**
- 80% of corporate themes define logos
- 95% use fixed positions (top-right, bottom-right)
- <5% need custom sizes beyond small/medium/large
- 0 backward compatibility issues

## Related Documents

- [Product Backlog](../../planning/product-backlog.md)
- [US-010: Theme Logo Support](../../planning/product-backlog.md#us-010-theme-logo-support-should)
- [PDR-011: Background Image Architecture](PDR-011-background-image-architecture.md)
- [US-006: Image Asset Copying](../../ceremonies/v0.3.0.md#us-006-image-asset-copying)
- [Retisio Theme Analysis](../../planning/retisio-theme-analysis.md)

---

**Decision Date:** 2024-12-26
**Deferral Date:** 2024-12-26
**Status:** 🔵 Deferred to v0.5.0+

## Deferral Rationale

Logo support has been deferred to a future release (v0.5.0 or later) for the following reasons:

1. **Focus on Background Images:** v0.4.0 is focused on background image architecture (US-011, US-012, US-016)
2. **Logo Positioning Complexity:** Logo positioning requires more design work to get right
   - Content images don't currently support positioning/sizing
   - Need to design unified image positioning system first
3. **Interim Solution Available:** Users can embed logos as content images for now
4. **Avoid Feature Creep:** Keep v0.4.0 scope manageable
5. **Architecture Foundation:** Directory-based themes (US-016) provides foundation for future logo support

## Placeholder for Future Work

When logo support is implemented in v0.5.0+, this PDR provides a starting point for the design. Key questions remain:

1. Should logo positioning be part of a general image positioning system?
2. How do logos interact with content images that may also be positioned?
3. Should logos be a special case or just "theme-level content images"?

**Next Steps (for v0.5.0+):**
- Review this PDR when logo support is prioritized
- Consider unified image positioning architecture
- Event Storming for US-010 at that time

**Status:** On hold until v0.4.0 background image features are complete
