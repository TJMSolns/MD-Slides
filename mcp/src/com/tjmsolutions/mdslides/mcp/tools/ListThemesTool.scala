package com.tjmsolutions.mdslides.mcp.tools

import cats.effect.IO
import com.tjmsolutions.mdslides.infrastructure.theme.ThemeLoader
import com.tjmsolutions.mdslides.mcp.model.ThemesResult
import io.circe.Json

import java.nio.file.{Files, Paths}

/** Handles the list_themes MCP tool call. */
object ListThemesTool:

  private val builtinThemes = List("corporate", "dark", "light")

  case class Params(themesDir: String = "./themes")

  def parseParams(args: Json): Either[String, Params] =
    args.asObject match
      case None => Left("arguments must be an object")
      case Some(obj) =>
        val themesDir = obj("themes_dir").flatMap(_.asString).getOrElse("./themes")
        Right(Params(themesDir))

  def execute(params: Params): IO[Either[String, ThemesResult]] =
    val themesDir = Paths.get(params.themesDir)
    if !Files.exists(themesDir) then
      IO.pure(Right(ThemesResult(builtinThemes)))
    else
      ThemeLoader.listAvailableThemes(themesDir)
        .attempt
        .map {
          case Right(directoryThemes) =>
            Right(ThemesResult((builtinThemes ++ directoryThemes).distinct.sorted))
          case Left(e) =>
            Left(s"Cannot read themes directory: ${params.themesDir} (${e.getMessage})")
        }
