package com.tjmsolutions.mdslides.mcp.tools

import cats.effect.IO
import com.tjmsolutions.mdslides.domain.{DiagramElement, SlideBackground, SlideDeck}
import com.tjmsolutions.mdslides.infrastructure.parser.{FlexmarkAdapter, MarkdownParser}
import com.tjmsolutions.mdslides.mcp.model.DeckInfo
import io.circe.Json

import java.nio.file.{Files, Paths}

/** Handles the get_deck_info MCP tool call. */
object GetDeckInfoTool:

  case class Params(inputPath: String)

  def parseParams(args: Json): Either[String, Params] =
    args.asObject match
      case None => Left("arguments must be an object")
      case Some(obj) =>
        obj("input_path").flatMap(_.asString)
          .toRight("input_path is required")
          .map(Params.apply)

  def execute(params: Params): IO[Either[String, DeckInfo]] =
    val inputPath = Paths.get(params.inputPath)
    if !Files.exists(inputPath) then
      IO.pure(Left(s"Input file not found: ${params.inputPath}"))
    else
      IO.blocking {
        try
          val markdown = Files.readString(inputPath)

          MarkdownParser.parse(markdown) match
            case Left(err) => Left(s"Parse failed: $err")
            case Right(deck) => Right(deckInfo(deck))
        catch case e: Exception =>
          Left(s"Failed to inspect deck: ${e.getMessage}")
      }

  private def deckInfo(deck: SlideDeck): DeckInfo =
    val formattedSlots = deck.slides.toList.flatMap(_.slots.values.map(FlexmarkAdapter.parseInlineFormatting))

    val contentImages = formattedSlots.flatMap(_.contentImages.map(_.url))
    val backgroundImages = deck.slides.toList.flatMap(_.backgroundImage.map(SlideBackground.getImagePath))
    val hasMermaidDiagrams = formattedSlots.exists(_.content.exists {
      case DiagramElement(_) => true
      case _ => false
    })

    DeckInfo(
      slideCount = deck.slideCount,
      templatesUsed = deck.slides.toList.map(_.templateName).distinct.sorted,
      imagesReferenced = (contentImages ++ backgroundImages).distinct,
      hasMermaidDiagrams = hasMermaidDiagrams
    )
