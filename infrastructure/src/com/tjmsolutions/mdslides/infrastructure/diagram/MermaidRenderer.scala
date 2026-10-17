package com.tjmsolutions.mdslides.infrastructure.diagram

import cats.effect.IO
import java.nio.file.{Files, Path, Paths}
import scala.sys.process.{Process, _}
import scala.util.{Try, Success, Failure}

/**
 * Server-side mermaid diagram renderer using mermaid-cli.
 *
 * Renders mermaid diagrams to SVG at build time for offline support.
 * Requires @mermaid-js/mermaid-cli (mmdc) to be installed globally.
 *
 * Installation: npm install -g @mermaid-js/mermaid-cli
 *
 * Related Governance:
 * - US-022: Mermaid Diagram Support
 * - Offline requirement: Presentations must work without internet
 */
object MermaidRenderer:

  /**
   * Check if mermaid-cli (mmdc) is installed.
   *
   * @return IO[Boolean] true if mmdc is available
   */
  def isMermaidCliInstalled: IO[Boolean] =
    IO {
      Try {
        "mmdc --version".!!
        true
      }.getOrElse(false)
    }

  /**
   * Render mermaid diagram source to SVG.
   *
   * Uses mermaid-cli (mmdc) to render diagram at build time.
   * Creates temporary files for input/output.
   *
   * @param diagramSource Mermaid diagram source code
   * @param diagramType Type of diagram (flowchart, sequence, etc.)
   * @param theme Mermaid theme (default, dark, neutral)
   * @return IO[Either[String, String]] Right(svg) on success, Left(error) on failure
   */
  def renderToSVG(
    diagramSource: String,
    diagramType: String,
    theme: String = "default"
  ): IO[Either[String, String]] =
    for {
      // Check if mmdc is installed
      isInstalled <- isMermaidCliInstalled
      result <- if !isInstalled then
        IO.pure(Left("mermaid-cli (mmdc) not found. Install: npm install -g @mermaid-js/mermaid-cli"))
      else
        renderWithMmdc(diagramSource, theme)
    } yield result

  /**
   * Render diagram using mmdc command.
   *
   * @param diagramSource Mermaid source code
   * @param theme Mermaid theme
   * @return IO[Either[String, String]] SVG content or error
   */
  private def renderWithMmdc(
    diagramSource: String,
    theme: String
  ): IO[Either[String, String]] =
    IO {
      // Create temp directory
      val tempDir = Files.createTempDirectory("mdslides-mermaid")
      val inputFile = tempDir.resolve("diagram.mmd")
      val outputFile = tempDir.resolve("diagram.svg")

      try {
        // Write diagram source to temp file
        Files.writeString(inputFile, diagramSource)

        // Run mmdc command with explicit Chromium path for Puppeteer
        // Try to find Chromium in common locations
        val chromiumPath = List("/usr/bin/chromium", "/usr/bin/chromium-browser", "/usr/bin/google-chrome")
          .find(p => Files.exists(Paths.get(p)))
          .getOrElse("/usr/bin/chromium")

        val command = Process(
          Seq("mmdc", "-i", inputFile.toString, "-o", outputFile.toString, "-t", theme, "-b", "transparent"),
          None,
          "PUPPETEER_EXECUTABLE_PATH" -> chromiumPath
        )
        val exitCode = command.!

        if exitCode == 0 then
          // Read SVG output
          val svg = Files.readString(outputFile)
          Right(svg)
        else
          Left(s"mmdc command failed with exit code $exitCode")
      } catch {
        case e: Exception =>
          Left(s"Failed to render diagram: ${e.getMessage}")
      } finally {
        // Clean up temp files
        Try {
          Files.deleteIfExists(outputFile)
          Files.deleteIfExists(inputFile)
          Files.deleteIfExists(tempDir)
        }
      }
    }

  /**
   * Generate fallback HTML for when mermaid-cli is not available.
   *
   * Shows diagram source in a code block with warning message.
   *
   * @param diagramSource Mermaid source code
   * @return String HTML fallback content
   */
  def generateFallbackHTML(diagramSource: String): String =
    s"""<div class="mermaid-fallback">
       |  <pre style="background: #f5f5f5; padding: 1em; border-radius: 4px; overflow: auto;"><code>$diagramSource</code></pre>
       |  <p style="color: #d32f2f; font-size: 0.9em; margin-top: 0.5em;">
       |    ⚠ Diagram rendering unavailable. Install mermaid-cli: npm install -g @mermaid-js/mermaid-cli
       |  </p>
       |</div>""".stripMargin

end MermaidRenderer
