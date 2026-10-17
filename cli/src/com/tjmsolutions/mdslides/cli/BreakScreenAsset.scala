package com.tjmsolutions.mdslides.cli

import java.nio.file.{Files, Path, StandardCopyOption}
import scala.util.Try

/** Puts the configured break-screen image where the rendered deck can load it. */
object BreakScreenAsset:

  /**
   * @param configured --break-screen / config `breakScreen` value
   * @param deckDir    folder of the Markdown source (searched first)
   * @param cwd        current folder (searched second)
   * @param outputDir  rendered deck folder; local images are copied to its `images/`
   * @return the path to use in the page, None for the default break screen, Left if a local file is missing
   */
  def place(configured: Option[String], deckDir: Path, cwd: Path, outputDir: Path): Either[String, Option[String]] =
    configured.map(_.trim).filter(_.nonEmpty) match
      case None => Right(None)
      case Some(url) if url.matches("(?i)^(https?:|data:).*") => Right(Some(url))
      case Some(local) =>
        List(deckDir.resolve(local), cwd.resolve(local)).find(Files.isRegularFile(_)) match
          case None => Left(s"Break screen image not found: $local (looked next to the deck and in the current folder)")
          case Some(source) =>
            val name = source.getFileName.toString
            Try {
              val target = outputDir.resolve("images").resolve(name)
              Files.createDirectories(target.getParent)
              Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
              s"images/$name"
            }.toEither.left.map(e => s"Could not copy break screen image $local: ${e.getMessage}").map(Some(_))
