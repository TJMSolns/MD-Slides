package com.tjmsolutions.mdslides.mcp.tools

import cats.effect.IO
import com.tjmsolutions.mdslides.infrastructure.validation.DeckCheck
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

          // Same checks as the CLI render, every problem reported together (site WQ-077)
          val check = DeckCheck.run(markdown, Option(inputPath.toAbsolutePath.getParent))
          Right(ValidationResult(
            valid = check.passed,
            slideCount = check.slideCount,
            errors = check.errors,
            warnings = check.warnings
          ))
        catch case e: Exception =>
          Left(s"Validation failed: ${e.getMessage}")
      }
