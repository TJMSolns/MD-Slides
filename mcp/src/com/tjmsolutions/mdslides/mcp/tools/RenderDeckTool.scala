package com.tjmsolutions.mdslides.mcp.tools

import cats.effect.IO
import com.tjmsolutions.mdslides.domain.{SlideDeck, ValidationError}
import com.tjmsolutions.mdslides.infrastructure.parser.MarkdownParser
import com.tjmsolutions.mdslides.infrastructure.renderer.HTMLRenderer
import com.tjmsolutions.mdslides.infrastructure.rendering.SpeakerViewRenderer
import com.tjmsolutions.mdslides.infrastructure.theme.ThemeJsonAdapter
import com.tjmsolutions.mdslides.mcp.model.RenderResult
import io.circe.Json

import java.nio.file.{Files, Paths}

/** Handles the render_deck MCP tool call. */
object RenderDeckTool:

  case class Params(
    inputPath: String,
    outputDir: String,
    theme: String = "light",
    noCopyImages: Boolean = false
  )

  def parseParams(args: Json): Either[String, Params] =
    args.asObject match
      case None => Left("arguments must be an object")
      case Some(obj) =>
        val inputPath = obj("input_path").flatMap(_.asString)
          .toRight("input_path is required")
        val outputDir = obj("output_dir").flatMap(_.asString)
          .toRight("output_dir is required")
        val theme = obj("theme").flatMap(_.asString).getOrElse("light")
        val noCopyImages = obj("no_copy_images").flatMap(_.asBoolean).getOrElse(false)
        for
          ip <- inputPath
          od <- outputDir
        yield Params(ip, od, theme, noCopyImages)

  def execute(params: Params): IO[Either[String, RenderResult]] =
    val inputPath = Paths.get(params.inputPath)
    if !Files.exists(inputPath) then
      IO.pure(Left(s"Input file not found: ${params.inputPath}"))
    else
      IO.blocking {
        try
          val markdown = Files.readString(inputPath)

          MarkdownParser.parse(markdown) match
            case Left(err) => Left(s"Parse failed: $err")
            case Right(deck) =>
              val (warnings, blockingErrors) = collectValidationIssues(deck)
              if blockingErrors.nonEmpty then
                Left(s"Validation failed: ${blockingErrors.mkString("; ")}")
              else
                val theme = resolveTheme(params.theme)
                val outputPath = Paths.get(params.outputDir)
                Files.createDirectories(outputPath)

                val html = HTMLRenderer.renderDeck(deck, theme)
                Files.writeString(outputPath.resolve("index.html"), html)

                val speakerHtml = SpeakerViewRenderer.render(deck, params.theme)
                Files.writeString(outputPath.resolve("speaker.html"), speakerHtml)

                Right(RenderResult(
                  success = true,
                  outputDir = outputPath.toAbsolutePath.toString,
                  files = List("index.html", "speaker.html"),
                  slideCount = deck.slideCount,
                  warnings = warnings,
                  errors = Nil
                ))
        catch case e: Exception =>
          Left(s"Render failed: ${e.getMessage}")
      }

  private def collectValidationIssues(deck: SlideDeck): (List[String], List[String]) =
    SlideDeck.validated(deck.slides) match
      case Right(_) => (Nil, Nil)
      case Left(issues) =>
        val (warns, errs) = issues.toList.partition {
          case _: ValidationError.DensityWarning => true
          case _ => false
        }
        (warns.map(_.displayMessage), errs.map(_.displayMessage))

  private def resolveTheme(themeName: String) = themeName match
    case "dark" => ThemeJsonAdapter.dark
    case "corporate" => ThemeJsonAdapter.corporate
    case _ => ThemeJsonAdapter.light
