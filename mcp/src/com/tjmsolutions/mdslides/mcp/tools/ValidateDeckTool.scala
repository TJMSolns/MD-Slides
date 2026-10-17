package com.tjmsolutions.mdslides.mcp.tools

import cats.effect.IO
import com.tjmsolutions.mdslides.domain.{SlideDeck, ValidationError}
import com.tjmsolutions.mdslides.infrastructure.parser.MarkdownParser
import com.tjmsolutions.mdslides.mcp.model.ValidationResult
import io.circe.Json

import java.nio.file.{Files, Paths}

/** Handles the validate_deck MCP tool call. */
object ValidateDeckTool:

  case class Params(inputPath: String)

  def parseParams(args: Json): Either[String, Params] =
    args.asObject match
      case None => Left("arguments must be an object")
      case Some(obj) =>
        obj("input_path").flatMap(_.asString)
          .toRight("input_path is required")
          .map(Params.apply)

  def execute(params: Params): IO[Either[String, ValidationResult]] =
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
              val (warnings, errors) = SlideDeck.validated(deck.slides) match
                case Right(_) => (Nil, Nil)
                case Left(issues) =>
                  val (warns, errs) = issues.toList.partition {
                    case _: ValidationError.DensityWarning => true
                    case _ => false
                  }
                  (warns.map(_.displayMessage), errs.map(_.displayMessage))

              Right(ValidationResult(
                valid = errors.isEmpty,
                slideCount = deck.slideCount,
                errors = errors,
                warnings = warnings
              ))
        catch case e: Exception =>
          Left(s"Validation failed: ${e.getMessage}")
      }
