package com.tjmsolutions.mdslides.domain

/**
 * Header/Footer Aggregate
 *
 * Manages header and footer templates with placeholder resolution.
 *
 * Supported Placeholders:
 * - Static: {title}, {author}, {date} (from metadata)
 * - Dynamic: {current-slide}, {total-slides}, {elapsed-time} (runtime values)
 *
 * Invariants:
 * - Slide numbers are 1-indexed for user display (internal 0-indexed)
 * - Missing metadata resolves to empty string
 * - Unknown placeholders are preserved as-is
 * - Header/footer can be independently enabled/disabled
 */
case class HeaderFooter(
  headerTemplate: Option[String],
  footerTemplate: Option[String],
  metadata: Map[String, String]
):

  /**
   * Render header with resolved placeholders.
   *
   * @param currentSlide 0-indexed current slide
   * @param totalSlides Total number of slides
   * @param elapsedTime Formatted elapsed time (HH:MM:SS)
   * @return Rendered header (None if no template)
   */
  def renderHeader(currentSlide: Int, totalSlides: Int, elapsedTime: String): Option[String] =
    headerTemplate.map { template =>
      HeaderFooter.resolvePlaceholders(template, metadata, currentSlide, totalSlides, elapsedTime)
    }

  /**
   * Render footer with resolved placeholders.
   *
   * @param currentSlide 0-indexed current slide
   * @param totalSlides Total number of slides
   * @param elapsedTime Formatted elapsed time (HH:MM:SS)
   * @return Rendered footer (None if no template)
   */
  def renderFooter(currentSlide: Int, totalSlides: Int, elapsedTime: String): Option[String] =
    footerTemplate.map { template =>
      HeaderFooter.resolvePlaceholders(template, metadata, currentSlide, totalSlides, elapsedTime)
    }

object HeaderFooter:

  /**
   * Resolve all placeholders in a template.
   *
   * AC-2, AC-3, AC-5, AC-6, AC-7 (Header/Footer): Placeholder resolution
   * - {{timer}}: Elapsed presentation time (hh:mm:ss)
   * - {{pageNumber}}: Current slide number (1-indexed)
   * - {{totalPages}}: Total number of slides
   * - {{title}}: Presentation title from metadata
   *
   * @param template Template string with placeholders
   * @param metadata Static metadata (title, author, date, etc.)
   * @param currentSlide 0-indexed current slide
   * @param totalSlides Total number of slides
   * @param elapsedTime Formatted elapsed time (HH:MM:SS)
   * @return Template with resolved placeholders
   */
  def resolvePlaceholders(
    template: String,
    metadata: Map[String, String],
    currentSlide: Int,
    totalSlides: Int,
    elapsedTime: String
  ): String =
    val replacements = List(
      ("{{title}}", metadata.getOrElse("title", "")),           // AC-7
      ("{{pageNumber}}", (currentSlide + 1).toString),          // AC-5: 1-indexed
      ("{{totalPages}}", totalSlides.toString),                 // AC-6: Static
      ("{{timer}}", elapsedTime)                                // AC-3, AC-4: Dynamic
    )

    replacements.foldLeft(template) { case (acc, (placeholder, value)) =>
      acc.replace(placeholder, value)
    }

  /**
   * Create HeaderFooter from config and metadata.
   *
   * @param config Configuration map (header, footer keys)
   * @param metadata Static metadata (title, author, date, etc.)
   * @return HeaderFooter aggregate
   */
  def create(
    config: Map[String, String],
    metadata: Map[String, String]
  ): Either[HeaderFooterError, HeaderFooter] =
    Right(HeaderFooter(
      headerTemplate = config.get("header"),
      footerTemplate = config.get("footer"),
      metadata = metadata
    ))
