package com.tjmsolutions.mdslides.infrastructure.accessibility

import com.tjmsolutions.mdslides.domain.AccessibilityReport
import io.circe.Json
import java.nio.file.{Files, Path, StandardOpenOption}
import java.time.Instant

/**
 * Writer for accessibility validation reports in JSON format.
 *
 * Output format follows Example Mapping Scenario 16-17:
 * {
 *   "timestamp": "2025-12-27T10:30:00Z",
 *   "deck": "deck.md",
 *   "theme": "light",
 *   "wcag_level": "AA",
 *   "checks": {
 *     "contrast": { ... },
 *     "alt_text": { ... },
 *     "keyboard": { ... }
 *   },
 *   "warnings": [ ... ],
 *   "passes": true
 * }
 *
 * Related Governance:
 * - US-014: Accessibility Validation
 * - Example Mapping Scenarios 16-17
 */
object AccessibilityReportWriter:

  /**
   * Write accessibility report to JSON file.
   *
   * @param report Accessibility validation report
   * @param deckPath Path to source markdown deck
   * @param outputPath Path to write JSON report
   */
  def writeReport(
    report: AccessibilityReport,
    deckPath: Path,
    outputPath: Path
  ): Unit =

    // Build contrast checks JSON
    val contrastChecks = report.contrastChecks.map { case (name, ratio) =>
      name -> Json.obj(
        "ratio" -> Json.fromDouble(ratio.ratio).getOrElse(Json.Null),
        "passes" -> Json.fromBoolean(ratio.passesWCAG_AA(normalText = true))
      )
    }

    // Build warnings JSON
    val warningsJson = report.warnings.map { w =>
      Json.obj(
        "level" -> Json.fromString("WARNING"),
        "category" -> Json.fromString(w.category),
        "message" -> Json.fromString(w.message),
        "wcag_criterion" -> Json.fromString(w.wcagCriterion)
      )
    }

    // Build complete JSON structure
    val json = Json.obj(
      "timestamp" -> Json.fromString(Instant.now().toString),
      "deck" -> Json.fromString(deckPath.getFileName.toString),
      "theme" -> Json.fromString(report.themeName),
      "wcag_level" -> Json.fromString(report.wcagLevel),
      "checks" -> Json.obj(
        "contrast" -> Json.obj(contrastChecks.toSeq*),
        "alt_text" -> Json.obj(
          "images_checked" -> Json.fromInt(report.altTextChecks.imagesChecked),
          "images_missing_alt" -> Json.fromInt(report.altTextChecks.imagesMissingAlt)
        ),
        "keyboard" -> Json.obj(
          "handlers_found" -> Json.fromValues(report.keyboardChecks.handlersFound.map(Json.fromString)),
          "handlers_missing" -> Json.fromValues(report.keyboardChecks.handlersMissing.map(Json.fromString))
        )
      ),
      "warnings" -> Json.fromValues(warningsJson),
      "passes" -> Json.fromBoolean(report.passes)
    )

    // Write to file with pretty formatting (2-space indentation)
    val jsonString = json.spaces2
    Files.writeString(
      outputPath,
      jsonString,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    )

end AccessibilityReportWriter
