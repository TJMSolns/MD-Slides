package com.tjmsolutions.mdslides.cli

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.*
import com.tjmsolutions.mdslides.domain.{SlideDeck, Theme, ContentImage, DefaultSettings, ConfigurationMerger, CLIOverrides, MergedConfiguration, GlobalConfig, ValidationError, MermaidDiagram, DiagramElement}
import com.tjmsolutions.mdslides.infrastructure.parser.{MarkdownParser, FlexmarkAdapter}
import com.tjmsolutions.mdslides.infrastructure.renderer.HTMLRenderer
import com.tjmsolutions.mdslides.infrastructure.rendering.SpeakerViewRenderer
import com.tjmsolutions.mdslides.infrastructure.diagram.MermaidRenderer
import com.tjmsolutions.mdslides.infrastructure.theme.{ThemeJsonAdapter, ThemeLoader}
import com.tjmsolutions.mdslides.infrastructure.assets.{ImageAssetCopier, CopiedImage}
import com.tjmsolutions.mdslides.infrastructure.accessibility.{AccessibilityService, AccessibilityReportWriter}
import com.tjmsolutions.mdslides.infrastructure.config.ConfigLoader

import java.nio.file.{Files, Path, Paths, StandardOpenOption}

/**
 * CLI entry point for MDSlides (v1.0).
 *
 * Commands:
 * - render: Convert markdown to HTML presentation directory
 *
 * Usage:
 * ```
 * # Simple form (inferred input/output)
 * mdslides render my-preso
 * mdslides render my-preso --theme dark
 *
 * # Explicit form (custom paths)
 * mdslides render -i slides.md -o output-dir
 * mdslides render --input slides.md --output dist --theme retisio
 *
 * # Flags
 * mdslides render my-preso --no-copy-images
 * ```
 *
 * Related Governance:
 * - US-019: Improved CLI UX (v1.0)
 * - PDR-015: CLI UX Design
 * - ADR-012: Image Asset Copying Strategy
 * - US-016: Directory-Based Themes
 */
object Main extends IOApp:

  /**
   * Load theme from name using directory-based themes (US-016).
   *
   * Uses theme directory from config (globalConfig.paths.themesDir) or defaults to ./themes
   */
  def loadTheme(themeName: String, globalConfig: Option[GlobalConfig]): IO[Theme] =
    themeName match
      case "light" => IO.pure(Theme.light)
      case "dark" => IO.pure(Theme.dark)
      case "corporate" => IO.pure(Theme.corporate)  // Legacy built-in theme
      case name =>
        // Get theme directory from config or use default
        val themeDir = globalConfig
          .flatMap(_.paths)
          .map(p => Paths.get(p.themesDir))
          .getOrElse(Paths.get("./themes"))

        ThemeLoader.loadTheme(name, themeDir)
          .flatTap(t => IO.println(s"✓ Loaded theme: ${t.name} v${t.version}"))
          .handleErrorWith { error =>
            IO.println(s"✗ Theme load error: ${error.getMessage}") *>
              IO.raiseError(error)
          }

  /**
   * Extract all image URLs from deck (content + backgrounds).
   */
  def extractImageUrls(deck: SlideDeck): List[String] =
    import com.tjmsolutions.mdslides.domain.{SlideBackground, BackgroundConfig}

    val contentImages = deck.slides.toList.flatMap { slide =>
      slide.slots.values.toList.flatMap { slotContent =>
        FlexmarkAdapter.parseInlineFormatting(slotContent).contentImages.map(_.url)
      }
    }

    val backgroundImages = deck.slides.toList.flatMap { slide =>
      slide.backgroundImage match
        case Some(bg: String) => List(bg)
        case Some(bg: BackgroundConfig) => List(bg.imagePath)
        case None => List()
    }

    (contentImages ++ backgroundImages).distinct

  /**
   * Extract theme background images (US-012).
   */
  def extractThemeImages(theme: Theme): List[String] =
    val defaultBg = theme.background.image.toList
    val templateBgs = theme.templateBackgrounds.values.toList
    (defaultBg ++ templateBgs).distinct

  /**
   * Extract all ContentImage objects from deck for accessibility validation.
   */
  def extractContentImages(deck: SlideDeck): List[ContentImage] =
    deck.slides.toList.flatMap { slide =>
      slide.slots.values.toList.flatMap { slotContent =>
        FlexmarkAdapter.parseInlineFormatting(slotContent).contentImages
      }
    }

  /**
   * Extract all MermaidDiagram objects from deck for server-side pre-rendering.
   *
   * Diagrams are rendered to SVG at build time for offline support (US-022).
   */
  def extractMermaidDiagrams(deck: SlideDeck): List[MermaidDiagram] =
    deck.slides.toList.flatMap { slide =>
      slide.slots.values.toList.flatMap { slotContentString =>
        val formatted = FlexmarkAdapter.parseInlineFormatting(slotContentString)
        formatted.content.collect {
          case DiagramElement(diagram: MermaidDiagram) => diagram
        }
      }
    }

  /**
   * Map MDSlides theme to appropriate Mermaid theme.
   *
   * Mermaid supports: default, dark, forest, neutral, base
   * We map based on background brightness for better diagram visibility.
   *
   * @param theme MDSlides theme
   * @return Mermaid theme name
   */
  def mermaidTheme(theme: Theme): String =
    theme.name.toLowerCase match
      case "dark" => "dark"
      case "forest" => "forest"
      case "neutral" => "neutral"
      case _ => "default" // light, retisio, tjm-solutions use default (light diagrams)

  /**
   * Display accessibility validation results to console.
   */
  def displayAccessibilityResults(report: com.tjmsolutions.mdslides.domain.AccessibilityReport): IO[Unit] =
    if report.passes then
      // Success case (Scenario 18)
      for {
        _ <- IO.println("\n✓ Accessibility validation passed (WCAG 2.1 AA)")
        _ <- IO.println(s"  ✓ Contrast ratios: ${report.contrastChecks.size}/${report.contrastChecks.size} passed")
        _ <- IO.println(s"  ✓ Alt text: ${report.altTextChecks.imagesChecked}/${report.altTextChecks.imagesChecked} images validated")
        _ <- if report.keyboardChecks.handlersMissing.isEmpty then
          IO.println("  ✓ Keyboard navigation: All handlers present")
        else
          IO.unit
      } yield ()
    else
      // Warning case (Scenario 19)
      val contrastPassed = report.contrastChecks.size - report.warnings.count(_.category == "contrast")
      val contrastTotal = report.contrastChecks.size

      for {
        _ <- IO.println("\n⚠ Accessibility validation completed with warnings")
        _ <- IO.println(s"  ${if contrastPassed == contrastTotal then "✓" else "✗"} Contrast ratios: $contrastPassed/$contrastTotal passed")

        // Display contrast warnings
        _ <- report.warnings.filter(_.category == "contrast").traverse_ { warning =>
          IO.println(s"    WARNING: ${warning.message}")
        }

        // Display alt text results
        _ <- if report.altTextChecks.imagesMissingAlt == 0 then
          IO.println(s"  ✓ Alt text: ${report.altTextChecks.imagesChecked}/${report.altTextChecks.imagesChecked} images validated")
        else
          IO.println(s"  ✗ Alt text: ${report.altTextChecks.imagesChecked - report.altTextChecks.imagesMissingAlt}/${report.altTextChecks.imagesChecked} images validated") *>
          report.warnings.filter(_.category == "alt-text").traverse_ { warning =>
            IO.println(s"    WARNING: ${warning.message}")
          }

        // Display keyboard navigation results
        _ <- if report.keyboardChecks.handlersMissing.isEmpty then
          IO.println("  ✓ Keyboard navigation: All handlers present")
        else
          IO.println(s"  ✗ Keyboard navigation: ${report.keyboardChecks.handlersFound.size}/${report.keyboardChecks.handlersFound.size + report.keyboardChecks.handlersMissing.size} handlers present") *>
          report.warnings.filter(_.category == "keyboard").traverse_ { warning =>
            IO.println(s"    WARNING: ${warning.message}")
          }

        _ <- IO.println("")
        _ <- IO.println("Note: Presentation generated despite warnings. Use --skip-accessibility to suppress.")
      } yield ()

  /**
   * Load and merge configuration from all sources (v2.0.0).
   *
   * Precedence: CLI > project > global > defaults
   *
   * @return (MergedConfiguration, Option[GlobalConfig]) - merged config and global config for theme loading
   */
  def loadConfiguration(cliArgs: CLIArguments, currentDir: Path): IO[(MergedConfiguration, Option[GlobalConfig])] =
    val configLoader = new ConfigLoader()

    for {
      // Load global config from ~/.mdslides/config.json (Scenario 32)
      globalConfig <- IO.fromEither(
        configLoader.loadGlobalConfig().left.map(new RuntimeException(_))
      ).handleErrorWith(error => IO.println(s"Warning: Failed to load global config: ${error.getMessage}").as(None))

      // Load project config from .mdslides/config.json with upward search (Scenarios 35-36)
      projectConfig <- IO.fromEither(
        configLoader.loadProjectConfig(currentDir).left.map(new RuntimeException(_))
      ).handleErrorWith(error => IO.println(s"Warning: Failed to load project config: ${error.getMessage}").as(None))

      // Hard-coded defaults
      defaults = DefaultSettings(
        theme = "light",
        copyImages = true,
        skipAccessibility = false,
        breakScreen = None
      )

      // Convert CLI arguments to CLIOverrides
      cliOverrides = CLIOverrides(
        theme = Some(cliArgs.themeName).filter(_ != defaults.theme),
        copyImages = if cliArgs.copyImages == defaults.copyImages then None else Some(cliArgs.copyImages),
        skipAccessibility = if cliArgs.skipAccessibility == defaults.skipAccessibility then None else Some(cliArgs.skipAccessibility),
        accessibilityReportPath = cliArgs.accessibilityReportPath.map(_.toString),
        breakScreen = cliArgs.breakScreen
      )

      // Merge all configurations (Scenario 37, 41)
      merged = ConfigurationMerger.merge(defaults, globalConfig, projectConfig, cliOverrides)

      _ <- IO.println(s"Configuration loaded: theme=${merged.theme}, copyImages=${merged.copyImages}, skipAccessibility=${merged.skipAccessibility}")
    } yield (merged, globalConfig)

  /**
   * Main rendering logic.
   */
  def renderPresentation(cliArgs: CLIArguments, config: MergedConfiguration, globalConfig: Option[GlobalConfig]): IO[ExitCode] =
    for {
      // Resolve input and output paths
      inputPath <- cliArgs.deckName match
        case Some(deckName) =>
          // Simple form: infer from deck name
          IO.fromEither(PathResolver.findInputFile(deckName).left.map(new RuntimeException(_)))
        case None =>
          // Explicit form: use provided input file
          IO.pure(cliArgs.inputFile.get)

      outputDir <- config.outputDir match
        case Some(configuredOutputDir) =>
          // Use output dir from project config (v2.0.0)
          IO.pure(Paths.get(configuredOutputDir))
        case None =>
          cliArgs.deckName match
            case Some(deckName) =>
              // Simple form: DECK_NAME → DECK_NAME/ directory
              IO.pure(PathResolver.determineOutputDir(deckName))
            case None =>
              // Explicit form: use provided output directory
              IO.pure(cliArgs.outputDir.get)

      // Create output directory
      _ <- PathResolver.ensureOutputDirExists(outputDir)

      // Load theme (use merged config theme name, but global config for theme directory)
      _ <- IO.println(s"Loading theme: ${config.theme}")
      theme <- loadTheme(config.theme, globalConfig)

      // Read input file
      _ <- IO.println(s"Reading markdown from: $inputPath")
      markdown <- IO.blocking(Files.readString(inputPath))

      // Parse markdown
      _ <- IO.println("Parsing markdown...")
      deck <- MarkdownParser.parse(markdown) match
        case Right(deck) =>
          IO.println(s"✓ Parsed ${deck.slideCount} slide(s)").as(deck)
        case Left(error) =>
          IO.println(s"✗ Parse error: $error") *> IO.raiseError(new RuntimeException(error))

      // Validate deck (PDR-001: separate warnings from errors)
      _ <- IO.println("Validating slide deck...")
      validatedDeck <- SlideDeck.validated(deck.slides) match
        case Right(validDeck) =>
          IO.println("✓ Validation passed").as(validDeck)
        case Left(allIssues) =>
          // Separate blocking errors from non-blocking warnings
          val (warnings, blockingErrors) = allIssues.toList.partition {
            case ValidationError.DensityWarning(_, _, _, _) => true
            case _ => false
          }

          // If only warnings (no blocking errors), continue with warnings
          if blockingErrors.isEmpty then
            val warningMessages = warnings.map(_.displayMessage).mkString("\n  - ")
            IO.println(s"✓ Validation passed with ${warnings.length} warning(s):\n  - $warningMessages")
              .as(SlideDeck(deck.slides))
          else
            // Blocking errors present - fail rendering
            val errorMessages = blockingErrors.map(_.displayMessage).mkString("\n  - ")
            IO.println(s"✗ Validation failed:\n  - $errorMessages") *>
              IO.raiseError(new RuntimeException("Validation failed"))

      // Copy images if enabled
      _ <- if cliArgs.copyImages then
        for {
          _ <- IO.println("Copying images...")

          // Extract deck images
          deckImages = extractImageUrls(validatedDeck)

          // Extract theme images
          themeImages = extractThemeImages(theme)

          sourceDir = inputPath.getParent match
            case null => Paths.get(".")
            case dir => dir

          // Copy deck images to OUTPUT_DIR/images/
          imagesDir = outputDir.resolve("images")
          deckResult <- if deckImages.nonEmpty then
            IO.fromEither(
              ImageAssetCopier.copyImages(deckImages, sourceDir, imagesDir)
                .left.map(err => new RuntimeException(err))
            )
          else
            IO.pure(List.empty[CopiedImage])

          // Copy theme images to OUTPUT_DIR/ (theme paths already include backgrounds/ prefix)
          themeResult <- if themeImages.nonEmpty && config.theme != "light" && config.theme != "dark" && config.theme != "corporate" then
            val themesBaseDir = globalConfig
              .flatMap(_.paths)
              .map(p => Paths.get(p.themesDir))
              .getOrElse(Paths.get("./themes"))
            val themeDir = themesBaseDir.resolve(config.theme)
            IO.fromEither(
              ImageAssetCopier.copyImages(themeImages, themeDir, outputDir)
                .left.map(err => new RuntimeException(err))
            )
          else
            IO.pure(List.empty[CopiedImage])

          result = deckResult ++ themeResult
          _ <- if result.isEmpty then
            IO.println("  No images to copy")
          else
            val totalSize = result.map(_.sizeBytes).sum
            val totalSizeKB = totalSize / 1024.0
            IO.println(s"✓ Copied ${result.length} image(s) (${f"$totalSizeKB%.1f"} KB total)")
        } yield ()
      else
        IO.println("Image copying disabled (--no-copy-images)")

      // Pre-render mermaid diagrams for offline support (US-022)
      diagrams = extractMermaidDiagrams(validatedDeck)
      _ <- if diagrams.nonEmpty then
        IO.println(s"Pre-rendering ${diagrams.length} mermaid diagram(s)...")
      else
        IO.unit
      preRenderedDiagrams <- diagrams.zipWithIndex.traverse { case (diagram, index) =>
        val diagramNum = index + 1
        val preview = diagram.source.take(50).replaceAll("\n", " ")
        IO.println(s"  Rendering diagram $diagramNum/${diagrams.length}: ${diagram.diagramType} ($preview...)") *>
        MermaidRenderer.renderToSVG(diagram.source, diagram.diagramType, mermaidTheme(theme)).map {
          case Right(svg) =>
            (diagram.source, svg)
          case Left(error) =>
            // Log warning but continue with fallback
            println(s"⚠ Warning: Failed to render diagram $diagramNum: $error")
            (diagram.source, "") // Empty string triggers fallback in renderer
        }
      }.map(_.toMap)
      _ <- if diagrams.nonEmpty && preRenderedDiagrams.nonEmpty then
        IO.println(s"✓ Pre-rendered ${preRenderedDiagrams.size} diagram(s)")
      else if diagrams.nonEmpty then
        IO.println("⚠ Diagrams will use fallback rendering (install mermaid-cli for full support)")
      else
        IO.unit

      // Render to HTML with theme, pre-rendered diagrams, header/footer, and break screen (v3.0.0)
      _ <- IO.println(s"Rendering HTML with theme: ${theme.name}")
      html = HTMLRenderer.renderDeck(validatedDeck, theme, preRenderedDiagrams, config.header, config.footer, config.breakScreen)
      _ <- IO.println(s"✓ Generated ${html.length} characters of HTML")

      // Write index.html to output directory
      indexPath = outputDir.resolve("index.html")
      _ <- IO.println(s"Writing HTML to: $indexPath")
      _ <- IO.blocking(Files.writeString(indexPath, html, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))

      // Render and write speaker.html (US-034, use merged config theme)
      _ <- IO.println("Generating speaker view...")
      speakerHtml = SpeakerViewRenderer.render(validatedDeck, config.theme)
      speakerPath = outputDir.resolve("speaker.html")
      _ <- IO.blocking(Files.writeString(speakerPath, speakerHtml, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))

      // Write sync.js (US-034)
      syncJsContent = scala.io.Source.fromResource("sync.js").mkString
      syncJsPath = outputDir.resolve("sync.js")
      _ <- IO.blocking(Files.writeString(syncJsPath, syncJsContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))

      // Write presentation-timer.js (v3.0.0)
      timerJsContent = scala.io.Source.fromResource("presentation-timer.js").mkString
      timerJsPath = outputDir.resolve("presentation-timer.js")
      _ <- IO.blocking(Files.writeString(timerJsPath, timerJsContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))

      // Write navigation-history.js (v3.0.0)
      navHistoryJsContent = scala.io.Source.fromResource("navigation-history.js").mkString
      navHistoryJsPath = outputDir.resolve("navigation-history.js")
      _ <- IO.blocking(Files.writeString(navHistoryJsPath, navHistoryJsContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))

      // Write goto-function.js (v3.0.0)
      gotoJsContent = scala.io.Source.fromResource("goto-function.js").mkString
      gotoJsPath = outputDir.resolve("goto-function.js")
      _ <- IO.blocking(Files.writeString(gotoJsPath, gotoJsContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))

      // Write history-logging.js (v3.0.0)
      historyLoggingJsContent = scala.io.Source.fromResource("history-logging.js").mkString
      historyLoggingJsPath = outputDir.resolve("history-logging.js")
      _ <- IO.blocking(Files.writeString(historyLoggingJsPath, historyLoggingJsContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))

      // Write header-footer.js (v3.0.0)
      headerFooterJsContent = scala.io.Source.fromResource("header-footer.js").mkString
      headerFooterJsPath = outputDir.resolve("header-footer.js")
      _ <- IO.blocking(Files.writeString(headerFooterJsPath, headerFooterJsContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))

      // Write break-mode.js (v3.0.0)
      breakModeJsContent = scala.io.Source.fromResource("break-mode.js").mkString
      breakModeJsPath = outputDir.resolve("break-mode.js")
      _ <- IO.blocking(Files.writeString(breakModeJsPath, breakModeJsContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))

      // Run accessibility validation (US-014) unless skipped (use merged config)
      _ <- if !config.skipAccessibility then
        for {
          _ <- IO.println("Running accessibility validation...")
          accessibilityService = new AccessibilityService()
          images = extractContentImages(validatedDeck)
          report <- IO.blocking(accessibilityService.validate(theme, images, indexPath))

          // Display results (Scenarios 18-19)
          _ <- displayAccessibilityResults(report)

          // Write JSON report if requested (Scenarios 16-17, use merged config)
          _ <- config.accessibilityReportPath match
            case Some(reportPathStr) =>
              val reportPath = Paths.get(reportPathStr)
              IO.blocking(AccessibilityReportWriter.writeReport(report, inputPath, reportPath)) *>
                IO.println(s"✓ Accessibility report written to: $reportPath")
            case None =>
              IO.unit
        } yield ()
      else
        IO.println("Accessibility validation skipped (--skip-accessibility)")

      _ <- IO.println(s"\n✓ Successfully created presentation: $outputDir/")
      _ <- IO.println(s"  - Main presentation: $outputDir/index.html")
      _ <- IO.println(s"  - Speaker view: $outputDir/speaker.html (press 'S' to open)")
    } yield ExitCode.Success

  /**
   * Show configuration information (config command).
   */
  def showConfig(): IO[ExitCode] =
    import com.tjmsolutions.mdslides.domain.{ProjectConfig, GlobalConfig}

    val configLoader = new ConfigLoader()
    val home = Paths.get(System.getProperty("user.home"))
    val globalConfigPath = home.resolve(".mdslides").resolve("config.json")

    for {
      currentDir <- IO.blocking(Paths.get(".").toAbsolutePath.normalize())

      // Display header
      _ <- IO.println("═" * 70)
      _ <- IO.println("MDSlides Configuration")
      _ <- IO.println("═" * 70)
      _ <- IO.println("")

      // Global config
      _ <- IO.println("Global Configuration:")
      _ <- IO.println(s"  Location: $globalConfigPath")
      globalConfig <- IO.fromEither(
        configLoader.loadGlobalConfig().left.map(err => new RuntimeException(err))
      ).handleErrorWith(err => IO.println(s"  Error loading: ${err.getMessage}").as(None))
      _ <- globalConfig match
        case Some(gc) =>
          IO.println("  Status: Found") *>
          IO.println("  Settings:") *>
          (gc.defaults match
            case Some(d) =>
              IO.println(s"    defaults.theme: ${d.theme}") *>
              IO.println(s"    defaults.copyImages: ${d.copyImages}") *>
              IO.println(s"    defaults.skipAccessibility: ${d.skipAccessibility}")
            case None => IO.unit) *>
          (gc.paths match
            case Some(p) => IO.println(s"    paths.themesDir: ${p.themesDir}")
            case None => IO.unit) *>
          (gc.author match
            case Some(a) =>
              IO.println(s"    author.name: ${a.name}") *>
              IO.println(s"    author.email: ${a.email}")
            case None => IO.unit)
        case None =>
          IO.println("  Status: Not found") *>
          IO.println(s"  Create: echo '{{}}' > $globalConfigPath")

      _ <- IO.println("")

      // Project config
      _ <- IO.println("Project Configuration:")
      projectConfigPath <- IO(configLoader.findProjectConfigPath(currentDir))
      projectConfig <- IO.fromEither(
        configLoader.loadProjectConfig(currentDir).left.map(err => new RuntimeException(err))
      ).handleErrorWith(err => IO.println(s"  Error loading: ${err.getMessage}").as(None))

      _ <- projectConfigPath match
        case Some(path) =>
          IO.println(s"  Location: $path") *>
          (projectConfig match
            case Some(pc) =>
              IO.println("  Status: Found") *>
              IO.println("  Settings:") *>
              (pc.theme match
                case Some(t) => IO.println(s"    theme: $t")
                case None => IO.unit) *>
              (pc.copyImages match
                case Some(ci) => IO.println(s"    copyImages: $ci")
                case None => IO.unit) *>
              (pc.skipAccessibility match
                case Some(sa) => IO.println(s"    skipAccessibility: $sa")
                case None => IO.unit) *>
              (pc.outputDir match
                case Some(od) => IO.println(s"    outputDir: $od")
                case None => IO.unit)
            case None => IO.println("  Status: Not found"))
        case None =>
          val suggestedPath = currentDir.resolve(".mdslides").resolve("config.json")
          IO.println("  Location: Not found (searched upward from current directory)") *>
          IO.println(s"  Create: mkdir -p .mdslides && echo '{{}}' > $suggestedPath")

      _ <- IO.println("")
      _ <- IO.println("─" * 70)
      _ <- IO.println("Configuration Hierarchy (highest to lowest priority):")
      _ <- IO.println("  1. CLI arguments (--theme, --no-copy-images, etc.)")
      _ <- IO.println("  2. Project config (.mdslides/config.json)")
      _ <- IO.println("  3. Global config (~/.mdslides/config.json)")
      _ <- IO.println("  4. Defaults (theme=light, copyImages=true)")
      _ <- IO.println("")
      _ <- IO.println("Documentation:")
      _ <- IO.println("  Project config fields: theme, copyImages, skipAccessibility, outputDir")
      _ <- IO.println("  Global config fields: defaults, author, paths")
      _ <- IO.println("  See examples/README.md for full documentation")
      _ <- IO.println("═" * 70)
    } yield ExitCode.Success

  /**
   * Initialize log file with session metadata (AC-3: History Logging).
   *
   * Creates a JSON log file with:
   * - sessionId (UUID)
   * - presentationName
   * - startTime (ISO 8601)
   * - theme (placeholder, will be filled by JS)
   * - totalSlides (placeholder, will be filled by JS)
   * - slides (empty array, will be filled by JS)
   * - events (empty array, will be filled by JS)
   *
   * AC-4 (Display Command): Log file overwrites previous session.
   */
  def initializeLogFile(logFile: Path, deckName: String): IO[Unit] =
    IO {
      import java.util.UUID
      import java.time.Instant
      import java.time.format.DateTimeFormatter

      val sessionId = UUID.randomUUID().toString
      val startTime = Instant.now().atZone(java.time.ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT)

      val logJson = s"""{
  "session": {
    "sessionId": "$sessionId",
    "presentationName": "$deckName",
    "startTime": "$startTime",
    "theme": "unknown",
    "totalSlides": 0,
    "endTime": null
  },
  "slides": [],
  "events": []
}"""

      // AC-4: Overwrite previous log (truncate)
      Files.writeString(logFile, logJson, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)
      println(s"  Log file initialized: ${logFile.getFileName}")
    }.handleErrorWith { error =>
      // AC-5 (Display Command): Log creation failure is non-fatal
      IO.println(s"  ⚠ Warning: Could not create log file: ${error.getMessage}")
    }

  /**
   * Display command: Open presentation in browser (v3.0.0).
   */
  def displayCommand(deckName: String): IO[ExitCode] =
    val outputDir = PathResolver.determineOutputDir(deckName)
    val indexPath = outputDir.resolve("index.html")
    val logFile = outputDir.resolve(s"${outputDir.getFileName}.log")

    for {
      // Check if presentation exists
      exists <- IO.blocking(Files.exists(indexPath))
      exitCode <- if !exists then
        IO.println(s"✗ Presentation not rendered. Run 'java -jar mdslides.jar render $deckName' first.").as(ExitCode.Error)
      else
        for {
          // AC-3 (History Logging): Create log file with session metadata
          _ <- initializeLogFile(logFile, deckName)
          // Launch browser
          exitCode <- launchBrowser(indexPath)
        } yield exitCode
    } yield exitCode

  /**
   * Launch browser with presentation.
   */
  def launchBrowser(htmlPath: Path): IO[ExitCode] =
    IO {
      val url = s"file://${htmlPath.toAbsolutePath}"
      val osName = System.getProperty("os.name").toLowerCase

      val command = if osName.contains("linux") then
        Seq("xdg-open", url)
      else if osName.contains("mac") then
        Seq("open", url)
      else // Windows
        Seq("cmd", "/c", "start", url)

      import scala.sys.process.*
      command.run()
      println(s"✓ Opened presentation in browser: $url")
      ExitCode.Success
    }.handleErrorWith { error =>
      IO.println(s"✗ Failed to open browser: ${error.getMessage}").as(ExitCode.Error)
    }

  /**
   * Report command: Display session log (v3.0.0).
   */
  def reportCommand(deckName: String): IO[ExitCode] =
    val outputDir = PathResolver.determineOutputDir(deckName)
    val logFile = outputDir.resolve(s"${outputDir.getFileName}.log")

    for {
      // Check if log file exists
      exists <- IO.blocking(Files.exists(logFile))
      exitCode <- if !exists then
        IO.println(s"✗ No log file found for presentation: $deckName").as(ExitCode.Error)
      else
        // Read and parse log file
        parseAndDisplayLog(logFile)
    } yield exitCode

  /**
   * Parse and display session log with Unicode table formatting (AC-5 through AC-11).
   */
  def parseAndDisplayLog(logFile: Path): IO[ExitCode] =
    IO.blocking {
      import io.circe.parser.parse
      import io.circe.Json

      val content = Files.readString(logFile)

      // Parse JSON
      parse(content) match {
        case Right(json) =>
          // Extract session info
          val session = json.hcursor.downField("session")
          val presentationName = session.get[String]("presentationName").getOrElse("Unknown")
          val startTime = session.get[String]("startTime").getOrElse("Unknown")
          val theme = session.get[String]("theme").getOrElse("unknown")
          val totalSlides = session.get[Int]("totalSlides").getOrElse(0)
          val endTime = session.get[Option[String]]("endTime").toOption.flatten

          // Extract slides array
          val slides = json.hcursor.downField("slides").focus.flatMap(_.asArray).getOrElse(Vector.empty)

          // Extract events array
          val events = json.hcursor.downField("events").focus.flatMap(_.asArray).getOrElse(Vector.empty)

          // Print header
          println("═" * 70)
          println(s"  Presentation Report: $presentationName")
          println("═" * 70)
          println()

          // AC-4: Session Information section
          println("Session Information:")
          println(s"  Started:        ${formatTimestamp(startTime)}")
          println(s"  Duration:       ${calculateDuration(startTime, endTime)}")
          println(s"  Theme:          $theme")
          println(s"  Total Slides:   $totalSlides")
          val uniqueSlides = slides.map(_.hcursor.get[Int]("slideIndex").getOrElse(0)).toSet.size
          println(s"  Slides Viewed:  $uniqueSlides/$totalSlides (${if totalSlides > 0 then (uniqueSlides * 100 / totalSlides) else 0}%)")
          println()

          // AC-5: Slide Timing Summary (Unicode box-drawing table)
          if (slides.nonEmpty) {
            println("Slide Timing Summary:")
            println("  ┌─────┬──────────────────────────────────────┬──────────┐")
            println("  │ No. │ Title                                │ Duration │")
            println("  ├─────┼──────────────────────────────────────┼──────────┤")

            slides.foreach { slideJson =>
              val cursor = slideJson.hcursor
              val slideIndex = cursor.get[Int]("slideIndex").getOrElse(0)
              val slideNumber = slideIndex + 1
              val duration = cursor.get[Long]("elapsedSeconds").getOrElse(0L)
              val title = s"Slide $slideNumber" // Title extraction would require HTML parsing

              println(f"  │ ${slideNumber}%3d │ ${title.take(36).padTo(36, ' ')} │ ${formatDuration(duration)}%8s │")
            }

            println("  └─────┴──────────────────────────────────────┴──────────┘")
            println()
          }

          // AC-6: Top 5 Longest Slides
          if (slides.nonEmpty) {
            val sortedSlides = slides.sortBy(_.hcursor.get[Long]("elapsedSeconds").getOrElse(0L))(Ordering[Long].reverse)
            val top5 = sortedSlides.take(5)

            println(s"Top ${top5.size} Longest Slides:")
            top5.zipWithIndex.foreach { case (slideJson, idx) =>
              val cursor = slideJson.hcursor
              val slideIndex = cursor.get[Int]("slideIndex").getOrElse(0)
              val slideNumber = slideIndex + 1
              val duration = cursor.get[Long]("elapsedSeconds").getOrElse(0L)
              val title = s"Slide $slideNumber"

              println(f"  ${idx + 1}. $title%-30s ${formatDurationShort(duration)}%6s")
            }
            println()
          }

          // AC-7: Events Log
          if (events.nonEmpty) {
            println("Events Log:")
            events.foreach { eventJson =>
              val cursor = eventJson.hcursor
              val timestamp = cursor.get[String]("timestamp").getOrElse("")
              val key = cursor.get[String]("key").getOrElse("?")
              val action = cursor.get[String]("action").getOrElse("unknown")

              println(s"  ${formatTime(timestamp)}  [$key] ${formatAction(action)}")
            }
            println()
          } else {
            println("Events Log:")
            println("  No events recorded during this session.")
            println()
          }

          // AC-8: Navigation Statistics
          val forwardNav = slides.count(_.hcursor.get[String]("navigationMethod").getOrElse("") == "next")
          val backwardNav = slides.count(_.hcursor.get[String]("navigationMethod").getOrElse("") == "previous")
          val gotoNav = slides.count(_.hcursor.get[String]("navigationMethod").getOrElse("") == "goto")
          val revisited = slides.size - uniqueSlides

          println("Navigation Statistics:")
          println(s"  Forward navigations:      $forwardNav")
          println(s"  Backward navigations:     $backwardNav")
          println(s"  Goto jumps:               $gotoNav")
          println(s"  Slides revisited:         $revisited")
          println()

          println("═" * 70)

          ExitCode.Success

        case Left(error) =>
          println(s"✗ Failed to parse log file: ${error.getMessage}")
          ExitCode.Error
      }
    }.handleErrorWith { error =>
      IO.println(s"✗ Failed to read log file: ${error.getMessage}").as(ExitCode.Error)
    }

  /**
   * Format ISO timestamp to readable format
   */
  private def formatTimestamp(timestamp: String): String =
    try {
      import java.time.Instant
      import java.time.format.DateTimeFormatter
      import java.time.ZoneId
      val instant = Instant.parse(timestamp)
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
      formatter.format(instant)
    } catch {
      case _: Exception => timestamp
    }

  /**
   * Format time only (HH:mm:ss)
   */
  private def formatTime(timestamp: String): String =
    try {
      import java.time.Instant
      import java.time.format.DateTimeFormatter
      import java.time.ZoneId
      val instant = Instant.parse(timestamp)
      val formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
      formatter.format(instant)
    } catch {
      case _: Exception => timestamp
    }

  /**
   * Calculate duration between start and end time
   */
  private def calculateDuration(startTime: String, endTime: Option[String]): String =
    try {
      import java.time.Instant
      import java.time.Duration
      val start = Instant.parse(startTime)
      val end = endTime.map(Instant.parse).getOrElse(Instant.now())
      val duration = Duration.between(start, end)

      val hours = duration.toHours
      val minutes = (duration.toMinutes % 60)
      val seconds = (duration.getSeconds % 60)

      f"$hours%02d:$minutes%02d:$seconds%02d${if (endTime.isEmpty) " (IN PROGRESS)" else ""}"
    } catch {
      case _: Exception => "Unknown"
    }

  /**
   * Format duration in seconds to hh:mm:ss
   */
  private def formatDuration(seconds: Long): String =
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    f"$hours%02d:$minutes%02d:$secs%02d"

  /**
   * Format duration in seconds to mm:ss (for top 5)
   */
  private def formatDurationShort(seconds: Long): String =
    val minutes = seconds / 60
    val secs = seconds % 60
    f"$minutes%02d:$secs%02d"

  /**
   * Format action for display
   */
  private def formatAction(action: String): String =
    action.replace("_", " ").split(" ").map(_.capitalize).mkString(" ")

  /**
   * Smart default command: Report → Render if needed → Display (v3.0.0).
   */
  def smartDefaultCommand(deckName: String, cliArgs: CLIArguments, config: MergedConfiguration, globalConfig: Option[GlobalConfig]): IO[ExitCode] =
    for {
      currentDir <- IO.blocking(Paths.get(".").toAbsolutePath.normalize())
      outputDir = PathResolver.determineOutputDir(deckName)
      logFile = outputDir.resolve(s"${outputDir.getFileName}.log")

      // Step 1: Check for log file and show report if exists
      hasLog <- IO.blocking(Files.exists(logFile))
      _ <- if hasLog then
        IO.println("Previous session log found:\n") *>
        reportCommand(deckName) *>
        IO.println("\nPress Enter to continue...") *>
        IO.readLine
      else
        IO.unit

      // Step 2: Check if render needed
      sourceFile <- IO.fromEither(
        PathResolver.findInputFile(deckName, Some(currentDir)).left.map(new RuntimeException(_))
      )
      indexFile = outputDir.resolve("index.html")
      outputExists <- IO.blocking(Files.exists(indexFile))

      needsRender <- if !outputExists then
        IO.pure(true)
      else
        IO.blocking {
          val sourceTime = Files.getLastModifiedTime(sourceFile)
          val outputTime = Files.getLastModifiedTime(indexFile)
          sourceTime.compareTo(outputTime) > 0
        }

      // Step 3: Render if needed
      _ <- if needsRender then
        IO.println(s"Rendering presentation: $deckName\n") *>
        renderPresentation(cliArgs, config, globalConfig)
      else
        IO.println(s"Presentation up-to-date: $deckName")

      // Step 4: Display
      exitCode <- displayCommand(deckName)
    } yield exitCode

  /**
   * Parse args and run program.
   */
  override def run(args: List[String]): IO[ExitCode] =
    // Check for global help/version flags first (before requiring command)
    if args.contains("--help") || args.contains("-h") then
      IO.println("""MDSlides v0.2.0 - Professional presentation tool from Markdown

Usage: mdslides <command> [options]

Commands:
  render DECK_NAME [options]    Convert markdown to HTML presentation
  display DECK_NAME             Open presentation in browser
  report DECK_NAME              Show session analytics and timing
  config                        Display configuration information
  DECK_NAME                     Smart default (report → render → display)

Render Options:
  -i, --input FILE              Input markdown file (explicit form)
  -o, --output DIR              Output directory (explicit form)
  --theme THEME                 Theme: light, dark, or custom (default: light)
  --no-copy-images              Don't copy image assets
  --skip-accessibility          Skip WCAG 2.1 AA validation
  --accessibility-report FILE   Write accessibility report to JSON file
  --break-screen IMAGE          Image path for break screen

Examples:
  mdslides render my-presentation
  mdslides render my-presentation --theme dark
  mdslides render -i deck.md -o output/ --theme light
  mdslides display my-presentation
  mdslides report my-presentation
  mdslides config

For more information, visit: https://github.com/tjm/mdslides""").as(ExitCode.Success)
    else if args.contains("--version") || args.contains("-v") then
      IO.println("mdslides v0.2.0").as(ExitCode.Success)
    else if args.isEmpty then
      IO.println("Usage: mdslides <command> [options]\n\nCommands:\n  render DECK_NAME [options]    Convert markdown to HTML\n  display DECK_NAME            Open presentation in browser\n  report DECK_NAME             Show session analytics\n  config                       Show configuration\n  DECK_NAME                    Smart default (report → render → display)\n\nUse 'mdslides --help' for more information").as(ExitCode.Error)
    else if args(0) == "config" then
      showConfig()
    else if args(0) == "display" && args.length >= 2 then
      // Display command: java -jar mdslides.jar display DECK_NAME
      displayCommand(args(1))
    else if args(0) == "report" && args.length >= 2 then
      // Report command: java -jar mdslides.jar report DECK_NAME
      reportCommand(args(1))
    else if args(0) == "render" then
      // Render command: java -jar mdslides.jar render DECK_NAME [options]
      CLIArguments.parse(args.toArray) match
        case Right(cliArgs) =>
          for {
            currentDir <- IO.blocking(Paths.get(".").toAbsolutePath.normalize())
            configAndGlobal <- loadConfiguration(cliArgs, currentDir)
            (config, globalConfig) = configAndGlobal
            exitCode <- renderPresentation(cliArgs, config, globalConfig).handleErrorWith { error =>
              IO.println(s"Error: ${error.getMessage}").as(ExitCode.Error)
            }
          } yield exitCode
        case Left(errorMessage) =>
          IO.println(errorMessage).as(ExitCode.Error)
    else
      // Smart default: java -jar mdslides.jar DECK_NAME
      val deckName = args(0)
      // Create minimal CLIArguments for smart default
      val cliArgs = CLIArguments(
        deckName = Some(deckName),
        inputFile = None,
        outputDir = None,
        themeName = "light",
        copyImages = true,
        skipAccessibility = false,
        accessibilityReportPath = None,
        breakScreen = None
      )
      for {
        currentDir <- IO.blocking(Paths.get(".").toAbsolutePath.normalize())
        configAndGlobal <- loadConfiguration(cliArgs, currentDir)
        (config, globalConfig) = configAndGlobal
        exitCode <- smartDefaultCommand(deckName, cliArgs, config, globalConfig).handleErrorWith { error =>
          IO.println(s"Error: ${error.getMessage}").as(ExitCode.Error)
        }
      } yield exitCode

end Main
