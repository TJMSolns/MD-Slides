package com.tjmsolutions.mdslides.infrastructure.validation

import com.tjmsolutions.mdslides.domain.{BackgroundConfig, Slide, SlideDeck, ValidationError}
import com.tjmsolutions.mdslides.infrastructure.assets.ImageAssetCopier
import com.tjmsolutions.mdslides.infrastructure.parser.{FlexmarkAdapter, MarkdownParser}
import cats.data.NonEmptyList
import java.nio.file.{Files, Path}

/**
 * The outcome of checking a deck before anything is rendered or written.
 *
 * @param slideCount Slides in the source, including any that failed to parse
 * @param errors     Blocking problems, in slide order
 * @param warnings   Density warnings (the render continues)
 * @param deck       The parsed deck, when every slide parsed
 */
final case class DeckCheckResult(slideCount: Int, errors: List[String], warnings: List[String], deck: Option[SlideDeck]):
  def passed: Boolean = errors.isEmpty

/**
 * Every check a deck must pass, run before anything is written, with every problem reported together
 * (site WQ-077: the docs promise "every problem in one pass").
 *
 * Covers:
 * - parse errors on every slide (unknown templates, column delimiters, images without alt text);
 * - slot and density validation on every slide that parsed;
 * - local image files that don't exist, when `imageSourceDir` is given (it is omitted when copying is off).
 */
object DeckCheck:

  def run(markdown: String, imageSourceDir: Option[Path]): DeckCheckResult =
    val (parseErrors, slides, slideCount) = MarkdownParser.parseCollecting(markdown)

    val (validationErrors, warnings) = NonEmptyList.fromList(slides) match
      case None => (Nil, Nil)
      case Some(parsed) =>
        SlideDeck.validated(parsed) match
          case Right(_) => (Nil, Nil)
          case Left(issues) =>
            val (warns, errs) = issues.toList.partition {
              case _: ValidationError.DensityWarning => true
              case _ => false
            }
            (errs.map(_.displayMessage), warns.map(_.displayMessage))

    val imageErrors = imageSourceDir.toList.flatMap(dir => missingImages(slides, dir))

    val errors = sortBySlide(parseErrors ++ validationErrors ++ imageErrors)
    val deck = if parseErrors.isEmpty then NonEmptyList.fromList(slides).map(SlideDeck(_)) else None
    DeckCheckResult(slideCount, errors, warnings, deck)

  /** Local images (content and per-slide backgrounds) referenced by a deck, without duplicates. */
  def localImageUrls(deck: SlideDeck): List[String] =
    deck.slides.toList.flatMap(slideImages).filter(ImageAssetCopier.isLocalPath).distinct

  private def slideImages(slide: Slide): List[String] =
    val content = slide.slots.values.toList.flatMap(slot => FlexmarkAdapter.parseInlineFormatting(slot).contentImages.map(_.url))
    val background = slide.backgroundImage match
      case Some(bg: String) => List(bg)
      case Some(bg: BackgroundConfig) => List(bg.imagePath)
      case None => Nil
    content ++ background

  private def missingImages(slides: List[Slide], sourceDir: Path): List[String] =
    slides.flatMap { slide =>
      slideImages(slide).filter(ImageAssetCopier.isLocalPath).distinct.flatMap { url =>
        ImageAssetCopier.resolvePath(url, sourceDir) match
          case Right(path) if Files.exists(path) => None
          case Right(path) => Some(s"Slide ${slide.id.toInt}: image file '$url' does not exist (looked for $path)")
          case Left(error) => Some(s"Slide ${slide.id.toInt}: $error")
      }
    }

  /** Keeps messages in slide order; messages without a slide number keep their place at the front. */
  private def sortBySlide(messages: List[String]): List[String] =
    val SlideNumber = """^Slide (\d+)\b.*""".r
    messages.zipWithIndex.sortBy { case (message, index) =>
      message.linesIterator.nextOption() match
        case Some(SlideNumber(n)) => (n.toInt, index)
        case _ => (0, index)
    }.map(_._1).flatMap(_.split("\n").toList)

end DeckCheck
