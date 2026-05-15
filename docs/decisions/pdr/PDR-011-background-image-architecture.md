# PDR-011: Background Image Architecture

**Status:** Approved
**Date:** 2024-12-26
**Deciders:** Product Team, Technical Lead
**Related:** US-011, US-012, US-016, product-backlog.md, v0.4.0-decisions-summary.md

## Context

We need to support three levels of background image configuration:

1. **Theme-level backgrounds** (already exists) - applies to all slides
2. **Template-specific backgrounds** (US-012) - title slides vs content slides get different backgrounds
3. **Per-slide backgrounds** (US-011) - individual slide override

**Key Design Questions:**

1. Where does background configuration belong in the domain model?
2. How do we represent the fallback chain (slide → template → theme)?
3. Should background be a slot (content) or a property (metadata)?

## Decision

### Background as Slide Metadata (Not a Slot)

**Background images are NOT content - they are presentation styling.**

**Rationale:**
- Slots contain **content** that users write (text, bullet points, code)
- Background images are **presentation properties** (how content is displayed)
- Background belongs with `templateName` (another presentation property) not with `slots` (content)

**Domain Model Change:**

```scala
// CURRENT (v0.3.0)
case class Slide(
  id: SlideId,
  templateName: String,
  slots: Map[String, String]
)

// PROPOSED (v0.4.0)
case class Slide(
  id: SlideId,
  templateName: String,
  slots: Map[String, String],
  backgroundImage: Option[SlideBackground] = None  // NEW: per-slide override (string OR object)
)

// Support both simple string and extended object syntax
type SlideBackground = String | BackgroundConfig

case class BackgroundConfig(
  image: String,
  opacity: Option[Double] = None
  // Future extensions: position, blendMode, etc.
)
```

### Directory-Based Themes (US-016)

**Themes are directories containing theme.json + assets.**

**Structure:**
```
themes/retisio/
  theme.json
  backgrounds/
    title-page.png
    content-page.png
  logos/
    retisio-logo.png (future use)
```

**Rationale:**
- Self-contained, portable theme packages
- Image paths relative to theme directory
- Aligns with slide deck directory structure
- Simplifies theme distribution

**Theme Image Path Resolution:**
- Paths in theme.json are relative to theme directory
- Example: `"templateBackgrounds": { "title": "backgrounds/title-page.png" }`
- Resolves to: `themes/retisio/backgrounds/title-page.png`

**Migration:**
- Clean break - no backward compatibility
- Existing `themes/retisio.json` → `themes/retisio/theme.json`
- Migration required for v0.4.0

### Template-Specific Backgrounds in Theme

**Template → Background mapping belongs in the Theme aggregate.**

**Rationale:**
- Templates define **structure** (what slots exist)
- Themes define **styling** (colors, fonts, backgrounds)
- "Title slides use this background" is a **styling decision**, not a structural one
- Different themes can use different backgrounds for the same template

**Domain Model Change:**

```scala
// CURRENT (v0.3.0)
case class Theme(
  name: String,
  version: String,
  background: Background,  // Single background for all slides
  // ... other fields
)

// PROPOSED (v0.4.0)
case class Theme(
  name: String,
  version: String,
  background: Background,                     // Default background
  templateBackgrounds: Map[String, String],   // NEW: template-specific backgrounds
  // ... other fields
)
```

**Example Theme JSON:**

```json
{
  "name": "Retisio Corporate",
  "background": {
    "color": "#FFFFFF",
    "image": "backgrounds/default.png"
  },
  "templateBackgrounds": {
    "title": "backgrounds/title-page.png",
    "content": "backgrounds/content-page.png"
  }
}
```

### Fallback Chain

**Background resolution priority:**

```
1. Slide.backgroundImage (highest priority - user override)
   ↓
2. Theme.templateBackgrounds[Slide.templateName] (template-specific)
   ↓
3. Theme.background.image (theme default)
   ↓
4. Theme.background.color (always present - color-only background)
```

**Implementation (Renderer):**

```scala
def getBackgroundImage(slide: Slide, theme: Theme): Option[String] =
  slide.backgroundImage                                // 1. Per-slide override
    .orElse(theme.templateBackgrounds.get(slide.templateName))  // 2. Template default
    .orElse(theme.background.image)                    // 3. Theme default
```

## Consequences

### Positive

1. **Clear Separation of Concerns:**
   - Content (slots) vs Presentation (background, template)
   - Structure (Template) vs Styling (Theme)

2. **Flexible Theming:**
   - One theme can style templates differently (Retisio: fancy title page, simple content)
   - Another theme can use uniform backgrounds across all templates

3. **User Control:**
   - Authors can override on individual slides when needed
   - Most slides inherit sensible defaults

4. **Backward Compatible:**
   - Existing slides without `backgroundImage` field work unchanged
   - Existing themes without `templateBackgrounds` work unchanged (fallback to `background.image`)

### Negative

1. **Domain Model Complexity:**
   - `Slide` aggregate grows (adds one optional field)
   - `Theme` aggregate grows (adds one map field)
   - **Mitigation:** Both are simple additions, well-isolated

2. **Validation Complexity:**
   - Must validate background image paths at multiple levels
   - **Mitigation:** Reuse US-006 image path resolution logic

3. **Migration Path:**
   - Existing presentations need no changes (optional fields default to None)
   - Existing themes need no changes (optional field defaults to empty map)

### Neutral

1. **Parser Changes:**
   - Frontmatter parsing must extract `background` field
   - Theme JSON parsing must extract `templateBackgrounds` field

2. **Renderer Changes:**
   - Background selection logic becomes three-level fallback
   - Still deterministic, just more steps

## Alternatives Considered

### Alternative 1: Background as a Slot

**Approach:** Add `background` slot to templates

```yaml
---
template: content
---
background: backgrounds/custom.png

## Slide Title
Content here...
```

**Pros:**
- Uses existing slot mechanism
- No domain model changes

**Cons:**
- **Conceptually wrong**: Background is not content, it's presentation
- Slots are for user-written text, not styling properties
- Would require special handling ("background slot is different from other slots")
- Violates separation of content vs presentation

**Decision:** Rejected. Background is not content.

### Alternative 2: Template Owns Background Defaults

**Approach:** Template definition includes default background

```scala
case class Template(
  name: String,
  requiredSlots: List[SlotDefinition],
  defaultBackground: Option[String]  // Template-specific default
)
```

**Pros:**
- Template is self-contained (structure + default styling)

**Cons:**
- **Violates separation**: Templates define structure, Themes define styling
- Different themes couldn't use different backgrounds for same template
- "Title template always uses this background" is too rigid
- Themes exist precisely to allow multiple styling of the same structure

**Decision:** Rejected. Templates are about structure, not styling.

### Alternative 3: Separate BackgroundConfig Aggregate

**Approach:** Create a new domain aggregate for background configuration

```scala
case class BackgroundConfig(
  themeDefault: String,
  templateDefaults: Map[String, String],
  slideOverrides: Map[SlideId, String]
)
```

**Pros:**
- Centralized background management

**Cons:**
- **Over-engineering**: Adds aggregate for simple optional fields
- Complicates relationships (Theme → BackgroundConfig, Slide → BackgroundConfig?)
- No clear ownership (who owns BackgroundConfig?)
- More complex than inline optional fields

**Decision:** Rejected. Too complex for the problem.

## Validation Strategy

### Theme JSON Validation (ThemeJsonAdapter)

When loading a theme:

```scala
// Parse templateBackgrounds
val templateBackgrounds = json.hcursor
  .downField("templateBackgrounds")
  .as[Map[String, String]]
  .getOrElse(Map.empty)

// Validate: template names exist?
val invalidTemplates = templateBackgrounds.keys
  .filterNot(name => Template.fromName(name).isRight)

if invalidTemplates.nonEmpty then
  IO.println(s"⚠ Warning: Theme references unknown templates: ${invalidTemplates.mkString(", ")}")
  // Don't fail - just warn and ignore invalid entries
```

**Rationale:** Templates are extensible. A theme might reference a template that doesn't exist *yet*. Warn but don't fail.

### Slide Frontmatter Validation (MarkdownParser)

When parsing a slide with `background: path/to/image.png`:

```scala
// Extract background from frontmatter
val background = frontmatter.get("background")

// Store in Slide aggregate
val slide = Slide(
  id = slideId,
  templateName = templateName,
  slots = slots,
  backgroundImage = background
)
```

**No validation at parse time.** Validation deferred to render time.

**Rationale:** Consistent with US-005 (images in content). We don't validate image existence at parse time.

### Render-Time Validation (HTMLRenderer)

When rendering a slide:

```scala
val backgroundImage = getBackgroundImage(slide, theme)

backgroundImage match
  case Some(path) if !ImageAssetCopier.isLocalPath(path) =>
    // External URL - render directly, no copying
    renderWithBackground(path)

  case Some(path) =>
    // Local file - check if exists, copy if needed
    val resolved = ImageAssetCopier.resolvePath(path, sourceDir)
    resolved match
      case Right(absolutePath) if Files.exists(absolutePath) =>
        // Copy and render
        ImageAssetCopier.copyImages(List(path), sourceDir, outputDir)
        renderWithBackground(path)

      case _ =>
        IO.println(s"⚠ Warning: Background image not found: $path (using theme background color)")
        renderWithoutBackground()

  case None =>
    // No background image, use theme background color only
    renderWithoutBackground()
```

**Rationale:** Fail gracefully. Missing backgrounds shouldn't break presentation generation.

## Image Copying Integration (US-006)

Background images follow the same copying behavior as content images:

1. **Unified image handling:**
   - Theme images: paths relative to theme directory
   - Slide images: paths relative to presentation directory
   - Remote URLs: Downloaded to output directory (unless `--no-copy-images`)
   - All images end up in output directory (self-contained presentation)

2. **Preserve directory structure:**
   - Theme: `themes/retisio/backgrounds/title.png` → `output/backgrounds/title.png`
   - Slide: `my-slides/images/diagram.png` → `output/images/diagram.png`
   - No flattening, no renaming
   - Collision handling: warn if same path, skip duplicate

3. **Respect --no-copy-images flag:**
   - If `--no-copy-images`, don't copy ANY images (theme + content)
   - HTML references original paths
   - User responsible for deployment

**Code Integration:**

```scala
// Main.scala - extract all images
def extractAllImages(deck: SlideDeck, theme: Theme): List[String] =
  val contentImages = extractImageUrls(deck)  // Existing function

  val backgroundImages = deck.slides.toList.flatMap { slide =>
    getBackgroundImage(slide, theme).toList
  }

  (contentImages ++ backgroundImages).distinct
```

## Frontmatter Syntax

### Per-Slide Background Override

**Simple form (string) - most common:**

```markdown
---
template: content
background: backgrounds/custom-slide.png
---

## Slide With Custom Background

This slide has a unique background image.
```

**Extended form (object) - future extensions:**

```markdown
---
template: content
background:
  image: backgrounds/custom-slide.png
  opacity: 0.3
---

## Slide With Semi-Transparent Background

Background is more subtle with lower opacity.
```

**Rationale:**
- Simple string syntax for 90% of use cases
- Object syntax allows future extensions (opacity, position, blend modes)
- Parser detects string vs object automatically
- Backward compatible migration path

### Template-Specific Backgrounds (Theme JSON)

```json
{
  "name": "Retisio Corporate",
  "templateBackgrounds": {
    "title": "backgrounds/retisio-title-page.png",
    "content": "backgrounds/retisio-content-page.png",
    "section-title": "backgrounds/retisio-section-title-page.png",
    "diagram": "backgrounds/retisio-diagram-page.png"
  }
}
```

**Note:** Template names must match exactly. Case-sensitive.

## Migration Path

### From v0.3.0 to v0.4.0

**No breaking changes.**

**Existing presentations:**
- Slides without `background` field → `backgroundImage = None` → use theme default
- Works unchanged

**Existing themes:**
- Themes without `templateBackgrounds` → empty map → fallback to `background.image`
- Works unchanged

**New features opt-in:**
- Add `templateBackgrounds` to theme JSON to enable template-specific backgrounds
- Add `background` to slide frontmatter to override specific slides

## Success Metrics

**Target:**
- 95% of slides use template defaults (no per-slide override needed)
- 90% of themes define template-specific backgrounds for title vs content
- 0 backward compatibility issues

## Related Documents

- [Product Backlog](../../planning/product-backlog.md)
- [US-011: Per-Slide Background Images](../../planning/product-backlog.md#us-011-per-slide-background-images-could)
- [US-012: Template-Specific Background Defaults](../../planning/product-backlog.md#us-012-template-specific-background-defaults-should)
- [US-006: Image Asset Copying](../../ceremonies/v0.3.0.md#us-006-image-asset-copying)
- [ADR-008: Slot-Based Content Model](../adr/ADR-008-slot-based-content-model.md) (existing)
- [Retisio Theme Analysis](../../planning/retisio-theme-analysis.md)

---

**Decision Date:** 2024-12-26
**Approval Date:** 2024-12-26
**Status:** ✅ Approved - Ready for Event Storming

## Stakeholder Decisions (Approved)

All decisions approved by Product Owner (TJM) on 2024-12-26:

1. ✅ **Background as Slide Property (not slot)** - APPROVED
2. ✅ **Template Backgrounds in Theme (not Template)** - APPROVED
3. ✅ **Fallback chain: Slide → Template → Theme** - APPROVED
4. ✅ **Graceful degradation (warn on missing, don't fail)** - APPROVED
5. ✅ **Integration with US-006 image copying** - APPROVED
6. ✅ **Directory-based themes (US-016)** - APPROVED
7. ✅ **String OR object syntax for backgrounds** - APPROVED
8. ✅ **Preserve directory structure in output** - APPROVED

**Next Steps:** Proceed to Event Storming for US-011, US-012, and US-016
