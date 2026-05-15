package com.tjmsolutions.mdslides.domain

/**
 * Content image - informational images embedded in slide body.
 *
 * Content images appear in markdown via `![alt text](url)`.
 * Alt text is required for accessibility (WCAG AA).
 *
 * Contrast with background images (decorative, theme-level).
 *
 * Related Governance:
 * - US-005: Image Embedding
 * - PDR-008: Image Policy (Background vs Content)
 * - PDR-005: Accessibility Requirements (WCAG AA)
 * - POL-001: Ubiquitous Language Enforcement
 *
 * @param url Image URL (relative path or absolute URL)
 * @param altText Alternative text for screen readers (required, non-empty)
 */
case class ContentImage(
  url: String,
  altText: String
)

object ContentImage:
  /**
   * Validated constructor - ensures alt text is non-empty.
   *
   * @param url Image URL
   * @param altText Alternative text (must be non-empty)
   * @return Either error message or valid ContentImage
   */
  def validated(url: String, altText: String): Either[String, ContentImage] =
    if altText.trim.isEmpty then
      Left(s"Image missing required alt text: $url")
    else
      Right(ContentImage(url, altText.trim))

  /**
   * Unsafe constructor - for testing only.
   *
   * @param url Image URL
   * @param altText Alternative text
   * @return ContentImage (no validation)
   */
  def unsafe(url: String, altText: String): ContentImage =
    ContentImage(url, altText)
