package com.tjmsolutions.mdslides.domain
package generators

import org.scalacheck.Gen

/**
 * ScalaCheck generators for domain entities.
 *
 * Generators produce valid domain data (not random primitives).
 * Include edge cases explicitly via Gen.frequency.
 *
 * Related Governance:
 * - POL-004: Property-Based Testing Requirements
 * - ADR-009: Property-Based Testing Strategy
 */
object DomainGenerators:

  /**
   * Generate valid SlideId (1-200).
   */
  val slideIdGen: Gen[SlideId] =
    Gen.choose(1, 200).map(SlideId.unsafe)

  /**
   * Generate template name.
   * Currently only "title" and "content" templates exist.
   */
  val templateNameGen: Gen[String] =
    Gen.oneOf("title", "content")

  /**
   * Generate short text (0-80 chars).
   * Includes edge cases: empty, boundary values.
   */
  val shortTextGen: Gen[String] =
    Gen.frequency(
      (80, Gen.alphaNumStr.map(_.take(50))),     // 80% normal cases
      (10, Gen.const("")),                        // 10% empty
      (10, Gen.alphaNumStr.map(_.take(80)))      // 10% at boundary
    )

  /**
   * Generate medium text (0-150 chars).
   */
  val mediumTextGen: Gen[String] =
    Gen.frequency(
      (80, Gen.alphaNumStr.map(_.take(100))),
      (10, Gen.const("")),
      (10, Gen.alphaNumStr.map(_.take(150)))
    )

  /**
   * Generate text with specific line count.
   *
   * @param lines Number of lines to generate
   */
  def textWithLines(lines: Int): Gen[String] =
    Gen.listOfN(lines, Gen.alphaNumStr.suchThat(_.nonEmpty).map(_.take(40)))
      .map(_.mkString("\n"))

  /**
   * Generate title slot content (max 2 lines).
   */
  val validTitleGen: Gen[String] =
    Gen.frequency(
      (50, Gen.alphaNumStr.map(_.take(40))),              // Single line
      (30, textWithLines(2)),                              // 2 lines (boundary)
      (10, Gen.const("Introduction to **Markdown**")),    // With formatting
      (10, Gen.const("Math Symbols: ∑ π ∫"))             // Unicode
    )

  /**
   * Generate INVALID title slot content (>2 lines).
   */
  val invalidTitleGen: Gen[String] =
    Gen.frequency(
      (50, textWithLines(3)),    // 3 lines
      (30, textWithLines(5)),    // 5 lines
      (20, textWithLines(10))    // 10 lines
    )

  /**
   * Generate author slot content (max 80 chars).
   */
  val validAuthorGen: Gen[String] =
    Gen.frequency(
      (70, Gen.alphaNumStr.map(_.take(50))),     // Normal length
      (20, Gen.alphaNumStr.map(_.take(80))),     // Boundary
      (10, Gen.const(""))                         // Empty (optional slot)
    )

  /**
   * Generate INVALID author slot content (>80 chars).
   */
  val invalidAuthorGen: Gen[String] =
    Gen.alphaNumStr.map(_.take(120))

  /**
   * Generate valid title slide slots.
   */
  val validTitleSlotsGen: Gen[Map[String, String]] =
    for
      title <- validTitleGen
      subtitle <- Gen.option(validTitleGen)
      author <- Gen.option(validAuthorGen)
    yield Map(
      "title" -> title
    ) ++ subtitle.map("subtitle" -> _).toMap
      ++ author.filter(_.nonEmpty).map("author" -> _).toMap

  /**
   * Generate INVALID title slide slots (missing required title).
   */
  val missingTitleSlotsGen: Gen[Map[String, String]] =
    for
      subtitle <- Gen.option(validTitleGen)
      author <- Gen.option(validAuthorGen)
    yield subtitle.map("subtitle" -> _).toMap
      ++ author.filter(_.nonEmpty).map("author" -> _).toMap

  /**
   * Generate INVALID title slide slots (title exceeds max lines).
   */
  val invalidTitleSlotsGen: Gen[Map[String, String]] =
    for
      title <- invalidTitleGen
      subtitle <- Gen.option(validTitleGen)
      author <- Gen.option(validAuthorGen)
    yield Map(
      "title" -> title
    ) ++ subtitle.map("subtitle" -> _).toMap
      ++ author.filter(_.nonEmpty).map("author" -> _).toMap

  /**
   * Generate valid Slide for title template.
   */
  val validTitleSlideGen: Gen[Slide] =
    for
      id <- slideIdGen
      slots <- validTitleSlotsGen
    yield Slide(id, "title", slots)

  /**
   * Generate Slide with structure errors (missing required slots).
   */
  val slideWithStructureErrorGen: Gen[Slide] =
    for
      id <- slideIdGen
      slots <- missingTitleSlotsGen
    yield Slide(id, "title", slots)

  /**
   * Generate Slide with content errors (constraint violations).
   */
  val slideWithContentErrorGen: Gen[Slide] =
    for
      id <- slideIdGen
      slots <- invalidTitleSlotsGen
    yield Slide(id, "title", slots)

  // ========================================
  // Content Slide Generators (US-002)
  // ========================================

  /**
   * Generate valid heading slot content (max 80 chars, 1 line).
   */
  val validHeadingGen: Gen[String] =
    Gen.frequency(
      (70, Gen.alphaNumStr.map(_.take(50))),     // Normal length
      (20, Gen.alphaNumStr.map(_.take(80))),     // Boundary
      (10, Gen.const("Key Principles"))          // Typical heading
    )

  /**
   * Generate INVALID heading slot content (>80 chars).
   */
  val invalidHeadingGen: Gen[String] =
    Gen.alphaNumStr.map(_.take(120))

  /**
   * Generate valid body slot content (max 12 lines, max 150 words).
   */
  val validBodyGen: Gen[String] =
    Gen.frequency(
      (40, textWithLines(5)),                     // Normal (5 lines)
      (30, textWithLines(8)),                     // Medium (8 lines)
      (20, textWithLines(12)),                    // Boundary (12 lines)
      (10, Gen.listOfN(100, Gen.alphaNumStr.suchThat(_.nonEmpty)).map(_.mkString(" ")))  // 100 words
    )

  /**
   * Generate INVALID body slot content (>12 lines).
   */
  val invalidBodyLinesGen: Gen[String] =
    Gen.frequency(
      (50, textWithLines(13)),    // 13 lines
      (30, textWithLines(15)),    // 15 lines
      (20, textWithLines(20))     // 20 lines
    )

  /**
   * Generate INVALID body slot content (>150 words).
   */
  val invalidBodyWordsGen: Gen[String] =
    Gen.choose(160, 200).flatMap { wordCount =>
      Gen.listOfN(wordCount, Gen.alphaNumStr.suchThat(_.nonEmpty)).map(_.mkString(" "))
    }

  /**
   * Generate valid content slide slots.
   */
  val validContentSlotsGen: Gen[Map[String, String]] =
    for
      heading <- validHeadingGen
      body <- validBodyGen
    yield Map(
      "heading" -> heading,
      "body" -> body
    )

  /**
   * Generate INVALID content slide slots (missing required heading).
   */
  val missingHeadingSlotsGen: Gen[Map[String, String]] =
    for
      body <- validBodyGen
    yield Map("body" -> body)

  /**
   * Generate INVALID content slide slots (missing required body).
   */
  val missingBodySlotsGen: Gen[Map[String, String]] =
    for
      heading <- validHeadingGen
    yield Map("heading" -> heading)

  /**
   * Generate INVALID content slide slots (body exceeds max lines).
   */
  val invalidContentSlotsLinesGen: Gen[Map[String, String]] =
    for
      heading <- validHeadingGen
      body <- invalidBodyLinesGen
    yield Map(
      "heading" -> heading,
      "body" -> body
    )

  /**
   * Generate INVALID content slide slots (body exceeds max words).
   */
  val invalidContentSlotsWordsGen: Gen[Map[String, String]] =
    for
      heading <- validHeadingGen
      body <- invalidBodyWordsGen
    yield Map(
      "heading" -> heading,
      "body" -> body
    )

  /**
   * Generate valid Slide for content template.
   */
  val validContentSlideGen: Gen[Slide] =
    for
      id <- slideIdGen
      slots <- validContentSlotsGen
    yield Slide(id, "content", slots)

  /**
   * Generate Slide with missing heading (structure error).
   */
  val contentSlideWithMissingHeadingGen: Gen[Slide] =
    for
      id <- slideIdGen
      slots <- missingHeadingSlotsGen
    yield Slide(id, "content", slots)

  /**
   * Generate Slide with missing body (structure error).
   */
  val contentSlideWithMissingBodyGen: Gen[Slide] =
    for
      id <- slideIdGen
      slots <- missingBodySlotsGen
    yield Slide(id, "content", slots)

  /**
   * Generate Slide with body exceeding max lines (content error).
   */
  val contentSlideWithExcessiveLinesGen: Gen[Slide] =
    for
      id <- slideIdGen
      slots <- invalidContentSlotsLinesGen
    yield Slide(id, "content", slots)

  /**
   * Generate Slide with body exceeding max words (content error).
   */
  val contentSlideWithExcessiveWordsGen: Gen[Slide] =
    for
      id <- slideIdGen
      slots <- invalidContentSlotsWordsGen
    yield Slide(id, "content", slots)

  // ========================================
  // SlideDeck Generators
  // ========================================

  /**
   * Generate a valid SlideDeck (1-200 slides, all valid).
   */
  val validSlideDeckGen: Gen[SlideDeck] =
    for
      count <- Gen.choose(1, 20)  // Use smaller counts for faster tests
      slideType <- Gen.oneOf("title", "content", "mixed")
      slides <- slideType match
        case "title" => Gen.listOfN(count, validTitleSlideGen)
        case "content" => Gen.listOfN(count, validContentSlideGen)
        case "mixed" => Gen.listOfN(count, Gen.oneOf(validTitleSlideGen, validContentSlideGen))
    yield
      // Re-index slides to ensure unique sequential IDs
      val reindexedSlides = slides.zipWithIndex.map { case (slide, index) =>
        slide.copy(id = SlideId.unsafe(index + 1))
      }
      SlideDeck.fromList(reindexedSlides).toOption.get

  /**
   * Generate a SlideDeck exceeding max 200 slides.
   */
  val slideDeckExceedingMaxSizeGen: Gen[SlideDeck] =
    for
      count <- Gen.choose(201, 250)
      slides <- Gen.listOfN(count, validContentSlideGen)
    yield
      val reindexedSlides = slides.zipWithIndex.map { case (slide, index) =>
        slide.copy(id = SlideId.unsafe(index + 1))
      }
      SlideDeck.fromList(reindexedSlides).toOption.get

  /**
   * Generate a SlideDeck with duplicate slide IDs.
   */
  val slideDeckWithDuplicateIdsGen: Gen[SlideDeck] =
    for
      count <- Gen.choose(3, 10)
      slides <- Gen.listOfN(count, validContentSlideGen)
      duplicateIndex <- Gen.choose(1, count - 1)
    yield
      // Create slides with a duplicate ID
      val slidesWithDuplicate = slides.zipWithIndex.map { case (slide, index) =>
        if index == duplicateIndex then
          slide.copy(id = SlideId.unsafe(1))  // Duplicate of first slide
        else
          slide.copy(id = SlideId.unsafe(index + 1))
      }
      SlideDeck.fromList(slidesWithDuplicate).toOption.get

  /**
   * Generate a SlideDeck with invalid slide content.
   */
  val slideDeckWithInvalidSlidesGen: Gen[SlideDeck] =
    for
      count <- Gen.choose(2, 5)
      validSlides <- Gen.listOfN(count - 1, validContentSlideGen)
      invalidSlide <- Gen.oneOf(
        contentSlideWithMissingHeadingGen,
        contentSlideWithMissingBodyGen,
        contentSlideWithExcessiveLinesGen
      )
    yield
      val allSlides = (validSlides :+ invalidSlide).zipWithIndex.map { case (slide, index) =>
        slide.copy(id = SlideId.unsafe(index + 1))
      }
      SlideDeck.fromList(allSlides).toOption.get

end DomainGenerators
