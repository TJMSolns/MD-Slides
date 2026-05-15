# PDR-014: Configuration Management Strategy

**Status:** Approved
**Date:** 2024-12-26
**Deciders:** Product Team, Technical Lead
**Related:** US-017, PDR-013, product-backlog.md, v0.4.0-decisions-summary.md

## Context

As MDSlides grows, users need a way to configure default behaviors and preferences. Currently (v0.3.0), all configuration happens via CLI flags, which becomes tedious for repeated use.

**Common User Needs:**

1. **Theme Directory:** Where to find themes (`--theme` flag looks here)
2. **Default Theme:** Which theme to use if not specified
3. **Image Copying:** Default behavior for `--no-copy-images`
4. **Output Directory:** Default location for rendered presentations

**Key Design Questions:**

1. Where should configuration files live?
2. How should users manage configuration?
3. Should configuration be interactive, non-interactive, or both?
4. How do we handle configuration precedence (project vs user vs system)?

## Decision

### Configuration File Locations

**Three-tier configuration with precedence:**

1. **Project-local** (highest priority): `./.mdslides/config`
   - Project-specific settings
   - Committed to version control (team-shared config)

2. **User-level**: `~/.mdslides/config`
   - Personal preferences
   - Not shared across machines

3. **System-level** (lowest priority): `/usr/local/etc/mdslides/config`
   - System-wide defaults
   - Requires admin privileges to modify

**Precedence:** Project → User → System → Built-in defaults

**Rationale:**
- Standard Unix convention (follows .gitconfig, .npmrc pattern)
- Allows team-shared project config + personal overrides
- Clear precedence order

### Configuration File Format

**Simple key-value format (HOCON-like):**

```
# MDSlides Configuration
# Lines starting with # are comments

# Theme settings
theme_dir=./themes
default_theme=minimal

# Image settings
copy_images=true

# Output settings
default_output_dir=./output

# Future settings...
# slide_counter_enabled=true
# syntax_highlighting=true
```

**Rationale:**
- Simpler than JSON/YAML (no nesting needed for v0.4.0)
- Easy for users to hand-edit
- Can extend to nested format later if needed

### CLI Configuration Management

**Dual-mode approach: Interactive + Non-Interactive**

#### Non-Interactive (for scripting)

```bash
# Set a value
mdslides config set theme-dir ~/.mdslides/themes
mdslides config set default-theme retisio

# Get a value
mdslides config get theme-dir
# Output: ~/.mdslides/themes

# List all settings
mdslides config list
# Output:
# theme_dir=~/.mdslides/themes (user)
# default_theme=retisio (user)
# copy_images=true (default)

# Delete a setting (revert to default)
mdslides config unset default-theme
```

**Exit codes:**
- `0` = success
- `1` = error (invalid key, parse error, etc.)

**Scriptable:**
```bash
#!/bin/bash
THEME_DIR=$(mdslides config get theme-dir)
if [ $? -eq 0 ]; then
  echo "Themes located at: $THEME_DIR"
fi
```

#### Interactive (for user-friendliness)

```bash
# Launch interactive configuration wizard
mdslides config
```

**Interactive flow:**

```
MDSlides Configuration

Where should themes be located?
Current: ./themes (project)

1. Keep current value (./themes)
2. Use user themes (~/.mdslides/themes)
3. Enter custom path
4. Skip

Choice [1-4]: 2

✓ Set theme_dir=~/.mdslides/themes (saved to ~/.mdslides/config)

---

What is your default theme?
Current: minimal (default)

Available themes:
  - minimal
  - retisio
  - tjm-solutions

1. Keep current value (minimal)
2. Select from available themes
3. Skip

Choice [1-3]: 2

Select theme:
  1. minimal
  2. retisio
  3. tjm-solutions

Choice [1-3]: 2

✓ Set default_theme=retisio (saved to ~/.mdslides/config)

---

Configuration complete!

Summary:
  theme_dir=~/.mdslides/themes (user)
  default_theme=retisio (user)
  copy_images=true (default)

View all settings: mdslides config list
```

**Rationale:**
- Interactive mode guides non-technical users
- Non-interactive mode supports scripting/automation
- Single `config` command handles both modes

### Configuration Scope

**Specify which config file to modify:**

```bash
# Modify user config (default)
mdslides config set theme-dir ~/themes

# Modify project config
mdslides config set --scope project theme-dir ./themes

# Modify system config (requires sudo)
sudo mdslides config set --scope system theme-dir /usr/share/mdslides/themes

# View effective configuration (merged precedence)
mdslides config list

# View specific scope
mdslides config list --scope user
mdslides config list --scope project
```

**Default scope:** User config (`~/.mdslides/config`)

**Rationale:**
- Most users want personal config
- Project scope available when needed
- Explicit scope prevents accidental system-wide changes

## Supported Configuration Keys

### v0.4.0 Configuration Keys

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `theme_dir` | Path | `./themes` | Directory containing themes |
| `default_theme` | String | `minimal` | Theme to use when `--theme` not specified |
| `copy_images` | Boolean | `true` | Copy images to output directory by default |
| `default_output_dir` | Path | `./output` | Default output directory |

### Future Configuration Keys (v0.5.0+)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `slide_counter_enabled` | Boolean | `true` | Show slide counter by default |
| `syntax_highlighting` | Boolean | `true` | Enable code syntax highlighting |
| `validation_warnings` | Boolean | `true` | Show validation warnings |

**Validation:**
- Unknown keys → Warning (ignore, for forward compatibility)
- Invalid values → Error (halt, explain issue)

## Domain Model

### Config Aggregate

```scala
package com.tjmsolutions.mdslides.domain

/**
 * Configuration settings for MDSlides CLI.
 *
 * Represents merged configuration from all sources (project, user, system).
 */
case class Config(
  themeDir: Path,
  defaultTheme: Option[String],
  copyImages: Boolean,
  defaultOutputDir: Path
)

object Config:
  /** Built-in defaults (no config files) */
  val defaults: Config = Config(
    themeDir = Paths.get("./themes"),
    defaultTheme = Some("minimal"),
    copyImages = true,
    defaultOutputDir = Paths.get("./output")
  )

  /**
   * Load configuration with precedence:
   * project → user → system → defaults
   */
  def load(): IO[Config] =
    for
      system  <- loadSystemConfig()
      user    <- loadUserConfig()
      project <- loadProjectConfig()
    yield merge(defaults, system, user, project)

  private def merge(configs: Config*): Config =
    configs.foldLeft(defaults) { (acc, cfg) =>
      // Later configs override earlier ones
      cfg.copy(
        themeDir = if cfg.themeDir != defaults.themeDir then cfg.themeDir else acc.themeDir,
        defaultTheme = cfg.defaultTheme.orElse(acc.defaultTheme),
        // ... merge all fields
      )
    }
```

### ConfigFile (Infrastructure)

```scala
package com.tjmsolutions.mdslides.infrastructure

/**
 * Reads/writes configuration files.
 *
 * File format: simple key=value pairs
 */
object ConfigFile:
  def read(path: Path): IO[Map[String, String]] =
    if Files.exists(path) then
      IO(Files.readAllLines(path))
        .map(parseLines)
    else
      IO.pure(Map.empty)

  def write(path: Path, config: Map[String, String]): IO[Unit] =
    val content = config.map { case (k, v) => s"$k=$v" }.mkString("\n")
    IO(Files.createDirectories(path.getParent)) *>
      IO(Files.writeString(path, content))

  private def parseLines(lines: java.util.List[String]): Map[String, String] =
    lines.asScala
      .map(_.trim)
      .filterNot(line => line.isEmpty || line.startsWith("#"))
      .flatMap { line =>
        line.split("=", 2) match
          case Array(key, value) => Some(key.trim -> value.trim)
          case _ => None  // Invalid line - skip
      }
      .toMap
```

## CLI Implementation

### Config Command

```scala
object ConfigCommand:
  def run(args: List[String]): IO[ExitCode] =
    args match
      // Interactive mode
      case Nil =>
        runInteractive()

      // Non-interactive set
      case "set" :: key :: value :: Nil =>
        ConfigFile.set(key, value, scope = Scope.User)

      case "set" :: "--scope" :: scope :: key :: value :: Nil =>
        ConfigFile.set(key, value, Scope.parse(scope))

      // Non-interactive get
      case "get" :: key :: Nil =>
        Config.load().flatMap { cfg =>
          cfg.get(key) match
            case Some(value) => IO.println(value).as(ExitCode.Success)
            case None => IO.println(s"Key not set: $key").as(ExitCode.Error)
        }

      // List all settings
      case "list" :: Nil =>
        Config.loadWithSources().flatMap { configsWithSources =>
          configsWithSources.traverse_ { case (key, value, source) =>
            IO.println(s"$key=$value ($source)")
          }
        }

      case "unset" :: key :: Nil =>
        ConfigFile.unset(key, Scope.User)

      case _ =>
        IO.println(ConfigCommand.helpText).as(ExitCode.Error)

  private def runInteractive(): IO[ExitCode] =
    for
      _ <- IO.println("MDSlides Configuration\n")
      _ <- configureThemeDir()
      _ <- configureDefaultTheme()
      _ <- configureCopyImages()
      _ <- configureOutputDir()
      _ <- IO.println("\nConfiguration complete!")
      _ <- showSummary()
    yield ExitCode.Success
```

### Integration with Main

```scala
// Main.scala
def run(args: List[String]): IO[ExitCode] =
  args match
    case "config" :: configArgs =>
      ConfigCommand.run(configArgs)

    case "render" :: inputFile :: rest =>
      for
        config <- Config.load()  // Load config early
        // ... use config for defaults
        result <- renderSlides(inputFile, config)
      yield result

    // ... other commands
```

**CLI flag precedence:**
```
CLI flags > Project config > User config > System config > Built-in defaults
```

**Example:**
```bash
# User config: default_theme=retisio
# Command: mdslides render slides.md output/ --theme minimal

# Result: Uses minimal (CLI flag wins)
```

## User Experience

### First-Time Setup

```bash
# User installs MDSlides
$ mdslides render slides.md output/

✓ Generated presentation in output/
✓ Used theme: minimal (default)

Tip: Configure your preferences with 'mdslides config'
```

### Configuration Wizard

```bash
$ mdslides config

MDSlides Configuration

Where should themes be located?
Current: ./themes (default)

1. Keep current value (./themes)
2. Use user themes (~/.mdslides/themes)
3. Enter custom path
4. Skip

Choice [1-4]: 2

✓ Set theme_dir=~/.mdslides/themes (saved to ~/.mdslides/config)

# ... more prompts ...

Configuration complete!

Summary:
  theme_dir=~/.mdslides/themes (user)
  default_theme=retisio (user)

View all settings: mdslides config list
```

### Scripted Configuration

```bash
#!/bin/bash
# setup-mdslides.sh

# Configure for this project
mdslides config set --scope project theme-dir ./custom-themes
mdslides config set --scope project default-theme corporate

echo "MDSlides configured for this project"
```

### Team-Shared Configuration

```bash
# In project root
$ cat .mdslides/config
theme_dir=./themes
default_theme=corporate
copy_images=true

# Committed to git - entire team uses same config
$ git add .mdslides/config
$ git commit -m "Add MDSlides project configuration"
```

## Validation

### Configuration Validation

```scala
def validateConfig(config: Config): IO[List[String]] =
  val warnings = List.newBuilder[String]

  // Validate theme directory exists
  if !Files.exists(config.themeDir) then
    warnings += s"⚠ Theme directory does not exist: ${config.themeDir}"

  // Validate default theme exists
  config.defaultTheme.foreach { themeName =>
    val themePath = config.themeDir.resolve(themeName).resolve("theme.json")
    if !Files.exists(themePath) then
      warnings += s"⚠ Default theme not found: $themeName"
  }

  IO.pure(warnings.result())
```

**Behavior:**
- Warnings printed when loading config
- Don't fail - allow user to fix
- Example:
  ```
  ⚠ Theme directory does not exist: ~/.mdslides/themes
    Create it with: mkdir -p ~/.mdslides/themes
  ```

## Error Handling

### Invalid Configuration Values

```bash
$ mdslides config set copy-images maybe

Error: Invalid value for 'copy_images': maybe
Expected: true or false
```

### Non-Existent Keys

```bash
$ mdslides config get nonexistent-key

Error: Unknown configuration key: nonexistent-key

Valid keys:
  - theme_dir
  - default_theme
  - copy_images
  - default_output_dir

See 'mdslides config list' for current values.
```

### Permission Errors

```bash
$ mdslides config set --scope system theme-dir /usr/share/themes

Error: Permission denied: /usr/local/etc/mdslides/config
Try: sudo mdslides config set --scope system theme-dir /usr/share/themes
```

## Success Metrics

**Target:**
- 80% of repeat users configure at least one setting
- 95% successfully use `mdslides config` without consulting docs
- 0 support requests about "where do I put themes?"
- Configuration precedence behaves predictably (0 bug reports)

## Related Documents

- [Product Backlog](../../planning/product-backlog.md)
- [US-017: Configuration Management](../../planning/product-backlog.md)
- [PDR-013: Directory-Based Theme Architecture](PDR-013-directory-based-theme-architecture.md)
- [v0.4.0 Decisions Summary](../../planning/v0.4.0-decisions-summary.md)

---

**Decision Date:** 2024-12-26
**Approval Date:** 2024-12-26
**Status:** ✅ Approved - Ready for Event Storming

## Stakeholder Decisions (Approved)

All decisions approved by Product Owner (TJM) on 2024-12-26:

1. ✅ **Three-tier config (project/user/system)** - APPROVED
2. ✅ **Simple key=value format** - APPROVED
3. ✅ **Dual-mode CLI (interactive + non-interactive)** - APPROVED
4. ✅ **Config precedence: CLI → Project → User → System → Defaults** - APPROVED
5. ✅ **Scope flag for targeting specific config files** - APPROVED

**Next Steps:** Proceed to Event Storming for US-017
