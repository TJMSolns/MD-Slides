# PDR-013: Directory-Based Theme Architecture

**Status:** Approved
**Date:** 2024-12-26
**Deciders:** Product Team, Technical Lead
**Related:** US-016, PDR-011, product-backlog.md, v0.4.0-decisions-summary.md

## Context

Currently (v0.3.0), themes are single JSON files (`themes/retisio.json`). This works for themes with no local assets, but creates path resolution challenges when themes need to reference images, fonts, or other assets.

**Problems with Single-File Themes:**

1. **Asset Path Ambiguity:** Where do theme images live?
   - In presentation directory? (couples theme to presentation)
   - In shared location? (requires global configuration)
   - Absolute paths? (not portable)

2. **Theme Distribution:** How do users share themes?
   - JSON file + separate instructions for assets
   - Not self-contained

3. **Inconsistent with Slide Decks:** Presentations are directories, but themes are files
   - Mixed mental model

**Key Design Question:** Should themes be directories containing `theme.json` + assets?

## Decision

### Themes as Directories

**Themes are directories containing `theme.json` plus any assets (images, fonts, etc.).**

**Structure:**
```
themes/
  retisio/
    theme.json
    backgrounds/
      title-page.png
      content-page.png
      diagram-page.png
    logos/
      retisio-logo-white.png  (future use)
  minimal/
    theme.json  (no assets)
  tjm-solutions/
    theme.json
    backgrounds/
      tjm-background.png
```

**Benefits:**
1. **Self-Contained:** Theme is a portable unit (zip directory, share theme)
2. **Clear Path Resolution:** Paths in `theme.json` relative to theme directory
3. **Consistent Model:** Aligns with slide deck directory structure
4. **Distribution:** Easy to package and share themes
5. **Organizational Clarity:** Assets grouped with theme definition

## Path Resolution

### Theme-Relative Paths

All paths in `theme.json` are relative to the theme directory.

**Example (`themes/retisio/theme.json`):**
```json
{
  "name": "Retisio",
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

**Resolution:**
- `"backgrounds/title-page.png"` → `themes/retisio/backgrounds/title-page.png`
- Loader resolves `themeDir.resolve(imagePath)`

### Absolute Paths (Escape Hatch)

Themes can still reference absolute paths if needed:

```json
{
  "background": {
    "image": "/usr/share/branding/corporate-bg.png"
  }
}
```

**Use case:** Corporate branding in system-wide location

### Remote URLs (Existing Behavior)

Themes can reference remote images:

```json
{
  "background": {
    "image": "https://cdn.example.com/background.png"
  }
}
```

**Behavior:** Downloaded to output directory (unless `--no-copy-images`)

## Theme Discovery

### Theme Lookup Algorithm

When user specifies `--theme retisio`, CLI:

1. **Checks configured theme directory** (from config file)
   - Default: `./themes/` (relative to current working directory)
   - Configurable via `mdslides config set theme-dir /path`

2. **Looks for theme directory:**
   - `${theme_dir}/retisio/theme.json`
   - Example: `./themes/retisio/theme.json`

3. **Fails with clear error if not found:**
   ```
   Error: Theme 'retisio' not found in ./themes/

   Searched for: ./themes/retisio/theme.json

   Available themes:
     - minimal
     - tjm-solutions

   Use 'mdslides config set theme-dir /path' to change theme directory.
   ```

### Configuration Precedence

Theme directory can be configured at three levels:

1. **Project-local** (highest priority): `./.mdslides/config`
2. **User-level**: `~/.mdslides/config`
3. **System-level** (lowest priority): `/usr/local/etc/mdslides/config`

**Config format:**
```
theme_dir=./themes
```

**Rationale:** Allows project-specific themes + user-installed themes + system themes

## Migration Strategy

### Breaking Change

**Directory-based themes are a breaking change from v0.3.0.**

**Migration required:**
- `themes/retisio.json` → `themes/retisio/theme.json`
- No automatic migration (clean break)

**Rationale for Clean Break:**
- v0.3.0 has minimal adoption (early development)
- Mixed support (files + directories) adds complexity
- Better to migrate now than support two formats long-term

### Migration Script (Future)

Could provide migration tool in v0.4.0:

```bash
mdslides migrate-themes ./themes
```

**Behavior:**
- Scans for `*.json` files in themes directory
- Creates directory for each theme
- Moves JSON file to `theme.json` inside directory
- Warns user to move any referenced assets

**Decision for v0.4.0:** Manual migration only (document in release notes)

## Theme Copying to Output

### Image Copying Behavior

Theme images follow same rules as content images (US-006):

**Default (copy images):**
- Theme images copied to output directory
- Preserves directory structure
- Output presentation is self-contained

**Example:**
```
themes/retisio/backgrounds/title.png
  → output/backgrounds/title.png

themes/retisio/logos/logo.png
  → output/logos/logo.png
```

**With `--no-copy-images`:**
- Theme images NOT copied
- HTML references original paths
- User responsible for deploying theme directory alongside presentation

### Directory Structure Preservation

**Preserve source directory structure in output:**

```
Input:
  themes/retisio/
    backgrounds/
      title.png
      content.png
  my-slides/
    images/
      diagram.png

Output:
  output/
    presentation.html
    backgrounds/      ← from theme
      title.png
      content.png
    images/           ← from slides
      diagram.png
```

**Collision handling:**
- Different directories → no collision
- Same directory + same filename → warn and skip duplicate

**Rationale:**
- HTML references match source structure
- No renaming needed
- Matches user's mental model

## Domain Model Changes

### ThemeLoader

**New responsibility:** Load theme from directory

```scala
object ThemeLoader:
  def loadTheme(themeName: String, configuredThemeDir: Path): IO[Theme] =
    val themeDir = configuredThemeDir.resolve(themeName)
    val themeJson = themeDir.resolve("theme.json")

    if !Files.exists(themeJson) then
      IO.raiseError(ThemeNotFoundError(themeName, configuredThemeDir))
    else
      for
        jsonContent <- IO(Files.readString(themeJson))
        theme <- ThemeJsonAdapter.parse(jsonContent, themeDir)
      yield theme
```

**Key change:** Pass `themeDir` to `ThemeJsonAdapter` for path resolution

### ThemeJsonAdapter

**Enhanced to resolve theme-relative paths:**

```scala
def parse(json: String, themeDir: Path): Either[String, Theme] =
  // Parse JSON
  val background = parseBackground(json, themeDir)  // NEW: resolve paths
  val templateBackgrounds = parseTemplateBackgrounds(json, themeDir)  // NEW

  // ...

def resolveImagePath(path: String, themeDir: Path): String =
  if path.startsWith("http://") || path.startsWith("https://") then
    path  // Remote URL - use as-is
  else if Paths.get(path).isAbsolute then
    path  // Absolute path - use as-is
  else
    themeDir.resolve(path).toString  // Relative path - resolve against theme dir
```

### ImageAssetCopier

**Update to handle theme images:**

```scala
def extractAllImages(
  deck: SlideDeck,
  theme: Theme,
  themeDir: Path,
  presentationDir: Path
): List[ImageSource] =

  val contentImages = deck.slides.flatMap(extractSlideImages)
    .map(path => ImageSource(path, presentationDir))

  val themeImages = extractThemeImages(theme)
    .map(path => ImageSource(path, themeDir))

  (contentImages ++ themeImages).distinct

case class ImageSource(
  path: String,
  sourceDir: Path  // Theme dir or presentation dir
)
```

**Key change:** Track source directory per image for correct resolution

## Validation

### Theme Directory Validation

When loading a theme:

```scala
def validateThemeDirectory(themeDir: Path, themeName: String): IO[Unit] =
  if !Files.exists(themeDir) then
    IO.raiseError(ThemeNotFoundError(themeName, themeDir.getParent))
  else if !Files.isDirectory(themeDir) then
    IO.raiseError(ThemeNotDirectoryError(themeName, themeDir))
  else if !Files.exists(themeDir.resolve("theme.json")) then
    IO.raiseError(ThemeJsonMissingError(themeName, themeDir))
  else
    IO.unit
```

### Image Path Validation

**Validate at render time (not load time):**

```scala
// When rendering, check if theme image exists
themeBackgroundPath match
  case path if isRemoteUrl(path) =>
    // Remote URL - will be downloaded
    renderWithBackground(path)

  case path =>
    val resolved = themeDir.resolve(path)
    if Files.exists(resolved) then
      copyAndRenderBackground(path)
    else
      IO.println(s"⚠ Warning: Theme background image not found: $path") *>
        renderWithoutBackground()
```

**Rationale:** Graceful degradation (warn, don't fail)

## CLI Changes

### Theme Selection (No Change)

```bash
mdslides render slides.md output/ --theme retisio
```

**Behavior change:** Now looks for `themes/retisio/theme.json` instead of `themes/retisio.json`

### Theme Configuration (New)

```bash
# Set theme directory
mdslides config set theme-dir ~/.mdslides/themes

# Get current theme directory
mdslides config get theme-dir

# Interactive configuration
mdslides config
```

**See PDR-014 for full configuration management design.**

## User Experience

### Creating a New Theme

**Before (v0.3.0):**
```bash
# Create theme JSON
vim themes/my-theme.json

# Images? Unclear where to put them...
```

**After (v0.4.0):**
```bash
# Create theme directory
mkdir -p themes/my-theme/backgrounds

# Create theme JSON
vim themes/my-theme/theme.json

# Add background images
cp images/*.png themes/my-theme/backgrounds/

# Self-contained theme ready to share
```

### Sharing a Theme

**Before (v0.3.0):**
```bash
# Share JSON file
cp themes/my-theme.json /path/to/share/

# Separately tell user where to put images...
# Error-prone
```

**After (v0.4.0):**
```bash
# Package entire theme
cd themes
tar czf my-theme.tar.gz my-theme/

# Or zip it
zip -r my-theme.zip my-theme/

# Share single archive - everything included
```

### Installing a Theme

**Before (v0.3.0):**
```bash
# Copy JSON
cp my-theme.json ~/themes/

# Also need to copy images somewhere...
# Unclear
```

**After (v0.4.0):**
```bash
# Extract theme archive
cd ~/.mdslides/themes
tar xzf my-theme.tar.gz

# Done - theme is self-contained
mdslides render slides.md output/ --theme my-theme
```

## Success Metrics

**Target:**
- 100% of built-in themes migrated to directory format
- 0 backward compatibility issues (clean break)
- Theme sharing workflow reduces steps from ~5 to ~2
- Clear error messages for theme-not-found (95% of users understand how to fix)

## Related Documents

- [Product Backlog](../../planning/product-backlog.md)
- [US-016: Directory-Based Themes](../../planning/product-backlog.md)
- [PDR-011: Background Image Architecture](PDR-011-background-image-architecture.md)
- [PDR-014: Configuration Management Strategy](PDR-014-configuration-management-strategy.md)
- [v0.4.0 Decisions Summary](../../planning/v0.4.0-decisions-summary.md)

---

**Decision Date:** 2024-12-26
**Approval Date:** 2024-12-26
**Status:** ✅ Approved - Ready for Event Storming

## Stakeholder Decisions (Approved)

All decisions approved by Product Owner (TJM) on 2024-12-26:

1. ✅ **Directory-based themes (not single files)** - APPROVED
2. ✅ **Theme-relative path resolution** - APPROVED
3. ✅ **Clean break migration (no backward compatibility)** - APPROVED
4. ✅ **Configurable theme directory** - APPROVED
5. ✅ **Preserve directory structure in output** - APPROVED
6. ✅ **Unified image copying (theme + content)** - APPROVED

**Next Steps:** Proceed to Event Storming for US-016
