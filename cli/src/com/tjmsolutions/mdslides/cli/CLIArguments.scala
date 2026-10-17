package com.tjmsolutions.mdslides.cli

import java.nio.file.Path

/**
 * Parsed command-line arguments for MDSlides render command.
 *
 * Supports two modes:
 * 1. Simple form: `mdslides render DECK_NAME [options]`
 *    - deckName is present, inputFile/outputDir are None
 *    - Input inferred: DECK_NAME.md or DECK_NAME.markdown
 *    - Output inferred: DECK_NAME/ directory
 *
 * 2. Explicit form: `mdslides render -i INPUT -o OUTPUT [options]`
 *    - inputFile and outputDir are present, deckName is None
 *    - Full control over input/output paths
 *
 * Modes are mutually exclusive (cannot mix).
 *
 * @param deckName Deck name stem (simple form only)
 * @param inputFile Explicit input file path (explicit form only)
 * @param outputDir Explicit output directory (explicit form only)
 * @param themeName Theme to apply (default: "light")
 * @param copyImages Whether to copy image assets (default: true)
 * @param skipAccessibility Skip WCAG 2.1 AA accessibility validation (default: false)
 * @param accessibilityReportPath Optional path to write JSON accessibility report
 * @param breakScreen Optional path to break screen image (v3.0.0)
 * @param watch Live-reload mode: re-render on file change + meta-refresh in browser (US-018)
 */
case class CLIArguments(
  deckName: Option[String],
  inputFile: Option[Path],
  outputDir: Option[Path],
  themeName: String = "light",
  copyImages: Boolean = true,
  skipAccessibility: Boolean = false,
  accessibilityReportPath: Option[Path] = None,
  breakScreen: Option[String] = None,
  watch: Boolean = false
)

object CLIArguments:

  /**
   * Parse command-line arguments into CLIArguments.
   *
   * @param args Command-line argument array
   * @return Right(CLIArguments) on success, Left(error message) on failure
   */
  def parse(args: Array[String]): Either[String, CLIArguments] =
    if args.isEmpty || args(0) != "render" then
      return Left("Usage: mdslides render DECK_NAME [options]\n       mdslides render -i INPUT -o OUTPUT [options]")

    // Skip "render" command
    val renderArgs = args.drop(1)

    if renderArgs.isEmpty then
      return Left("Missing required argument: DECK_NAME or -i/-o\n\nUsage:\n  mdslides render DECK_NAME\n  mdslides render -i INPUT -o OUTPUT")

    // Parse arguments
    var deckName: Option[String] = None
    var inputFile: Option[Path] = None
    var outputDir: Option[Path] = None
    var themeName: String = "light"
    var copyImages: Boolean = true
    var skipAccessibility: Boolean = false
    var accessibilityReportPath: Option[Path] = None
    var breakScreen: Option[String] = None
    var watch: Boolean = false

    var i = 0
    while i < renderArgs.length do
      renderArgs(i) match
        case "-i" | "--input" =>
          if i + 1 >= renderArgs.length then
            return Left("Missing value for -i/--input")
          inputFile = Some(Path.of(renderArgs(i + 1)))
          i += 2

        case "-o" | "--output" =>
          if i + 1 >= renderArgs.length then
            return Left("Missing value for -o/--output")
          outputDir = Some(Path.of(renderArgs(i + 1)))
          i += 2

        case "--theme" =>
          if i + 1 >= renderArgs.length then
            return Left("Missing value for --theme")
          themeName = renderArgs(i + 1)
          i += 2

        case "--no-copy-images" =>
          copyImages = false
          i += 1

        case "--skip-accessibility" =>
          skipAccessibility = true
          i += 1

        case "--accessibility-report" =>
          if i + 1 >= renderArgs.length then
            return Left("Missing value for --accessibility-report")
          accessibilityReportPath = Some(Path.of(renderArgs(i + 1)))
          i += 2

        case "--break-screen" =>
          if i + 1 >= renderArgs.length then
            return Left("Missing value for --break-screen")
          breakScreen = Some(renderArgs(i + 1))
          i += 2

        case "--watch" | "-w" =>
          watch = true
          i += 1

        case arg if !arg.startsWith("-") =>
          // Positional argument (deck name in simple form)
          deckName = Some(arg)
          i += 1

        case unknown =>
          return Left(s"Unknown option: $unknown")

    // Validate modes are not mixed
    (deckName, inputFile, outputDir) match
      case (Some(_), Some(_), _) | (Some(_), _, Some(_)) =>
        Left("Cannot mix simple form (DECK_NAME) with explicit form (-i/-o)\n\nUsage:\n  Simple:   mdslides render DECK_NAME\n  Explicit: mdslides render -i INPUT -o OUTPUT")

      case (None, Some(_), None) =>
        Left("Missing -o/--output in explicit form. Both -i and -o are required.")

      case (None, None, Some(_)) =>
        Left("Missing -i/--input in explicit form. Both -i and -o are required.")

      case (None, None, None) =>
        Left("Missing required argument: DECK_NAME or -i/-o")

      case (Some(_), None, None) =>
        // Simple form: OK
        Right(CLIArguments(deckName, None, None, themeName, copyImages, skipAccessibility, accessibilityReportPath, breakScreen, watch))

      case (None, Some(_), Some(_)) =>
        // Explicit form: OK
        Right(CLIArguments(None, inputFile, outputDir, themeName, copyImages, skipAccessibility, accessibilityReportPath, breakScreen, watch))

      case _ =>
        Left("Invalid argument combination")

end CLIArguments
